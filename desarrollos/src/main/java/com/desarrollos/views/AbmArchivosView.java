package com.desarrollos.views;

import com.desarrollos.base.CrudView;
import com.desarrollos.entities.Archivo;
import com.desarrollos.services.ArchivoService;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.SortDirection;
import org.springframework.data.domain.Sort;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Archivos")
@Route(value = "ABMarchivos", layout = MainLayout.class)
public class AbmArchivosView extends CrudView<Archivo> implements BeforeEnterObserver {
    private final ArchivoService service;
    private CallbackDataProvider<Archivo, Void> gridProvider;
    private ComboBox<String> filtroEstado;

    public AbmArchivosView(ArchivoService service) {
        super(Archivo.class);
        this.service = service;
        setTitulo(getTranslation("app.archivos"));
        inicializarDataProvider();
    }

    private void inicializarDataProvider() {
        gridProvider = DataProvider.fromCallbacks(
            (Query<Archivo, Void> query) -> {
                String codigo   = filtrosActivos.getOrDefault(getTranslation("archivo.codigo"), "");
                String nombre   = filtrosActivos.getOrDefault(getTranslation("archivo.nombre"), "");
                String estadoDB = mapearEstado(filtrosActivos.getOrDefault(getTranslation("archivo.estado"), ""));
                Sort sort = query.getSortOrders().stream()
                        .findFirst()
                        .map(o -> Sort.by(
                                o.getDirection() == SortDirection.ASCENDING ? Sort.Direction.ASC : Sort.Direction.DESC,
                                "codigoNumerico"))
                        .orElse(Sort.by(Sort.Direction.ASC, "codigoNumerico"));
                int pageSize = Math.max(query.getLimit(), 1);
                int pageNum  = query.getOffset() / pageSize;
                return service.listarPaginado(pageNum, pageSize, codigo, nombre, estadoDB, sort).stream();
            },
            (Query<Archivo, Void> query) -> {
                String codigo   = filtrosActivos.getOrDefault(getTranslation("archivo.codigo"), "");
                String nombre   = filtrosActivos.getOrDefault(getTranslation("archivo.nombre"), "");
                String estadoDB = mapearEstado(filtrosActivos.getOrDefault(getTranslation("archivo.estado"), ""));
                return (int) service.contarFiltrado(codigo, nombre, estadoDB);
            }
        );
        grid.setItems(gridProvider);
    }

    @Override
    protected void configurarColumnasEspecificas() {
        grid.removeAllColumns();
        agregarColumna(Archivo::getCodigo, getTranslation("archivo.codigo"));
        agregarColumna(a -> {
            String n = a.getNombre();
            if (n == null) return "";
            return n.length() > 20 ? n.substring(0, 20) + "..." : n;
        }, getTranslation("archivo.nombre"));
        agregarColumnaEstado(getTranslation("archivo.estado"));
    }

    @Override
    protected void actualizarLista() {
        if (gridProvider != null) gridProvider.refreshAll();
    }

    private String mapearEstado(String displayValue) {
        if (displayValue == null || displayValue.isEmpty()) return "";
        return switch (displayValue.toLowerCase()) {
            case "pendiente a procesar" -> "PENDIENTE";
            case "procesado"            -> "PROCESADO";
            case "procesado error"      -> "PROCESADO_ERROR";
            default                     -> "";
        };
    }

    private void agregarColumnaEstado(String cabecera) {
        ComboBox<String> filtro = new ComboBox<>();
        this.filtroEstado = filtro;
        filtro.setItems("Todos", "Pendiente a procesar", "Procesado", "Procesado error");
        filtro.setValue("Todos");
        filtro.setClearButtonVisible(true);
        filtro.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
        filtro.setWidthFull();
        filtro.getStyle().set("font-size", "0.875rem");
        filtro.addValueChangeListener(e -> {
            String sel = e.getValue();
            ejecutarFiltro(cabecera, (sel == null || sel.equals("Todos")) ? "" : sel);
        });

        VerticalLayout layoutCabecera = new VerticalLayout(crearTituloCabecera(cabecera), filtro);
        layoutCabecera.setAlignItems(FlexComponent.Alignment.CENTER);
        layoutCabecera.setSpacing(false);
        layoutCabecera.setPadding(false);

        grid.addComponentColumn(archivo -> {
            String estado = archivo.getEstadoConversion();
            if (estado == null) estado = "PENDIENTE";

            Icon icono;
            Span texto;
            String bgColor, fgColor;

            if ("PROCESADO".equals(estado)) {
                icono = VaadinIcon.CHECK_CIRCLE.create();
                texto = new Span("Procesado");
                bgColor = "#dcfce7";
                fgColor = "#16a34a";
            } else if ("PROCESADO_ERROR".equals(estado)) {
                icono = VaadinIcon.WARNING.create();
                texto = new Span("Error");
                bgColor = "#fee2e2";
                fgColor = "#dc2626";

                icono.setSize("13px");
                icono.getStyle().set("color", fgColor);
                texto.getStyle().set("font-size", "0.75rem").set("font-weight", "600").set("color", fgColor);

                HorizontalLayout errorPill = new HorizontalLayout(icono, texto);
                errorPill.setAlignItems(FlexComponent.Alignment.CENTER);
                errorPill.setSpacing(false);
                errorPill.setPadding(false);
                errorPill.getStyle()
                        .set("background-color", bgColor)
                        .set("border-radius", "20px")
                        .set("padding", "4px 10px")
                        .set("gap", "5px")
                        .set("display", "inline-flex")
                        .set("align-items", "center")
                        .set("cursor", "pointer");

                errorPill.addClickListener(e -> {
                    Dialog dialog = new Dialog();
                    dialog.setModal(true);
                    dialog.setWidth("500px");

                    Icon iconoD = VaadinIcon.WARNING.create();
                    iconoD.setSize("48px");
                    iconoD.setColor("#dc2626");

                    H3 titulo = new H3("Detalle del error");
                    titulo.getStyle().set("color", "#dc2626").set("margin", "8px 0 0 0").set("font-weight", "700");

                    String causa = archivo.getMensajeError();
                    Paragraph mensaje = new Paragraph(causa != null && !causa.isBlank() ? causa : "No hay información adicional disponible.");
                    mensaje.getStyle()
                            .set("text-align", "center").set("color", "#475569")
                            .set("font-size", "0.875rem").set("white-space", "pre-line").set("margin", "0");

                    VerticalLayout cajaDetalle = new VerticalLayout();
                    cajaDetalle.setPadding(false);
                    cajaDetalle.setSpacing(false);
                    cajaDetalle.getStyle()
                            .set("background-color", "#fef2f2").set("border", "1px solid #fecaca")
                            .set("border-radius", "8px").set("padding", "12px").set("gap", "4px")
                            .set("width", "100%").set("margin-top", "8px");
                    Span tituloDetalle = new Span("Motivo del error:");
                    tituloDetalle.getStyle().set("font-weight", "600").set("color", "#dc2626").set("font-size", "0.8rem");
                    Span cuerpo = new Span(causa != null && !causa.isBlank() ? causa : "No hay información adicional disponible.");
                    cuerpo.getStyle().set("color", "#991b1b").set("font-size", "0.8rem")
                            .set("word-break", "break-word").set("white-space", "pre-wrap");
                    cajaDetalle.add(tituloDetalle, cuerpo);

                    VerticalLayout contenido = new VerticalLayout(iconoD, titulo, cajaDetalle);
                    contenido.setAlignItems(FlexComponent.Alignment.CENTER);
                    contenido.setPadding(true);
                    contenido.setSpacing(true);

                    Button btnCerrar = new Button("Cerrar", ev -> dialog.close());
                    btnCerrar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    btnCerrar.getStyle().set("background-color", "#dc2626").set("color", "white");

                    dialog.add(contenido);
                    dialog.getFooter().add(btnCerrar);
                    dialog.open();
                });

                return errorPill;
            } else {
                icono = VaadinIcon.CLOCK.create();
                texto = new Span("Pendiente");
                bgColor = "#fef3c7";
                fgColor = "#d97706";
            }

            icono.setSize("13px");
            icono.getStyle().set("color", fgColor);
            texto.getStyle().set("font-size", "0.75rem").set("font-weight", "600").set("color", fgColor);

            // Badge pill
            HorizontalLayout pill = new HorizontalLayout(icono, texto);
            pill.setAlignItems(FlexComponent.Alignment.CENTER);
            pill.setSpacing(false);
            pill.setPadding(false);
            pill.getStyle()
                    .set("background-color", bgColor)
                    .set("border-radius", "20px")
                    .set("padding", "4px 10px")
                    .set("gap", "5px")
                    .set("display", "inline-flex")
                    .set("align-items", "center");
            return pill;
        })
        .setHeader(layoutCabecera)
        .setKey(cabecera)
        .setTextAlign(ColumnTextAlign.CENTER)
        .setAutoWidth(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        java.util.List<String> estadoParam = event.getLocation()
                .getQueryParameters().getParameters().get("estado");
        if (estadoParam != null && !estadoParam.isEmpty() && filtroEstado != null) {
            filtroEstado.setValue(estadoParam.get(0));
        }
    }

    @Override
    protected void accionNuevo() {
        getUI().ifPresent(ui -> ui.navigate(ArchivoView.class));
    }

    @Override
    protected void accionVisualizar(Archivo item) {
        getUI().ifPresent(ui -> ui.navigate(ArchivoView.class, item.getId() + "/read"));
    }

    @Override
    protected void accionEditar(Archivo item) {
        if ("PROCESADO".equals(item.getEstadoConversion())) {
            Notification.show("No se puede editar un archivo ya procesado. Usá Visualizar para ver su contenido.")
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        getUI().ifPresent(ui -> ui.navigate(ArchivoView.class, item.getId().toString()));
    }

    @Override
    protected void accionBorrar(Archivo item) {
        ConfirmDialog dialog = new ConfirmDialog();
        
        dialog.setHeader(getTranslation("app.borrar.titulo"));
        dialog.setText(getTranslation("app.borrar.mensaje", item.getNombre()));

        dialog.setCancelable(true);
        dialog.setCancelText(getTranslation("app.cancelar"));

        dialog.setConfirmText(getTranslation("app.borrar.confirmar"));
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(event -> {
            try {
                service.borrar(item);
                actualizarLista();
                Notification.show(getTranslation("app.borrar.exito"))
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                Notification.show("Error al borrar: " + e.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.open();
    }
}