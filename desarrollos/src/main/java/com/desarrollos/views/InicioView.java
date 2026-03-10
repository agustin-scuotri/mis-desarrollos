package com.desarrollos.views;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.desarrollos.entities.Archivo;
import com.desarrollos.services.ArchivoService;
import com.desarrollos.services.DocumentoConvertidoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

@PageTitle("Inicio")
@Route(value = "", layout = MainLayout.class)
public class InicioView extends VerticalLayout {

    private final ArchivoService archivoService;
    private final DocumentoConvertidoService documentoConvertidoService;

    private List<Archivo> archivos;
    private long procesados, pendientes, errores;

    private Div barCanvas;
    private Button btnActivo;

    public InicioView(ArchivoService archivoService, DocumentoConvertidoService documentoConvertidoService) {
        this.archivoService = archivoService;
        this.documentoConvertidoService = documentoConvertidoService;

        setPadding(true);
        setSpacing(false);
        getStyle()
                .set("background-color", "var(--lumo-contrast-5pct, #f8fafc)")
                .set("min-height", "100%");

        // ── Encabezado ────────────────────────────────────────────────────────
        H2 titulo = new H2(getTranslation("app.bienvenida"));
        titulo.getStyle()
                .set("color", "var(--lumo-header-text-color, #1e293b)")
                .set("font-size", "1.5rem")
                .set("font-weight", "700")
                .set("letter-spacing", "-0.4px")
                .set("margin", "0 0 4px 0");

        Paragraph subtitulo = new Paragraph(getTranslation("app.seleccione"));
        subtitulo.getStyle()
                .set("color", "var(--lumo-secondary-text-color, #64748b)")
                .set("font-size", "0.95rem")
                .set("margin", "0 0 28px 0");

        // ── Métricas ──────────────────────────────────────────────────────────
        archivos = archivoService.listarTodos();
        long total  = archivos.size();
        procesados  = archivos.stream().filter(a -> "PROCESADO".equals(a.getEstadoConversion())).count();
        pendientes  = archivos.stream()
                .filter(a -> a.getEstadoConversion() == null || "PENDIENTE".equals(a.getEstadoConversion()))
                .count();
        errores     = archivos.stream().filter(a -> "PROCESADO_ERROR".equals(a.getEstadoConversion())).count();
        long documentos = documentoConvertidoService.listarTodos().size();

        VerticalLayout cardArchivos   = crearCard("Archivos Cargados", String.valueOf(total),      VaadinIcon.COPY_O,       "#2563eb", "#eff6ff");
        VerticalLayout cardProcesados = crearCard("Procesados",        String.valueOf(procesados), VaadinIcon.CHECK_CIRCLE, "#16a34a", "#f0fdf4");
        VerticalLayout cardPendientes = crearCard("Pendientes",        String.valueOf(pendientes), VaadinIcon.CLOCK,        "#d97706", "#fffbeb");
        VerticalLayout cardErrores    = crearCard("Errores",           String.valueOf(errores),    VaadinIcon.WARNING,      "#dc2626", "#fef2f2");
        VerticalLayout cardConvert    = crearCard("Docs. Convertidos", String.valueOf(documentos), VaadinIcon.FILE_TABLE,   "#7c3aed", "#f5f3ff");

        hacerClickeable(cardArchivos,   () -> getUI().ifPresent(ui -> ui.navigate("ABMarchivos")));
        hacerClickeable(cardProcesados, () -> getUI().ifPresent(ui -> ui.navigate("ABMarchivos",
                new QueryParameters(Map.of("estado", List.of("Procesado"))))));
        hacerClickeable(cardPendientes, () -> getUI().ifPresent(ui -> ui.navigate("ABMarchivos",
                new QueryParameters(Map.of("estado", List.of("Pendiente a procesar"))))));
        hacerClickeable(cardErrores,    () -> getUI().ifPresent(ui -> ui.navigate("ABMarchivos",
                new QueryParameters(Map.of("estado", List.of("Procesado error"))))));
        hacerClickeable(cardConvert,    () -> getUI().ifPresent(ui -> ui.navigate("lista-jsons")));

        HorizontalLayout cards = new HorizontalLayout(
                cardArchivos, cardProcesados, cardPendientes, cardErrores, cardConvert);
        cards.setWidthFull();
        cards.setSpacing(true);
        cards.getStyle().set("flex-wrap", "wrap");

        // ── Gráficos ──────────────────────────────────────────────────────────
        Div chartsRow = new Div();
        chartsRow.setWidthFull();
        chartsRow.getStyle()
                .set("display", "flex")
                .set("gap", "16px")
                .set("margin-top", "20px")
                .set("flex-wrap", "wrap");

        // Chart 1 ── Donut interactivo
        String donutData   = "[" + procesados + "," + pendientes + "," + errores + "]";
        String donutLabels = "[\"Procesados\",\"Pendientes\",\"Errores\"]";
        String donutColors = "[\"#16a34a\",\"#d97706\",\"#dc2626\"]";

        Div donutCanvas = new Div();
        donutCanvas.getStyle().set("width", "100%").set("max-height", "220px");
        donutCanvas.getElement().executeJs(loaderScript(donutScript(donutLabels, donutData, donutColors)));
        String[] estadosNav = {"Procesado", "Pendiente a procesar", "Procesado error"};
        donutCanvas.getElement()
                .addEventListener("donut-click", e -> {
                    int index = e.getEventData().get("event.detail.index").asInt();
                    if (index >= 0 && index < estadosNav.length) {
                        String estadoParam = estadosNav[index];
                        getUI().ifPresent(ui -> ui.navigate("ABMarchivos",
                                new QueryParameters(Map.of("estado", List.of(estadoParam)))));
                    }
                })
                .addEventData("event.detail.index");

        Div donutCard = crearCardChart("Estado de Archivos", donutCanvas, "1", "250px", "340px");

        // Chart 2 ── Barras con toggle de período
        barCanvas = new Div();
        barCanvas.getStyle().set("width", "100%").set("max-height", "220px");
        cargarBarChart("7d");


        Button btn7d    = crearBotonPeriodo("7 días");
        Button btnMes   = crearBotonPeriodo("Mes");
        Button btnAnio  = crearBotonPeriodo("Año");
        Button btnRango = crearBotonPeriodo("Rango");
        activarBoton(btn7d);

        DatePicker dpDesde = new DatePicker();
        dpDesde.setPlaceholder("Desde");
        dpDesde.setWidth("130px");
        DatePicker dpHasta = new DatePicker();
        dpHasta.setPlaceholder("Hasta");
        dpHasta.setWidth("130px");
        Button btnAplicar = new Button("Aplicar");
        btnAplicar.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        btnAplicar.getStyle().set("font-size", "0.8rem");
        btnAplicar.addClickListener(ev -> {
            LocalDate d = dpDesde.getValue(), h = dpHasta.getValue();
            if (d != null && h != null && !d.isAfter(h)) cargarBarChartRango(d, h);
        });

        HorizontalLayout rangoRow = new HorizontalLayout(dpDesde, dpHasta, btnAplicar);
        rangoRow.setSpacing(false);
        rangoRow.getStyle().set("gap", "8px").set("align-items", "center")
                .set("margin-top", "10px").set("flex-wrap", "wrap");
        rangoRow.setVisible(false);

        btn7d.addClickListener(e   -> { activarBoton(btn7d);   rangoRow.setVisible(false); cargarBarChart("7d");   });
        btnMes.addClickListener(e  -> { activarBoton(btnMes);  rangoRow.setVisible(false); cargarBarChart("mes");  });
        btnAnio.addClickListener(e -> { activarBoton(btnAnio); rangoRow.setVisible(false); cargarBarChart("anio"); });
        btnRango.addClickListener(e -> { activarBoton(btnRango); rangoRow.setVisible(true); });

        HorizontalLayout toggles = new HorizontalLayout(btn7d, btnMes, btnAnio, btnRango);
        toggles.setSpacing(false);
        toggles.getStyle().set("gap", "6px").set("margin-bottom", "10px");

        Div barCard = crearCardChartConToggle("Actividad", toggles, barCanvas, "2", "250px", null);
        barCard.addComponentAtIndex(1, rangoRow);

        chartsRow.add(donutCard, barCard);
        add(titulo, subtitulo, cards, chartsRow);
    }

    // ── Carga/recarga el gráfico de barras según período ──────────────────────
    private void cargarBarChart(String periodo) {
        long[]   datos;
        String[] etiquetas;
        switch (periodo) {
            case "mes":
                datos     = documentoConvertidoService.conversionesPorMes();
                etiquetas = documentoConvertidoService.etiquetasMes();
                break;
            case "anio":
                datos     = documentoConvertidoService.conversionesPorAnio();
                etiquetas = documentoConvertidoService.etiquetasAnio();
                break;
            default:
                datos     = documentoConvertidoService.conversionesPorDia();
                etiquetas = documentoConvertidoService.etiquetasDias();
        }
        String barData   = Arrays.stream(datos).mapToObj(String::valueOf).collect(Collectors.joining(",", "[", "]"));
        String barLabels = "[\"" + String.join("\",\"", etiquetas) + "\"]";
        barCanvas.getElement().executeJs(loaderScript(barScript(barLabels, barData)));
    }

    private void cargarBarChartRango(LocalDate desde, LocalDate hasta) {
        long[]   datos     = documentoConvertidoService.conversionesPorRango(desde, hasta);
        String[] etiquetas = documentoConvertidoService.etiquetasRango(desde, hasta);
        String barData   = Arrays.stream(datos).mapToObj(String::valueOf).collect(Collectors.joining(",", "[", "]"));
        String barLabels = "[\"" + String.join("\",\"", etiquetas) + "\"]";
        barCanvas.getElement().executeJs(loaderScript(barScript(barLabels, barData)));
    }

    // ── Botón de período ──────────────────────────────────────────────────────
    private Button crearBotonPeriodo(String label) {
        Button btn = new Button(label);
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        btn.getStyle()
                .set("border", "1px solid #e2e8f0")
                .set("border-radius", "8px")
                .set("font-size", "0.8rem")
                .set("cursor", "pointer")
                .set("transition", "all 0.15s ease");
        return btn;
    }

    private void activarBoton(Button btn) {
        if (btnActivo != null) {
            btnActivo.getStyle()
                    .set("background", "transparent")
                    .set("color", "#64748b")
                    .set("font-weight", "400")
                    .set("border-color", "#e2e8f0");
        }
        btnActivo = btn;
        btn.getStyle()
                .set("background", "#002060")
                .set("color", "white")
                .set("font-weight", "600")
                .set("border-color", "#002060");
    }

    // ── Hace una card navegable con hover ─────────────────────────────────────
    private void hacerClickeable(VerticalLayout card, Runnable accion) {
        card.getStyle()
                .set("cursor", "pointer")
                .set("transition", "box-shadow 0.15s ease, transform 0.1s ease");
        card.getElement().addEventListener("mouseenter", e ->
                card.getStyle()
                        .set("box-shadow", "0 4px 12px rgba(0,0,0,0.12)")
                        .set("transform", "translateY(-2px)"));
        card.getElement().addEventListener("mouseleave", e ->
                card.getStyle()
                        .set("box-shadow", "0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.05)")
                        .set("transform", "translateY(0)"));
        card.addClickListener(e -> accion.run());
    }

    // ── Card métrica ──────────────────────────────────────────────────────────
    private VerticalLayout crearCard(String etiqueta, String valor, VaadinIcon icono,
                                     String color, String bgIcono) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(false);
        card.setSpacing(false);
        card.getStyle()
                .set("background", "var(--lumo-base-color, white)")
                .set("border-radius", "14px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.05)")
                .set("padding", "20px 24px")
                .set("flex", "1")
                .set("min-width", "150px")
                .set("gap", "6px");

        Icon icon = icono.create();
        icon.setSize("22px");
        icon.getStyle()
                .set("color", color)
                .set("background-color", bgIcono)
                .set("border-radius", "8px")
                .set("padding", "8px")
                .set("box-sizing", "content-box")
                .set("margin-bottom", "8px");

        Span valorSpan = new Span(valor);
        valorSpan.getStyle()
                .set("font-size", "2rem")
                .set("font-weight", "700")
                .set("color", "var(--lumo-header-text-color, #1e293b)")
                .set("line-height", "1")
                .set("letter-spacing", "-1px");

        Span etiquetaSpan = new Span(etiqueta);
        etiquetaSpan.getStyle()
                .set("font-size", "0.8rem")
                .set("font-weight", "500")
                .set("color", "var(--lumo-secondary-text-color, #64748b)")
                .set("margin-top", "2px");

        card.add(icon, valorSpan, etiquetaSpan);
        return card;
    }

    // ── Card contenedor de gráfico (simple) ───────────────────────────────────
    private Div crearCardChart(String titulo, Div canvas, String flex,
                               String minWidth, String maxWidth) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color, white)")
                .set("border-radius", "14px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.08)")
                .set("padding", "20px 24px")
                .set("flex", flex)
                .set("min-width", minWidth);
        if (maxWidth != null) card.getStyle().set("max-width", maxWidth);

        Span tituloSpan = new Span(titulo);
        tituloSpan.getStyle()
                .set("font-weight", "600")
                .set("font-size", "0.9rem")
                .set("color", "var(--lumo-header-text-color, #1e293b)")
                .set("display", "block")
                .set("margin-bottom", "14px");

        card.add(tituloSpan, canvas);
        return card;
    }

    // ── Card contenedor de gráfico con toggle de período ─────────────────────
    private Div crearCardChartConToggle(String titulo, HorizontalLayout toggles, Div canvas,
                                        String flex, String minWidth, String maxWidth) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color, white)")
                .set("border-radius", "14px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.08)")
                .set("padding", "20px 24px")
                .set("flex", flex)
                .set("min-width", minWidth);
        if (maxWidth != null) card.getStyle().set("max-width", maxWidth);

        Span tituloSpan = new Span(titulo);
        tituloSpan.getStyle()
                .set("font-weight", "600")
                .set("font-size", "0.9rem")
                .set("color", "var(--lumo-header-text-color, #1e293b)");

        Div header = new Div();
        header.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("margin-bottom", "14px");
        header.add(tituloSpan, toggles);

        card.add(header, canvas);
        return card;
    }

    // ── Envuelve el script con el loader de Chart.js ──────────────────────────
    private String loaderScript(String chartJs) {
        return "const me = this;" +
               "const render = () => { " + chartJs + " };" +
               "if (typeof Chart !== 'undefined') { render(); } else {" +
               "  const s = document.createElement('script');" +
               "  s.src = 'https://cdn.jsdelivr.net/npm/chart.js@4/dist/chart.umd.min.js';" +
               "  s.onload = render;" +
               "  document.head.appendChild(s); }";
    }

    // ── Script Chart.js: Donut interactivo ────────────────────────────────────
    private String donutScript(String labels, String data, String colors) {
        return "const isDark = document.documentElement.getAttribute('theme') === 'dark';" +
               "const cvs = document.createElement('canvas'); cvs.style.maxHeight = '200px'; me.appendChild(cvs);" +
               "new Chart(cvs, { type: 'doughnut', data: {" +
               "  labels: " + labels + "," +
               "  datasets: [{ data: " + data + ", backgroundColor: " + colors + "," +
               "    borderColor: isDark ? '#1e293b' : 'white', borderWidth: 2 }]" +
               "}, options: { responsive: true, maintainAspectRatio: true," +
               "  onClick: (evt, elements) => {" +
               "    if (elements.length > 0) {" +
               "      me.dispatchEvent(new CustomEvent('donut-click', { bubbles: true, detail: { index: elements[0].index } }));" +
               "    }" +
               "  }," +
               "  onHover: (evt, elements) => {" +
               "    evt.native.target.style.cursor = elements.length > 0 ? 'pointer' : 'default';" +
               "  }," +
               "  plugins: { legend: { position: 'bottom', labels: {" +
               "    color: isDark ? '#94a3b8' : '#475569'," +
               "    font: { size: 12, family: 'Inter, sans-serif' }, boxWidth: 12, padding: 10" +
               "  }}}}" +
               "});";
    }

    // ── Script Chart.js: Barras (reutiliza canvas, destruye chart previo) ─────
    private String barScript(String labels, String data) {
        return "const isDark = document.documentElement.getAttribute('theme') === 'dark';" +
               "const barColor  = isDark ? 'rgba(96,165,250,0.75)' : 'rgba(37,99,235,0.75)';" +
               "const borderClr = isDark ? '#60a5fa' : '#2563eb';" +
               "const tickClr   = isDark ? '#94a3b8' : '#64748b';" +
               "const gridClr   = isDark ? 'rgba(255,255,255,0.07)' : 'rgba(0,0,0,0.05)';" +
               "if (me._barChart) { me._barChart.destroy(); }" +
               "const cvs = me._barCvs || (() => {" +
               "  const c = document.createElement('canvas'); c.style.maxHeight='200px';" +
               "  me.appendChild(c); me._barCvs = c; return c;" +
               "})();" +
               "me._barChart = new Chart(cvs, { type: 'bar', data: {" +
               "  labels: " + labels + "," +
               "  datasets: [{ label: 'Conversiones', data: " + data + "," +
               "    backgroundColor: barColor, borderColor: borderClr," +
               "    borderWidth: 1, borderRadius: 6 }]" +
               "}, options: { responsive: true, maintainAspectRatio: true," +
               "  plugins: { legend: { display: false } }," +
               "  scales: {" +
               "    x: { ticks: { color: tickClr, font: { size: 11 } }, grid: { color: gridClr } }," +
               "    y: { beginAtZero: true, ticks: { stepSize: 1, precision: 0, color: tickClr, font: { size: 11 } }, grid: { color: gridClr } }" +
               "  }}" +
               "});";
    }
}
