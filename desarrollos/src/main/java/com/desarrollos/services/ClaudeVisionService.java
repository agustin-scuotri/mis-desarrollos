package com.desarrollos.services;

import java.time.Duration;
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
                  "max_tokens": 8192,
                  "messages": [
                    {
                      "role": "user",
                      "content": [
                        %s,
                        {
                          "type": "text",
                          "text": "IMPORTANTE sobre múltiples facturas en el documento:\\n1) Si el documento contiene copias de LA MISMA factura (original, duplicado, triplicado — mismo número de comprobante, mismo CUIT, mismo total): tratalo como UNA sola factura, extraé los datos de una única copia e ignorá las demás. No sumes importes ni dupliques productos.\\n2) Si el documento contiene FACTURAS DISTINTAS (diferente número de comprobante, diferente CUIT emisor, o diferentes totales): extraé CADA factura por separado.\\n\\nSi hay UNA factura (o copias de la misma): respondé con UN OBJETO JSON.\\nSi hay MÚLTIPLES facturas distintas: respondé con un ARRAY JSON: [ {...}, {...} ]\\n\\nAnalizá este documento argentino y extraé los datos. Respondé ÚNICAMENTE con un JSON (objeto o array según corresponda), sin explicaciones ni texto adicional. Formato de cada factura: { \\"cuit\\": \\"valor\\", \\"razonSocial\\": \\"valor\\", \\"situacionIva\\": \\"valor\\", \\"direccion\\": \\"calle + numero + piso + departamento concatenados\\", \\"ciudad\\": \\"valor\\", \\"codigoPostal\\": \\"valor\\", \\"provincia\\": \\"valor\\", \\"pais\\": \\"valor\\", \\"telefono\\": \\"valor\\", \\"mail\\": \\"valor\\", \\"codigoArca\\": \\"codigo ARCA\\", \\"letra\\": \\"letra del comprobante\\", \\"centroEmision\\": \\"punto de venta\\", \\"numeroComprobante\\": \\"numero de comprobante\\", \\"fechaEmision\\": \\"fecha de emision\\", \\"cae\\": \\"numero CAE\\", \\"fechaVencimientoCae\\": \\"fecha vencimiento CAE\\", \\"moneda\\": \\"moneda (ej: ARS, USD)\\", \\"cotizacion\\": \\"cotizacion\\", \\"ordenCompra\\": \\"numero de orden de compra global del comprobante\\", \\"productosConceptos\\": [ { \\"sku\\": \\"codigo o SKU\\", \\"descripcion\\": \\"descripcion\\", \\"cantidad\\": \\"cantidad\\", \\"precioUnitario\\": \\"precio unitario\\", \\"descuento\\": \\"descuento\\", \\"subTotal\\": \\"subtotal\\", \\"alicuotaIva\\": \\"alicuota de IVA del producto (ej: 21%%, 10.5%%)\\", \\"ordenCompra\\": \\"OC del producto o la del comprobante si es global\\", \\"remito\\": \\"numero de remito del producto o el global si aplica\\" } ], \\"netosGravados\\": [ { \\"alicuota\\": \\"alicuota (ej: 21%%, 10.5%%)\\", \\"importeNetoGravado\\": \\"importe neto gravado\\", \\"iva\\": \\"importe IVA\\" } ], \\"subTotalNoGravado\\": \\"importe neto no gravado\\", \\"percepcionesIIBB\\": [ { \\"provincia\\": \\"provincia\\", \\"alicuota\\": \\"alicuota\\", \\"importe\\": \\"importe\\" } ], \\"percepcionesIVA\\": [ { \\"alicuota\\": \\"alicuota\\", \\"importe\\": \\"importe\\" } ], \\"total\\": \\"importe total\\", \\"vencimientos\\": [ { \\"fecha\\": \\"fecha del vencimiento\\", \\"importe\\": \\"importe del vencimiento\\" } ] }. productosConceptos, netosGravados, percepcionesIIBB, percepcionesIVA y vencimientos son arrays. Si algun campo no existe, usa null. Si ordenCompra o remito son globales (en cabecera o pie), replicarlos en todos los productos."
                        }
                      ]
                    }
                  ]
                }
                """, model, contentBlock);

        // exchangeToMono lee siempre el body sin importar el HTTP status code.
        return llamarApi(requestBody, true);
    }

    /**
     * Hace la llamada HTTP a Claude.
     * Si la API responde rate_limit_error espera ~65 s y reintenta una vez.
     * Si responde overloaded_error lanza ApiSaturadaException inmediatamente.
     */
    private String llamarApi(String requestBody, boolean permitirReintentoRateLimit) throws Exception {
        String respuesta = webClient.post()
                .uri(apiUrl)
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .bodyValue(requestBody)
                .exchangeToMono(response -> response.bodyToMono(String.class))
                .timeout(Duration.ofMinutes(2))
                .block();

        JsonNode root = objectMapper.readTree(respuesta);

        // Detectar errores devueltos por la API de Claude
        if ("error".equals(root.path("type").asText())) {
            String tipo    = root.path("error").path("type").asText();
            String mensaje = root.path("error").path("message").asText();

            if ("overloaded_error".equals(tipo)) {
                throw new ApiSaturadaException();
            }

            if ("rate_limit_error".equals(tipo)) {
                if (permitirReintentoRateLimit) {
                    // El bucket de tokens se recarga en 1 minuto; esperamos 65 s por seguridad
                    try { Thread.sleep(65_000); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    return llamarApi(requestBody, false);
                }
                throw new RuntimeException(
                        "Límite de tokens por minuto alcanzado. Esperá un momento e intentá de nuevo.");
            }

            throw new RuntimeException("Error Claude [" + tipo + "]: " + mensaje);
        }

        // Extraemos el texto de la respuesta formato Anthropic
        return root.path("content").get(0).path("text").asText();
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
