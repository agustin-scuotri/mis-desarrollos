package com.desarrollos.services;

import java.math.BigDecimal;
import java.text.NumberFormat;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.desarrollos.entities.Archivo;
import com.desarrollos.entities.DocumentoConvertido;
import com.desarrollos.entities.NetoGravado;
import com.desarrollos.entities.PercepcionIIBB;
import com.desarrollos.entities.PercepcionIVA;
import com.desarrollos.entities.ProductoConcepto;
import com.desarrollos.entities.DescuentoRecargo;
import com.desarrollos.entities.Tasa;
import com.desarrollos.entities.Vencimiento;
import com.desarrollos.repositories.DocumentoConvertidoRepository;
import java.security.MessageDigest;
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

        String hash = calcularHash(archivoCompleto.getContenido());
        archivoService.buscarPorHashProcesado(hash).ifPresent(original -> {
            throw new ArchivoDuplicadoException(original.getCodigo(), original.getNombre());
        });

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
        if (!exitosos.isEmpty()) {
            archivoCompleto.setEstadoConversion("PROCESADO");
            archivoCompleto.setMensajeError(null);
            archivoCompleto.setHashContenido(hash);
        } else {
            archivoCompleto.setEstadoConversion("PROCESADO_ERROR");
            String msg = errores.stream().map(ErrorFactura::getDetalle).collect(Collectors.joining("; "));
            archivoCompleto.setMensajeError(msg);
        }
        archivoService.guardar(archivoCompleto);

        return new ResultadoConversion(exitosos, errores);
    }

    /** Construye un DocumentoConvertido a partir de un nodo JSON y su texto original. */
    private DocumentoConvertido mapearDesdeNodo(JsonNode json, Archivo archivo, String jsonOriginal) {
        DocumentoConvertido doc = new DocumentoConvertido();
        doc.setArchivo(archivo);
        String cuitSanitizado = sanitizarCuit(json.path("cuit").asText(null));
        doc.setCuit(cuitSanitizado);
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
        doc.setFechaEmision(parsearFecha(json.path("fechaEmision").asText(null)));
        doc.setCae(json.path("cae").asText(null));
        doc.setFechaVencimientoCae(parsearFecha(json.path("fechaVencimientoCae").asText(null)));
        doc.setMoneda(json.path("moneda").asText(null));
        doc.setCotizacion(parsearImporte(json, "cotizacion"));
        doc.setOrdenCompra(json.path("ordenCompra").asText(null));
        doc.setSubTotalNoGravado(parsearImporte(json, "subTotalNoGravado"));
        doc.setImpuestoInterno(parsearImporte(json, "impuestoInterno"));
        doc.setTotal(parsearImporte(json, "total"));
        // Limitar a 20 000 chars si el JSON es muy grande
        // Guardar el JSON formateado (indentado) para que se vea legible en el visor
        if (json.isObject()) {
            com.fasterxml.jackson.databind.node.ObjectNode root = (com.fasterxml.jackson.databind.node.ObjectNode) json;
            if (cuitSanitizado != null) root.put("cuit", cuitSanitizado);
            formatearImportesEnJson(root);
            formatearFechaEnJson(root, "fechaEmision",        doc.getFechaEmision());
            formatearFechaEnJson(root, "fechaVencimientoCae", doc.getFechaVencimientoCae());
        }
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
                producto.setCantidad(parsearImporte(itemNode, "cantidad"));
                producto.setPrecioUnitario(parsearImporte(itemNode, "precioUnitario"));
                producto.setDescuento(parsearImporte(itemNode, "descuento"));
                producto.setSubTotal(parsearImporte(itemNode, "subTotal"));
                producto.setAlicuotaIva(normalizarAlicuota(itemNode.path("alicuotaIva").asText(null)));
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
                neto.setAlicuota(normalizarAlicuota(netoNode.path("alicuota").asText(null)));
                neto.setImporteNetoGravado(parsearImporte(netoNode, "importeNetoGravado"));
                neto.setIva(parsearImporte(netoNode, "iva"));
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
                perc.setAlicuota(normalizarAlicuota(percNode.path("alicuota").asText(null)));
                perc.setImporte(parsearImporte(percNode, "importe"));
                doc.getPercepcionesIIBB().add(perc);
            }
        }

        JsonNode percepcionesIVANode = json.path("percepcionesIVA");
        if (percepcionesIVANode.isArray()) {
            for (JsonNode percNode : percepcionesIVANode) {
                PercepcionIVA perc = new PercepcionIVA();
                perc.setDocumento(doc);
                perc.setAlicuota(normalizarAlicuota(percNode.path("alicuota").asText(null)));
                perc.setImporte(parsearImporte(percNode, "importe"));
                doc.getPercepcionesIVA().add(perc);
            }
        }

        JsonNode vencimientosNode = json.path("vencimientos");
        if (vencimientosNode.isArray()) {
            for (JsonNode vencNode : vencimientosNode) {
                Vencimiento venc = new Vencimiento();
                venc.setDocumento(doc);
                venc.setFecha(vencNode.path("fecha").asText(null));
                venc.setImporte(parsearImporte(vencNode, "importe"));
                doc.getVencimientos().add(venc);
            }
        }

        JsonNode tasasNode = json.path("tasas");
        if (tasasNode.isArray()) {
            for (JsonNode tasaNode : tasasNode) {
                Tasa tasa = new Tasa();
                tasa.setDocumento(doc);
                tasa.setDescripcion(tasaNode.path("descripcion").asText(null));
                tasa.setImporte(parsearImporte(tasaNode, "importe"));
                doc.getTasas().add(tasa);
            }
        }

        JsonNode descuentosNode = json.path("descuentosRecargos");
        if (descuentosNode.isArray()) {
            for (JsonNode drNode : descuentosNode) {
                DescuentoRecargo dr = new DescuentoRecargo();
                dr.setDocumento(doc);
                dr.setDescripcion(drNode.path("descripcion").asText(null));
                dr.setAlicuota(normalizarAlicuota(drNode.path("alicuota").asText(null)));
                dr.setImporte(parsearImporte(drNode, "importe"));
                doc.getDescuentosRecargos().add(dr);
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
        if (doc.getFechaEmision() == null)         faltantes.add("Fecha de Emisión");
        if (estaVacio(doc.getMoneda()))            faltantes.add("Moneda");
        if (doc.getTotal() == null)               faltantes.add("Total");
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
        doc.getTasas().size();
        doc.getDescuentosRecargos().size();
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
        if (doc.getTotal() != null && doc.getTotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new TotalNegativoException(doc.getTotal().toPlainString());
        }
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.isEmpty() || valor.equals("null");
    }

    private String calcularHash(byte[] contenido) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(contenido);
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) hex.append(String.format("%02x", b));
        return hex.toString();
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

    /**
     * Sanitiza y valida el CUIT leído por la IA.
     * Formatos válidos:
     *   - Con guiones:   XX-XXXXXXXX-X  (2 dígitos, guión, 8 dígitos, guión, 1 dígito)
     *   - Sin guiones:   11 dígitos consecutivos
     * Cualquier otro carácter que no sea dígito o guión se elimina antes de validar.
     * Si el resultado no cumple ningún formato, devuelve null.
     */
    private static final NumberFormat NF_AR = NumberFormat.getNumberInstance(new Locale("es", "AR"));

    private void formatearImportesEnJson(com.fasterxml.jackson.databind.node.ObjectNode root) {
        for (String campo : new String[]{"cotizacion", "subTotalNoGravado", "impuestoInterno", "total"}) {
            formatNodoNumerico(root, campo);
        }
        for (String arr : new String[]{"productosConceptos"}) {
            formatArrayImportes(root, arr, new String[]{"cantidad", "precioUnitario", "descuento", "subTotal"});
        }
        formatArrayImportes(root, "netosGravados", new String[]{"importeNetoGravado", "iva"});
        for (String arr : new String[]{"percepcionesIIBB", "percepcionesIVA", "tasas", "descuentosRecargos", "vencimientos"}) {
            formatArrayImportes(root, arr, new String[]{"importe"});
        }
    }

    private void formatArrayImportes(com.fasterxml.jackson.databind.node.ObjectNode root, String arrayField, String[] campos) {
        com.fasterxml.jackson.databind.JsonNode arr = root.path(arrayField);
        if (arr.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode item : arr) {
                if (item.isObject()) {
                    com.fasterxml.jackson.databind.node.ObjectNode obj = (com.fasterxml.jackson.databind.node.ObjectNode) item;
                    for (String campo : campos) formatNodoNumerico(obj, campo);
                }
            }
        }
    }

    private void formatNodoNumerico(com.fasterxml.jackson.databind.node.ObjectNode node, String campo) {
        com.fasterxml.jackson.databind.JsonNode n = node.path(campo);
        if (!n.isMissingNode() && !n.isNull() && n.isNumber()) {
            node.put(campo, NF_AR.format(n.decimalValue()));
        }
    }

    private static final DateTimeFormatter FMT_AR   = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yy"),
        DateTimeFormatter.ofPattern("d/M/yy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("d-M-yyyy")
    );

    private LocalDate parsearFecha(String raw) {
        if (raw == null || raw.isBlank() || raw.equals("null")) return null;
        String s = raw.trim();
        for (DateTimeFormatter fmt : FORMATOS_FECHA) {
            try { return LocalDate.parse(s, fmt); } catch (Exception ignored) {}
        }
        return null;
    }

    private void formatearFechaEnJson(com.fasterxml.jackson.databind.node.ObjectNode node,
                                      String campo, LocalDate fecha) {
        if (fecha != null) node.put(campo, fecha.format(FMT_AR));
    }

    private String sanitizarCuit(String raw) {
        if (raw == null || raw.isBlank() || raw.equals("null")) return null;
        String digitos = raw.replaceAll("[^0-9]", "");
        if (digitos.length() != 11) return null;
        return digitos.substring(0, 2) + "-" + digitos.substring(2, 10) + "-" + digitos.charAt(10);
    }

    private String limpiarJson(String texto) {
        texto = texto.trim();
        if (texto.startsWith("```json")) texto = texto.substring(7);
        if (texto.startsWith("```")) texto = texto.substring(3);
        if (texto.endsWith("```")) texto = texto.substring(0, texto.length() - 3);
        return texto.trim();
    }

    /**
     * Parsea un campo numérico del nodo JSON a BigDecimal.
     * Acepta tanto número JSON nativo como string con formato argentino (1.200,50)
     * o internacional (1200.50). Retorna null si el campo está ausente o vacío.
     */
    private BigDecimal parsearImporte(JsonNode node, String campo) {
        JsonNode nodo = node.path(campo);
        if (nodo.isMissingNode() || nodo.isNull()) return null;
        if (nodo.isNumber()) return nodo.decimalValue();
        String raw = nodo.asText(null);
        if (estaVacio(raw)) return null;
        // Quitar símbolo de moneda y espacios
        String limpio = raw.replaceAll("[$ ]", "").trim();
        if (limpio.isEmpty() || limpio.equals("-")) return null;
        try {
            // Detectar formato argentino: punto como separador de miles, coma como decimal
            if (limpio.matches(".*\\d\\.\\d{3}.*") || (limpio.contains(",") && limpio.contains("."))) {
                limpio = limpio.replace(".", "").replace(",", ".");
            } else {
                limpio = limpio.replace(",", ".");
            }
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Normaliza un campo de alícuota garantizando que siempre termine con "%".
     * Retorna null si el valor está vacío.
     */
    private String normalizarAlicuota(String valor) {
        if (estaVacio(valor)) return null;
        String limpio = valor.trim();
        return limpio.endsWith("%") ? limpio : limpio + "%";
    }
}