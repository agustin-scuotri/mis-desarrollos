package com.desarrollos.views;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.desarrollos.base.CrudView;
import com.desarrollos.entities.DocumentoConvertido;
import com.desarrollos.entities.NetoGravado;
import com.desarrollos.entities.PercepcionIIBB;
import com.desarrollos.entities.PercepcionIVA;
import com.desarrollos.entities.ProductoConcepto;
import com.desarrollos.entities.Vencimiento;
import com.desarrollos.services.DocumentoConvertidoService;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

@PageTitle("Lista de JSONs")
@Route(value = "lista-jsons", layout = MainLayout.class)
public class AbmDocumentosConvertidosView extends CrudView<DocumentoConvertido> {

    private final DocumentoConvertidoService service;
    private CallbackDataProvider<DocumentoConvertido, Void> gridProvider;

    public AbmDocumentosConvertidosView(DocumentoConvertidoService service) {
        super(DocumentoConvertido.class);
        this.service = service;
        setTitulo("Lista de JSONs");
        inicializarDataProvider();
    }

    private void inicializarDataProvider() {
        gridProvider = DataProvider.fromCallbacks(
            (Query<DocumentoConvertido, Void> query) -> {
                String codigo        = filtrosActivos.getOrDefault("Código", "");
                String nombre        = filtrosActivos.getOrDefault("Nombre", "");
                String cuit          = filtrosActivos.getOrDefault("Cuit del emisor", "");
                String comprobante   = filtrosActivos.getOrDefault("Nro. comprobante", "");
                int pageSize = Math.max(query.getLimit(), 1);
                int pageNum  = query.getOffset() / pageSize;
                return service.listarPaginado(pageNum, pageSize, codigo, nombre, cuit, "", comprobante).stream();
            },
            (Query<DocumentoConvertido, Void> query) -> {
                String codigo        = filtrosActivos.getOrDefault("Código", "");
                String nombre        = filtrosActivos.getOrDefault("Nombre", "");
                String cuit          = filtrosActivos.getOrDefault("Cuit del emisor", "");
                String comprobante   = filtrosActivos.getOrDefault("Nro. comprobante", "");
                return (int) service.contarFiltrado(codigo, nombre, cuit, "", comprobante);
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

        agregarColumna(doc -> doc.getArchivo() != null ? doc.getArchivo().getCodigo() : "", "Código");
        agregarColumna(doc -> {
            String n = doc.getArchivo() != null ? doc.getArchivo().getNombre() : "";
            if (n == null || n.isEmpty()) return "";
            return n.length() > 25 ? n.substring(0, 25) + "..." : n;
        }, "Nombre");
        agregarColumna(DocumentoConvertido::getCuit, "Cuit del emisor");
        agregarColumna(DocumentoConvertido::getNumeroComprobante, "Nro. comprobante");
    }

    @Override
    protected void actualizarLista() {
        if (gridProvider != null) gridProvider.refreshAll();
    }

    @Override
    protected void accionNuevo() { /* no aplica */ }

    @Override
    protected void accionVisualizar(DocumentoConvertido item) {
        DocumentoConvertido doc;
        try {
            doc = service.buscarCompleto(item.getId());
        } catch (Exception ex) {
            Notification.show("Error al cargar el documento: " + ex.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        String nombreDoc = doc.getArchivo() != null ? doc.getArchivo().getNombre() : "Documento";
        String nroComp   = estaVacio(doc.getNumeroComprobante()) ? "" : "  —  Nro. " + doc.getNumeroComprobante();

        Dialog dialog = new Dialog();
        dialog.setWidth("1200px");
        dialog.setHeight("88vh");
        dialog.setResizable(true);
        dialog.setHeaderTitle("Factura · " + nombreDoc + nroComp);

        // ── Contenedor principal scrolleable ──────────────────────────────────
        VerticalLayout contenido = new VerticalLayout();
        contenido.setPadding(false);
        contenido.setSpacing(true);
        contenido.setWidthFull();
        contenido.getStyle().set("overflow-y", "auto").set("padding-right", "4px");

        // ── Datos clave de la factura ─────────────────────────────────────────
        HorizontalLayout panelDatos = new HorizontalLayout();
        panelDatos.setWidthFull();
        panelDatos.setSpacing(false);
        panelDatos.getStyle()
                .set("background-color", "#f0f9ff")
                .set("border", "1px solid #bae6fd")
                .set("border-radius", "10px")
                .set("padding", "14px 18px")
                .set("display", "flex")
                .set("flex-direction", "row")
                .set("align-items", "flex-start")
                .set("gap", "40px");

        panelDatos.add(
            crearCampoInfo("CUIT del Emisor",     estaVacio(doc.getCuit())              ? "—" : doc.getCuit()),
            crearCampoInfo("N° Comprobante",       estaVacio(doc.getNumeroComprobante()) ? "—" : doc.getNumeroComprobante()),
            crearCampoInfo("Centro de Emisión",    estaVacio(doc.getCentroEmision())     ? "—" : doc.getCentroEmision()),
            crearCampoInfo("Total",                estaVacio(doc.getTotal())             ? "—" : doc.getTotal())
        );
        contenido.add(panelDatos);

        // ── JSON viewer (colapsado por defecto) ───────────────────────────────
        H3 tituloJson = new H3("Resultado JSON");
        tituloJson.getStyle()
                .set("color", "#1e293b").set("margin", "0").set("font-weight", "700")
                .set("font-size", "1rem").set("letter-spacing", "-0.2px");

        String json = doc.getJsonResultado() != null ? doc.getJsonResultado() : "";
        Pre jsonPre = new Pre(json);
        jsonPre.getStyle()
                .set("background-color", "#1e1e2e").set("color", "#cdd6f4").set("border", "none")
                .set("border-radius", "12px").set("padding", "20px")
                .set("font-family", "'JetBrains Mono', 'Fira Code', 'Courier New', monospace")
                .set("font-size", "13px").set("width", "100%").set("white-space", "pre-wrap")
                .set("word-break", "break-word").set("box-shadow", "inset 0 2px 8px rgba(0,0,0,0.4)")
                .set("line-height", "1.6").set("overflow-y", "auto").set("max-height", "340px");
        jsonPre.setVisible(false);

        Button btnVerMas   = new Button("Ver más",   VaadinIcon.CHEVRON_DOWN.create());
        Button btnVerMenos = new Button("Ver menos", VaadinIcon.CHEVRON_UP.create());
        btnVerMas.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnVerMas.getStyle().set("align-self", "flex-start");
        btnVerMenos.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnVerMenos.getStyle().set("align-self", "flex-start");
        btnVerMenos.setVisible(false);

        btnVerMas.addClickListener(e -> { jsonPre.setVisible(true);  btnVerMas.setVisible(false); btnVerMenos.setVisible(true);  });
        btnVerMenos.addClickListener(e -> { jsonPre.setVisible(false); btnVerMas.setVisible(true);  btnVerMenos.setVisible(false); });

        contenido.add(tituloJson, btnVerMas, jsonPre, btnVerMenos);

        // ── Productos / Conceptos ─────────────────────────────────────────────
        if (!doc.getProductosConceptos().isEmpty()) {
            H4 t = new H4("Productos / Conceptos");
            t.getStyle().set("color", "#002060").set("margin", "16px 0 4px 0");
            Grid<ProductoConcepto> g = new Grid<>(ProductoConcepto.class, false);
            g.addColumn(ProductoConcepto::getSku).setHeader("SKU").setWidth("130px").setFlexGrow(0);
            g.addColumn(ProductoConcepto::getDescripcion).setHeader("Descripción").setFlexGrow(1);
            g.addColumn(ProductoConcepto::getCantidad).setHeader("Cant.").setWidth("70px").setFlexGrow(0);
            g.addColumn(ProductoConcepto::getPrecioUnitario).setHeader("P. Unit.").setWidth("95px").setFlexGrow(0);
            g.addColumn(ProductoConcepto::getDescuento).setHeader("Desc.").setWidth("80px").setFlexGrow(0);
            g.addColumn(ProductoConcepto::getSubTotal).setHeader("Subtotal").setWidth("95px").setFlexGrow(0);
            g.addColumn(ProductoConcepto::getAlicuotaIva).setHeader("IVA").setWidth("75px").setFlexGrow(0);
            g.addColumn(ProductoConcepto::getOrdenCompra).setHeader("OC").setWidth("90px").setFlexGrow(0);
            g.addColumn(ProductoConcepto::getRemito).setHeader("Remito").setWidth("130px").setFlexGrow(0);
            g.setItems(doc.getProductosConceptos());
            g.setAllRowsVisible(true);
            g.getStyle().set("margin-top", "4px");
            contenido.add(t, g);
        }

        // ── Netos Gravados e IVA ──────────────────────────────────────────────
        if (!doc.getNetosGravados().isEmpty()) {
            H4 t = new H4("Netos Gravados e IVA");
            t.getStyle().set("color", "#002060").set("margin", "16px 0 4px 0");
            Grid<NetoGravado> g = new Grid<>(NetoGravado.class, false);
            g.addColumn(NetoGravado::getAlicuota).setHeader("Alícuota").setWidth("110px").setFlexGrow(0);
            g.addColumn(NetoGravado::getImporteNetoGravado).setHeader("Importe Neto Gravado").setFlexGrow(1);
            g.addColumn(NetoGravado::getIva).setHeader("IVA").setWidth("130px").setFlexGrow(0);
            g.setItems(doc.getNetosGravados());
            g.setAllRowsVisible(true);
            g.getStyle().set("margin-top", "4px");
            contenido.add(t, g);
        }

        // ── Percepciones IIBB ─────────────────────────────────────────────────
        if (!doc.getPercepcionesIIBB().isEmpty()) {
            H4 t = new H4("Percepciones IIBB");
            t.getStyle().set("color", "#002060").set("margin", "16px 0 4px 0");
            Grid<PercepcionIIBB> g = new Grid<>(PercepcionIIBB.class, false);
            g.addColumn(PercepcionIIBB::getProvincia).setHeader("Provincia").setFlexGrow(1);
            g.addColumn(PercepcionIIBB::getAlicuota).setHeader("Alícuota").setWidth("110px").setFlexGrow(0);
            g.addColumn(PercepcionIIBB::getImporte).setHeader("Importe").setWidth("130px").setFlexGrow(0);
            g.setItems(doc.getPercepcionesIIBB());
            g.setAllRowsVisible(true);
            g.getStyle().set("margin-top", "4px");
            contenido.add(t, g);
        }

        // ── Percepciones IVA ──────────────────────────────────────────────────
        if (!doc.getPercepcionesIVA().isEmpty()) {
            H4 t = new H4("Percepciones IVA");
            t.getStyle().set("color", "#002060").set("margin", "16px 0 4px 0");
            Grid<PercepcionIVA> g = new Grid<>(PercepcionIVA.class, false);
            g.addColumn(PercepcionIVA::getAlicuota).setHeader("Alícuota").setWidth("150px").setFlexGrow(0);
            g.addColumn(PercepcionIVA::getImporte).setHeader("Importe").setFlexGrow(1);
            g.setItems(doc.getPercepcionesIVA());
            g.setAllRowsVisible(true);
            g.getStyle().set("margin-top", "4px");
            contenido.add(t, g);
        }

        // ── Vencimientos ──────────────────────────────────────────────────────
        if (!doc.getVencimientos().isEmpty()) {
            H4 t = new H4("Vencimientos");
            t.getStyle().set("color", "#002060").set("margin", "16px 0 4px 0");
            Grid<Vencimiento> g = new Grid<>(Vencimiento.class, false);
            g.addColumn(Vencimiento::getFecha).setHeader("Fecha").setWidth("150px").setFlexGrow(0);
            g.addColumn(Vencimiento::getImporte).setHeader("Importe").setFlexGrow(1);
            g.setItems(doc.getVencimientos());
            g.setAllRowsVisible(true);
            g.getStyle().set("margin-top", "4px");
            contenido.add(t, g);
        }

        // ── Notas de campos faltantes ─────────────────────────────────────────
        String notasTexto = generarNotas(doc);
        if (!notasTexto.isEmpty()) {
            Paragraph pNotas = new Paragraph("⚠ Campos no encontrados:\n" + notasTexto);
            pNotas.getStyle()
                    .set("color", "#92400e").set("font-size", "0.875rem").set("margin-top", "10px")
                    .set("white-space", "pre-line").set("background-color", "#fffbeb")
                    .set("border", "1px solid #fde68a").set("border-radius", "8px").set("padding", "12px");
            contenido.add(pNotas);
        }

        dialog.add(contenido);

        // ── Footer ────────────────────────────────────────────────────────────
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

    /** Crea un bloque etiqueta + valor para el panel de datos clave. */
    private VerticalLayout crearCampoInfo(String etiqueta, String valor) {
        com.vaadin.flow.component.html.Span lbl = new com.vaadin.flow.component.html.Span(etiqueta);
        lbl.getStyle()
                .set("font-size", "0.72rem").set("font-weight", "600")
                .set("color", "#0369a1").set("text-transform", "uppercase")
                .set("letter-spacing", "0.05em");
        com.vaadin.flow.component.html.Span val = new com.vaadin.flow.component.html.Span(valor);
        val.getStyle()
                .set("font-size", "1rem").set("font-weight", "700")
                .set("color", "#0c4a6e");
        VerticalLayout campo = new VerticalLayout(lbl, val);
        campo.setPadding(false);
        campo.setSpacing(false);
        campo.setWidth("auto");   // evita que VerticalLayout ocupe 100% del ancho
        campo.getStyle().set("gap", "2px").set("flex-shrink", "0");
        return campo;
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
