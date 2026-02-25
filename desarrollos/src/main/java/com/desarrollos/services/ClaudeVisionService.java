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
                  "max_tokens": 4000,
                  "messages": [
                    {
                      "role": "user",
                      "content": [
                        %s,
                        {
                          "type": "text",
                          "text": "IMPORTANTE: Este documento puede contener múltiples copias de la misma factura (por ejemplo: original, duplicado, triplicado o varias páginas con el mismo comprobante). Si detectás que el documento tiene más de una copia del mismo comprobante (mismo número, mismo CUIT, mismo total), tratalo como una sola factura y extraé los datos de una única copia, ignorando completamente las demás. No sumes importes ni dupliques productos por el hecho de que aparezcan en más de una copia. Analizá esta factura argentina y extraé los siguientes datos. Respondé ÚNICAMENTE con un JSON con este formato exacto, sin explicaciones ni texto adicional: { \\"cuit\\": \\"valor\\", \\"razonSocial\\": \\"valor\\", \\"situacionIva\\": \\"valor\\", \\"direccion\\": \\"calle + numero + piso + departamento concatenados\\", \\"ciudad\\": \\"valor\\", \\"codigoPostal\\": \\"valor\\", \\"provincia\\": \\"valor\\", \\"pais\\": \\"valor\\", \\"telefono\\": \\"valor\\", \\"mail\\": \\"valor\\", \\"codigoArca\\": \\"codigo ARCA\\", \\"letra\\": \\"letra del comprobante\\", \\"centroEmision\\": \\"punto de venta\\", \\"numeroComprobante\\": \\"numero de comprobante\\", \\"fechaEmision\\": \\"fecha de emision\\", \\"cae\\": \\"numero CAE\\", \\"fechaVencimientoCae\\": \\"fecha vencimiento CAE\\", \\"moneda\\": \\"moneda (ej: ARS, USD)\\", \\"cotizacion\\": \\"cotizacion\\", \\"ordenCompra\\": \\"numero de orden de compra global del comprobante\\", \\"productosConceptos\\": [ { \\"sku\\": \\"codigo o SKU\\", \\"descripcion\\": \\"descripcion\\", \\"cantidad\\": \\"cantidad\\", \\"precioUnitario\\": \\"precio unitario\\", \\"descuento\\": \\"descuento\\", \\"subTotal\\": \\"subtotal\\", \\"alicuotaIva\\": \\"alicuota de IVA del producto (ej: 21%%, 10.5%%)\\", \\"ordenCompra\\": \\"OC del producto o la del comprobante si es global\\", \\"remito\\": \\"numero de remito del producto o el global si aplica\\" } ], \\"netosGravados\\": [ { \\"alicuota\\": \\"alicuota (ej: 21%%, 10.5%%)\\", \\"importeNetoGravado\\": \\"importe neto gravado\\", \\"iva\\": \\"importe IVA\\" } ], \\"subTotalNoGravado\\": \\"importe neto no gravado\\", \\"percepcionesIIBB\\": [ { \\"provincia\\": \\"provincia\\", \\"alicuota\\": \\"alicuota\\", \\"importe\\": \\"importe\\" } ], \\"percepcionesIVA\\": [ { \\"alicuota\\": \\"alicuota\\", \\"importe\\": \\"importe\\" } ], \\"total\\": \\"importe total\\", \\"vencimientos\\": [ { \\"fecha\\": \\"fecha del vencimiento\\", \\"importe\\": \\"importe del vencimiento\\" } ] }. productosConceptos, netosGravados, percepcionesIIBB, percepcionesIVA y vencimientos son arrays. Si algun campo no existe, usa null. Si ordenCompra o remito son globales (en cabecera o pie), replicarlos en todos los productos."
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
