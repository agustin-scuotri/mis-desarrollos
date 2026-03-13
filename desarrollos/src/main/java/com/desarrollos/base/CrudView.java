package com.desarrollos.base;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
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

    // ── Estado de paginación ──────────────────────────────────────────────────
    protected int paginaActual = 0;
    protected int filasPorPagina = 25;
    protected long totalRegistros = 0;

    protected HorizontalLayout barraPaginacion;
    private Span spanPagina;
    private Button btnAnterior;
    private Button btnSiguiente;

    public CrudView(Class<T> claseEntidad) {
        setSizeFull();
        setSpacing(true);
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        configurarComponentes(claseEntidad);
        barraPaginacion = crearBarraPaginacion();
        add(barraHerramientas, grid, barraPaginacion);
        setFlexGrow(1, grid);
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
        btnConfiguracion.setTooltipText("Configurar columnas visibles");

        HorizontalLayout layoutTitulo = new HorizontalLayout(btnConfiguracion, tituloPrograma);
        layoutTitulo.setAlignItems(Alignment.CENTER);
        layoutTitulo.setSpacing(false);

        // ── Botón Nuevo ───────────────────────────────────────────────────────
        btnNuevo = new Button(getTranslation("app.agregar"), VaadinIcon.PLUS.create());
        btnNuevo.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        btnNuevo.addClickListener(e -> accionNuevo());

        // ── Combo filas por página ─────────────────────────────────────────────
        ComboBox<Integer> comboFilas = new ComboBox<>();
        comboFilas.setItems(10, 25, 50, 100);
        comboFilas.setValue(25);
        comboFilas.setWidth("85px");
        comboFilas.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
        comboFilas.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                filasPorPagina = e.getValue();
                paginaActual = 0;
                actualizarLista();
            }
        });

        Span labelFilas = new Span("Filas:");
        labelFilas.getStyle()
                .set("font-size", "0.8rem")
                .set("color", "#64748b")
                .set("white-space", "nowrap");

        HorizontalLayout layoutFilas = new HorizontalLayout(labelFilas, comboFilas);
        layoutFilas.setAlignItems(Alignment.CENTER);
        layoutFilas.setSpacing(false);
        layoutFilas.getStyle().set("gap", "6px");

        // ── Barra herramientas ────────────────────────────────────────────────
        HorizontalLayout ladoDerecho;
        if (mostrarBotonNuevo()) {
            ladoDerecho = new HorizontalLayout(layoutFilas, btnNuevo);
        } else {
            ladoDerecho = new HorizontalLayout(layoutFilas);
        }
        ladoDerecho.setAlignItems(Alignment.CENTER);
        ladoDerecho.setSpacing(false);
        ladoDerecho.getStyle().set("gap", "10px");

        barraHerramientas = new HorizontalLayout(layoutTitulo, ladoDerecho);
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
                "    clip-path: inset(0);" +
                "    cursor: default !important;" +
                "  }" +
                "  [part~='header-cell'][frozen-to-end] {" +
                "    box-shadow: -2px 0 6px rgba(0,0,0,0.08);" +
                "    overflow: hidden;" +
                "    clip-path: inset(0);" +
                "  }" +
                "  [part~='resize-handle'] {" +
                "    display: none !important;" +
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
                "    overflow: hidden;" +
                "    clip-path: inset(0);" +
                "  }" +
                "  [part~='row']:hover [frozen-to-end] {" +
                "    background-color: #f5f6f9 !important;" +
                "  }" +
                "  [part~='row'][selected] [frozen-to-end] {" +
                "    background-color: white !important;" +
                "  }" +
                "  [part~='row'][selected]:hover [frozen-to-end] {" +
                "    background-color: #f5f6f9 !important;" +
                "  }" +
                "`;" +
                "grid.shadowRoot.appendChild(style);");
        grid.setSelectionMode(Grid.SelectionMode.NONE);

        // ── Columna acciones ──────────────────────────────────────────────────
        configurarColumnasEspecificas();

        Icon iconoAcciones = VaadinIcon.COG.create();
        iconoAcciones.setSize("13px");
        iconoAcciones.getStyle().set("color", "#64748b").set("flex-shrink", "0");
        Span textoAcciones = new Span(getTranslation("archivo.acciones"));
        textoAcciones.getStyle().set("font-weight", "600").set("color", "#334155");
        HorizontalLayout cabeceraAcciones = new HorizontalLayout(iconoAcciones, textoAcciones);
        cabeceraAcciones.setAlignItems(Alignment.CENTER);
        cabeceraAcciones.setSpacing(false);
        cabeceraAcciones.getStyle().set("gap", "5px");

        grid.addComponentColumn(item -> crearBotonesAccion(item))
                .setHeader(cabeceraAcciones)
                .setKey("acciones")
                .setFrozenToEnd(true)
                .setWidth(anchoColumnaAcciones())
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);
    }

    // ── Barra de paginación ───────────────────────────────────────────────────
    private HorizontalLayout crearBarraPaginacion() {
        btnAnterior = new Button(VaadinIcon.CHEVRON_LEFT.create());
        btnAnterior.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        btnAnterior.setEnabled(false);
        btnAnterior.addClickListener(e -> {
            if (paginaActual > 0) {
                paginaActual--;
                actualizarLista();
            }
        });

        btnSiguiente = new Button(VaadinIcon.CHEVRON_RIGHT.create());
        btnSiguiente.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        btnSiguiente.setEnabled(false);
        btnSiguiente.addClickListener(e -> {
            int totalPaginas = (int) Math.ceil((double) totalRegistros / filasPorPagina);
            if (paginaActual + 1 < totalPaginas) {
                paginaActual++;
                actualizarLista();
            }
        });

        spanPagina = new Span("Página 1 de 1");
        spanPagina.getStyle()
                .set("font-size", "0.8rem")
                .set("color", "#64748b")
                .set("padding", "0 8px");

        HorizontalLayout barra = new HorizontalLayout(btnAnterior, spanPagina, btnSiguiente);
        barra.setAlignItems(Alignment.CENTER);
        barra.setSpacing(false);
        barra.setWidthFull();
        barra.setJustifyContentMode(JustifyContentMode.CENTER);
        barra.getStyle()
                .set("padding", "6px 0")
                .set("border-top", "1px solid #e2e8f0");
        return barra;
    }

    // ── Actualizar indicador de paginación ────────────────────────────────────
    protected void actualizarPaginacion() {
        if (spanPagina == null) return;
        int totalPaginas = totalRegistros == 0 ? 1 : (int) Math.ceil((double) totalRegistros / filasPorPagina);
        spanPagina.setText("Página " + (paginaActual + 1) + " de " + totalPaginas
                + "  ·  " + totalRegistros + " registros");
        btnAnterior.setEnabled(paginaActual > 0);
        btnSiguiente.setEnabled((paginaActual + 1) < totalPaginas);
    }

    // ── Ícono por nombre de cabecera ──────────────────────────────────────────
    private Icon iconoParaCabecera(String cabecera) {
        String lower = cabecera == null ? "" : cabecera.toLowerCase();
        VaadinIcon vi;
        if (lower.contains("código") || lower.contains("codigo"))      vi = VaadinIcon.HASH;
        else if (lower.contains("nombre"))                              vi = VaadinIcon.TAG;
        else if (lower.contains("estado"))                              vi = VaadinIcon.FLAG;
        else if (lower.contains("cuit"))                                vi = VaadinIcon.BUILDING;
        else if (lower.contains("comprobante") || lower.contains("nro.")) vi = VaadinIcon.FILE_TEXT;
        else if (lower.contains("fecha"))                               vi = VaadinIcon.CALENDAR;
        else                                                            vi = VaadinIcon.LINES;
        Icon icon = vi.create();
        icon.setSize("13px");
        icon.getStyle().set("color", "#64748b").set("flex-shrink", "0");
        return icon;
    }

    // ── Cabecera con ícono + texto ─────────────────────────────────────────────
    protected HorizontalLayout crearTituloCabecera(String cabecera) {
        Span texto = new Span(cabecera);
        texto.getStyle().set("font-weight", "600").set("color", "#334155");
        HorizontalLayout hl = new HorizontalLayout(iconoParaCabecera(cabecera), texto);
        hl.setAlignItems(Alignment.CENTER);
        hl.setSpacing(false);
        hl.getStyle().set("gap", "5px");
        return hl;
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
            filtro.addValueChangeListener(e -> ejecutarFiltro(cabecera, e.getValue()));
        } else if (cabecera.toLowerCase().contains("cuit")) {
            filtro.setMaxLength(13);
            filtro.getElement().setAttribute("inputmode", "numeric");
            filtro.addValueChangeListener(e -> {
                String raw = e.getValue() != null ? e.getValue() : "";
                String digits = raw.replaceAll("[^0-9]", "");
                if (digits.length() > 11) digits = digits.substring(0, 11);
                String formatted = formatearCuit(digits);
                if (!formatted.equals(raw)) {
                    filtro.setValue(formatted);
                } else {
                    ejecutarFiltro(cabecera, formatted);
                }
            });
        } else {
            filtro.addValueChangeListener(e -> ejecutarFiltro(cabecera, e.getValue()));
        }

        VerticalLayout layoutCabecera = new VerticalLayout(crearTituloCabecera(cabecera), filtro);
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

        VerticalLayout layoutCabecera = new VerticalLayout(crearTituloCabecera(cabecera));
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

        VerticalLayout layoutCabecera = new VerticalLayout(crearTituloCabecera(cabecera), filtro);
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

    // ── Botones de acción por fila: Ver directo + menú "⋮" para editar/eliminar ─
    private HorizontalLayout crearBotonesAccion(T item) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(false);
        layout.getStyle().set("gap", "2px");
        layout.setAlignItems(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.CENTER);

        // Acción extra (ej.: descarga) — antes del resto
        com.vaadin.flow.component.Component extra = crearBotonAccionExtra(item);
        if (extra != null) layout.add(extra);

        // Ver → azul, siempre visible como ícono directo
        Icon vIcon = VaadinIcon.EYE.create();
        vIcon.getStyle().set("color", "#2563eb");
        vIcon.setSize("17px");
        Button btnV = new Button(vIcon);
        btnV.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        btnV.addClickListener(ev -> accionVisualizar(item));
        btnV.setTooltipText("Visualizar");
        layout.add(btnV);

        // Menú "⋮" con Editar (condicional) y Eliminar
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY, MenuBarVariant.LUMO_SMALL,
                MenuBarVariant.LUMO_ICON);
        Icon moreIcon = VaadinIcon.ELLIPSIS_DOTS_V.create();
        moreIcon.setSize("16px");
        MenuItem moreItem = menuBar.addItem(moreIcon);
        SubMenu sub = moreItem.getSubMenu();

        if (mostrarBotonEditar(item)) {
            sub.addItem(crearItemSubMenu(VaadinIcon.EDIT, "Editar", "#d97706"),
                    e -> accionEditar(item));
        }
        HorizontalLayout deleteHL = crearItemSubMenu(VaadinIcon.TRASH, "Eliminar", "#dc2626");
        if (mostrarBotonEditar(item)) {
            deleteHL.getStyle()
                    .set("border-top", "1px solid #e2e8f0")
                    .set("padding-top", "6px")
                    .set("margin-top", "2px");
        }
        sub.addItem(deleteHL, e -> accionBorrar(item));

        layout.add(menuBar);
        layout.setWidthFull();
        return layout;
    }

    private HorizontalLayout crearItemSubMenu(VaadinIcon icono, String texto, String color) {
        Icon icon = icono.create();
        icon.setSize("14px");
        icon.getStyle().set("color", color);
        Span span = new Span(texto);
        span.getStyle().set("font-size", "0.875rem").set("color", "#1e293b");
        HorizontalLayout hl = new HorizontalLayout(icon, span);
        hl.setAlignItems(Alignment.CENTER);
        hl.setSpacing(false);
        hl.getStyle().set("gap", "8px").set("padding", "2px 4px");
        return hl;
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
        paginaActual = 0;
        actualizarLista();
    }

    // ── Hooks de visibilidad (pueden sobreescribirse) ─────────────────────────
    protected boolean mostrarBotonNuevo() { return true; }
    protected boolean mostrarBotonEditar()        { return true; }
    protected boolean mostrarBotonEditar(T item)  { return mostrarBotonEditar(); }
    protected String anchoColumnaAcciones() { return "130px"; }
    protected com.vaadin.flow.component.Component crearBotonAccionExtra(T item) { return null; }

    // ── Formato CUIT XX-XXXXXXXX-X ────────────────────────────────────────────
    private static String formatearCuit(String soloDigitos) {
        if (soloDigitos.isEmpty()) return "";
        if (soloDigitos.length() <= 2) return soloDigitos;
        if (soloDigitos.length() <= 10)
            return soloDigitos.substring(0, 2) + "-" + soloDigitos.substring(2);
        return soloDigitos.substring(0, 2) + "-" + soloDigitos.substring(2, 10) + "-" + soloDigitos.substring(10, 11);
    }

    // ── Métodos abstractos ────────────────────────────────────────────────────
    protected abstract void configurarColumnasEspecificas();
    protected abstract void actualizarLista();
    protected abstract void accionNuevo();
    protected abstract void accionVisualizar(T item);
    protected abstract void accionEditar(T item);
    protected abstract void accionBorrar(T item);
}
