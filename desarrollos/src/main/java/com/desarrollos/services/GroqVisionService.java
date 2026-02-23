package com.desarrollos.services;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GroqVisionService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.api.model}")
    private String model;

    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extraerDatos(byte[] imagen, String mimeType) throws Exception {
        String imagenBase64 = Base64.getEncoder().encodeToString(imagen);

        String requestBody = String.format("""
                {
                  "model": "%s",
                  "messages": [
                    {
                      "role": "user",
                      "content": [
                        {
                          "type": "text",
                          "text": "Analizá esta imagen y extraé los siguientes datos si están presentes: nombre, apellido, dni. Respondé ÚNICAMENTE con un JSON con este formato exacto, sin explicaciones ni texto adicional: { \\"nombre\\": \\"valor\\", \\"apellido\\": \\"valor\\", \\"dni\\": \\"valor\\" }"
                        },
                        {
                          "type": "image_url",
                          "image_url": {
                            "url": "data:%s;base64,%s"
                          }
                        }
                      ]
                    }
                  ],
                  "max_tokens": 500
                }
                """, model, mimeType, imagenBase64);

        String respuesta = webClient.post()
                .uri(apiUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Error Groq: " + body)))
                .bodyToMono(String.class)
                .block();

        // Extraemos el texto de la respuesta formato OpenAI
        JsonNode root = objectMapper.readTree(respuesta);
        return root.path("choices")
                   .get(0)
                   .path("message")
                   .path("content")
                   .asText();
    }
}