package com.desarrollos.views;

import com.desarrollos.base.CrudView;
import com.desarrollos.entities.Archivo;
import com.desarrollos.services.ArchivoService;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@PageTitle("Archivos")
@Route(value = "ABMarchivos", layout = MainLayout.class)
public class AbmArchivosView extends CrudView<Archivo> {
    private final ArchivoService service;

    public AbmArchivosView(ArchivoService service) {
        super(Archivo.class);
        this.service = service;
        setTitulo(getTranslation("app.archivos"));
        actualizarLista();
    }

    @Override
    protected void configurarColumnasEspecificas() {
        grid.removeAllColumns();

        agregarColumna(Archivo::getCodigo, getTranslation("archivo.codigo"));
        agregarColumna(Archivo::getNombre, getTranslation("archivo.nombre"));
        agregarColumna(Archivo::getDescripcion, getTranslation("archivo.descripcion"));
        agregarColumnaEstado(getTranslation("archivo.estado"));
    }

    @Override
    protected void actualizarLista() {
        if (service == null) return;

        // 1. Obtenemos la lista completa del servicio
        List<Archivo> todos = service.listarTodos();

        // 2. Aplicamos los filtros del mapa filtrosActivos
        List<Archivo> filtrados = todos.stream().filter(archivo -> {
            for (Map.Entry<String, String> filtro : filtrosActivos.entrySet()) {
                String columna = filtro.getKey();
                String valorFiltro = filtro.getValue().toLowerCase();

                // Filtrado por Código
                if (columna.equals(getTranslation("archivo.codigo"))) {
                    String codigo = archivo.getCodigo() != null ? archivo.getCodigo().toString() : "";
                    if (!codigo.contains(valorFiltro)) return false;
                }
                // Filtrado por Nombre
                else if (columna.equals(getTranslation("archivo.nombre"))) {
                    String nombre = archivo.getNombre() != null ? archivo.getNombre().toLowerCase() : "";
                    if (!nombre.contains(valorFiltro)) return false;
                }
                // Filtrado por Descripción
                else if (columna.equals(getTranslation("archivo.descripcion"))) {
                    String desc = archivo.getDescripcion() != null ? archivo.getDescripcion().toLowerCase() : "";
                    if (!desc.contains(valorFiltro)) return false;
                }
                // Filtrado por Estado (3 estados)
                else if (columna.equals(getTranslation("archivo.estado"))) {
                    String estadoArch = archivo.getEstadoConversion();
                    if (estadoArch == null) estadoArch = "PENDIENTE";
                    boolean match;
                    if (valorFiltro.equals("pendiente a procesar")) {
                        match = "PENDIENTE".equalsIgnoreCase(estadoArch);
                    } else if (valorFiltro.equals("procesado")) {
                        match = "PROCESADO".equalsIgnoreCase(estadoArch);
                    } else if (valorFiltro.equals("procesado error")) {
                        match = "PROCESADO_ERROR".equalsIgnoreCase(estadoArch);
                    } else {
                        match = true;
                    }
                    if (!match) return false;
                }
            }
            return true;
        }).collect(Collectors.toList());

        // 3. Pasamos los resultados filtrados al Grid
        // Si 'filtrados' está vacío, aparecerá el mensaje "No existen archivos"
        grid.setItems(filtrados);
    }

    private void agregarColumnaEstado(String cabecera) {
        ComboBox<String> filtro = new ComboBox<>();
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

        Span textoCabecera = new Span(cabecera);
        textoCabecera.getStyle()
                .set("font-weight", "600")
                .set("color", "#334155");

        VerticalLayout layoutCabecera = new VerticalLayout(textoCabecera, filtro);
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
    protected void accionNuevo() {
        getUI().ifPresent(ui -> ui.navigate(ArchivoView.class));
    }

    @Override
    protected void accionVisualizar(Archivo item) {
        getUI().ifPresent(ui -> ui.navigate(ArchivoView.class, item.getId() + "/read"));
    }

    @Override
    protected void accionEditar(Archivo item) {
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