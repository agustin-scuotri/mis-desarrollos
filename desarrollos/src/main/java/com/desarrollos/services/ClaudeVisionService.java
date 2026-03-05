package com.desarrollos.services;

import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
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

    private static final int MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024; // 20 MB

    private final WebClient webClient = WebClient.builder()
            .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(c -> c.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                    .build())
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extraerDatos(byte[] contenido, String mimeType) throws Exception {
        if (contenido.length > MAX_FILE_SIZE_BYTES) {
            throw new RuntimeException(
                    "El archivo supera el tamaño máximo permitido de 20 MB. "
                    + "Reducí la resolución del escaneo o dividí el documento en partes más pequeñas.");
        }

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
                          "text": "BÚSQUEDA EXHAUSTIVA: Antes de responder revisá el documento COMPLETO con máximo detalle: cabecera, cuerpo, pie de página, recuadros, sellos, notas al margen. No dejés ningún campo como null si la información está disponible, aunque aparezca en formato inusual, abreviado o en una ubicación inesperada.\\n\\nIMPORTANTE sobre múltiples facturas en el documento:\\n1) Si el documento contiene copias de LA MISMA factura (original, duplicado, triplicado — mismo número de comprobante, mismo CUIT, mismo total): tratalo como UNA sola factura, extraé los datos de una única copia e ignorá las demás. No sumes importes ni dupliques productos.\\n2) Si el documento contiene FACTURAS DISTINTAS (diferente número de comprobante, diferente CUIT emisor, o diferentes totales): extraé CADA factura por separado.\\n\\nSi hay UNA factura (o copias de la misma): respondé con UN OBJETO JSON.\\nSi hay MÚLTIPLES facturas distintas: respondé con un ARRAY JSON: [ {...}, {...} ]\\n\\nREGLA CRÍTICA — ALÍCUOTAS: En TODOS los campos llamados \\"alicuota\\" o \\"alicuotaIva\\" el valor es SIEMPRE un porcentaje numérico (ej: 21%%, 10.5%%, 3%%, 0.5%%). NUNCA uses como alicuota: nombres de regímenes, códigos de resolución (ej: RG 2408/08), descripciones, textos ni cualquier otro valor que no sea un número seguido de %%. Si el porcentaje no aparece explícito en el documento, CALCULALO dividiendo el importe IVA por el importe neto gravado correspondiente y redondeá al porcentaje estándar más cercano (0.5%%, 3%%, 10.5%%, 21%%, etc.). Solo usá null si no hay suficientes datos para calcularlo.\\n\\nAnalizá este documento argentino y extraé los datos. Respondé ÚNICAMENTE con un JSON (objeto o array según corresponda), sin explicaciones ni texto adicional. Formato de cada factura: { \\"cuit\\": \\"CUIT del emisor tal como aparece (con o sin guiones)\\", \\"razonSocial\\": \\"nombre o razón social del emisor\\", \\"situacionIva\\": \\"situación frente al IVA del emisor (ej: Responsable Inscripto, Monotributista, Exento)\\", \\"direccion\\": \\"calle + numero + piso + departamento del emisor concatenados\\", \\"ciudad\\": \\"ciudad del emisor\\", \\"codigoPostal\\": \\"código postal del emisor\\", \\"provincia\\": \\"provincia del emisor\\", \\"pais\\": \\"país del emisor (default Argentina si no dice)\\", \\"telefono\\": \\"teléfono del emisor\\", \\"mail\\": \\"dirección de email del emisor: DEBE contener el símbolo @ (ej: ventas@empresa.com.ar). NUNCA pongas aquí una URL de página web (ej: www.empresa.com, http://..., empresa.com.ar) aunque esté impresa cerca de los datos de contacto — eso va en otro campo que no existe en este JSON, así que descartalo. Solo es válido un email con formato usuario@dominio. Si no hay un email con @ en el documento, usá null\\", \\"codigoArca\\": \\"código numérico ARCA: buscalo DENTRO O INMEDIATAMENTE DEBAJO del recuadro que muestra la letra del comprobante (etiquetas típicas: Cod., COD. N°, Código). Extraé SOLO el número que esté escrito explícitamente (ej: 01, 06, 11). Si el campo aparece vacío, en blanco o no tiene ningún número visible → null. NUNCA deduzcas ni inferás el código a partir del tipo de comprobante o la letra\\", \\"letra\\": \\"letra del comprobante (A, B, C, E, M...) — letra grande en el recuadro central del encabezado\\", \\"centroEmision\\": \\"punto de venta: número de 4-5 dígitos ANTES del guion en el número de comprobante (ej: en 0001-00012345 el centro es 0001). También puede llamarse Punto de Venta, Sucursal, P.V.\\", \\"numeroComprobante\\": \\"número de comprobante: dígitos DESPUÉS del guion (ej: en 0001-00012345 es 00012345)\\", \\"fechaEmision\\": \\"fecha de emisión del comprobante\\", \\"cae\\": \\"Código de Autorización Electrónica: número de 14 dígitos en el pie del documento, etiquetado como CAE, C.A.E. o Código de Autorización Electrónica\\", \\"fechaVencimientoCae\\": \\"fecha de vencimiento del CAE\\", \\"moneda\\": \\"moneda del comprobante (ej: ARS, USD) — si no se indica usá ARS\\", \\"cotizacion\\": \\"cotización de la moneda si aplica\\", \\"ordenCompra\\": \\"número de orden de compra global del comprobante\\", \\"productosConceptos\\": [ { \\"sku\\": \\"código o SKU del producto\\", \\"descripcion\\": \\"descripción del producto o servicio\\", \\"cantidad\\": \\"cantidad\\", \\"precioUnitario\\": \\"precio unitario\\", \\"descuento\\": \\"descuento\\", \\"subTotal\\": \\"subtotal de la línea\\", \\"alicuotaIva\\": \\"porcentaje de IVA (ej: 21%%, 10.5%%): si no aparece explícito calculalo como el IVA de la línea dividido su subtotal neto, redondeado al estándar más cercano\\", \\"ordenCompra\\": \\"OC del producto o la del comprobante si es global\\", \\"remito\\": \\"número de remito del producto o el global si aplica\\" } ], \\"netosGravados\\": [ { \\"alicuota\\": \\"porcentaje (ej: 21%%, 10.5%%): si no aparece explícito calculalo como iva/importeNetoGravado redondeado al estándar más cercano\\", \\"importeNetoGravado\\": \\"importe neto gravado\\", \\"iva\\": \\"importe IVA\\" } ], \\"subTotalNoGravado\\": \\"importe neto no gravado\\", \\"impuestoInterno\\": \\"importe total de impuesto interno u otros tributos: buscá líneas etiquetadas como 'Impuesto interno', 'Imp. interno', 'IMPORTE TOTAL OTROS TRIBUTOS', 'Otros Tributos' o similares. Si hay varios ítems de impuesto interno sumalos y devolvé el total. null si no existe\\", \\"percepcionesIIBB\\": [ { \\"provincia\\": \\"provincia\\", \\"alicuota\\": \\"porcentaje (ej: 3%%, 1.5%%) o null si no hay %% explicito\\", \\"importe\\": \\"importe\\" } ], \\"percepcionesIVA\\": [ { \\"alicuota\\": \\"porcentaje (ej: 10%%, 3.5%%) o null si no hay %% explicito\\", \\"importe\\": \\"importe\\" } ], \\"total\\": \\"importe total del comprobante\\", \\"vencimientos\\": [ { \\"fecha\\": \\"fecha del vencimiento\\", \\"importe\\": \\"importe del vencimiento\\" } ], \\"tasas\\": [ { \\"descripcion\\": \\"descripción de la tasa (ej: Tasa Gral, Tasa Municipal, TASA GRAL.)\\", \\"importe\\": \\"importe de la tasa\\" } ], \\"descuentosRecargos\\": [ { \\"descripcion\\": \\"tipo de ajuste: Bonificacion, Descuento, Recargo, Bonif., Dto. o similar\\", \\"importe\\": \\"importe del descuento/recargo\\" } ] }. productosConceptos, netosGravados, percepcionesIIBB, percepcionesIVA y vencimientos son arrays. Si algún campo realmente no existe en el documento, usá null. Si ordenCompra o remito son globales (en cabecera o pie), replicalos en todos los productos."
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
                .timeout(Duration.ofMinutes(5))
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
