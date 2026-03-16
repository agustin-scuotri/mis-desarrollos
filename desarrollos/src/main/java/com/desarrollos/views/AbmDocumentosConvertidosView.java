package com.desarrollos.views;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.desarrollos.base.CrudView;
import com.desarrollos.entities.DocumentoConvertido;
import com.desarrollos.entities.NetoGravado;
import com.desarrollos.entities.PercepcionIIBB;
import com.desarrollos.entities.PercepcionIVA;
import com.desarrollos.entities.ProductoConcepto;
import com.desarrollos.entities.DescuentoRecargo;
import com.desarrollos.entities.Tasa;
import com.desarrollos.entities.Vencimiento;
import com.desarrollos.services.DocumentoConvertidoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.desarrollos.base.Toast;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.PermitAll;

@PageTitle("Lista de JSONs")
@Route(value = "lista-jsons", layout = MainLayout.class)
@PermitAll
public class AbmDocumentosConvertidosView extends CrudView<DocumentoConvertido> {

    private final DocumentoConvertidoService service;
    private VerticalLayout panelDetalle;

    public AbmDocumentosConvertidosView(DocumentoConvertidoService service) {
        super(DocumentoConvertido.class);
        this.service = service;
        setTitulo("Lista de JSONs");
        configurarSplitLayout();
        actualizarLista();
    }

    // ── SplitLayout: grid a la izquierda, panel de detalle a la derecha ───────
    private void configurarSplitLayout() {
        // Panel lateral de detalle (placeholder inicial)
        panelDetalle = new VerticalLayout();
        panelDetalle.setSizeFull();
        panelDetalle.setPadding(false);
        panelDetalle.setSpacing(false);
        panelDetalle.getStyle()
                .set("overflow-y", "auto")
                .set("border-left", "1px solid var(--lumo-contrast-20pct, #e2e8f0)")
                .set("background", "var(--lumo-base-color, white)");

        // Retirar grid y barra de paginación del layout padre
        remove(grid);
        remove(barraPaginacion);

        // Panel primario: grid + paginación apilados
        VerticalLayout primaryPanel = new VerticalLayout(grid, barraPaginacion);
        primaryPanel.setSizeFull();
        primaryPanel.setPadding(false);
        primaryPanel.setSpacing(false);
        primaryPanel.setFlexGrow(1, grid);

        // SplitLayout horizontal
        SplitLayout splitLayout = new SplitLayout(primaryPanel, panelDetalle);
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(62);

        addComponentAtIndex(1, splitLayout);
        setFlexGrow(1, splitLayout);

        // Habilitar selección de fila
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.addSelectionListener(event ->
                event.getFirstSelectedItem().ifPresentOrElse(
                        this::mostrarDetalleEnPanel,
                        this::mostrarPanelPlaceholder
                )
        );
        mostrarPanelPlaceholder();
    }

    // ── Placeholder cuando no hay documento seleccionado ─────────────────────
    private void mostrarPanelPlaceholder() {
        panelDetalle.removeAll();
        panelDetalle.setAlignItems(FlexComponent.Alignment.CENTER);
        panelDetalle.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        Icon icono = VaadinIcon.FILE_TEXT_O.create();
        icono.setSize("48px");
        icono.getStyle().set("color", "#cbd5e1");

        Span titulo = new Span("Seleccioná un documento");
        titulo.getStyle().set("font-weight", "600").set("color", "var(--lumo-secondary-text-color, #64748b)")
                .set("font-size", "0.95rem").set("margin-top", "12px");

        Span subtitulo = new Span("Hacé click en una fila para ver sus detalles");
        subtitulo.getStyle().set("color", "var(--lumo-tertiary-text-color, #94a3b8)").set("font-size", "0.8rem");

        panelDetalle.add(icono, titulo, subtitulo);
    }

    // ── Detalle con accordion al seleccionar una fila ─────────────────────────
    private void mostrarDetalleEnPanel(DocumentoConvertido item) {
        DocumentoConvertido doc;
        try {
            doc = service.buscarCompleto(item.getId());
        } catch (Exception ex) {
            Toast.error("Error al cargar el documento: " + ex.getMessage());
            return;
        }

        panelDetalle.removeAll();
        panelDetalle.setAlignItems(FlexComponent.Alignment.START);
        panelDetalle.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        panelDetalle.setPadding(true);
        panelDetalle.setSpacing(false);
        panelDetalle.getStyle().set("gap", "8px");

        // ── Encabezado ────────────────────────────────────────────────────────
        String nombreDoc = doc.getArchivo() != null ? doc.getArchivo().getNombre() : "Documento";
        Span nombreSpan = new Span(nombreDoc);
        nombreSpan.getStyle().set("font-weight", "700").set("font-size", "1rem")
                .set("color", "var(--lumo-header-text-color, #1e293b)").set("word-break", "break-word");

        String nroComp = estaVacio(doc.getNumeroComprobante()) ? "" : "Nro. " + doc.getNumeroComprobante();
        Span nroSpan = new Span(nroComp);
        nroSpan.getStyle().set("font-size", "0.8rem").set("color", "var(--lumo-secondary-text-color, #64748b)");

        Div header = new Div(nombreSpan, nroSpan);
        header.getStyle()
                .set("display", "flex").set("flex-direction", "column").set("gap", "2px")
                .set("padding", "12px 16px").set("background", "var(--lumo-contrast-5pct, #f8fafc)")
                .set("border-radius", "10px").set("margin-bottom", "4px")
                .set("border", "1px solid var(--lumo-contrast-20pct, #e2e8f0)").set("width", "100%");

        // ── Accordion: Emisor (abierto por defecto) ───────────────────────────
        Details emisorDetails = new Details("Datos del Emisor",
                crearPanelCamposPanel(
                    crearCampo("Razón Social",   txt(doc.getRazonSocial())),
                    crearCampo("CUIT",           txt(doc.getCuit())),
                    crearCampo("Situación IVA",  txt(doc.getSituacionIva())),
                    crearCampo("Dirección",      txt(doc.getDireccion())),
                    crearCampo("Ciudad",         txt(doc.getCiudad())),
                    crearCampo("Provincia",      txt(doc.getProvincia()))
                ));
        emisorDetails.setOpened(true);
        estilizarDetails(emisorDetails, "#0369a1");

        // ── Accordion: Comprobante ────────────────────────────────────────────
        Details comprobanteDetails = new Details("Datos del Comprobante",
                crearPanelCamposPanel(
                    crearCampo("Cód. ARCA",         txt(doc.getCodigoArca())),
                    crearCampo("Letra",             txt(doc.getLetra())),
                    crearCampo("Centro Emisión",    txt(doc.getCentroEmision())),
                    crearCampo("N° Comprobante",    txt(doc.getNumeroComprobante())),
                    crearCampo("Fecha Emisión",     formatFecha(doc.getFechaEmision())),
                    crearCampo("CAE",               txt(doc.getCae())),
                    crearCampo("Moneda",            txt(doc.getMoneda()))
                ));
        estilizarDetails(comprobanteDetails, "#065f46");

        // ── Accordion: Financiero ─────────────────────────────────────────────
        String totalStr = doc.getTotal() == null ? "—" : "$ " + formatImporte(doc.getTotal());
        Details financieroDetails = new Details("Financiero",
                crearPanelCamposPanel(
                    crearCampo("Total",             totalStr),
                    crearCampo("Neto No Gravado",   doc.getSubTotalNoGravado() == null ? "—" : formatImporte(doc.getSubTotalNoGravado())),
                    crearCampo("Imp. Interno",      doc.getImpuestoInterno() == null   ? "—" : formatImporte(doc.getImpuestoInterno())),
                    crearCampo("Cotización",        doc.getCotizacion() == null        ? "—" : formatImporte(doc.getCotizacion()))
                ));
        estilizarDetails(financieroDetails, "#5b21b6");

        // ── Accordion: Conceptos ──────────────────────────────────────────────
        int nConceptos = doc.getProductosConceptos().size();
        Details conceptosDetails = new Details(
                "Conceptos (" + (nConceptos == 0 ? "ninguno" : nConceptos + " ítem" + (nConceptos == 1 ? "" : "s")) + ")",
                crearMiniGridConceptos(doc));
        estilizarDetails(conceptosDetails, "#92400e");

        // ── Botón descargar JSON ──────────────────────────────────────────────
        String jsonStr = doc.getJsonResultado() != null ? doc.getJsonResultado() : "";
        String nombreArchivo = nombreDoc.replaceAll("\\s+", "_") + ".json";
        StreamResource resource = new StreamResource(nombreArchivo,
                () -> new java.io.ByteArrayInputStream(jsonStr.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        Anchor anchorDescarga = new Anchor(resource, "");
        anchorDescarga.getElement().setAttribute("download", true);
        Button botonDescarga = new Button("Descargar JSON", VaadinIcon.DOWNLOAD.create());
        botonDescarga.getStyle().set("background-color", "#2563eb").set("color", "white");
        botonDescarga.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        anchorDescarga.add(botonDescarga);
        anchorDescarga.getStyle().set("margin-top", "8px");

        // Botón copiar JSON
        Button btnCopiar = new Button("Copiar JSON", VaadinIcon.COPY.create());
        btnCopiar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        btnCopiar.getStyle().set("color", "var(--lumo-body-text-color, #475569)");
        final String jsonParaCopiar = jsonStr;
        btnCopiar.addClickListener(e -> {
            btnCopiar.getElement().executeJs(
                "navigator.clipboard.writeText($0).catch(()=>{});", jsonParaCopiar);
            Toast.success("JSON copiado al portapapeles");
        });

        HorizontalLayout accionesBotones = new HorizontalLayout(anchorDescarga, btnCopiar);
        accionesBotones.setSpacing(true);
        accionesBotones.setAlignItems(FlexComponent.Alignment.CENTER);
        accionesBotones.getStyle().set("margin-top", "8px");

        panelDetalle.add(header, emisorDetails, comprobanteDetails, financieroDetails,
                conceptosDetails, accionesBotones);
    }

    private void estilizarDetails(Details details, String color) {
        details.getStyle()
                .set("border-left", "3px solid " + color)
                .set("border-radius", "0 8px 8px 0")
                .set("padding-left", "4px")
                .set("width", "100%");
        details.getElement().getStyle()
                .set("--details-summary-color", color);
    }

    private VerticalLayout crearPanelCamposPanel(VerticalLayout... campos) {
        VerticalLayout panel = new VerticalLayout();
        panel.setPadding(true);
        panel.setSpacing(false);
        panel.getStyle()
                .set("gap", "8px").set("background", "var(--lumo-contrast-5pct, #f8fafc)")
                .set("border-radius", "6px").set("padding", "10px 12px");
        for (VerticalLayout c : campos) panel.add(c);
        return panel;
    }

    private com.vaadin.flow.component.Component crearMiniGridConceptos(DocumentoConvertido doc) {
        if (doc.getProductosConceptos().isEmpty()) {
            Span vacio = new Span("No hay conceptos registrados");
            vacio.getStyle().set("color", "var(--lumo-tertiary-text-color, #94a3b8)").set("font-size", "0.8rem")
                    .set("padding", "8px 12px").set("display", "block");
            return vacio;
        }
        Grid<ProductoConcepto> mini = new Grid<>(ProductoConcepto.class, false);
        mini.addColumn(ProductoConcepto::getDescripcion).setHeader("Descripción").setFlexGrow(1);
        mini.addColumn(p -> formatImporte(p.getSubTotal())).setHeader("Subtotal").setWidth("90px").setFlexGrow(0);
        mini.setItems(doc.getProductosConceptos());
        mini.setAllRowsVisible(true);
        mini.getStyle().set("font-size", "0.8rem").set("border-radius", "6px");
        return mini;
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
        agregarColumnaFecha(DocumentoConvertido::getFechaConversion, "Fecha de conversión");
    }

    @Override
    protected void actualizarLista() {
        String codigo      = filtrosActivos.getOrDefault("Código", "");
        String nombre      = filtrosActivos.getOrDefault("Nombre", "");
        String cuit        = filtrosActivos.getOrDefault("Cuit del emisor", "");
        String comprobante = filtrosActivos.getOrDefault("Nro. comprobante", "");
        totalRegistros = service.contarFiltrado(codigo, nombre, cuit, "", comprobante);
        grid.setItems(service.listarPaginado(paginaActual, filasPorPagina, codigo, nombre, cuit, "", comprobante));
        actualizarPaginacion();
    }

    @Override
    protected void accionNuevo() { /* no aplica */ }

    @Override
    protected void accionVisualizar(DocumentoConvertido item) {
        DocumentoConvertido doc;
        try {
            doc = service.buscarCompleto(item.getId());
        } catch (Exception ex) {
            Toast.error("Error al cargar el documento: " + ex.getMessage());
            return;
        }

        String nombreDoc = doc.getArchivo() != null ? doc.getArchivo().getNombre() : "Documento";
        String nroComp   = estaVacio(doc.getNumeroComprobante()) ? "" : "  —  Nro. " + doc.getNumeroComprobante();

        Dialog dialog = new Dialog();
        dialog.setWidth("1200px");
        dialog.setHeight("92vh");
        dialog.setResizable(true);
        dialog.setHeaderTitle("Factura · " + nombreDoc + nroComp);

        VerticalLayout contenido = new VerticalLayout();
        contenido.setPadding(false);
        contenido.setSpacing(false);
        contenido.setWidthFull();
        contenido.getStyle().set("overflow-y", "auto").set("padding-right", "4px").set("gap", "0");

        // ── EMISOR ────────────────────────────────────────────────────────────
        Details emisorSection = new Details("Datos del Emisor",
                crearPanelCampos(
                    crearCampo("Razón Social",   txt(doc.getRazonSocial())),
                    crearCampo("CUIT",           txt(doc.getCuit())),
                    crearCampo("Situación IVA",  txt(doc.getSituacionIva())),
                    crearCampo("Dirección",      txt(doc.getDireccion())),
                    crearCampo("Ciudad",         txt(doc.getCiudad())),
                    crearCampo("Cód. Postal",    txt(doc.getCodigoPostal())),
                    crearCampo("Provincia",      txt(doc.getProvincia())),
                    crearCampo("País",           txt(doc.getPais())),
                    crearCampo("Teléfono",       txt(doc.getTelefono())),
                    crearCampo("Mail",           txt(doc.getMail()))
                ));
        emisorSection.setOpened(true);
        aplicarEstiloSeccion(emisorSection, "#0369a1");
        contenido.add(emisorSection);

        // ── COMPROBANTE ───────────────────────────────────────────────────────
        Details comprobanteSection = new Details("Datos del Comprobante",
                crearPanelCampos(
                    crearCampo("Cód. ARCA",         txt(doc.getCodigoArca())),
                    crearCampo("Letra",             txt(doc.getLetra())),
                    crearCampo("Centro de Emisión", txt(doc.getCentroEmision())),
                    crearCampo("N° Comprobante",    txt(doc.getNumeroComprobante())),
                    crearCampo("Fecha Emisión",     formatFecha(doc.getFechaEmision())),
                    crearCampo("CAE",               txt(doc.getCae())),
                    crearCampo("Venc. CAE",         formatFecha(doc.getFechaVencimientoCae())),
                    crearCampo("Moneda",            txt(doc.getMoneda())),
                    crearCampo("Cotización",        doc.getCotizacion() == null ? "—" : formatImporte(doc.getCotizacion())),
                    crearCampo("Orden de Compra",   txt(doc.getOrdenCompra()))
                ));
        comprobanteSection.setOpened(true);
        aplicarEstiloSeccion(comprobanteSection, "#065f46");
        contenido.add(comprobanteSection);

        // ── PRODUCTOS / CONCEPTOS ─────────────────────────────────────────────
        if (!doc.getProductosConceptos().isEmpty()) {
            Grid<ProductoConcepto> g = new Grid<>(ProductoConcepto.class, false);
            g.addColumn(ProductoConcepto::getSku).setHeader("SKU").setWidth("130px").setFlexGrow(0);
            g.addColumn(ProductoConcepto::getDescripcion).setHeader("Descripción").setFlexGrow(1);
            g.addColumn(p -> formatImporte(p.getCantidad())).setHeader("Cant.").setWidth("70px").setFlexGrow(0);
            g.addColumn(p -> formatImporte(p.getPrecioUnitario())).setHeader("P. Unit.").setWidth("95px").setFlexGrow(0);
            g.addColumn(p -> formatImporte(p.getDescuento())).setHeader("Desc.").setWidth("80px").setFlexGrow(0);
            g.addColumn(p -> formatImporte(p.getSubTotal())).setHeader("Subtotal").setWidth("95px").setFlexGrow(0);
            g.addColumn(ProductoConcepto::getAlicuotaIva).setHeader("IVA").setWidth("75px").setFlexGrow(0);
            g.addColumn(ProductoConcepto::getOrdenCompra).setHeader("OC").setWidth("90px").setFlexGrow(0);
            g.addColumn(ProductoConcepto::getRemito).setHeader("Remito").setWidth("130px").setFlexGrow(0);
            g.addColumn(ProductoConcepto::getNumeroDespacho).setHeader("Nro. Despacho").setWidth("170px").setFlexGrow(0);
            g.addColumn(ProductoConcepto::getFechaDespacho).setHeader("Fecha Despacho").setWidth("130px").setFlexGrow(0);
            g.addColumn(ProductoConcepto::getRegistroOficializacion).setHeader("Reg. Ofic.").setWidth("100px").setFlexGrow(0);
            g.setItems(doc.getProductosConceptos());
            g.setAllRowsVisible(true);
            g.getStyle().set("margin-top", "4px");
            Details productosSection = new Details(
                    "Productos / Conceptos (" + doc.getProductosConceptos().size() + ")", g);
            productosSection.setOpened(true);
            aplicarEstiloSeccion(productosSection, "#92400e");
            contenido.add(productosSection);
        }

        // ── NETOS GRAVADOS E IVA ──────────────────────────────────────────────
        if (!doc.getNetosGravados().isEmpty()) {
            Grid<NetoGravado> g = new Grid<>(NetoGravado.class, false);
            g.addColumn(NetoGravado::getAlicuota).setHeader("Alícuota").setWidth("110px").setFlexGrow(0);
            g.addColumn(n -> formatImporte(n.getImporteNetoGravado())).setHeader("Importe Neto Gravado").setFlexGrow(1);
            g.addColumn(n -> formatImporte(n.getIva())).setHeader("IVA").setWidth("130px").setFlexGrow(0);
            g.setItems(doc.getNetosGravados());
            g.setAllRowsVisible(true);
            Details netosSection = new Details("Netos Gravados e IVA", g);
            netosSection.setOpened(true);
            aplicarEstiloSeccion(netosSection, "#5b21b6");
            contenido.add(netosSection);
        }

        // ── PERCEPCIONES IIBB ─────────────────────────────────────────────────
        if (!doc.getPercepcionesIIBB().isEmpty()) {
            Grid<PercepcionIIBB> g = new Grid<>(PercepcionIIBB.class, false);
            g.addColumn(PercepcionIIBB::getProvincia).setHeader("Provincia").setFlexGrow(1);
            g.addColumn(PercepcionIIBB::getAlicuota).setHeader("Alícuota").setWidth("110px").setFlexGrow(0);
            g.addColumn(p -> formatImporte(p.getImporte())).setHeader("Importe").setWidth("130px").setFlexGrow(0);
            g.setItems(doc.getPercepcionesIIBB());
            g.setAllRowsVisible(true);
            Details iibbSection = new Details("Percepciones IIBB", g);
            aplicarEstiloSeccion(iibbSection, "#9f1239");
            contenido.add(iibbSection);
        }

        // ── PERCEPCIONES IVA ──────────────────────────────────────────────────
        if (!doc.getPercepcionesIVA().isEmpty()) {
            Grid<PercepcionIVA> g = new Grid<>(PercepcionIVA.class, false);
            g.addColumn(PercepcionIVA::getAlicuota).setHeader("Alícuota").setWidth("150px").setFlexGrow(0);
            g.addColumn(p -> formatImporte(p.getImporte())).setHeader("Importe").setFlexGrow(1);
            g.setItems(doc.getPercepcionesIVA());
            g.setAllRowsVisible(true);
            Details ivaSection = new Details("Percepciones IVA", g);
            aplicarEstiloSeccion(ivaSection, "#9f1239");
            contenido.add(ivaSection);
        }

        // ── TASAS ─────────────────────────────────────────────────────────────
        if (!doc.getTasas().isEmpty()) {
            Grid<Tasa> g = new Grid<>(Tasa.class, false);
            g.addColumn(Tasa::getDescripcion).setHeader("Descripción").setFlexGrow(1);
            g.addColumn(tasa -> formatImporte(tasa.getImporte())).setHeader("Importe").setWidth("130px").setFlexGrow(0);
            g.setItems(doc.getTasas());
            g.setAllRowsVisible(true);
            Details tasasSection = new Details("Tasas", g);
            aplicarEstiloSeccion(tasasSection, "#065f46");
            contenido.add(tasasSection);
        }

        // ── DESCUENTOS / RECARGOS ─────────────────────────────────────────────
        if (!doc.getDescuentosRecargos().isEmpty()) {
            Grid<DescuentoRecargo> g = new Grid<>(DescuentoRecargo.class, false);
            g.addColumn(DescuentoRecargo::getDescripcion).setHeader("Descripción").setFlexGrow(1);
            g.addColumn(DescuentoRecargo::getAlicuota).setHeader("Alícuota").setWidth("110px").setFlexGrow(0);
            g.addColumn(d -> formatImporte(d.getImporte())).setHeader("Importe").setWidth("130px").setFlexGrow(0);
            g.setItems(doc.getDescuentosRecargos());
            g.setAllRowsVisible(true);
            Details descSection = new Details("Descuentos / Recargos", g);
            aplicarEstiloSeccion(descSection, "#b45309");
            contenido.add(descSection);
        }

        // ── VENCIMIENTOS ──────────────────────────────────────────────────────
        if (!doc.getVencimientos().isEmpty()) {
            Grid<Vencimiento> g = new Grid<>(Vencimiento.class, false);
            g.addColumn(Vencimiento::getFecha).setHeader("Fecha").setWidth("150px").setFlexGrow(0);
            g.addColumn(v -> formatImporte(v.getImporte())).setHeader("Importe").setFlexGrow(1);
            g.setItems(doc.getVencimientos());
            g.setAllRowsVisible(true);
            Details vencSection = new Details("Vencimientos", g);
            aplicarEstiloSeccion(vencSection, "#0369a1");
            contenido.add(vencSection);
        }

        // ── TOTALES ───────────────────────────────────────────────────────────
        VerticalLayout panelTotales = new VerticalLayout();
        panelTotales.setSpacing(false);
        panelTotales.setPadding(true);
        panelTotales.setWidth("360px");
        panelTotales.getStyle()
                .set("background", "var(--lumo-contrast-5pct, #f8fafc)").set("border-radius", "10px")
                .set("border", "1px solid var(--lumo-contrast-20pct, #e2e8f0)").set("margin-left", "auto").set("gap", "6px");
        if (doc.getSubTotalNoGravado() != null)
            panelTotales.add(crearFilaTotal("Neto No Gravado", formatImporte(doc.getSubTotalNoGravado()), false));
        if (doc.getImpuestoInterno() != null)
            panelTotales.add(crearFilaTotal("Impuesto Interno / Otros Tributos", formatImporte(doc.getImpuestoInterno()), false));
        panelTotales.add(crearFilaTotal("TOTAL", doc.getTotal() == null ? "—" : formatImporte(doc.getTotal()), true));
        Details totalesSection = new Details("Totales", panelTotales);
        totalesSection.setOpened(true);
        aplicarEstiloSeccion(totalesSection, "#1e293b");
        contenido.add(totalesSection);

        // ── JSON viewer (colapsado como Details) ──────────────────────────────
        String json = doc.getJsonResultado() != null ? doc.getJsonResultado() : "";
        Pre jsonPre = new Pre(json);
        jsonPre.getStyle()
                .set("background-color", "#1e1e2e").set("color", "#cdd6f4").set("border", "none")
                .set("border-radius", "12px").set("padding", "20px")
                .set("font-family", "'JetBrains Mono', 'Fira Code', 'Courier New', monospace")
                .set("font-size", "13px").set("width", "100%").set("white-space", "pre-wrap")
                .set("word-break", "break-word").set("box-shadow", "inset 0 2px 8px rgba(0,0,0,0.4)")
                .set("line-height", "1.6").set("overflow-y", "auto").set("max-height", "340px");
        Details jsonSection = new Details("Resultado JSON", jsonPre);
        aplicarEstiloSeccion(jsonSection, "#334155");
        contenido.add(jsonSection);

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

        Button btnCopiarJson = new Button("Copiar JSON", VaadinIcon.COPY.create());
        btnCopiarJson.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        final String jsonACopiar = json;
        btnCopiarJson.addClickListener(e -> {
            btnCopiarJson.getElement().executeJs(
                "navigator.clipboard.writeText($0).catch(()=>{});", jsonACopiar);
            Toast.success("JSON copiado al portapapeles");
        });

        dialog.getFooter().add(anchorDescarga, btnCopiarJson, btnCerrar);
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
                Toast.success(getTranslation("app.borrar.exito"));
            } catch (Exception e) {
                Toast.error("Error al borrar: " + e.getMessage());
            }
        });

        dialog.open();
    }

    private String txt(String valor) {
        return estaVacio(valor) ? "—" : valor;
    }

    private void aplicarEstiloSeccion(Details details, String color) {
        details.getStyle()
                .set("border-left", "3px solid " + color)
                .set("padding-left", "6px")
                .set("margin-top", "8px")
                .set("width", "100%");
    }

    private VerticalLayout crearCampo(String etiqueta, String valor) {
        com.vaadin.flow.component.html.Span lbl = new com.vaadin.flow.component.html.Span(etiqueta);
        lbl.getStyle()
                .set("font-size", "0.68rem").set("font-weight", "600")
                .set("color", "var(--lumo-tertiary-text-color, #94a3b8)").set("text-transform", "uppercase")
                .set("letter-spacing", "0.05em");
        com.vaadin.flow.component.html.Span val = new com.vaadin.flow.component.html.Span(valor);
        val.getStyle()
                .set("font-size", "0.875rem").set("font-weight", "500")
                .set("color", "var(--lumo-header-text-color, #1e293b)");
        VerticalLayout campo = new VerticalLayout(lbl, val);
        campo.setPadding(false);
        campo.setSpacing(false);
        campo.setWidth("auto");
        campo.getStyle().set("gap", "1px").set("flex-shrink", "0").set("min-width", "110px");
        return campo;
    }

    private HorizontalLayout crearPanelCampos(VerticalLayout... campos) {
        HorizontalLayout panel = new HorizontalLayout();
        panel.setWidthFull();
        panel.setSpacing(false);
        panel.getStyle()
                .set("flex-wrap", "wrap").set("gap", "12px 24px")
                .set("padding", "12px 16px").set("background", "var(--lumo-contrast-5pct, #f8fafc)")
                .set("border-radius", "8px");
        for (VerticalLayout c : campos) panel.add(c);
        return panel;
    }

    private HorizontalLayout crearFilaTotal(String label, String valor, boolean destacado) {
        com.vaadin.flow.component.html.Span lbl = new com.vaadin.flow.component.html.Span(label);
        lbl.getStyle()
                .set("font-size", destacado ? "1rem" : "0.875rem")
                .set("font-weight", destacado ? "700" : "500")
                .set("color", destacado ? "#0c4a6e" : "var(--lumo-body-text-color, #475569)");
        com.vaadin.flow.component.html.Span val = new com.vaadin.flow.component.html.Span(valor);
        val.getStyle()
                .set("font-size", destacado ? "1.1rem" : "0.875rem")
                .set("font-weight", "700")
                .set("color", destacado ? "#002060" : "var(--lumo-header-text-color, #1e293b)");
        HorizontalLayout row = new HorizontalLayout(lbl, val);
        row.setWidthFull();
        row.setSpacing(false);
        row.getStyle().set("justify-content", "space-between").set("align-items", "center");
        if (destacado) {
            row.getStyle()
                    .set("border-top", "2px solid #cbd5e1")
                    .set("padding-top", "8px").set("margin-top", "4px");
        }
        return row;
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
        if (doc.getFechaEmision() == null)           notas.append("• No se encontró la fecha de emisión.\n");
        if (estaVacio(doc.getCae()))                 notas.append("• No se encontró el CAE.\n");
        if (doc.getFechaVencimientoCae() == null)    notas.append("• No se encontró la fecha de vencimiento del CAE.\n");
        if (estaVacio(doc.getMoneda()))             notas.append("• No se encontró la moneda.\n");
        if (doc.getCotizacion() == null)             notas.append("• No se encontró la cotización.\n");
        if (estaVacio(doc.getOrdenCompra()))        notas.append("• No se encontró la orden de compra.\n");
        if (doc.getProductosConceptos().isEmpty())  notas.append("• No se encontraron productos/conceptos.\n");
        if (doc.getNetosGravados().isEmpty())       notas.append("• No se encontraron netos gravados e IVA.\n");
        if (doc.getSubTotalNoGravado() == null)      notas.append("• No se encontró el importe neto no gravado.\n");
        if (doc.getImpuestoInterno() == null)        notas.append("• No se encontró impuesto interno / otros tributos.\n");
        if (doc.getPercepcionesIIBB().isEmpty())    notas.append("• No se encontraron percepciones de IIBB.\n");
        if (doc.getPercepcionesIVA().isEmpty())      notas.append("• No se encontraron percepciones de IVA.\n");
        if (doc.getTasas().isEmpty())                notas.append("• No se encontraron tasas.\n");
        if (doc.getTotal() == null)                 notas.append("• No se encontró el total del comprobante.\n");
        if (doc.getVencimientos().isEmpty())        notas.append("• No se encontraron vencimientos.\n");
        return notas.toString();
    }


    private boolean estaVacio(String valor) {
        return valor == null || valor.isEmpty() || valor.equals("null");
    }

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static String formatFecha(LocalDate fecha) {
        return fecha == null ? "—" : fecha.format(FMT_FECHA);
    }

    private static String formatImporte(BigDecimal valor) {
        if (valor == null) return "";
        return NumberFormat.getNumberInstance(new Locale("es", "AR")).format(valor);
    }
}
