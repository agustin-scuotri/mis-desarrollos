package com.desarrollos.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.desarrollos.entities.Archivo;
import com.desarrollos.entities.DocumentoConvertido;
import com.desarrollos.repositories.DocumentoConvertidoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DocumentoConvertidoService {

    @Autowired
    private DocumentoConvertidoRepository repository;

    @Autowired
    private GroqVisionService groqVisionService;

    @Autowired
    private ArchivoService archivoService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public DocumentoConvertido convertir(Archivo archivo) throws Exception {
        Archivo archivoCompleto = archivoService.buscarPorIdConContenido(archivo.getId());
        String mimeType = detectarMimeType(archivoCompleto.getNombreOriginal());
        String jsonTexto = groqVisionService.extraerDatos(archivoCompleto.getContenido(), mimeType);

        jsonTexto = limpiarJson(jsonTexto);

        JsonNode json = objectMapper.readTree(jsonTexto);

        DocumentoConvertido doc = new DocumentoConvertido();
        doc.setArchivo(archivoCompleto);
        doc.setNombre(json.path("nombre").asText());
        doc.setApellido(json.path("apellido").asText());
        doc.setDni(json.path("dni").asText());
        doc.setJsonResultado(jsonTexto);

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