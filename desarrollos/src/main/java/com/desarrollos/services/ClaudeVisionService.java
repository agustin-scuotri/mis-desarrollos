package com.desarrollos.services;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ClaudeVisionService {

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.url}")
    private String apiUrl;

    @Value("${claude.api.model}")
    private String model;

    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extraerDatos(byte[] contenido, String mimeType) throws Exception {
        String contenidoBase64 = Base64.getEncoder().encodeToString(contenido);

        String contentBlock = buildContentBlock(mimeType, contenidoBase64);

        String requestBody = String.format("""
                {
                  "model": "%s",
                  "max_tokens": 500,
                  "messages": [
                    {
                      "role": "user",
                      "content": [
                        %s,
                        {
                          "type": "text",
                          "text": "Analizá esta factura argentina y extraé los siguientes datos. Respondé ÚNICAMENTE con un JSON con este formato exacto, sin explicaciones ni texto adicional: { \\"cuit\\": \\"valor\\", \\"razonSocial\\": \\"valor\\", \\"situacionIva\\": \\"valor\\", \\"direccion\\": \\"calle + numero + piso + departamento u oficina concatenados en un solo texto\\", \\"ciudad\\": \\"valor\\", \\"codigoPostal\\": \\"valor\\", \\"provincia\\": \\"valor\\", \\"pais\\": \\"valor\\", \\"telefono\\": \\"valor\\", \\"mail\\": \\"valor\\", \\"codigoArca\\": \\"codigo ARCA que aparece debajo de la letra del comprobante (ej: COD 001)\\", \\"letra\\": \\"letra del comprobante (A, B, C, etc.)\\", \\"centroEmision\\": \\"primer bloque numerico del numero de comprobante (punto de venta)\\", \\"numeroComprobante\\": \\"segundo bloque numerico del numero de comprobante\\", \\"fechaEmision\\": \\"fecha de emision del comprobante\\", \\"cae\\": \\"numero CAE\\", \\"fechaVencimientoCae\\": \\"fecha de vencimiento del CAE\\", \\"moneda\\": \\"moneda en que se expresan los importes (ej: ARS, USD)\\", \\"cotizacion\\": \\"cotizacion o tasa de cambio de la moneda\\" }. Si algún campo no está presente en el documento, usá null como valor."
                        }
                      ]
                    }
                  ]
                }
                """, model, contentBlock);

        String respuesta = webClient.post()
                .uri(apiUrl)
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Error Claude: " + body)))
                .bodyToMono(String.class)
                .block();

        // Extraemos el texto de la respuesta formato Anthropic
        JsonNode root = objectMapper.readTree(respuesta);
        return root.path("content")
                   .get(0)
                   .path("text")
                   .asText();
    }

    private String buildContentBlock(String mimeType, String base64Data) {
        if ("application/pdf".equals(mimeType)) {
            return String.format("""
                    {
                      "type": "document",
                      "source": {
                        "type": "base64",
                        "media_type": "application/pdf",
                        "data": "%s"
                      }
                    }""", base64Data);
        } else {
            return String.format("""
                    {
                      "type": "image",
                      "source": {
                        "type": "base64",
                        "media_type": "%s",
                        "data": "%s"
                      }
                    }""", mimeType, base64Data);
        }
    }
}
