package com.desarrollos.base;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.ValueProvider;

public abstract class CrudView<T> extends VerticalLayout {

    protected Grid<T> grid;
    protected Button btnNuevo;
    protected H2 tituloPrograma;
    protected HorizontalLayout barraHerramientas;
    protected Button btnConfiguracion;
    protected HeaderRow filaFiltros;
    protected Map<String, String> filtrosActivos = new HashMap<>();

    public CrudView(Class<T> claseEntidad) {
        setSizeFull();
        setSpacing(true);
        setPadding(true);
        getStyle().set("font-family", "Verdana, sans-serif");

        configurarComponentes(claseEntidad);
        add(barraHerramientas, grid);
    }

    private void configurarComponentes(Class<T> claseEntidad) {

        // ── Título ────────────────────────────────────────────────────────────
        tituloPrograma = new H2();
        tituloPrograma.getStyle()
                .set("margin", "0")
                .set("font-size", "1.5rem")
                .set("font-weight", "bold")
                .set("color", "#002060")
                .set("font-family", "Verdana, sans-serif");

        // ── Botón configuración ───────────────────────────────────────────────
        Icon iconoConfig = VaadinIcon.OPTIONS.create();
        iconoConfig.getStyle().set("color", "#002060");
        iconoConfig.setSize("20px");

        btnConfiguracion = new Button(iconoConfig);
        btnConfiguracion.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnConfiguracion.getStyle()
                .set("margin-right", "15px")
                .set("cursor", "pointer")
                .set("transition", "color 0.2s");
        btnConfiguracion.getElement().addEventListener("mouseover",
                e -> btnConfiguracion.getStyle().set("color", "#00aaff"));
        btnConfiguracion.getElement().addEventListener("mouseout",
                e -> btnConfiguracion.getStyle().set("color", "#002060"));
        btnConfiguracion.addClickListener(e -> abrirDialogoColumnas());

        HorizontalLayout layoutTitulo = new HorizontalLayout(btnConfiguracion, tituloPrograma);
        layoutTitulo.setAlignItems(Alignment.CENTER);
        layoutTitulo.setSpacing(false);

        // ── Botón Nuevo ───────────────────────────────────────────────────────
        btnNuevo = new Button(getTranslation("app.agregar"), VaadinIcon.PLUS.create());
        btnNuevo.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        btnNuevo.addClickListener(e -> accionNuevo());
        btnNuevo.getStyle().set("font-family", "Verdana, sans-serif");

        // ── Barra herramientas ────────────────────────────────────────────────
        barraHerramientas = new HorizontalLayout(layoutTitulo, btnNuevo);
        barraHerramientas.setWidthFull();
        barraHerramientas.setJustifyContentMode(JustifyContentMode.BETWEEN);
        barraHerramientas.setAlignItems(Alignment.CENTER);

        // ── Grilla ────────────────────────────────────────────────────────────
        grid = new Grid<>(claseEntidad);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        grid.getStyle()
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 8px rgba(0,32,96,0.1)")
                .set("font-family", "Verdana, sans-serif");

        // ── Mensaje estado vacío ──────────────────────────────────────────────
        VerticalLayout layoutVacio = new VerticalLayout();
        layoutVacio.setSizeFull();
        layoutVacio.setJustifyContentMode(JustifyContentMode.CENTER);
        layoutVacio.setAlignItems(Alignment.CENTER);
        layoutVacio.getStyle().set("opacity", "0.8");

        Span mensajeVacio = new Span("No existen archivos");
        mensajeVacio.getStyle()
                .set("color", "red")
                .set("font-weight", "bold")
                .set("font-size", "1.2rem")
                .set("font-family", "Verdana, sans-serif");

        layoutVacio.add(mensajeVacio);
        grid.setEmptyStateComponent(layoutVacio);

        // ── CSS interno de la grilla ──────────────────────────────────────────
        grid.getElement().executeJs(
                "const grid = this;" +
                "const style = document.createElement('style');" +
                "style.innerHTML = `" +
                "  vaadin-grid-cell-content {" +
                "    text-align: center;" +
                "    justify-content: center;" +
                "    display: flex;" +
                "    font-family: Verdana, sans-serif;" +
                "  }" +
                "  [part~='header-cell'] {" +
                "    border-bottom: 2px solid rgba(0,32,96,0.15) !important;" +
                "    background-color: #f0f4ff !important;" +
                "    font-weight: bold;" +
                "  }" +
                "  [part~='cell']:not([part~='header-cell']) {" +
                "    border-right: 1px solid rgba(0,32,96,0.08) !important;" +
                "  }" +
                "  [part~='row']:hover > [part~='cell'] {" +
                "    background-color: rgba(0,32,96,0.05) !important;" +
                "  }" +
                // ── NUEVO: elimina el resaltado de fila seleccionada ──
                "  [part~='row'][selected] > [part~='cell'] {" +
                "    background-color: transparent !important;" +
                "  }" +
                "  [part~='row'][selected]:hover > [part~='cell'] {" +
                "    background-color: rgba(0,32,96,0.05) !important;" +
                "  }" +
                "`;" +
                "grid.shadowRoot.appendChild(style);");
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        // ── Columna acciones ──────────────────────────────────────────────────
        configurarColumnasEspecificas();

        grid.addComponentColumn(item -> crearBotonesAccion(item))
                .setHeader(getTranslation("archivo.acciones"))
                .setKey("acciones")
                .setFrozenToEnd(true)
                .setWidth("150px")
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);
    }

    // ── Columna texto con filtro ──────────────────────────────────────────────
    protected Grid.Column<T> agregarColumna(ValueProvider<T, ?> valueProvider, String cabecera) {
        com.vaadin.flow.component.textfield.TextField filtro =
                new com.vaadin.flow.component.textfield.TextField();
        filtro.setClearButtonVisible(true);
        filtro.addThemeVariants(com.vaadin.flow.component.textfield.TextFieldVariant.LUMO_SMALL);
        filtro.setWidthFull();
        filtro.getStyle()
                .set("text-align", "left")
                .set("font-family", "Verdana, sans-serif");
        filtro.getElement().executeJs("this.inputElement.style.textAlign = 'left'");

        if (cabecera.equalsIgnoreCase("Código") || cabecera.toLowerCase().contains("codigo")) {
            filtro.setAllowedCharPattern("[0-9]");
            filtro.getElement().executeJs(
                    "this.inputElement.addEventListener('input', function(e) {" +
                    "  this.value = this.value.replace(/[^0-9]/g, '');" +
                    "});");
            filtro.getElement().setAttribute("inputmode", "numeric");
        }

        filtro.addValueChangeListener(e -> ejecutarFiltro(cabecera, e.getValue()));

        Span textoCabecera = new Span(cabecera);
        textoCabecera.getStyle()
                .set("font-weight", "bold")
                .set("color", "#002060")
                .set("font-family", "Verdana, sans-serif");

        VerticalLayout layoutCabecera = new VerticalLayout(textoCabecera, filtro);
        layoutCabecera.setAlignItems(Alignment.CENTER);
        layoutCabecera.setSpacing(false);
        layoutCabecera.setPadding(false);

        return grid.addColumn(valueProvider)
                .setHeader(layoutCabecera)
                .setKey(cabecera)
                .setTextAlign(ColumnTextAlign.CENTER)
                .setSortable(true)
                .setAutoWidth(true);
    }

    // ── Columna fecha ─────────────────────────────────────────────────────────
    protected Grid.Column<T> agregarColumnaFecha(ValueProvider<T, LocalDateTime> valueProvider, String cabecera) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        Span textoCabecera = new Span(cabecera);
        textoCabecera.getStyle()
                .set("font-weight", "bold")
                .set("color", "#002060")
                .set("font-family", "Verdana, sans-serif");

        VerticalLayout layoutCabecera = new VerticalLayout(textoCabecera);
        layoutCabecera.setAlignItems(Alignment.CENTER);
        layoutCabecera.setSpacing(false);
        layoutCabecera.setPadding(false);

        return grid.addColumn(item -> {
            LocalDateTime fecha = valueProvider.apply(item);
            return fecha != null ? fecha.format(formatter) : "";
        })
        .setHeader(layoutCabecera)
        .setKey(cabecera)
        .setTextAlign(ColumnTextAlign.CENTER)
        .setSortable(true)
        .setAutoWidth(true);
    }

    // ── Columna booleana con ícono ────────────────────────────────────────────
    protected Grid.Column<T> agregarColumnaBooleana(ValueProvider<T, Boolean> valueProvider, String cabecera) {
        com.vaadin.flow.component.combobox.ComboBox<String> filtro =
                new com.vaadin.flow.component.combobox.ComboBox<>();
        filtro.setItems("Todos", "Sí", "No");
        filtro.setValue("Todos");
        filtro.setClearButtonVisible(true);
        filtro.addThemeVariants(com.vaadin.flow.component.combobox.ComboBoxVariant.LUMO_SMALL);
        filtro.setWidthFull();
        filtro.getStyle()
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("font-family", "Verdana, sans-serif");

        filtro.addValueChangeListener(e -> {
            String seleccion = e.getValue();
            ejecutarFiltro(cabecera,
                    (seleccion == null || seleccion.equals("Todos")) ? "" : seleccion);
        });

        Span textoCabecera = new Span(cabecera);
        textoCabecera.getStyle()
                .set("font-weight", "bold")
                .set("color", "#002060")
                .set("font-family", "Verdana, sans-serif");

        VerticalLayout layoutCabecera = new VerticalLayout(textoCabecera, filtro);
        layoutCabecera.setAlignItems(Alignment.CENTER);
        layoutCabecera.setSpacing(false);
        layoutCabecera.setPadding(false);

        return grid.addComponentColumn(item -> {
            Boolean valor = valueProvider.apply(item);
            Icon icono;
            if (valor != null && valor) {
                icono = VaadinIcon.CHECK_CIRCLE.create();
                icono.setColor("green");
            } else {
                icono = VaadinIcon.CLOSE_CIRCLE.create();
                icono.setColor("red");
            }
            return icono;
        })
        .setHeader(layoutCabecera)
        .setKey(cabecera)
        .setTextAlign(ColumnTextAlign.CENTER)
        .setAutoWidth(true);
    }

    // ── Botones de acción por fila ────────────────────────────────────────────
    private HorizontalLayout crearBotonesAccion(T item) {
        String colorGris = "#555555";

        Icon v = VaadinIcon.EYE.create();
        v.getStyle().set("color", colorGris);
        Button btnV = new Button(v);
        btnV.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        btnV.addClickListener(ev -> accionVisualizar(item));

        Icon e = VaadinIcon.EDIT.create();
        e.getStyle().set("color", colorGris);
        Button btnE = new Button(e);
        btnE.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        btnE.addClickListener(click -> accionEditar(item));

        Icon b = VaadinIcon.TRASH.create();
        b.getStyle().set("color", colorGris);
        Button btnB = new Button(b);
        btnB.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        btnB.addClickListener(event -> accionBorrar(item));

        HorizontalLayout layout = new HorizontalLayout(btnV, btnE, btnB);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        layout.setAlignItems(Alignment.CENTER);
        return layout;
    }

    // ── Diálogo configurar columnas ───────────────────────────────────────────
    private void abrirDialogoColumnas() {
        Dialog dialog = new Dialog();
        dialog.setWidth("450px");
        dialog.setHeight("auto");
        dialog.setModal(true);
        dialog.setHeaderTitle(getTranslation("app.configurar.columnas"));

        VerticalLayout contenido = new VerticalLayout();
        contenido.setPadding(false);
        contenido.setSpacing(false);
        contenido.getStyle()
                .set("gap", "15px")
                .set("font-family", "Verdana, sans-serif");

        grid.getColumns().forEach(col -> {
            String key = col.getKey();
            if (key != null && !key.equals("acciones")) {
                Checkbox cb = new Checkbox(key);
                cb.setValue(col.isVisible());
                cb.addValueChangeListener(e -> col.setVisible(e.getValue()));
                cb.getStyle().set("margin-bottom", "5px");
                contenido.add(cb);
            }
        });

        dialog.add(contenido);
        Button btnCerrar = new Button(getTranslation("app.aceptar"), e -> dialog.close());
        btnCerrar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(btnCerrar);
        dialog.open();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    protected void setTitulo(String texto) {
        tituloPrograma.setText(texto);
    }

    protected void ejecutarFiltro(String columna, String valor) {
        if (valor == null || valor.isEmpty() || valor.equals("Todos")) {
            filtrosActivos.remove(columna);
        } else {
            filtrosActivos.put(columna, valor);
        }
        actualizarLista();
    }

    // ── Métodos abstractos ────────────────────────────────────────────────────
    protected abstract void configurarColumnasEspecificas();
    protected abstract void actualizarLista();
    protected abstract void accionNuevo();
    protected abstract void accionVisualizar(T item);
    protected abstract void accionEditar(T item);
    protected abstract void accionBorrar(T item);
}