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
        getStyle().set("background-color", "#f8fafc");

        configurarComponentes(claseEntidad);
        add(barraHerramientas, grid);
    }

    private void configurarComponentes(Class<T> claseEntidad) {

        // ── Título ────────────────────────────────────────────────────────────
        tituloPrograma = new H2();
        tituloPrograma.getStyle()
                .set("margin", "0")
                .set("font-size", "1.4rem")
                .set("font-weight", "700")
                .set("color", "#1e293b")
                .set("letter-spacing", "-0.3px");

        // ── Botón configuración ───────────────────────────────────────────────
        Icon iconoConfig = VaadinIcon.OPTIONS.create();
        iconoConfig.getStyle().set("color", "#64748b");
        iconoConfig.setSize("20px");

        btnConfiguracion = new Button(iconoConfig);
        btnConfiguracion.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnConfiguracion.getStyle()
                .set("margin-right", "12px")
                .set("cursor", "pointer");
        btnConfiguracion.addClickListener(e -> abrirDialogoColumnas());

        HorizontalLayout layoutTitulo = new HorizontalLayout(btnConfiguracion, tituloPrograma);
        layoutTitulo.setAlignItems(Alignment.CENTER);
        layoutTitulo.setSpacing(false);

        // ── Botón Nuevo ───────────────────────────────────────────────────────
        btnNuevo = new Button(getTranslation("app.agregar"), VaadinIcon.PLUS.create());
        btnNuevo.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        btnNuevo.addClickListener(e -> accionNuevo());

        // ── Barra herramientas ────────────────────────────────────────────────
        if (mostrarBotonNuevo()) {
            barraHerramientas = new HorizontalLayout(layoutTitulo, btnNuevo);
        } else {
            barraHerramientas = new HorizontalLayout(layoutTitulo);
        }
        barraHerramientas.setWidthFull();
        barraHerramientas.setJustifyContentMode(JustifyContentMode.BETWEEN);
        barraHerramientas.setAlignItems(Alignment.CENTER);

        // ── Grilla ────────────────────────────────────────────────────────────
        grid = new Grid<>(claseEntidad);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        grid.getStyle()
                .set("border-radius", "12px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.05)")
                .set("background", "white");

        // ── Estado vacío mejorado: ícono + título + subtítulo ─────────────────
        VerticalLayout layoutVacio = new VerticalLayout();
        layoutVacio.setSizeFull();
        layoutVacio.setJustifyContentMode(JustifyContentMode.CENTER);
        layoutVacio.setAlignItems(Alignment.CENTER);
        layoutVacio.getStyle().set("gap", "8px");

        Icon emptyIcon = VaadinIcon.INBOX.create();
        emptyIcon.setSize("56px");
        emptyIcon.getStyle().set("color", "#cbd5e1");

        Span tituloVacio = new Span("No hay registros");
        tituloVacio.getStyle()
                .set("font-size", "1rem")
                .set("font-weight", "600")
                .set("color", "#64748b");

        Span subtituloVacio = new Span("Usá el botón \"Agregar\" para crear uno nuevo");
        subtituloVacio.getStyle()
                .set("font-size", "0.8rem")
                .set("color", "#94a3b8");

        layoutVacio.add(emptyIcon, tituloVacio, subtituloVacio);
        grid.setEmptyStateComponent(layoutVacio);

        // ── CSS interno de la grilla ──────────────────────────────────────────
        grid.getElement().executeJs(
                "const grid = this;" +
                "const style = document.createElement('style');" +
                "style.innerHTML = `" +
                "  vaadin-grid-cell-content {" +
                "    text-align: left;" +
                "    justify-content: flex-start;" +
                "    display: flex;" +
                "    font-family: 'Inter', -apple-system, sans-serif;" +
                "    font-size: 0.875rem;" +
                "  }" +
                "  [part~='header-cell'] {" +
                "    border-bottom: 2px solid rgba(0,32,96,0.12) !important;" +
                "    background-color: #f8fafc !important;" +
                "    font-weight: 600;" +
                "    overflow: hidden;" +
                "  }" +
                "  [part~='header-cell'][frozen-to-end] {" +
                "    box-shadow: -2px 0 6px rgba(0,0,0,0.08);" +
                "    overflow: hidden;" +
                "  }" +
                "  [part~='cell']:not([part~='header-cell']) {" +
                "    border-right: 1px solid rgba(0,32,96,0.06) !important;" +
                "  }" +
                "  [part~='row']:hover > [part~='cell'] {" +
                "    background-color: rgba(0,32,96,0.04) !important;" +
                "  }" +
                "  [part~='row'][selected] > [part~='cell'] {" +
                "    background-color: transparent !important;" +
                "  }" +
                "  [part~='row'][selected]:hover > [part~='cell'] {" +
                "    background-color: rgba(0,32,96,0.04) !important;" +
                "  }" +
                "  [frozen-to-end] {" +
                "    z-index: 3 !important;" +
                "    background-color: white;" +
                "  }" +
                "  [part~='row']:hover [frozen-to-end] {" +
                "    background-color: rgba(0,32,96,0.04) !important;" +
                "  }" +
                "  [part~='row'][selected] [frozen-to-end] {" +
                "    background-color: white !important;" +
                "  }" +
                "  [part~='row'][selected]:hover [frozen-to-end] {" +
                "    background-color: rgba(0,32,96,0.04) !important;" +
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
                .setWidth(anchoColumnaAcciones())
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
        filtro.getStyle().set("text-align", "left");
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
                .set("font-weight", "600")
                .set("color", "#334155");

        VerticalLayout layoutCabecera = new VerticalLayout(textoCabecera, filtro);
        layoutCabecera.setAlignItems(Alignment.CENTER);
        layoutCabecera.setSpacing(false);
        layoutCabecera.setPadding(false);

        return grid.addColumn(valueProvider)
                .setHeader(layoutCabecera)
                .setKey(cabecera)
                .setTextAlign(ColumnTextAlign.START)
                .setSortable(true)
                .setAutoWidth(true);
    }

    // ── Columna fecha ─────────────────────────────────────────────────────────
    protected Grid.Column<T> agregarColumnaFecha(ValueProvider<T, LocalDateTime> valueProvider, String cabecera) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        Span textoCabecera = new Span(cabecera);
        textoCabecera.getStyle()
                .set("font-weight", "600")
                .set("color", "#334155");

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
        .setTextAlign(ColumnTextAlign.START)
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

        filtro.addValueChangeListener(e -> {
            String seleccion = e.getValue();
            ejecutarFiltro(cabecera,
                    (seleccion == null || seleccion.equals("Todos")) ? "" : seleccion);
        });

        Span textoCabecera = new Span(cabecera);
        textoCabecera.getStyle()
                .set("font-weight", "600")
                .set("color", "#334155");

        VerticalLayout layoutCabecera = new VerticalLayout(textoCabecera, filtro);
        layoutCabecera.setAlignItems(Alignment.CENTER);
        layoutCabecera.setSpacing(false);
        layoutCabecera.setPadding(false);

        return grid.addComponentColumn(item -> {
            Boolean valor = valueProvider.apply(item);
            Icon icono;
            if (valor != null && valor) {
                icono = VaadinIcon.CHECK_CIRCLE.create();
                icono.setColor("#16a34a");
            } else {
                icono = VaadinIcon.CLOSE_CIRCLE.create();
                icono.setColor("#dc2626");
            }
            return icono;
        })
        .setHeader(layoutCabecera)
        .setKey(cabecera)
        .setTextAlign(ColumnTextAlign.CENTER)
        .setAutoWidth(true);
    }

    // ── Botones de acción por fila: colores semánticos ────────────────────────
    private HorizontalLayout crearBotonesAccion(T item) {

        // Ver → azul
        Icon v = VaadinIcon.EYE.create();
        v.getStyle().set("color", "#2563eb");
        v.setSize("17px");
        Button btnV = new Button(v);
        btnV.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        btnV.getElement().setAttribute("title", "Visualizar");
        btnV.addClickListener(ev -> accionVisualizar(item));

        // Editar → ámbar
        Icon e = VaadinIcon.EDIT.create();
        e.getStyle().set("color", "#d97706");
        e.setSize("17px");
        Button btnE = new Button(e);
        btnE.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        btnE.getElement().setAttribute("title", "Editar");
        btnE.addClickListener(click -> accionEditar(item));

        // Borrar → rojo
        Icon b = VaadinIcon.TRASH.create();
        b.getStyle().set("color", "#dc2626");
        b.setSize("17px");
        Button btnB = new Button(b);
        btnB.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        btnB.getElement().setAttribute("title", "Eliminar");
        btnB.addClickListener(event -> accionBorrar(item));

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(btnV);
        com.vaadin.flow.component.Component extra = crearBotonAccionExtra(item);
        if (extra != null) layout.add(extra);
        if (mostrarBotonEditar()) layout.add(btnE);
        layout.add(btnB);
        layout.setSpacing(false);
        layout.getStyle().set("gap", "4px");
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
        contenido.getStyle().set("gap", "15px");

        grid.getColumns().forEach(col -> {
            String key = col.getKey();
            if (key != null && !key.equals("acciones")) {
                Checkbox cb = new Checkbox(key);
                cb.setValue(col.isVisible());
                cb.addValueChangeListener(ev -> col.setVisible(ev.getValue()));
                cb.getStyle().set("margin-bottom", "5px");
                contenido.add(cb);
            }
        });

        dialog.add(contenido);
        Button btnCerrar = new Button(getTranslation("app.aceptar"), ev -> dialog.close());
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

    // ── Hooks de visibilidad (pueden sobreescribirse) ─────────────────────────
    protected boolean mostrarBotonNuevo() { return true; }
    protected boolean mostrarBotonEditar() { return true; }
    protected String anchoColumnaAcciones() { return "130px"; }
    protected com.vaadin.flow.component.Component crearBotonAccionExtra(T item) { return null; }

    // ── Métodos abstractos ────────────────────────────────────────────────────
    protected abstract void configurarColumnasEspecificas();
    protected abstract void actualizarLista();
    protected abstract void accionNuevo();
    protected abstract void accionVisualizar(T item);
    protected abstract void accionEditar(T item);
    protected abstract void accionBorrar(T item);
}
