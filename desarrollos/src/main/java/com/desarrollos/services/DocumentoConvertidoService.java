package com.desarrollos.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.desarrollos.entities.Archivo;
import com.desarrollos.entities.DocumentoConvertido;
import com.desarrollos.entities.NetoGravado;
import com.desarrollos.entities.PercepcionIIBB;
import com.desarrollos.entities.PercepcionIVA;
import com.desarrollos.entities.ProductoConcepto;
import com.desarrollos.entities.Vencimiento;
import com.desarrollos.repositories.DocumentoConvertidoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DocumentoConvertidoService {

    @Autowired
    private DocumentoConvertidoRepository repository;

    @Autowired
    private ClaudeVisionService claudeVisionService;

    @Autowired
    private ArchivoService archivoService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ResultadoConversion convertir(Archivo archivo) throws Exception {
        Archivo archivoCompleto = archivoService.buscarPorIdConContenido(archivo.getId());

        String mimeType = detectarMimeType(archivoCompleto.getNombreOriginal());
        String jsonTexto = claudeVisionService.extraerDatos(archivoCompleto.getContenido(), mimeType);
        jsonTexto = limpiarJson(jsonTexto);

        JsonNode raiz;
        try {
            raiz = objectMapper.readTree(jsonTexto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                "La factura tiene demasiados ítems y la respuesta de la IA fue cortada. " +
                "Intentá dividir el archivo en páginas más cortas o reducir la cantidad de productos por archivo.", e);
        }

        List<DocumentoConvertido> exitosos = new ArrayList<>();
        List<ErrorFactura>        errores  = new ArrayList<>();

        if (raiz.isArray()) {
            // El modelo detectó múltiples facturas distintas en el mismo archivo.
            // Cada factura se procesa de forma independiente: solo se guardan las completamente válidas.
            int indice = 1;
            for (JsonNode nodo : raiz) {
                try {
                    DocumentoConvertido doc = mapearDesdeNodo(nodo, archivoCompleto, nodo.toString());
                    List<String> faltantes = obtenerCamposFaltantes(doc);
                    if (!faltantes.isEmpty()) {
                        errores.add(ErrorFactura.camposFaltantes(indice, faltantes));
                    } else {
                        validarNoDuplicada(doc);
                        validarTotalNoNegativo(doc);
                        repository.save(doc);
                        exitosos.add(doc);
                    }
                } catch (FacturaDuplicadaException ex) {
                    errores.add(ErrorFactura.duplicada(indice, ex));
                } catch (TotalNegativoException ex) {
                    errores.add(ErrorFactura.totalNegativo(indice, ex));
                }
                indice++;
            }
        } else {
            // Factura única — misma lógica que el array: validar campos antes de guardar
            DocumentoConvertido doc = mapearDesdeNodo(raiz, archivoCompleto, jsonTexto);
            List<String> faltantes = obtenerCamposFaltantes(doc);
            if (!faltantes.isEmpty()) {
                errores.add(ErrorFactura.camposFaltantes(1, faltantes));
            } else {
                validarNoDuplicada(doc);
                validarTotalNoNegativo(doc);
                repository.save(doc);
                exitosos.add(doc);
            }
        }

        // PROCESADO si al menos una factura fue guardada; PROCESADO_ERROR solo si ninguna pudo guardarse
        archivoCompleto.setConvertido(true);
        archivoCompleto.setEstadoConversion(!exitosos.isEmpty() ? "PROCESADO" : "PROCESADO_ERROR");
        archivoService.guardar(archivoCompleto);

        return new ResultadoConversion(exitosos, errores);
    }

    /** Construye un DocumentoConvertido a partir de un nodo JSON y su texto original. */
    private DocumentoConvertido mapearDesdeNodo(JsonNode json, Archivo archivo, String jsonOriginal) {
        DocumentoConvertido doc = new DocumentoConvertido();
        doc.setArchivo(archivo);
        doc.setCuit(json.path("cuit").asText(null));
        doc.setRazonSocial(json.path("razonSocial").asText(null));
        doc.setSituacionIva(json.path("situacionIva").asText(null));
        doc.setDireccion(json.path("direccion").asText(null));
        doc.setCiudad(json.path("ciudad").asText(null));
        doc.setCodigoPostal(json.path("codigoPostal").asText(null));
        doc.setProvincia(json.path("provincia").asText(null));
        doc.setPais(json.path("pais").asText(null));
        doc.setTelefono(json.path("telefono").asText(null));
        doc.setMail(json.path("mail").asText(null));
        doc.setCodigoArca(json.path("codigoArca").asText(null));
        doc.setLetra(json.path("letra").asText(null));
        doc.setCentroEmision(json.path("centroEmision").asText(null));
        doc.setNumeroComprobante(json.path("numeroComprobante").asText(null));
        doc.setFechaEmision(json.path("fechaEmision").asText(null));
        doc.setCae(json.path("cae").asText(null));
        doc.setFechaVencimientoCae(json.path("fechaVencimientoCae").asText(null));
        doc.setMoneda(json.path("moneda").asText(null));
        doc.setCotizacion(json.path("cotizacion").asText(null));
        doc.setOrdenCompra(json.path("ordenCompra").asText(null));
        doc.setSubTotalNoGravado(json.path("subTotalNoGravado").asText(null));
        doc.setTotal(json.path("total").asText(null));
        // Limitar a 20 000 chars si el JSON es muy grande
        // Guardar el JSON formateado (indentado) para que se vea legible en el visor
        String jsonFormateado;
        try {
            jsonFormateado = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            jsonFormateado = jsonOriginal;
        }
        doc.setJsonResultado(jsonFormateado.length() <= 20000 ? jsonFormateado : jsonFormateado.substring(0, 20000));

        JsonNode productosNode = json.path("productosConceptos");
        if (productosNode.isArray()) {
            for (JsonNode itemNode : productosNode) {
                ProductoConcepto producto = new ProductoConcepto();
                producto.setDocumento(doc);
                producto.setSku(itemNode.path("sku").asText(null));
                producto.setDescripcion(itemNode.path("descripcion").asText(null));
                producto.setCantidad(itemNode.path("cantidad").asText(null));
                producto.setPrecioUnitario(itemNode.path("precioUnitario").asText(null));
                producto.setDescuento(itemNode.path("descuento").asText(null));
                producto.setSubTotal(itemNode.path("subTotal").asText(null));
                producto.setAlicuotaIva(itemNode.path("alicuotaIva").asText(null));
                producto.setOrdenCompra(itemNode.path("ordenCompra").asText(null));
                producto.setRemito(itemNode.path("remito").asText(null));
                doc.getProductosConceptos().add(producto);
            }
        }

        JsonNode netosNode = json.path("netosGravados");
        if (netosNode.isArray()) {
            for (JsonNode netoNode : netosNode) {
                NetoGravado neto = new NetoGravado();
                neto.setDocumento(doc);
                neto.setAlicuota(netoNode.path("alicuota").asText(null));
                neto.setImporteNetoGravado(netoNode.path("importeNetoGravado").asText(null));
                neto.setIva(netoNode.path("iva").asText(null));
                doc.getNetosGravados().add(neto);
            }
        }

        JsonNode percepcionesNode = json.path("percepcionesIIBB");
        if (percepcionesNode.isArray()) {
            for (JsonNode percNode : percepcionesNode) {
                PercepcionIIBB perc = new PercepcionIIBB();
                perc.setDocumento(doc);
                String provinciaPerc = percNode.path("provincia").asText(null);
                // Si no hay provincia en la percepción pero sí hay importe, usar la provincia del emisor
                if (estaVacio(provinciaPerc) && !estaVacio(percNode.path("importe").asText(null))) {
                    provinciaPerc = doc.getProvincia();
                }
                perc.setProvincia(provinciaPerc);
                perc.setAlicuota(percNode.path("alicuota").asText(null));
                perc.setImporte(percNode.path("importe").asText(null));
                doc.getPercepcionesIIBB().add(perc);
            }
        }

        JsonNode percepcionesIVANode = json.path("percepcionesIVA");
        if (percepcionesIVANode.isArray()) {
            for (JsonNode percNode : percepcionesIVANode) {
                PercepcionIVA perc = new PercepcionIVA();
                perc.setDocumento(doc);
                perc.setAlicuota(percNode.path("alicuota").asText(null));
                perc.setImporte(percNode.path("importe").asText(null));
                doc.getPercepcionesIVA().add(perc);
            }
        }

        JsonNode vencimientosNode = json.path("vencimientos");
        if (vencimientosNode.isArray()) {
            for (JsonNode vencNode : vencimientosNode) {
                Vencimiento venc = new Vencimiento();
                venc.setDocumento(doc);
                venc.setFecha(vencNode.path("fecha").asText(null));
                venc.setImporte(vencNode.path("importe").asText(null));
                doc.getVencimientos().add(venc);
            }
        }

        return doc;
    }

    /** Devuelve la lista de campos obligatorios ausentes (vacía si la factura es válida). */
    private List<String> obtenerCamposFaltantes(DocumentoConvertido doc) {
        List<String> faltantes = new ArrayList<>();
        if (estaVacio(doc.getCuit()))              faltantes.add("CUIT del Emisor");
        if (estaVacio(doc.getCodigoArca()))        faltantes.add("Código ARCA");
        if (estaVacio(doc.getCentroEmision()))     faltantes.add("Centro de Emisión");
        if (estaVacio(doc.getNumeroComprobante())) faltantes.add("N° Comprobante");
        if (estaVacio(doc.getFechaEmision()))      faltantes.add("Fecha de Emisión");
        if (estaVacio(doc.getMoneda()))            faltantes.add("Moneda");
        if (estaVacio(doc.getTotal()))             faltantes.add("Total");
        return faltantes;
    }

    private boolean camposObligatoriosOk(DocumentoConvertido doc) {
        return obtenerCamposFaltantes(doc).isEmpty();
    }

    public List<DocumentoConvertido> listarTodos() {
        return repository.findAll();
    }

    // ── Paginación server-side para AbmDocumentosConvertidosView ─────────────
    public List<DocumentoConvertido> listarPaginado(int page, int size,
            String codigo, String nombre, String cuit, String centroEmision, String comprobante) {
        Pageable pageable = PageRequest.of(page, Math.max(size, 1));
        return repository.findFiltrado(
                codigo != null ? codigo : "",
                nombre != null ? nombre.toLowerCase() : "",
                cuit != null ? cuit : "",
                centroEmision != null ? centroEmision.toLowerCase() : "",
                comprobante != null ? comprobante : "",
                pageable);
    }

    public long contarFiltrado(String codigo, String nombre, String cuit, String centroEmision, String comprobante) {
        return repository.countFiltrado(
                codigo != null ? codigo : "",
                nombre != null ? nombre.toLowerCase() : "",
                cuit != null ? cuit : "",
                centroEmision != null ? centroEmision.toLowerCase() : "",
                comprobante != null ? comprobante : "");
    }

    // ── Datos para gráfico de actividad ──────────────────────────────────────
    public long[] conversionesPorDia() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime desde = hoy.minusDays(6).atStartOfDay();
        Map<LocalDate, Long> mapa = repository.findFechasDesde(desde).stream()
                .filter(f -> f != null)
                .collect(Collectors.groupingBy(LocalDateTime::toLocalDate, Collectors.counting()));
        long[] datos = new long[7];
        for (int i = 0; i < 7; i++) {
            datos[i] = mapa.getOrDefault(hoy.minusDays(6 - i), 0L);
        }
        return datos;
    }

    public String[] etiquetasDias() {
        LocalDate hoy = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        String[] labels = new String[7];
        for (int i = 0; i < 7; i++) {
            labels[i] = hoy.minusDays(6 - i).format(fmt);
        }
        return labels;
    }

    public long[] conversionesPorMes() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime desde = hoy.minusDays(29).atStartOfDay();
        Map<LocalDate, Long> mapa = repository.findFechasDesde(desde).stream()
                .filter(f -> f != null)
                .collect(Collectors.groupingBy(LocalDateTime::toLocalDate, Collectors.counting()));
        long[] datos = new long[30];
        for (int i = 0; i < 30; i++) datos[i] = mapa.getOrDefault(hoy.minusDays(29 - i), 0L);
        return datos;
    }

    public String[] etiquetasMes() {
        LocalDate hoy = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        String[] labels = new String[30];
        for (int i = 0; i < 30; i++) labels[i] = hoy.minusDays(29 - i).format(fmt);
        return labels;
    }

    public long[] conversionesPorAnio() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime desde = hoy.minusMonths(11).withDayOfMonth(1).atStartOfDay();
        Map<YearMonth, Long> mapa = repository.findFechasDesde(desde).stream()
                .filter(f -> f != null)
                .collect(Collectors.groupingBy(f -> YearMonth.from(f), Collectors.counting()));
        long[] datos = new long[12];
        for (int i = 0; i < 12; i++) datos[i] = mapa.getOrDefault(YearMonth.now().minusMonths(11 - i), 0L);
        return datos;
    }

    public String[] etiquetasAnio() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yy", new Locale("es", "AR"));
        String[] labels = new String[12];
        for (int i = 0; i < 12; i++) labels[i] = YearMonth.now().minusMonths(11 - i).format(fmt);
        return labels;
    }

    @Transactional
    public DocumentoConvertido buscarCompleto(Long id) {
        DocumentoConvertido doc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));
        // Inicializar colecciones lazy para que estén disponibles fuera de la transacción
        doc.getProductosConceptos().size();
        doc.getNetosGravados().size();
        doc.getPercepcionesIIBB().size();
        doc.getPercepcionesIVA().size();
        doc.getVencimientos().size();
        return doc;
    }

    @Transactional
    public void borrar(DocumentoConvertido doc) {
        Archivo archivo = doc.getArchivo();
        repository.delete(doc);
        repository.flush();
        if (archivo != null) {
            // Solo resetear el archivo a PENDIENTE cuando ya no queda ningún documento convertido
            boolean tieneOtros = !repository.findByArchivo_Id(archivo.getId()).isEmpty();
            if (!tieneOtros) {
                archivoService.actualizarEstado(archivo, "PENDIENTE");
                archivo.setConvertido(false);
                try { archivoService.guardar(archivo); } catch (Exception ignored) {}
            }
        }
    }

    /** Lanza FacturaDuplicadaException si ya existe un documento con la misma clave de negocio. */
    private void validarNoDuplicada(DocumentoConvertido doc) {
        if (!estaVacio(doc.getCuit()) && !estaVacio(doc.getCodigoArca())
                && !estaVacio(doc.getCentroEmision()) && !estaVacio(doc.getNumeroComprobante())) {
            long count = repository.countDuplicado(
                    doc.getCuit(), doc.getCodigoArca(),
                    doc.getCentroEmision(), doc.getNumeroComprobante());
            if (count > 0) {
                throw new FacturaDuplicadaException(
                        doc.getCuit(), doc.getCodigoArca(),
                        doc.getCentroEmision(), doc.getNumeroComprobante());
            }
        }
    }

    /** Lanza TotalNegativoException si el total de la factura es un número negativo. */
    private void validarTotalNoNegativo(DocumentoConvertido doc) {
        if (!estaVacio(doc.getTotal())) {
            // Quitar símbolos de moneda y espacios, luego intentar parsear
            String limpio = doc.getTotal().replaceAll("[$\\s]", "").trim();
            // Si empieza con '-' ya es negativo sin necesidad de parsear
            if (limpio.startsWith("-")) {
                throw new TotalNegativoException(doc.getTotal());
            }
            try {
                // Normalizar formato: puntos de miles y coma decimal (ej: 1.234,56)
                if (limpio.matches(".*\\d\\.\\d{3}.*")) {
                    limpio = limpio.replace(".", "").replace(",", ".");
                } else {
                    limpio = limpio.replace(",", ".");
                }
                double valor = Double.parseDouble(limpio);
                if (valor < 0) {
                    throw new TotalNegativoException(doc.getTotal());
                }
            } catch (NumberFormatException ignored) {
                // No se puede parsear; otras validaciones ya lo marcarán si corresponde
            }
        }
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.isEmpty() || valor.equals("null");
    }

    private String detectarMimeType(String nombreArchivo) {
        if (nombreArchivo == null) return "image/jpeg";
        String lower = nombreArchivo.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private String limpiarJson(String texto) {
        texto = texto.trim();
        if (texto.startsWith("```json")) texto = texto.substring(7);
        if (texto.startsWith("```")) texto = texto.substring(3);
        if (texto.endsWith("```")) texto = texto.substring(0, texto.length() - 3);
        return texto.trim();
    }
}