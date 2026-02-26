package com.desarrollos.views;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.desarrollos.entities.Archivo;
import com.desarrollos.services.ArchivoService;
import com.desarrollos.services.DocumentoConvertidoService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Inicio")
@Route(value = "", layout = MainLayout.class)
public class InicioView extends VerticalLayout {

    public InicioView(ArchivoService archivoService, DocumentoConvertidoService documentoConvertidoService) {
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
        List<Archivo> archivos = archivoService.listarTodos();
        long total      = archivos.size();
        long procesados = archivos.stream().filter(a -> "PROCESADO".equals(a.getEstadoConversion())).count();
        long pendientes = archivos.stream()
                .filter(a -> a.getEstadoConversion() == null || "PENDIENTE".equals(a.getEstadoConversion()))
                .count();
        long errores    = archivos.stream().filter(a -> "PROCESADO_ERROR".equals(a.getEstadoConversion())).count();
        long documentos = documentoConvertidoService.listarTodos().size();

        // ── Cards de métricas (colores Lumo → adaptan a dark mode) ───────────
        HorizontalLayout cards = new HorizontalLayout(
            crearCard("Archivos Cargados", String.valueOf(total),      VaadinIcon.COPY_O,       "#2563eb", "#eff6ff"),
            crearCard("Procesados",        String.valueOf(procesados), VaadinIcon.CHECK_CIRCLE, "#16a34a", "#f0fdf4"),
            crearCard("Pendientes",        String.valueOf(pendientes), VaadinIcon.CLOCK,        "#d97706", "#fffbeb"),
            crearCard("Errores",           String.valueOf(errores),    VaadinIcon.WARNING,      "#dc2626", "#fef2f2"),
            crearCard("Docs. Convertidos", String.valueOf(documentos), VaadinIcon.FILE_TABLE,   "#7c3aed", "#f5f3ff")
        );
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

        // Chart 1 ── Donut de distribución de archivos por estado
        String donutData   = "[" + procesados + "," + pendientes + "," + errores + "]";
        String donutLabels = "[\"Procesados\",\"Pendientes\",\"Errores\"]";
        String donutColors = "[\"#16a34a\",\"#d97706\",\"#dc2626\"]";
        Div donutCard = crearCardChart("Estado de Archivos", donutScript(donutLabels, donutData, donutColors),
                "1", "250px", "340px");

        // Chart 2 ── Barras de actividad últimos 7 días
        long[]   datosBar     = documentoConvertidoService.conversionesPorDia();
        String[] etiquetasBar = documentoConvertidoService.etiquetasDias();
        String barData   = Arrays.stream(datosBar).mapToObj(String::valueOf).collect(Collectors.joining(",", "[", "]"));
        String barLabels = "[\"" + String.join("\",\"", etiquetasBar) + "\"]";
        Div barCard = crearCardChart("Actividad — Últimos 7 días", barScript(barLabels, barData),
                "2", "250px", null);

        chartsRow.add(donutCard, barCard);

        add(titulo, subtitulo, cards, chartsRow);
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

    // ── Card contenedor de gráfico ────────────────────────────────────────────
    private Div crearCardChart(String titulo, String chartJs, String flex,
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

        Div canvas = new Div();
        canvas.getStyle().set("width", "100%").set("max-height", "220px");

        // Cargar Chart.js desde CDN si no está disponible y renderizar
        String loaderJs =
                "const me = this;" +
                "const render = () => { " + chartJs + " };" +
                "if (typeof Chart !== 'undefined') { render(); } else {" +
                "  const s = document.createElement('script');" +
                "  s.src = 'https://cdn.jsdelivr.net/npm/chart.js@4/dist/chart.umd.min.js';" +
                "  s.onload = render;" +
                "  document.head.appendChild(s); }";
        canvas.getElement().executeJs(loaderJs);

        card.add(tituloSpan, canvas);
        return card;
    }

    // ── Script Chart.js: Donut ────────────────────────────────────────────────
    private String donutScript(String labels, String data, String colors) {
        return "const isDark = document.documentElement.getAttribute('theme') === 'dark';" +
               "const cvs = document.createElement('canvas'); cvs.style.maxHeight = '200px'; me.appendChild(cvs);" +
               "new Chart(cvs, { type: 'doughnut', data: {" +
               "  labels: " + labels + "," +
               "  datasets: [{ data: " + data + ", backgroundColor: " + colors + "," +
               "    borderColor: isDark ? '#1e293b' : 'white', borderWidth: 2 }]" +
               "}, options: { responsive: true, maintainAspectRatio: true," +
               "  plugins: { legend: { position: 'bottom', labels: {" +
               "    color: isDark ? '#94a3b8' : '#475569'," +
               "    font: { size: 12, family: 'Inter, sans-serif' }, boxWidth: 12, padding: 10" +
               "  }}}}" +
               "});";
    }

    // ── Script Chart.js: Barras ───────────────────────────────────────────────
    private String barScript(String labels, String data) {
        return "const isDark = document.documentElement.getAttribute('theme') === 'dark';" +
               "const barColor   = isDark ? 'rgba(96,165,250,0.75)' : 'rgba(37,99,235,0.75)';" +
               "const borderClr  = isDark ? '#60a5fa' : '#2563eb';" +
               "const tickClr    = isDark ? '#94a3b8' : '#64748b';" +
               "const gridClr    = isDark ? 'rgba(255,255,255,0.07)' : 'rgba(0,0,0,0.05)';" +
               "const cvs = document.createElement('canvas'); cvs.style.maxHeight = '200px'; me.appendChild(cvs);" +
               "new Chart(cvs, { type: 'bar', data: {" +
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
