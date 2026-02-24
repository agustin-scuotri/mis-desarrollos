package com.desarrollos.views;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.desarrollos.base.CrudView;
import com.desarrollos.entities.DocumentoConvertido;
import com.desarrollos.services.DocumentoConvertidoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

@PageTitle("Documentos Convertidos")
@Route(value = "documentos-convertidos", layout = MainLayout.class)
public class AbmDocumentosConvertidosView extends CrudView<DocumentoConvertido> {

    private final DocumentoConvertidoService service;

    public AbmDocumentosConvertidosView(DocumentoConvertidoService service) {
        super(DocumentoConvertido.class);
        this.service = service;
        setTitulo("Documentos Convertidos");
        actualizarLista();
    }

    @Override
    protected boolean mostrarBotonNuevo() { return false; }

    @Override
    protected boolean mostrarBotonEditar() { return false; }

    @Override
    protected void configurarColumnasEspecificas() {
        grid.removeAllColumns();

        agregarColumna(doc -> doc.getArchivo() != null ? doc.getArchivo().getNombre() : "", "Archivo");
        agregarColumna(DocumentoConvertido::getRazonSocial, "Razón Social");
        agregarColumna(DocumentoConvertido::getCuit, "CUIT");
        agregarColumna(DocumentoConvertido::getNumeroComprobante, "N° Comprobante");
        agregarColumnaFecha(DocumentoConvertido::getFechaConversion, "Fecha Conversión");
    }

    @Override
    protected void actualizarLista() {
        if (service == null) return;

        List<DocumentoConvertido> todos = service.listarTodos();

        List<DocumentoConvertido> filtrados = todos.stream().filter(doc -> {
            for (Map.Entry<String, String> filtro : filtrosActivos.entrySet()) {
                String columna = filtro.getKey();
                String valorFiltro = filtro.getValue().toLowerCase();

                if (columna.equals("Archivo")) {
                    String val = (doc.getArchivo() != null && doc.getArchivo().getNombre() != null)
                            ? doc.getArchivo().getNombre().toLowerCase() : "";
                    if (!val.contains(valorFiltro)) return false;
                } else if (columna.equals("Razón Social")) {
                    String val = doc.getRazonSocial() != null ? doc.getRazonSocial().toLowerCase() : "";
                    if (!val.contains(valorFiltro)) return false;
                } else if (columna.equals("CUIT")) {
                    String val = doc.getCuit() != null ? doc.getCuit().toLowerCase() : "";
                    if (!val.contains(valorFiltro)) return false;
                } else if (columna.equals("N° Comprobante")) {
                    String val = doc.getNumeroComprobante() != null ? doc.getNumeroComprobante().toLowerCase() : "";
                    if (!val.contains(valorFiltro)) return false;
                }
            }
            return true;
        }).collect(Collectors.toList());

        grid.setItems(filtrados);
    }

    @Override
    protected void accionNuevo() { /* no aplica */ }

    @Override
    protected void accionVisualizar(DocumentoConvertido item) {
        Dialog dialog = new Dialog();
        dialog.setWidth("860px");
        dialog.setHeight("600px");
        String nombreDoc = item.getArchivo() != null ? item.getArchivo().getNombre() : "Documento";
        dialog.setHeaderTitle("JSON · " + nombreDoc);

        String json = item.getJsonResultado() != null ? item.getJsonResultado() : "";

        Pre jsonPre = new Pre(json);
        jsonPre.getStyle()
                .set("background-color", "#f0f4ff")
                .set("border", "1px solid rgba(0,32,96,0.2)")
                .set("border-radius", "8px")
                .set("padding", "15px")
                .set("font-family", "monospace")
                .set("font-size", "13px")
                .set("white-space", "pre-wrap")
                .set("word-break", "break-word")
                .set("overflow-y", "auto")
                .set("flex", "1");

        VerticalLayout contenido = new VerticalLayout();
        contenido.setPadding(false);
        contenido.setSpacing(true);
        contenido.setSizeFull();

        String notasTexto = generarNotas(item);
        if (!notasTexto.isEmpty()) {
            Paragraph pNotas = new Paragraph("⚠ Campos no encontrados:\n" + notasTexto);
            pNotas.getStyle()
                    .set("color", "#cc6600")
                    .set("font-family", "Verdana, sans-serif")
                    .set("font-size", "0.85rem")
                    .set("white-space", "pre-line")
                    .set("margin", "0")
                    .set("background-color", "#fff8f0")
                    .set("border", "1px solid #e07b00")
                    .set("border-radius", "6px")
                    .set("padding", "10px");
            contenido.add(pNotas);
        }

        contenido.add(jsonPre);
        contenido.expand(jsonPre);
        dialog.add(contenido);

        // Botón descargar
        String nombreArchivo = nombreDoc.replaceAll("\\s+", "_") + ".json";
        StreamResource resource = new StreamResource(nombreArchivo,
                () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        Anchor anchorDescarga = new Anchor(resource, "");
        anchorDescarga.getElement().setAttribute("download", true);
        Button botonDescarga = new Button("Descargar JSON", VaadinIcon.DOWNLOAD.create());
        botonDescarga.getStyle().set("background-color", "#002060").set("color", "white");
        anchorDescarga.add(botonDescarga);

        Button btnCerrar = new Button("Cerrar", e -> dialog.close());
        btnCerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(anchorDescarga, btnCerrar);
        dialog.open();
    }

    @Override
    protected void accionEditar(DocumentoConvertido item) {
        accionVisualizar(item);
    }

    @Override
    protected void accionBorrar(DocumentoConvertido item) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("app.borrar.titulo"));
        String nombre = item.getArchivo() != null ? item.getArchivo().getNombre() : "este documento";
        dialog.setText(getTranslation("app.borrar.mensaje", nombre));
        dialog.setCancelable(true);
        dialog.setCancelText(getTranslation("app.cancelar"));
        dialog.setConfirmText(getTranslation("app.borrar.confirmar"));
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(event -> {
            try {
                service.borrar(item);
                actualizarLista();
                Notification.show(getTranslation("app.borrar.exito"))
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                Notification.show("Error al borrar: " + e.getMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.open();
    }

    private String generarNotas(DocumentoConvertido doc) {
        StringBuilder notas = new StringBuilder();
        if (estaVacio(doc.getCuit()))               notas.append("• No se encontró el CUIT.\n");
        if (estaVacio(doc.getRazonSocial()))        notas.append("• No se encontró la razón social.\n");
        if (estaVacio(doc.getSituacionIva()))       notas.append("• No se encontró la situación ante IVA.\n");
        if (estaVacio(doc.getDireccion()))          notas.append("• No se encontró la dirección.\n");
        if (estaVacio(doc.getCiudad()))             notas.append("• No se encontró la ciudad/localidad.\n");
        if (estaVacio(doc.getCodigoPostal()))       notas.append("• No se encontró el código postal.\n");
        if (estaVacio(doc.getProvincia()))          notas.append("• No se encontró la provincia.\n");
        if (estaVacio(doc.getPais()))               notas.append("• No se encontró el país.\n");
        if (estaVacio(doc.getTelefono()))           notas.append("• No se encontró el teléfono.\n");
        if (estaVacio(doc.getMail()))               notas.append("• No se encontró el mail.\n");
        if (estaVacio(doc.getCodigoArca()))         notas.append("• No se encontró el código ARCA.\n");
        if (estaVacio(doc.getLetra()))              notas.append("• No se encontró la letra del comprobante.\n");
        if (estaVacio(doc.getCentroEmision()))      notas.append("• No se encontró el centro de emisión.\n");
        if (estaVacio(doc.getNumeroComprobante()))  notas.append("• No se encontró el número de comprobante.\n");
        if (estaVacio(doc.getFechaEmision()))       notas.append("• No se encontró la fecha de emisión.\n");
        if (estaVacio(doc.getCae()))                notas.append("• No se encontró el CAE.\n");
        if (estaVacio(doc.getFechaVencimientoCae()))notas.append("• No se encontró la fecha de vencimiento del CAE.\n");
        if (estaVacio(doc.getMoneda()))             notas.append("• No se encontró la moneda.\n");
        if (estaVacio(doc.getCotizacion()))         notas.append("• No se encontró la cotización.\n");
        if (estaVacio(doc.getOrdenCompra()))        notas.append("• No se encontró la orden de compra.\n");
        if (doc.getProductosConceptos().isEmpty())  notas.append("• No se encontraron productos/conceptos.\n");
        if (doc.getNetosGravados().isEmpty())       notas.append("• No se encontraron netos gravados e IVA.\n");
        if (estaVacio(doc.getSubTotalNoGravado()))  notas.append("• No se encontró el importe neto no gravado.\n");
        if (doc.getPercepcionesIIBB().isEmpty())    notas.append("• No se encontraron percepciones de IIBB.\n");
        if (doc.getPercepcionesIVA().isEmpty())     notas.append("• No se encontraron percepciones de IVA.\n");
        if (estaVacio(doc.getTotal()))              notas.append("• No se encontró el total del comprobante.\n");
        if (doc.getVencimientos().isEmpty())        notas.append("• No se encontraron vencimientos.\n");
        return notas.toString();
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.isEmpty() || valor.equals("null");
    }
}
