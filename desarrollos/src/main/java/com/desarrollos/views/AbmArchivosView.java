package com.desarrollos.views;

import com.desarrollos.base.CrudView;
import com.desarrollos.entities.Archivo;
import com.desarrollos.services.ArchivoService;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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
        agregarColumnaBooleana(Archivo::isConvertido, getTranslation("archivo.estado"));
        agregarColumnaFecha(Archivo::getFechaCreacion, getTranslation("archivo.fechaCreacion"));
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
                // Filtrado por Estado (Booleano)
                else if (columna.equals(getTranslation("archivo.estado"))) {
                    String estadoTexto = archivo.isConvertido() ? "Sí" : "No";
                    if (!estadoTexto.equalsIgnoreCase(filtro.getValue())) return false;
                }
            }
            return true;
        }).collect(Collectors.toList());

        // 3. Pasamos los resultados filtrados al Grid
        // Si 'filtrados' está vacío, aparecerá el mensaje "No existen archivos"
        grid.setItems(filtrados);
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