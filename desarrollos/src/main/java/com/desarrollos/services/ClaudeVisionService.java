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
                          "text": "BÚSQUEDA EXHAUSTIVA: Antes de responder revisá el documento COMPLETO con máximo detalle: cabecera, cuerpo, pie de página, recuadros, sellos, notas al margen. No dejés ningún campo como null si la información está disponible, aunque aparezca en formato inusual, abreviado o en una ubicación inesperada.\\n\\nIMPORTANTE sobre múltiples facturas en el documento:\\n1) Si el documento contiene copias de LA MISMA factura (original, duplicado, triplicado — mismo número de comprobante, mismo CUIT, mismo total): tratalo como UNA sola factura, extraé los datos de una única copia e ignorá las demás. No sumes importes ni dupliques productos.\\n2) Si el documento contiene FACTURAS DISTINTAS (diferente número de comprobante, diferente CUIT emisor, o diferentes totales): extraé CADA factura por separado.\\n\\nSi hay UNA factura (o copias de la misma): respondé con UN OBJETO JSON.\\nSi hay MÚLTIPLES facturas distintas: respondé con un ARRAY JSON: [ {...}, {...} ]\\n\\nREGLA CRÍTICA — IMPORTES Y CANTIDADES: Todos los valores numéricos de dinero y cantidades devolvelos SIEMPRE como número puro con punto decimal, sin símbolo de moneda ($, €, etc.), sin separadores de miles (ej: 1200.50, 0.50, 21500.00, 3.00). NUNCA uses formato con coma decimal ni separadores de miles. Los campos de porcentaje (alícuotas) SIEMPRE deben incluir el símbolo %% (ej: 21%%, 10.5%%, 3%%, 0.5%%).\\n\\nREGLA CRÍTICA — ALÍCUOTAS: En TODOS los campos llamados \\"alicuota\\" o \\"alicuotaIva\\" el valor es SIEMPRE un porcentaje numérico (ej: 21%%, 10.5%%, 3%%, 0.5%%). NUNCA uses como alicuota: nombres de regímenes, códigos de resolución (ej: RG 2408/08), descripciones, textos ni cualquier otro valor que no sea un número seguido de %%. Si el porcentaje no aparece explícito en el documento, CALCULALO dividiendo el importe IVA por el importe neto gravado correspondiente y redondeá al porcentaje estándar más cercano (0.5%%, 3%%, 10.5%%, 21%%, etc.). Solo usá null si no hay suficientes datos para calcularlo.\\n\\nREGLA CRÍTICA — SECCIONES A IGNORAR: Si el documento contiene páginas o secciones denominadas 'SÍNTESIS DE CARGOS', 'SÍNTESIS DE CUENTA', 'RESUMEN DE CUENTA', 'DETALLE DE CARGOS POR LÍNEA' o cualquier resumen agrupado por línea/abonado/número de teléfono, IGNORALAS COMPLETAMENTE. Solo extraé los datos de la sección principal de productos/conceptos de la factura (la tabla central de ítems con sus importes individuales).\\n\\nREGLA CRÍTICA — DESCUENTOS DENTRO DE LA LISTA DE PRODUCTOS: Si dentro de la lista de productos/conceptos aparece una línea con importe negativo (descuento aplicado sobre otro producto, ej: 'Desc. Especial 90%%', 'Desc. Especial Telefonía 10%%'), debés: (1) dejar la descripción tal cual, (2) convertir la cantidad a negativa (ej: si era 1 → -1), (3) convertir el importe a positivo (ej: si era -42975.00 → 42975.00), (4) mantener la alícuota IVA sin cambios. Estas líneas van en productosConceptos y NUNCA deben agregarse a descuentosRecargos.\\n\\nREGLA CRÍTICA — CLASIFICACIÓN DE PERCEPCIONES EN EL PIE DE FACTURA (APLICA A TODOS LOS TIPOS DE COMPROBANTE): Cuando en el pie o en el resumen de importes de una factura aparezcan líneas de percepción, clasificalas según estas reglas en orden de prioridad:\\n\\n1) Si la descripción contiene 'DN/B38', 'B38/95', 'Perc.DN/B38', 'Perc. DN/B38' o cualquier variante que haga referencia al Decreto 38/95 → es una PERCEPCIÓN DE INGRESOS BRUTOS DE LA PROVINCIA DE BUENOS AIRES. Agregala a percepcionesIIBB con provincia = 'Buenos Aires'. NUNCA la clasifiques como percepciónIVA aunque empiece con 'Perc.'.\\n\\n2) Si la descripción contiene 'Perc.IIBB', 'Perc. IIBB', 'IIBB', 'Ingresos Brutos', 'Ing. Brutos', 'Ing.Brutos' → percepcionesIIBB. La provincia se infiere del texto si está indicada (ej: 'CABA', 'Capital Federal', 'Bs.As.', 'Buenos Aires', 'Santa Fe'); si no se puede inferir, usá null.\\n\\n3) Si la descripción contiene explícitamente 'Perc. IVA', 'Percepción IVA', 'Percep. IVA' o 'Perc.IVA' (con la palabra IVA) → percepcionesIVA.\\n\\n4) REGLA GENERAL: NUNCA clasifiques una percepción como IVA únicamente porque su descripción empiece con 'Perc.' sin que también mencione explícitamente la palabra 'IVA'. Si no queda claro, preferí clasificarla como IIBB antes que IVA.\\n\\nREGLA CRÍTICA — RECLASIFICACIÓN DE ÍTEMS EN LIQUIDACIÓN DE SERVICIOS PÚBLICOS (ARCA 17): Si la factura es una 'LIQUIDACIÓN DE SERVICIOS PÚBLICOS' (código ARCA 17), ciertos ítems que aparecen en la lista de productos/conceptos NO son productos: deben reclasificarse y eliminarse de productosConceptos. Aplicá estas reglas en orden:\\n\\nA) → percepcionesIIBB: Cualquier ítem cuya descripción contenga 'Ing. Brutos', 'Ing.Brutos', 'IIBB', 'Ingresos Brutos' (en cualquier combinación, ej: 'Imp.Ing.Brutos (Dist.)', 'Imp.Ing.Brutos (Transporte)', 'Imp.s/IIBB Gas Retenido Transporte'). Mapeá: provincia = inferila del contexto si es posible (ej: 'Santa Fe') o null, alicuota = porcentaje si aparece explícito o null, importe = importe del ítem.\\n\\nB) → tasas: Cualquier ítem cuya descripción contenga 'Créd' y 'Déb' (ej: 'Imp.s/Créd. y Déb.', 'Impuesto al Crédito y Débito'), O contenga 'Fdo.Fid' o 'Fdo Fid' (ej: 'Fdo.Fid.Art.75 Leyes25565y27637'), O contenga 'DRei' (ej: 'Rec.Costo DRei-Santa Fe'). Mapeá: descripcion = descripción del ítem, importe = importe del ítem.\\n\\nC) → percepcionesIVA: Cualquier ítem cuya descripción contenga 'Percepción IVA', 'Percep. IVA', 'Perc. IVA', 'Percepcion IVA' o similar (ej: 'Percepción IVA - RG 2408/08 (3%%)'). Mapeá: alicuota = porcentaje si aparece explícito (ej: 3%%) o null, importe = importe del ítem.\\n\\nD) → eliminar de productosConceptos sin agregar a ningún otro array: Cualquier ítem cuya descripción corresponda al IVA propiamente dicho (ej: 'IVA Inscripto (27%%)', 'IVA 27%%', 'IVA 21%%', 'IVA 10.5%%'). Estos importes ya quedan reflejados en netosGravados, por lo tanto NO los agregues a productosConceptos ni a ningún otro campo.\\n\\nTodos los ítems reclasificados según A), B), C) o D) deben ELIMINARSE de productosConceptos. El resto de ítems (cargo fijo, consumo, IVA propiamente dicho, etc.) permanecen en productosConceptos con normalidad.\\n\\nAnalizá este documento argentino y extraé los datos. Respondé ÚNICAMENTE con un JSON (objeto o array según corresponda), sin explicaciones ni texto adicional. Formato de cada factura: { \\"cuit\\": \\"CUIT del emisor tal como aparece (con o sin guiones)\\", \\"razonSocial\\": \\"nombre o razón social del emisor\\", \\"situacionIva\\": \\"situación frente al IVA del emisor (ej: Responsable Inscripto, Monotributista, Exento)\\", \\"direccion\\": \\"calle + numero + piso + departamento del emisor concatenados. Si hay múltiples domicilios del emisor listados (ej: 'SANTA FE - San Luis 2673' y 'ROSARIO - Avda. Pellegrini 2774'), tomá SIEMPRE el PRIMERO que aparezca en el documento e ignorá los demás\\", \\"ciudad\\": \\"ciudad del emisor: si hay múltiples domicilios, tomá la ciudad correspondiente al PRIMERO que aparezca en el documento\\", \\"codigoPostal\\": \\"código postal del emisor: si hay múltiples domicilios, tomá el correspondiente al PRIMERO que aparezca en el documento\\", \\"provincia\\": \\"provincia del emisor: si hay múltiples domicilios, tomá la provincia correspondiente al PRIMERO que aparezca en el documento\\", \\"pais\\": \\"país del emisor (default Argentina si no dice)\\", \\"telefono\\": \\"teléfono del emisor\\", \\"mail\\": \\"dirección de email del emisor: DEBE contener el símbolo @ (ej: ventas@empresa.com.ar). NUNCA pongas aquí una URL de página web (ej: www.empresa.com, http://..., empresa.com.ar) aunque esté impresa cerca de los datos de contacto — eso va en otro campo que no existe en este JSON, así que descartalo. Solo es válido un email con formato usuario@dominio. Si no hay un email con @ en el documento, usá null\\", \\"codigoArca\\": \\"código numérico ARCA: EXCEPCIÓN OBLIGATORIA — si en el documento aparece la leyenda 'LIQUIDACIÓN DE SERVICIOS PÚBLICOS' o 'LIQUIDACION DE SERVICIOS PUBLICOS' en cualquier parte, el código ARCA es SIEMPRE 17, sin importar lo que diga el recuadro. Para cualquier otro tipo de comprobante: buscalo DENTRO O INMEDIATAMENTE DEBAJO del recuadro que muestra la letra del comprobante (etiquetas típicas: Cod., COD. N°, Código). Extraé SOLO el número que esté escrito explícitamente (ej: 01, 06, 11). Si el campo aparece vacío, en blanco o no tiene ningún número visible → null. NUNCA deduzcas ni inferás el código a partir del tipo de comprobante o la letra (salvo la excepción indicada para liquidación de servicios públicos)\\", \\"letra\\": \\"letra del comprobante (A, B, C, E, M...) — letra grande en el recuadro central del encabezado\\", \\"centroEmision\\": \\"punto de venta: número de 4-5 dígitos ANTES del guion en el número de comprobante (ej: en 0001-00012345 el centro es 0001). También puede llamarse Punto de Venta, Sucursal, P.V.\\", \\"numeroComprobante\\": \\"número de comprobante: dígitos DESPUÉS del guion (ej: en 0001-00012345 es 00012345)\\", \\"fechaEmision\\": \\"fecha de emisión del comprobante\\", \\"cae\\": \\"Código de Autorización Electrónica: número de 14 dígitos en el pie del documento, etiquetado como CAE, C.A.E. o Código de Autorización Electrónica\\", \\"fechaVencimientoCae\\": \\"fecha de vencimiento del CAE\\", \\"moneda\\": \\"moneda del comprobante (ej: ARS, USD) — si no se indica usá ARS\\", \\"cotizacion\\": \\"cotización de la moneda si aplica. La sigla TC o T.C. significa Tasa de Cambio y equivale a la cotización; si aparece en el documento usá ese valor como cotización\\", \\"ordenCompra\\": \\"número de orden de compra global del comprobante (etiquetas típicas: Orden de Compra, O.C., OC, Nro. OC, N° OC). REGLA DE FALLBACK: si el campo Orden de Compra está vacío, en blanco o directamente ausente en el documento, y existe un campo llamado 'Nota de Pedido', 'Nota Pedido', 'N° Pedido', 'Nro. Pedido', 'NP' o similar con un valor, usá ese valor como ordenCompra. Si ambos campos existen y tienen valor, priorizá siempre la Orden de Compra e ignorá la Nota de Pedido\\", \\"productosConceptos\\": [ { \\"sku\\": \\"código o SKU del producto\\", \\"descripcion\\": \\"descripción del producto o servicio\\", \\"cantidad\\": \\"cantidad\\", \\"precioUnitario\\": \\"precio unitario\\", \\"descuento\\": \\"descuento\\", \\"subTotal\\": \\"subtotal de la línea\\", \\"alicuotaIva\\": \\"porcentaje de IVA (ej: 21%%, 10.5%%): si no aparece explícito calculalo como el IVA de la línea dividido su subtotal neto, redondeado al estándar más cercano\\", \\"ordenCompra\\": \\"OC del producto o la del comprobante si es global\\", \\"remito\\": \\"número de remito del producto o el global si aplica\\", \\"numeroDespacho\\": \\"número de despacho de importación asociado al producto (puede aparecer como 'Despacho: XXXXXXX' debajo de la descripción del producto), null si no existe\\", \\"fechaDespacho\\": \\"fecha de despacho u oficialización asociada al producto (puede aparecer como 'Fecha: DD/MM/AA' o 'Fecha oficializacion: DD/MM/AAAA' junto al número de despacho), null si no existe\\", \\"registroOficializacion\\": \\"sigla o registro de oficialización del despacho (ej: DE, IC, etc., puede aparecer inmediatamente después de la fecha de despacho), null si no existe\\" } ], \\"netosGravados\\": [ { \\"alicuota\\": \\"porcentaje (ej: 21%%, 10.5%%): si no aparece explícito calculalo como iva/importeNetoGravado redondeado al estándar más cercano\\", \\"importeNetoGravado\\": \\"importe neto gravado\\", \\"iva\\": \\"importe IVA\\" } ], \\"subTotalNoGravado\\": \\"importe neto no gravado\\", \\"impuestoInterno\\": \\"importe total de impuesto interno: buscá líneas etiquetadas como 'Impuesto interno', 'Imp. interno' o similares. Si hay varios ítems de impuesto interno sumalos y devolvé el total. null si no existe\\", \\"percepcionesIIBB\\": [ { \\"provincia\\": \\"provincia\\", \\"alicuota\\": \\"porcentaje (ej: 3%%, 1.5%%) o null si no hay %% explicito\\", \\"importe\\": \\"importe\\" } ], \\"percepcionesIVA\\": [ { \\"alicuota\\": \\"porcentaje (ej: 10%%, 3.5%%) o null si no hay %% explicito\\", \\"importe\\": \\"importe\\" } ], \\"total\\": \\"importe total del comprobante\\", \\"vencimientos\\": [ { \\"fecha\\": \\"fecha del vencimiento\\", \\"importe\\": \\"importe del vencimiento\\" } ], \\"tasas\\": [ { \\"descripcion\\": \\"descripción de la tasa (ej: Tasa Gral, Tasa Municipal, TASA GRAL., Importe Otros Tributos). IMPORTANTE: las líneas del pie de factura etiquetadas como 'Importe Otros Tributos', 'IMPORTE TOTAL OTROS TRIBUTOS' u 'Otros Tributos' también van aquí con esa misma etiqueta como descripcion\\", \\"importe\\": \\"importe de la tasa\\" } ], \\"descuentosRecargos\\": [ { \\"descripcion\\": \\"tipo de ajuste: Bonificacion, Descuento, Recargo, Bonif., Dto. o similar\\", \\"alicuota\\": \\"porcentaje del descuento o recargo: si aparece explícito usalo directamente (ej: 15%%). Si NO aparece explícito, CALCULALO dividiendo el importe del descuento/recargo por el subtotal bruto antes del ajuste y redondeá al entero o decimal estándar más cercano (ej: 5%%, 10%%, 15%%, 20%%). Solo usá null si no hay suficientes datos para calcularlo\\", \\"importe\\": \\"importe del descuento/recargo\\" } ]. IMPORTANTE: descuentosRecargos se completa ÚNICAMENTE con descuentos, bonificaciones o recargos que aparezcan AL PIE de la factura (fuera de la lista de productos). NUNCA incluyas aquí las líneas de descuento que figuran dentro de la lista de productos/conceptos — esas van en productosConceptos con cantidad negativa e importe positivo según la regla indicada arriba }. productosConceptos, netosGravados, percepcionesIIBB, percepcionesIVA y vencimientos son arrays. Si algún campo realmente no existe en el documento, usá null. Si ordenCompra o remito son globales (en cabecera o pie), replicalos en todos los productos."
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
