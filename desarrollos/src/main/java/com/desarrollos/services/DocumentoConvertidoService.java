package com.desarrollos.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
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
    public DocumentoConvertido convertir(Archivo archivo) throws Exception {
        Archivo archivoCompleto = archivoService.buscarPorIdConContenido(archivo.getId());

        String mimeType = detectarMimeType(archivoCompleto.getNombreOriginal());
        String jsonTexto = claudeVisionService.extraerDatos(archivoCompleto.getContenido(), mimeType);

        jsonTexto = limpiarJson(jsonTexto);

        JsonNode json;
        try {
            json = objectMapper.readTree(jsonTexto);
        } catch (JsonProcessingException e) {
            // El JSON llegó incompleto: Claude cortó la respuesta por límite de tokens.
            // Esto ocurre con facturas muy extensas (muchos ítems).
            throw new RuntimeException(
                "La factura tiene demasiados ítems y la respuesta de la IA fue cortada. " +
                "Intentá dividir el archivo en páginas más cortas o reducir la cantidad de productos por archivo.", e);
        }

        DocumentoConvertido doc = new DocumentoConvertido();
        doc.setArchivo(archivoCompleto);
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
        doc.setJsonResultado(jsonTexto);

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
                perc.setProvincia(percNode.path("provincia").asText(null));
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

        repository.save(doc);

        // Si falta algún campo obligatorio, el estado es PROCESADO_ERROR
        boolean camposObligatoriosOk = !estaVacio(doc.getCuit())
                && !estaVacio(doc.getCodigoArca())
                && !estaVacio(doc.getCentroEmision())
                && !estaVacio(doc.getNumeroComprobante())
                && !estaVacio(doc.getFechaEmision())
                && !estaVacio(doc.getMoneda())
                && !estaVacio(doc.getTotal());

        archivoCompleto.setConvertido(true);
        archivoCompleto.setEstadoConversion(camposObligatoriosOk ? "PROCESADO" : "PROCESADO_ERROR");
        archivoService.guardar(archivoCompleto);

        return doc;
    }

    public List<DocumentoConvertido> listarTodos() {
        return repository.findAll();
    }

    // ── Paginación server-side para AbmDocumentosConvertidosView ─────────────
    public List<DocumentoConvertido> listarPaginado(int page, int size,
            String archivo, String razonSocial, String cuit, String comprobante) {
        Pageable pageable = PageRequest.of(page, Math.max(size, 1));
        return repository.findFiltrado(
                archivo != null ? archivo.toLowerCase() : "",
                razonSocial != null ? razonSocial.toLowerCase() : "",
                cuit != null ? cuit : "",
                comprobante != null ? comprobante : "",
                pageable);
    }

    public long contarFiltrado(String archivo, String razonSocial, String cuit, String comprobante) {
        return repository.countFiltrado(
                archivo != null ? archivo.toLowerCase() : "",
                razonSocial != null ? razonSocial.toLowerCase() : "",
                cuit != null ? cuit : "",
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
        if (archivo != null) {
            archivoService.actualizarEstado(archivo, "PENDIENTE");
            archivo.setConvertido(false);
            try { archivoService.guardar(archivo); } catch (Exception ignored) {}
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