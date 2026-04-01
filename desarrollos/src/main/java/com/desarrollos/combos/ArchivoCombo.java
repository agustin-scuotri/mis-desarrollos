package com.desarrollos.combos;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

import com.desarrollos.entities.Archivo;
import com.desarrollos.services.ArchivoService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;

public class ArchivoCombo extends HorizontalLayout {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ComboBox<Archivo> combo = new ComboBox<>();
    private final Button btnBuscar = new Button(VaadinIcon.SEARCH.create());
    private final Div wrapperLupa = new Div();
    private final ArchivoService archivoService;
    private DataProvider<Archivo, String> dataProvider;

    public ArchivoCombo(ArchivoService archivoService) {
        this.archivoService = archivoService;
        configurar();
    }

    private void configurar() {
        setAlignItems(Alignment.END);
        setSpacing(true);
        setPadding(false);

        // ── ComboBox ──────────────────────────────────────────────────────────
        combo.setWidth("400px");
        combo.setClearButtonVisible(true);
        combo.setItemLabelGenerator(a -> a.getCodigo() + " - " + a.getNombre());

        dataProvider = DataProvider.fromFilteringCallbacks(
            (CallbackDataProvider.FetchCallback<Archivo, String>) query -> {
                String f = query.getFilter().orElse("").trim().toLowerCase();
                return archivoService.listarNoConvertidos().stream()
                    .filter(a -> matchesFiltro(a, f))
                    .sorted(Comparator.comparing(Archivo::getFechaCreacion,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .skip(query.getOffset())
                    .limit(query.getLimit());
            },
            (CallbackDataProvider.CountCallback<Archivo, String>) query -> {
                String f = query.getFilter().orElse("").trim().toLowerCase();
                return (int) archivoService.listarNoConvertidos().stream()
                    .filter(a -> matchesFiltro(a, f))
                    .count();
            }
        );
        combo.setItems(dataProvider);

        // ── Botón lupa ────────────────────────────────────────────────────────
        btnBuscar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnBuscar.getStyle()
                .set("color", "var(--section-title-color, #002060)")
                .set("cursor", "pointer");
        btnBuscar.getElement().addEventListener("mouseover",
                e -> btnBuscar.getStyle().set("color", "#00aaff"));
        btnBuscar.getElement().addEventListener("mouseout",
                e -> btnBuscar.getStyle().set("color", "var(--section-title-color, #002060)"));
        btnBuscar.addClickListener(e -> abrirDialogoBusqueda());

        wrapperLupa.getStyle()
                .set("position", "relative")
                .set("display", "inline-block")
                .set("align-self", "flex-end");
        wrapperLupa.add(btnBuscar);

        actualizarBadge();

        add(combo, wrapperLupa);
    }

    // ── API pública ───────────────────────────────────────────────────────────
    public Archivo getValor() {
        return combo.getValue();
    }

    public void setLabel(String label) {
        combo.setLabel(label);
    }

    public void setPlaceholder(String placeholder) {
        combo.setPlaceholder(placeholder);
    }

    public void limpiar() {
        combo.clear();
    }

    public ComboBox<Archivo> getCombo() {
        return combo;
    }

    public void refrescar() {
        dataProvider.refreshAll();
        actualizarBadge();
    }

    // ── Helpers privados ──────────────────────────────────────────────────────
    private void actualizarBadge() {
        int count = archivoService.listarNoConvertidos().size();
        btnBuscar.setEnabled(count > 0);
        combo.setEnabled(count > 0);
        btnBuscar.getElement().setAttribute("title",
                count == 0 ? "No hay archivos pendientes"
                           : count + " archivo(s) pendiente(s)");
    }

    private boolean matchesFiltro(Archivo a, String f) {
        return f.isEmpty()
            || (a.getCodigo() != null && a.getCodigo().toLowerCase().contains(f))
            || (a.getNombre() != null && a.getNombre().toLowerCase().contains(f));
    }

    private void abrirDialogoBusqueda() {
        Dialog dialog = new Dialog();
        dialog.setWidth("850px");
        dialog.setMaxWidth("95vw");
        dialog.setHeight("560px");
        dialog.setHeaderTitle(getTranslation("conversor.seleccione.archivo"));

        // ── Grilla ────────────────────────────────────────────────────────────
        Grid<Archivo> grilla = new Grid<>(Archivo.class, false);
        grilla.setSizeFull();
        grilla.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        grilla.getStyle()
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 8px rgba(0,32,96,0.1)");

        grilla.addColumn(Archivo::getCodigo)
                .setHeader("Código")
                .setSortable(true)
                .setWidth("90px")
                .setFlexGrow(0);

        grilla.addColumn(Archivo::getNombre)
                .setHeader("Nombre")
                .setSortable(true)
                .setFlexGrow(1);

        grilla.addColumn(Archivo::getNombreOriginal)
                .setHeader("Archivo")
                .setSortable(true)
                .setFlexGrow(1);

        // ── Columna Estado ────────────────────────────────────────────────────
        grilla.addComponentColumn(archivo -> {
            Span estadoBadge = new Span("Pendiente");
            estadoBadge.getStyle()
                    .set("background", "#fef3c7")
                    .set("color", "#92400e")
                    .set("padding", "2px 8px")
                    .set("border-radius", "9999px")
                    .set("font-size", "11px")
                    .set("font-weight", "600");
            return estadoBadge;
        }).setHeader("Estado").setWidth("110px").setFlexGrow(0);

        // ── Columna Fecha ─────────────────────────────────────────────────────
        grilla.addColumn(a -> a.getFechaCreacion() != null
                        ? a.getFechaCreacion().format(FMT) : "")
                .setHeader("Fecha")
                .setSortable(true)
                .setWidth("135px")
                .setFlexGrow(0);

        // ── Panel de preview lateral ──────────────────────────────────────────
        VerticalLayout previewPanel = new VerticalLayout();
        previewPanel.setVisible(false);
        previewPanel.setWidth("480px");
        previewPanel.setPadding(true);
        previewPanel.setSpacing(false);
        previewPanel.getStyle()
                .set("border-left", "1px solid #e2e8f0")
                .set("background", "#f8fafc")
                .set("border-radius", "0 8px 8px 0")
                .set("overflow", "auto")
                .set("flex-shrink", "0");

        Div previewContainer = new Div();
        previewContainer.setWidthFull();
        previewContainer.getStyle().set("flex", "1").set("overflow", "auto");

        Button btnCerrarPreview = new Button(VaadinIcon.CLOSE_SMALL.create(), ev -> {
            previewPanel.setVisible(false);
            dialog.setWidth("850px");
        });
        btnCerrarPreview.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        Span previewTitulo = new Span("Vista previa");
        previewTitulo.getStyle().set("font-weight", "600").set("color", "#374151");

        HorizontalLayout previewHeader = new HorizontalLayout(previewTitulo, btnCerrarPreview);
        previewHeader.setWidthFull();
        previewHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        previewHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        previewHeader.getStyle().set("margin-bottom", "8px");

        previewPanel.add(previewHeader, previewContainer);
        previewPanel.expand(previewContainer);

        // ── Columna Ver (preview inline) ──────────────────────────────────────
        grilla.addComponentColumn(archivo -> {
            Icon icono = VaadinIcon.EYE.create();
            icono.getStyle().set("color", "#2563eb");
            icono.setSize("17px");
            Button btn = new Button(icono);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getElement().setAttribute("title", "Vista previa");
            btn.addClickListener(e ->
                    mostrarPreview(archivo, previewContainer, previewPanel, dialog));
            return btn;
        }).setHeader("Ver").setWidth("60px").setFlexGrow(0);

        // ── DataProvider server-side para el grid ─────────────────────────────
        final String[] filtroActual = {""};
        CallbackDataProvider<Archivo, Void> gridProvider = DataProvider.fromCallbacks(
            query -> archivoService.listarNoConvertidos().stream()
                .filter(a -> matchesFiltro(a, filtroActual[0]))
                .sorted(Comparator.comparing(Archivo::getFechaCreacion,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .skip(query.getOffset())
                .limit(query.getLimit()),
            query -> (int) archivoService.listarNoConvertidos().stream()
                .filter(a -> matchesFiltro(a, filtroActual[0]))
                .count()
        );
        grilla.setItems(gridProvider);
        grilla.setEmptyStateText("No hay archivos pendientes de conversión");

        // ── Selección ─────────────────────────────────────────────────────────
        final Archivo[] seleccionado = {null};
        grilla.addSelectionListener(e ->
                seleccionado[0] = e.getFirstSelectedItem().orElse(null));

        grilla.addItemDoubleClickListener(e -> {
            dataProvider.refreshAll();
            combo.setValue(e.getItem());
            dialog.close();
        });

        // ── Buscador ──────────────────────────────────────────────────────────
        TextField buscadorDialog = new TextField();
        buscadorDialog.setPlaceholder("Buscar por código o nombre...");
        buscadorDialog.setPrefixComponent(VaadinIcon.SEARCH.create());
        buscadorDialog.setWidth("100%");
        buscadorDialog.setClearButtonVisible(true);
        buscadorDialog.setValueChangeMode(ValueChangeMode.LAZY);
        buscadorDialog.addValueChangeListener(ev -> {
            filtroActual[0] = ev.getValue().trim().toLowerCase();
            gridProvider.refreshAll();
        });

        // ── Layout ────────────────────────────────────────────────────────────
        VerticalLayout leftPanel = new VerticalLayout(buscadorDialog, grilla);
        leftPanel.setSizeFull();
        leftPanel.setPadding(false);
        leftPanel.expand(grilla);

        HorizontalLayout mainLayout = new HorizontalLayout(leftPanel, previewPanel);
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);
        mainLayout.expand(leftPanel);

        dialog.add(mainLayout);

        // ── Footer ────────────────────────────────────────────────────────────
        Button btnCerrar = new Button("Cancelar", ev -> dialog.close());
        btnCerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button btnSeleccionar = new Button("Seleccionar", VaadinIcon.CHECK.create(), ev -> {
            if (seleccionado[0] != null) {
                dataProvider.refreshAll();
                combo.setValue(seleccionado[0]);
                dialog.close();
            }
        });
        btnSeleccionar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        dialog.getFooter().add(btnCerrar, btnSeleccionar);

        // ── Shortcuts: Enter selecciona; Escape cierra ────────────────────────
        Shortcuts.addShortcutListener(grilla, e -> {
            if (seleccionado[0] != null) {
                dataProvider.refreshAll();
                combo.setValue(seleccionado[0]);
                dialog.close();
            }
        }, Key.ENTER);

        dialog.open();
        buscadorDialog.focus();
    }

    private void mostrarPreview(Archivo archivo, Div previewContainer,
                                VerticalLayout previewPanel, Dialog dialog) {
        try {
            Archivo completo = archivoService.buscarPorIdConContenido(archivo.getId());
            if (completo.getContenido() == null) {
                Notification.show("El archivo no tiene contenido adjunto")
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            StreamResource res = new StreamResource(
                    completo.getNombreOriginal(),
                    () -> new ByteArrayInputStream(completo.getContenido()));
            var reg = VaadinSession.getCurrent().getResourceRegistry().registerResource(res);
            String url = reg.getResourceUri().toString();

            previewContainer.removeAll();
            String nombreLower = completo.getNombreOriginal().toLowerCase();

            if (nombreLower.endsWith(".pdf")) {
                com.vaadin.flow.dom.Element iframe = new com.vaadin.flow.dom.Element("iframe");
                iframe.setAttribute("src", url);
                iframe.getStyle()
                        .set("width", "100%")
                        .set("height", "430px")
                        .set("border", "none")
                        .set("border-radius", "6px");
                Div wrapper = new Div();
                wrapper.setWidthFull();
                wrapper.getElement().appendChild(iframe);
                previewContainer.add(wrapper);
            } else if (nombreLower.endsWith(".png") || nombreLower.endsWith(".jpg")
                    || nombreLower.endsWith(".jpeg") || nombreLower.endsWith(".webp")) {
                Image img = new Image(url, "Vista previa");
                img.setWidth("100%");
                img.getStyle().set("border-radius", "6px").set("object-fit", "contain");
                previewContainer.add(img);
            } else {
                Span msg = new Span("No se puede previsualizar este tipo de archivo.");
                msg.getStyle().set("color", "#6b7280").set("font-size", "13px");
                previewContainer.add(msg);
            }

            previewPanel.setVisible(true);
            dialog.setWidth("1370px");
        } catch (Exception ex) {
            Notification.show("Error al previsualizar: " + ex.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
