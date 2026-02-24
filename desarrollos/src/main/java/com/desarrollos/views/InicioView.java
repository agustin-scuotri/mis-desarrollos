package com.desarrollos.views;

import java.util.List;

import com.desarrollos.entities.Archivo;
import com.desarrollos.services.ArchivoService;
import com.desarrollos.services.DocumentoConvertidoService;
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
                .set("background-color", "#f8fafc")
                .set("min-height", "100%");

        // ── Encabezado ────────────────────────────────────────────────────────
        H2 titulo = new H2(getTranslation("app.bienvenida"));
        titulo.getStyle()
                .set("color", "#1e293b")
                .set("font-size", "1.5rem")
                .set("font-weight", "700")
                .set("letter-spacing", "-0.4px")
                .set("margin", "0 0 4px 0");

        Paragraph subtitulo = new Paragraph(getTranslation("app.seleccione"));
        subtitulo.getStyle()
                .set("color", "#64748b")
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

        // ── Cards de métricas ─────────────────────────────────────────────────
        HorizontalLayout cards = new HorizontalLayout(
            crearCard("Archivos Cargados",   String.valueOf(total),      VaadinIcon.COPY_O,       "#2563eb", "#eff6ff"),
            crearCard("Procesados",          String.valueOf(procesados), VaadinIcon.CHECK_CIRCLE, "#16a34a", "#f0fdf4"),
            crearCard("Pendientes",          String.valueOf(pendientes), VaadinIcon.CLOCK,        "#d97706", "#fffbeb"),
            crearCard("Errores",             String.valueOf(errores),    VaadinIcon.WARNING,      "#dc2626", "#fef2f2"),
            crearCard("Docs. Convertidos",   String.valueOf(documentos), VaadinIcon.FILE_TABLE,   "#7c3aed", "#f5f3ff")
        );
        cards.setWidthFull();
        cards.setSpacing(true);
        cards.getStyle().set("flex-wrap", "wrap");

        add(titulo, subtitulo, cards);
    }

    private VerticalLayout crearCard(String etiqueta, String valor, VaadinIcon icono,
                                     String color, String bgIcono) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(false);
        card.setSpacing(false);
        card.getStyle()
                .set("background", "white")
                .set("border-radius", "14px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.05)")
                .set("padding", "20px 24px")
                .set("flex", "1")
                .set("min-width", "150px")
                .set("gap", "6px");

        // Ícono con fondo tintado
        Icon icon = icono.create();
        icon.setSize("22px");
        icon.getStyle()
                .set("color", color)
                .set("background-color", bgIcono)
                .set("border-radius", "8px")
                .set("padding", "8px")
                .set("box-sizing", "content-box")
                .set("margin-bottom", "8px");

        // Valor grande
        Span valorSpan = new Span(valor);
        valorSpan.getStyle()
                .set("font-size", "2rem")
                .set("font-weight", "700")
                .set("color", "#1e293b")
                .set("line-height", "1")
                .set("letter-spacing", "-1px");

        // Etiqueta pequeña
        Span etiquetaSpan = new Span(etiqueta);
        etiquetaSpan.getStyle()
                .set("font-size", "0.8rem")
                .set("font-weight", "500")
                .set("color", "#64748b")
                .set("margin-top", "2px");

        card.add(icon, valorSpan, etiquetaSpan);
        return card;
    }
}
