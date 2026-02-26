package com.desarrollos.views;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.desarrollos.base.CrudView;
import com.desarrollos.entities.DocumentoConvertido;
import com.desarrollos.services.DocumentoConvertidoService;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.Icon;
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
    private CallbackDataProvider<DocumentoConvertido, Void> gridProvider;

    public AbmDocumentosConvertidosView(DocumentoConvertidoService service) {
        super(DocumentoConvertido.class);
        this.service = service;
        setTitulo("Documentos Convertidos");
        inicializarDataProvider();
    }

    private void inicializarDataProvider() {
        gridProvider = DataProvider.fromCallbacks(
            (Query<DocumentoConvertido, Void> query) -> {
                String archivo     = filtrosActivos.getOrDefault("Archivo", "");
                String razonSocial = filtrosActivos.getOrDefault("Razón Social", "");
                String cuit        = filtrosActivos.getOrDefault("CUIT", "");
                String comprobante = filtrosActivos.getOrDefault("N° Comprobante", "");
                int pageSize = Math.max(query.getLimit(), 1);
                int pageNum  = query.getOffset() / pageSize;
                return service.listarPaginado(pageNum, pageSize, archivo, razonSocial, cuit, comprobante).stream();
            },
            (Query<DocumentoConvertido, Void> query) -> {
                String archivo     = filtrosActivos.getOrDefault("Archivo", "");
                String razonSocial = filtrosActivos.getOrDefault("Razón Social", "");
                String cuit        = filtrosActivos.getOrDefault("CUIT", "");
                String comprobante = filtrosActivos.getOrDefault("N° Comprobante", "");
                return (int) service.contarFiltrado(archivo, razonSocial, cuit, comprobante);
            }
        );
        grid.setItems(gridProvider);
    }

    @Override
    protected boolean mostrarBotonNuevo() { return false; }

    @Override
    protected boolean mostrarBotonEditar() { return false; }

    @Override
    protected String anchoColumnaAcciones() { return "170px"; }

    @Override
    protected com.vaadin.flow.component.Component crearBotonAccionExtra(DocumentoConvertido item) {
        String nombreArchivo = (item.getArchivo() != null
                ? item.getArchivo().getNombre().replaceAll("\\s+", "_") : "documento") + ".json";
        String json = item.getJsonResultado() != null ? item.getJsonResultado() : "";
        StreamResource resource = new StreamResource(nombreArchivo,
                () -> new ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        Anchor anchor = new Anchor(resource, "");
        anchor.getElement().setAttribute("download", true);
        Icon icono = VaadinIcon.DOWNLOAD.create();
        icono.getStyle().set("color", "#16a34a");
        icono.setSize("17px");
        Button btn = new Button(icono);
        btn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY,
                com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL);
        btn.getElement().setAttribute("title", "Descargar JSON");
        anchor.add(btn);
        return anchor;
    }

    @Override
    protected void configurarColumnasEspecificas() {
        grid.removeAllColumns();

        agregarColumna(doc -> {
            String n = doc.getArchivo() != null ? doc.getArchivo().getNombre() : "";
            if (n == null || n.isEmpty()) return "";
            return n.length() > 20 ? n.substring(0, 20) + "..." : n;
        }, "Archivo");
        agregarColumna(DocumentoConvertido::getRazonSocial, "Razón Social");
        agregarColumna(DocumentoConvertido::getCuit, "CUIT");
        agregarColumna(DocumentoConvertido::getNumeroComprobante, "N° Comprobante");
    }

    @Override
    protected void actualizarLista() {
        if (gridProvider != null) gridProvider.refreshAll();
    }

    @Override
    protected void accionNuevo() { /* no aplica */ }

    @Override
    protected void accionVisualizar(DocumentoConvertido item) {
        // Recargar con colecciones inicializadas (evita LazyInitializationException)
        DocumentoConvertido item2;
        try {
            item2 = service.buscarCompleto(item.getId());
        } catch (Exception ex) {
            Notification.show("Error al cargar el documento: " + ex.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setWidth("860px");
        dialog.setHeight("600px");
        String nombreDoc = item2.getArchivo() != null ? item2.getArchivo().getNombre() : "Documento";
        dialog.setHeaderTitle("JSON · " + nombreDoc);

        String json = item2.getJsonResultado() != null ? item2.getJsonResultado() : "";

        Pre jsonPre = new Pre(json);
        jsonPre.getStyle()
                .set("background-color", "#1e1e2e")
                .set("color", "#cdd6f4")
                .set("border", "none")
                .set("border-radius", "12px")
                .set("padding", "20px")
                .set("font-family", "'JetBrains Mono', 'Fira Code', 'Courier New', monospace")
                .set("font-size", "13px")
                .set("white-space", "pre-wrap")
                .set("word-break", "break-word")
                .set("overflow-y", "auto")
                .set("flex", "1")
                .set("line-height", "1.6")
                .set("box-shadow", "inset 0 2px 8px rgba(0,0,0,0.4)");

        VerticalLayout contenido = new VerticalLayout();
        contenido.setPadding(false);
        contenido.setSpacing(true);
        contenido.setSizeFull();

        String notasTexto = generarNotas(item2);
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
        botonDescarga.getStyle().set("background-color", "#2563eb").set("color", "white");
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
