package com.desarrollos.combos;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.desarrollos.entities.Archivo;
import com.desarrollos.services.ArchivoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

public class ArchivoCombo extends HorizontalLayout {

    private final ComboBox<Archivo> combo = new ComboBox<>();
    private final Button btnBuscar = new Button(VaadinIcon.SEARCH.create());
    private final ArchivoService archivoService;

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

        // Filtrado al escribir
        combo.addCustomValueSetListener(e -> cargarOpciones(e.getDetail()));

        // Carga inicial completa
        cargarOpciones("");

        // ── Botón lupa ────────────────────────────────────────────────────────
        btnBuscar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnBuscar.getStyle()
                .set("color", "#002060")
                .set("cursor", "pointer");
        btnBuscar.getElement().addEventListener("mouseover",
                e -> btnBuscar.getStyle().set("color", "#00aaff"));
        btnBuscar.getElement().addEventListener("mouseout",
                e -> btnBuscar.getStyle().set("color", "#002060"));

        // Al hacer click en la lupa recarga todas las opciones y abre el dropdown
        btnBuscar.addClickListener(e -> abrirDialogoBusqueda());

        add(combo, btnBuscar);
    }

    private void cargarOpciones(String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();

        List<Archivo> opciones = archivoService.listarNoConvertidos()
                .stream()
                .filter(a -> f.isEmpty()
                        || (a.getCodigo() != null && a.getCodigo().toLowerCase().contains(f))
                        || (a.getNombre() != null && a.getNombre().toLowerCase().contains(f)))
                .collect(Collectors.toList());

        combo.setItems(opciones);
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
        cargarOpciones("");
    }
    
    private void abrirDialogoBusqueda() {
    Dialog dialog = new Dialog();
    dialog.setWidth("800px");
    dialog.setHeight("500px");
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
            .setWidth("120px")
            .setFlexGrow(0);

    grilla.addColumn(Archivo::getNombre)
            .setHeader("Nombre")
            .setSortable(true)
            .setFlexGrow(1);

    grilla.addColumn(Archivo::getNombreOriginal)
            .setHeader("Archivo Original")
            .setSortable(true)
            .setFlexGrow(1);

    grilla.addColumn(archivo -> archivo.getFechaCreacion() != null
                    ? archivo.getFechaCreacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "-")
            .setHeader("Fecha Creación")
            .setSortable(true)
            .setWidth("140px")
            .setFlexGrow(0);

    grilla.setItems(archivoService.listarNoConvertidos());

    // ── Single click guarda selección para el botón Seleccionar ──────────
    final Archivo[] seleccionado = {null};
    grilla.addSelectionListener(e ->
            seleccionado[0] = e.getFirstSelectedItem().orElse(null));

    // ── Doble click selecciona y cierra ───────────────────────────────────
    grilla.addItemDoubleClickListener(e -> {
        combo.setItems(archivoService.listarNoConvertidos());
        combo.setValue(e.getItem());
        dialog.close();
    });

    // ── Buscador dentro del dialog ────────────────────────────────────────
    TextField buscadorDialog = new TextField();
    buscadorDialog.setPlaceholder("Buscar por código o nombre...");
    buscadorDialog.setPrefixComponent(VaadinIcon.SEARCH.create());
    buscadorDialog.setWidth("100%");
    buscadorDialog.setClearButtonVisible(true);
    buscadorDialog.setValueChangeMode(ValueChangeMode.LAZY);
    buscadorDialog.addValueChangeListener(ev -> {
        String f = ev.getValue().trim().toLowerCase();
        grilla.setItems(archivoService.listarNoConvertidos().stream()
                .filter(a -> f.isEmpty()
                        || (a.getCodigo() != null && a.getCodigo().toLowerCase().contains(f))
                        || (a.getNombre() != null && a.getNombre().toLowerCase().contains(f)))
                .collect(Collectors.toList()));
    });

    VerticalLayout contenido = new VerticalLayout(buscadorDialog, grilla);
    contenido.setSizeFull();
    contenido.setPadding(false);
    contenido.expand(grilla);

    dialog.add(contenido);

    // ── Botones footer ────────────────────────────────────────────────────
    Button btnCerrar = new Button("Cancelar", ev -> dialog.close());
    btnCerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    Button btnSeleccionar = new Button("Seleccionar", VaadinIcon.CHECK.create(), ev -> {
        if (seleccionado[0] != null) {
            combo.setItems(archivoService.listarNoConvertidos());
            combo.setValue(seleccionado[0]);
            dialog.close();
        }
    });
    btnSeleccionar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

    dialog.getFooter().add(btnCerrar, btnSeleccionar);

    dialog.open();
}
}