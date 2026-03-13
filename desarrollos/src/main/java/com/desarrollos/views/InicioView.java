package com.desarrollos.views;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.desarrollos.entities.Archivo;
import com.desarrollos.entities.DocumentoConvertido;
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
    private Div barMontosCanvas;
    private Button btnActivo;
    private Button btnMontoActivo;

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

        // ── Comparativa mes anterior ──────────────────────────────────────────
        YearMonth mesActual  = YearMonth.now();
        YearMonth mesAnterior = mesActual.minusMonths(1);
        long totalMesAnt      = archivoService.contarCreadosEnMes(mesAnterior);
        long procesadosMesAnt = archivoService.contarProcesadosEnMes(mesAnterior);
        long erroresMesAnt    = archivoService.contarErroresEnMes(mesAnterior);
        long docsMesAnt       = documentoConvertidoService.contarConvertidosEnMes(mesAnterior);
        long totalMesActual   = archivoService.contarCreadosEnMes(mesActual);
        long docsMesActual    = documentoConvertidoService.contarConvertidosEnMes(mesActual);

        VerticalLayout cardArchivos   = crearCard("Archivos Cargados", String.valueOf(total),      VaadinIcon.COPY_O,       "#2563eb", "#eff6ff", totalMesActual, totalMesAnt);
        VerticalLayout cardProcesados = crearCard("Procesados",        String.valueOf(procesados), VaadinIcon.CHECK_CIRCLE, "#16a34a", "#f0fdf4", archivoService.contarProcesadosEnMes(mesActual), procesadosMesAnt);
        VerticalLayout cardPendientes = crearCard("Pendientes",        String.valueOf(pendientes), VaadinIcon.CLOCK,        "#d97706", "#fffbeb", -1, -1);
        VerticalLayout cardErrores    = crearCard("Errores",           String.valueOf(errores),    VaadinIcon.WARNING,      "#dc2626", "#fef2f2", archivoService.contarErroresEnMes(mesActual), erroresMesAnt);
        VerticalLayout cardConvert    = crearCard("Docs. Convertidos", String.valueOf(documentos), VaadinIcon.FILE_TABLE,   "#7c3aed", "#f5f3ff", docsMesActual, docsMesAnt);
        String tasaError = (procesados + errores) > 0
                ? String.format("%.1f%%", (errores * 100.0) / (procesados + errores)) : "—";
        VerticalLayout cardTasaError  = crearCard("Tasa de Error",     tasaError,                  VaadinIcon.CHART_LINE,   "#9333ea", "#faf5ff", -1, -1);

        hacerClickeable(cardArchivos,   () -> getUI().ifPresent(ui -> ui.navigate("ABMarchivos")));
        hacerClickeable(cardProcesados, () -> getUI().ifPresent(ui -> ui.navigate("ABMarchivos",
                new QueryParameters(Map.of("estado", List.of("Procesado"))))));
        hacerClickeable(cardPendientes, () -> getUI().ifPresent(ui -> ui.navigate("ABMarchivos",
                new QueryParameters(Map.of("estado", List.of("Pendiente a procesar"))))));
        hacerClickeable(cardErrores,    () -> getUI().ifPresent(ui -> ui.navigate("ABMarchivos",
                new QueryParameters(Map.of("estado", List.of("Procesado error"))))));
        hacerClickeable(cardConvert,    () -> getUI().ifPresent(ui -> ui.navigate("lista-jsons")));

        HorizontalLayout cards = new HorizontalLayout(
                cardArchivos, cardProcesados, cardPendientes, cardErrores, cardConvert, cardTasaError);
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

        // ── Segunda fila: Top Proveedores + Montos Facturados ─────────────────
        Div chartsRow2 = new Div();
        chartsRow2.setWidthFull();
        chartsRow2.getStyle()
                .set("display", "flex")
                .set("gap", "16px")
                .set("margin-top", "16px")
                .set("flex-wrap", "wrap");

        // Chart 3 — Top Proveedores (barra horizontal)
        String[] provLabels = documentoConvertidoService.topProveedoresLabels(8);
        long[]   provCants  = documentoConvertidoService.topProveedoresCantidades(8);
        String provLabelsJson = provLabels.length == 0 ? "[]"
                : "[\"" + String.join("\",\"", provLabels) + "\"]";
        String provDataJson = Arrays.stream(provCants).mapToObj(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
        int provHeight = Math.max(120, provLabels.length * 40 + 40);
        Div provCanvas = new Div();
        provCanvas.getStyle().set("width", "100%").set("height", provHeight + "px");
        provCanvas.getElement().executeJs(loaderScript(proveedoresScript(provLabelsJson, provDataJson)));
        Div provCard = crearCardChart("Top Proveedores por Facturas", provCanvas, "1", "280px", null);

        // Chart 4 — Montos Facturados (línea con toggle de período)
        barMontosCanvas = new Div();
        barMontosCanvas.getStyle().set("width", "100%").set("max-height", "220px");
        cargarMontoChart("7d");

        Button btnM7d    = crearBotonPeriodo("7 días");
        Button btnMMes   = crearBotonPeriodo("Mes");
        Button btnMAnio  = crearBotonPeriodo("Año");
        Button btnMRango = crearBotonPeriodo("Rango");
        activarBotonMonto(btnM7d);

        DatePicker dpMDesde = new DatePicker();
        dpMDesde.setPlaceholder("Desde");
        dpMDesde.setWidth("130px");
        DatePicker dpMHasta = new DatePicker();
        dpMHasta.setPlaceholder("Hasta");
        dpMHasta.setWidth("130px");
        Button btnMAplicar = new Button("Aplicar");
        btnMAplicar.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        btnMAplicar.getStyle().set("font-size", "0.8rem");
        btnMAplicar.addClickListener(ev -> {
            LocalDate d = dpMDesde.getValue(), h = dpMHasta.getValue();
            if (d != null && h != null && !d.isAfter(h)) cargarMontoChartRango(d, h);
        });

        HorizontalLayout rangoMRow = new HorizontalLayout(dpMDesde, dpMHasta, btnMAplicar);
        rangoMRow.setSpacing(false);
        rangoMRow.getStyle().set("gap", "8px").set("align-items", "center")
                .set("margin-top", "10px").set("flex-wrap", "wrap");
        rangoMRow.setVisible(false);

        btnM7d.addClickListener(e    -> { activarBotonMonto(btnM7d);    rangoMRow.setVisible(false); cargarMontoChart("7d");   });
        btnMMes.addClickListener(e   -> { activarBotonMonto(btnMMes);   rangoMRow.setVisible(false); cargarMontoChart("mes");  });
        btnMAnio.addClickListener(e  -> { activarBotonMonto(btnMAnio);  rangoMRow.setVisible(false); cargarMontoChart("anio"); });
        btnMRango.addClickListener(e -> { activarBotonMonto(btnMRango); rangoMRow.setVisible(true);  });

        HorizontalLayout mToggles = new HorizontalLayout(btnM7d, btnMMes, btnMAnio, btnMRango);
        mToggles.setSpacing(false);
        mToggles.getStyle().set("gap", "6px").set("margin-bottom", "10px");

        Div montosCard = crearCardChartConToggle("Montos Facturados ($)", mToggles, barMontosCanvas, "2", "280px", null);
        montosCard.addComponentAtIndex(1, rangoMRow);

        chartsRow2.add(provCard, montosCard);

        // ── Timeline de actividad reciente ────────────────────────────────────
        Div timelineCard = crearTimeline();

        add(titulo, subtitulo, cards, chartsRow, chartsRow2, timelineCard);
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
                    .set("color", "var(--lumo-secondary-text-color, #64748b)")
                    .set("font-weight", "400")
                    .set("border-color", "var(--lumo-contrast-20pct, #e2e8f0)");
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

    // ── Card métrica con comparativa mes anterior ─────────────────────────────
    private VerticalLayout crearCard(String etiqueta, String valor, VaadinIcon icono,
                                     String color, String bgIcono, long mesActual, long mesAnt) {
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

        Span valorSpan = new Span("0");
        valorSpan.getStyle()
                .set("font-size", "2rem")
                .set("font-weight", "700")
                .set("color", "var(--lumo-header-text-color, #1e293b)")
                .set("line-height", "1")
                .set("letter-spacing", "-1px");

        // Animate counter from 0 to target value (only for numeric values)
        try {
            long target = Long.parseLong(valor);
            valorSpan.getElement().executeJs(
                "const target = " + target + ";" +
                "if (target === 0) { this.textContent = '0'; return; }" +
                "const duration = 900;" +
                "const startTime = performance.now();" +
                "const step = (now) => {" +
                "  const progress = Math.min((now - startTime) / duration, 1);" +
                "  const ease = 1 - Math.pow(1 - progress, 3);" +
                "  this.textContent = Math.round(ease * target);" +
                "  if (progress < 1) requestAnimationFrame(step);" +
                "};" +
                "requestAnimationFrame(step);"
            );
        } catch (NumberFormatException e) {
            valorSpan.setText(valor);
        }

        Span etiquetaSpan = new Span(etiqueta);
        etiquetaSpan.getStyle()
                .set("font-size", "0.8rem")
                .set("font-weight", "500")
                .set("color", "var(--lumo-secondary-text-color, #64748b)")
                .set("margin-top", "2px");

        card.add(icon, valorSpan, etiquetaSpan);

        // ── Delta vs mes anterior ─────────────────────────────────────────────
        if (mesActual >= 0 && mesAnt >= 0) {
            Span delta = crearDeltaMes(mesActual, mesAnt);
            card.add(delta);
        }

        return card;
    }

    private Span crearDeltaMes(long actual, long anterior) {
        Span delta = new Span();
        delta.getStyle().set("font-size", "0.72rem").set("font-weight", "600").set("margin-top", "2px");

        if (anterior == 0 && actual == 0) {
            delta.setText("Sin datos este mes");
            delta.getStyle().set("color", "var(--lumo-tertiary-text-color, #94a3b8)");
        } else if (anterior == 0) {
            delta.setText("↑ nuevo este mes");
            delta.getStyle().set("color", "#16a34a");
        } else {
            long diff = actual - anterior;
            double pct = Math.abs(diff * 100.0 / anterior);
            String pctStr = pct >= 1 ? String.format("%.0f%%", pct) : "<1%";
            if (diff > 0) {
                delta.setText("↑ " + pctStr + " vs mes ant.");
                delta.getStyle().set("color", "#16a34a");
            } else if (diff < 0) {
                delta.setText("↓ " + pctStr + " vs mes ant.");
                delta.getStyle().set("color", "#dc2626");
            } else {
                delta.setText("= igual que mes ant.");
                delta.getStyle().set("color", "var(--lumo-secondary-text-color, #64748b)");
            }
        }
        return delta;
    }

    // ── Timeline de actividad reciente ─────────────────────────────────────────
    private Div crearTimeline() {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color, white)")
                .set("border-radius", "14px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.08)")
                .set("padding", "20px 24px")
                .set("margin-top", "16px")
                .set("width", "100%");

        Span titulo = new Span("Actividad reciente");
        titulo.getStyle()
                .set("font-weight", "700")
                .set("font-size", "1rem")
                .set("color", "var(--lumo-header-text-color, #1e293b)")
                .set("display", "block")
                .set("margin-bottom", "16px");

        List<EventoActividad> eventos = obtenerActividadReciente();

        Div feed = new Div();
        feed.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "0");

        if (eventos.isEmpty()) {
            Span vacio = new Span("No hay actividad reciente registrada.");
            vacio.getStyle().set("color", "var(--lumo-secondary-text-color, #64748b)")
                    .set("font-size", "0.875rem");
            feed.add(vacio);
        } else {
            for (int i = 0; i < eventos.size(); i++) {
                feed.add(crearEntradaTimeline(eventos.get(i), i == eventos.size() - 1));
            }
        }

        card.add(titulo, feed);
        return card;
    }

    private Div crearEntradaTimeline(EventoActividad ev, boolean esUltimo) {
        // Punto del timeline
        Div punto = new Div();
        punto.getStyle()
                .set("width", "10px").set("height", "10px")
                .set("border-radius", "50%")
                .set("background-color", ev.color())
                .set("flex-shrink", "0")
                .set("margin-top", "5px");

        // Línea vertical conectora
        Div lineaWrapper = new Div(punto);
        lineaWrapper.getStyle()
                .set("display", "flex").set("flex-direction", "column")
                .set("align-items", "center").set("width", "20px").set("flex-shrink", "0");

        if (!esUltimo) {
            Div linea = new Div();
            linea.getStyle()
                    .set("width", "2px").set("flex-grow", "1").set("min-height", "24px")
                    .set("background-color", "var(--lumo-contrast-20pct, #e2e8f0)")
                    .set("margin-top", "4px");
            lineaWrapper.add(linea);
        }

        // Texto
        Span descripcion = new Span(ev.descripcion());
        descripcion.getStyle()
                .set("font-size", "0.875rem").set("font-weight", "500")
                .set("color", "var(--lumo-header-text-color, #1e293b)");

        Span tiempo = new Span(tiempoRelativo(ev.tiempo()));
        tiempo.getStyle()
                .set("font-size", "0.75rem")
                .set("color", "var(--lumo-secondary-text-color, #64748b)")
                .set("margin-left", "8px");

        Div textoRow = new Div(descripcion, tiempo);
        textoRow.getStyle().set("display", "flex").set("align-items", "baseline")
                .set("flex-wrap", "wrap").set("gap", "0");

        Div entrada = new Div(lineaWrapper, textoRow);
        entrada.getStyle()
                .set("display", "flex").set("gap", "12px")
                .set("align-items", "flex-start").set("min-height", "32px");
        return entrada;
    }

    private List<EventoActividad> obtenerActividadReciente() {
        List<EventoActividad> eventos = new ArrayList<>();

        // Conversiones exitosas
        documentoConvertidoService.obtenerRecientes(8).forEach(doc -> {
            if (doc.getFechaConversion() == null) return;
            String codigo = doc.getArchivo() != null ? doc.getArchivo().getCodigo() : "?";
            String nro = doc.getNumeroComprobante() != null && !doc.getNumeroComprobante().isBlank()
                    ? " · Nro. " + doc.getNumeroComprobante() : "";
            String razon = doc.getRazonSocial() != null && !doc.getRazonSocial().isBlank()
                    ? " (" + doc.getRazonSocial() + ")" : "";
            eventos.add(new EventoActividad(
                    doc.getFechaConversion(),
                    "Archivo " + codigo + nro + razon + " — convertido exitosamente",
                    "#16a34a"
            ));
        });

        // Errores recientes
        archivoService.obtenerErroresRecientes(5).forEach(arch -> {
            if (arch.getFechaCreacion() == null) return;
            String msg = arch.getMensajeError() != null && !arch.getMensajeError().isBlank()
                    ? ": " + arch.getMensajeError().substring(0, Math.min(60, arch.getMensajeError().length()))
                    : "";
            eventos.add(new EventoActividad(
                    arch.getFechaCreacion(),
                    "Archivo " + arch.getCodigo() + " — error en conversión" + msg,
                    "#dc2626"
            ));
        });

        // Archivos nuevos cargados (últimos 5)
        archivoService.obtenerRecientes(5).forEach(arch -> {
            if (arch.getFechaCreacion() == null) return;
            // Solo mostrar como "nuevo" si no está ya cubierto por otro evento
            boolean yaIncluido = eventos.stream().anyMatch(e ->
                    e.descripcion().startsWith("Archivo " + arch.getCodigo() + " —"));
            if (!yaIncluido) {
                eventos.add(new EventoActividad(
                        arch.getFechaCreacion(),
                        "Archivo " + arch.getCodigo() + " — cargado al sistema",
                        "#2563eb"
                ));
            }
        });

        return eventos.stream()
                .sorted(Comparator.comparing(EventoActividad::tiempo).reversed())
                .limit(12)
                .collect(Collectors.toList());
    }

    private String tiempoRelativo(LocalDateTime tiempo) {
        LocalDateTime ahora = LocalDateTime.now();
        long minutos = ChronoUnit.MINUTES.between(tiempo, ahora);
        if (minutos < 1) return "ahora mismo";
        if (minutos < 60) return "hace " + minutos + " min";
        long horas = ChronoUnit.HOURS.between(tiempo, ahora);
        if (horas < 24) return "hace " + horas + " h";
        long dias = ChronoUnit.DAYS.between(tiempo.toLocalDate(), ahora.toLocalDate());
        if (dias < 30) return "hace " + dias + " d";
        return tiempo.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private record EventoActividad(LocalDateTime tiempo, String descripcion, String color) {}

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
                .set("min-width", minWidth)
                .set("position", "relative");
        if (maxWidth != null) card.getStyle().set("max-width", maxWidth);

        Span tituloSpan = new Span(titulo);
        tituloSpan.getStyle()
                .set("font-weight", "600")
                .set("font-size", "0.9rem")
                .set("color", "var(--lumo-header-text-color, #1e293b)")
                .set("display", "block")
                .set("margin-bottom", "14px");

        Div wrapper = new Div(crearSkeleton("200px"), canvas);
        wrapper.getStyle().set("position", "relative");
        card.add(tituloSpan, wrapper);
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

    // ── Carga/recarga el gráfico de montos según período ─────────────────────
    private void cargarMontoChart(String periodo) {
        double[] datos;
        String[] etiquetas;
        switch (periodo) {
            case "mes":
                datos     = documentoConvertidoService.montosPorMes();
                etiquetas = documentoConvertidoService.etiquetasMes();
                break;
            case "anio":
                datos     = documentoConvertidoService.montosPorAnio();
                etiquetas = documentoConvertidoService.etiquetasAnio();
                break;
            default:
                datos     = documentoConvertidoService.montosPorDia();
                etiquetas = documentoConvertidoService.etiquetasDias();
        }
        String montosData   = Arrays.stream(datos).mapToObj(String::valueOf).collect(Collectors.joining(",", "[", "]"));
        String montosLabels = "[\"" + String.join("\",\"", etiquetas) + "\"]";
        barMontosCanvas.getElement().executeJs(loaderScript(montosScript(montosLabels, montosData)));
    }

    private void cargarMontoChartRango(LocalDate desde, LocalDate hasta) {
        double[]   datos     = documentoConvertidoService.montosPorRango(desde, hasta);
        String[]   etiquetas = documentoConvertidoService.etiquetasRango(desde, hasta);
        String montosData   = Arrays.stream(datos).mapToObj(String::valueOf).collect(Collectors.joining(",", "[", "]"));
        String montosLabels = "[\"" + String.join("\",\"", etiquetas) + "\"]";
        barMontosCanvas.getElement().executeJs(loaderScript(montosScript(montosLabels, montosData)));
    }

    private void activarBotonMonto(Button btn) {
        if (btnMontoActivo != null) {
            btnMontoActivo.getStyle()
                    .set("background", "transparent")
                    .set("color", "var(--lumo-secondary-text-color, #64748b)")
                    .set("font-weight", "400")
                    .set("border-color", "var(--lumo-contrast-20pct, #e2e8f0)");
        }
        btnMontoActivo = btn;
        btn.getStyle()
                .set("background", "#002060")
                .set("color", "white")
                .set("font-weight", "600")
                .set("border-color", "#002060");
    }

    // ── Envuelve el script con el loader de Chart.js (elimina skeleton al cargar)
    private String loaderScript(String chartJs) {
        return "const me = this;" +
               "const skeleton = me.parentElement ? me.parentElement.querySelector('.chart-skeleton') : null;" +
               "const render = () => {" +
               "  if (skeleton) skeleton.style.display = 'none';" +
               "  me.style.opacity = '0'; me.style.transition = 'opacity 0.3s ease';" +
               "  " + chartJs +
               "  requestAnimationFrame(() => { me.style.opacity = '1'; });" +
               "};" +
               "if (typeof Chart !== 'undefined') { render(); } else {" +
               "  const s = document.createElement('script');" +
               "  s.src = 'https://cdn.jsdelivr.net/npm/chart.js@4/dist/chart.umd.min.js';" +
               "  s.onload = render;" +
               "  document.head.appendChild(s); }";
    }

    // ── Crea un skeleton shimmer para el chart ────────────────────────────────
    private Div crearSkeleton(String height) {
        Div skeleton = new Div();
        skeleton.addClassName("chart-skeleton");
        skeleton.getStyle()
                .set("width", "100%")
                .set("height", height)
                .set("border-radius", "8px")
                .set("background", "linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%)")
                .set("background-size", "200% 100%")
                .set("animation", "shimmer 1.5s infinite");
        skeleton.getElement().executeJs(
            "if (!document.getElementById('shimmer-keyframes')) {" +
            "  const s = document.createElement('style');" +
            "  s.id = 'shimmer-keyframes';" +
            "  s.textContent = '@keyframes shimmer { 0%{background-position:200% 0} 100%{background-position:-200% 0} }';" +
            "  document.head.appendChild(s);" +
            "}"
        );
        return skeleton;
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

    // ── Script Chart.js: Línea de montos (reutiliza canvas, destruye chart previo)
    private String montosScript(String labels, String data) {
        return "const isDark = document.documentElement.getAttribute('theme') === 'dark';" +
               "const lineColor = isDark ? '#60a5fa' : '#2563eb';" +
               "const fillColor = isDark ? 'rgba(96,165,250,0.10)' : 'rgba(37,99,235,0.07)';" +
               "const tickClr   = isDark ? '#94a3b8' : '#64748b';" +
               "const gridClr   = isDark ? 'rgba(255,255,255,0.07)' : 'rgba(0,0,0,0.05)';" +
               "if (me._montosChart) { me._montosChart.destroy(); }" +
               "const cvs = me._montosCvs || (() => {" +
               "  const c = document.createElement('canvas'); c.style.maxHeight='200px';" +
               "  me.appendChild(c); me._montosCvs = c; return c;" +
               "})();" +
               "me._montosChart = new Chart(cvs, { type: 'line', data: {" +
               "  labels: " + labels + "," +
               "  datasets: [{ data: " + data + "," +
               "    borderColor: lineColor, backgroundColor: fillColor," +
               "    borderWidth: 2, pointRadius: 3, pointHoverRadius: 5," +
               "    fill: true, tension: 0.4 }]" +
               "}, options: { responsive: true, maintainAspectRatio: true," +
               "  plugins: { legend: { display: false }," +
               "    tooltip: { callbacks: { label: function(ctx) {" +
               "      return '$ ' + ctx.parsed.y.toLocaleString('es-AR', {minimumFractionDigits:2,maximumFractionDigits:2});" +
               "    }}}}," +
               "  scales: {" +
               "    x: { ticks: { color: tickClr, font: { size: 11 } }, grid: { color: gridClr } }," +
               "    y: { beginAtZero: true, ticks: { color: tickClr, font: { size: 11 }," +
               "      callback: function(v) { return '$' + v.toLocaleString('es-AR'); }" +
               "    }, grid: { color: gridClr } }" +
               "  }}" +
               "});";
    }

    // ── Script Chart.js: Barra horizontal de top proveedores ─────────────────
    private String proveedoresScript(String labels, String data) {
        return "const isDark = document.documentElement.getAttribute('theme') === 'dark';" +
               "const palette = isDark" +
               "  ? ['rgba(96,165,250,.8)','rgba(52,211,153,.8)','rgba(251,191,36,.8)'," +
               "     'rgba(248,113,113,.8)','rgba(167,139,250,.8)','rgba(34,211,238,.8)'," +
               "     'rgba(249,115,22,.8)','rgba(236,72,153,.8)']" +
               "  : ['rgba(37,99,235,.8)','rgba(5,150,105,.8)','rgba(217,119,6,.8)'," +
               "     'rgba(220,38,38,.8)','rgba(124,58,237,.8)','rgba(14,165,233,.8)'," +
               "     'rgba(234,88,12,.8)','rgba(219,39,119,.8)'];" +
               "const tickClr = isDark ? '#94a3b8' : '#64748b';" +
               "const gridClr = isDark ? 'rgba(255,255,255,0.07)' : 'rgba(0,0,0,0.05)';" +
               "const dataArr = " + data + ";" +
               "const cvs = document.createElement('canvas');" +
               "cvs.style.cssText = 'width:100%;height:100%;';" +
               "me.appendChild(cvs);" +
               "new Chart(cvs, { type: 'bar', data: {" +
               "  labels: " + labels + "," +
               "  datasets: [{ data: dataArr," +
               "    backgroundColor: palette.slice(0, dataArr.length)," +
               "    borderWidth: 0, borderRadius: 4 }]" +
               "}, options: { indexAxis: 'y', responsive: true, maintainAspectRatio: false," +
               "  plugins: { legend: { display: false }," +
               "    tooltip: { callbacks: { label: function(ctx) {" +
               "      return ctx.parsed.x + (ctx.parsed.x === 1 ? ' factura' : ' facturas');" +
               "    }}}}," +
               "  scales: {" +
               "    x: { beginAtZero: true," +
               "      ticks: { stepSize: 1, precision: 0, color: tickClr, font: { size: 11 } }," +
               "      grid: { color: gridClr } }," +
               "    y: { ticks: { color: tickClr, font: { size: 12 } }, grid: { display: false } }" +
               "  }}" +
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
