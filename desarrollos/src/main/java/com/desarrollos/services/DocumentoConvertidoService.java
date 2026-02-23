package com.desarrollos.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.desarrollos.entities.Archivo;
import com.desarrollos.entities.DocumentoConvertido;
import com.desarrollos.entities.ItemFactura;
import com.desarrollos.repositories.DocumentoConvertidoRepository;
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

        JsonNode json = objectMapper.readTree(jsonTexto);

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
        doc.setJsonResultado(jsonTexto);

        JsonNode itemsNode = json.path("items");
        if (itemsNode.isArray()) {
            for (JsonNode itemNode : itemsNode) {
                ItemFactura item = new ItemFactura();
                item.setDocumento(doc);
                item.setSku(itemNode.path("sku").asText(null));
                item.setDescripcion(itemNode.path("descripcion").asText(null));
                item.setCantidad(itemNode.path("cantidad").asText(null));
                item.setPrecioUnitario(itemNode.path("precioUnitario").asText(null));
                item.setDescuento(itemNode.path("descuento").asText(null));
                item.setSubTotal(itemNode.path("subTotal").asText(null));
                doc.getItems().add(item);
            }
        }

        repository.save(doc);

        archivoCompleto.setConvertido(true);
        archivoService.guardar(archivoCompleto);

        return doc;
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