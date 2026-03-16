package com.desarrollos.views;

import com.desarrollos.base.CrudView;
import com.desarrollos.base.Toast;
import com.desarrollos.entities.Usuario;
import com.desarrollos.services.UsuarioService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.stream.Collectors;

@PageTitle("Usuarios")
@Route(value = "usuarios", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class AbmUsuariosView extends CrudView<Usuario> {

    private final UsuarioService usuarioService;

    public AbmUsuariosView(UsuarioService usuarioService) {
        super(Usuario.class);
        this.usuarioService = usuarioService;
        setTitulo("Usuarios");
        btnNuevo.setText("Crear usuario");
        btnNuevo.setIcon(VaadinIcon.PLUS.create());
        btnNuevo.setIconAfterText(true);
        actualizarLista();
    }

    @Override
    protected void configurarColumnasEspecificas() {
        grid.removeAllColumns();
        agregarColumna(Usuario::getUsername, "Usuario");
        agregarColumna(Usuario::getEmail, "Email");
        agregarColumna(Usuario::getRol, "Rol");
        agregarColumnaBooleana(Usuario::isHabilitado, "Habilitado");
    }

    @Override
    protected void actualizarLista() {
        String filtroUsuario   = filtrosActivos.getOrDefault("Usuario", "");
        String filtroEmail     = filtrosActivos.getOrDefault("Email", "");
        String filtroRol       = filtrosActivos.getOrDefault("Rol", "");
        String filtroHabilitado = filtrosActivos.getOrDefault("Habilitado", "");

        List<Usuario> todos = usuarioService.listarTodos();
        List<Usuario> filtrados = todos.stream()
                .filter(u -> filtroUsuario.isBlank()   || u.getUsername().toLowerCase().contains(filtroUsuario.toLowerCase()))
                .filter(u -> filtroEmail.isBlank()     || u.getEmail().toLowerCase().contains(filtroEmail.toLowerCase()))
                .filter(u -> filtroRol.isBlank()       || u.getRol().toLowerCase().contains(filtroRol.toLowerCase()))
                .filter(u -> {
                    if (filtroHabilitado.isBlank() || filtroHabilitado.equals("Todos")) return true;
                    return filtroHabilitado.equals("Sí") ? u.isHabilitado() : !u.isHabilitado();
                })
                .collect(Collectors.toList());

        int offset = paginaActual * filasPorPagina;
        totalRegistros = filtrados.size();
        List<Usuario> pagina = filtrados.stream()
                .skip(offset)
                .limit(filasPorPagina)
                .collect(Collectors.toList());

        grid.setItems(pagina);
        actualizarPaginacion();
    }

    @Override
    protected void accionNuevo() {
        abrirDialogo(null, false);
    }

    @Override
    protected void accionVisualizar(Usuario item) {
        abrirDialogo(item, true);
    }

    @Override
    protected void accionEditar(Usuario item) {
        abrirDialogo(item, false);
    }

    @Override
    protected void accionBorrar(Usuario item) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        if (item.getUsername().equals(usuarioActual)) {
            Toast.warning("No podés eliminar tu propio usuario.");
            return;
        }
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Eliminar usuario");
        confirm.setText("¿Eliminar al usuario \"" + item.getUsername() + "\"? Esta acción no se puede deshacer.");
        confirm.setCancelable(true);
        confirm.setCancelText("Cancelar");
        confirm.setConfirmText("Eliminar");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(ev -> {
            usuarioService.eliminar(item.getId());
            Toast.success("Usuario eliminado");
            actualizarLista();
        });
        confirm.open();
    }

    @Override
    protected boolean mostrarBotonEditar(Usuario item) {
        return true;
    }

    @Override
    protected String anchoColumnaAcciones() {
        return "130px";
    }

    // ── Diálogo crear / editar / ver ──────────────────────────────────────────
    private void abrirDialogo(Usuario usuarioEditar, boolean soloLectura) {
        boolean esNuevo = (usuarioEditar == null);
        Dialog dialog = new Dialog();
        dialog.setWidth("440px");
        dialog.setCloseOnOutsideClick(false);

        H3 tituloDialog = new H3(esNuevo ? "Nuevo usuario" : soloLectura ? "Ver usuario" : "Editar usuario");
        tituloDialog.getStyle().set("margin", "0 0 16px 0").set("font-size", "1.1rem");

        TextField txtUsername = new TextField("Usuario");
        txtUsername.setWidthFull();
        txtUsername.setRequired(!soloLectura);
        txtUsername.setReadOnly(soloLectura);

        EmailField txtEmail = new EmailField("Email");
        txtEmail.setWidthFull();
        txtEmail.setRequired(!soloLectura);
        txtEmail.setReadOnly(soloLectura);

        PasswordField txtPassword = new PasswordField(
                esNuevo ? "Contraseña" : "Nueva contraseña (dejar vacío para no cambiar)");
        txtPassword.setWidthFull();
        txtPassword.setRequired(esNuevo);
        txtPassword.setVisible(!soloLectura);

        ComboBox<String> cmbRol = new ComboBox<>("Rol");
        cmbRol.setItems("USER", "ADMIN");
        cmbRol.setWidthFull();
        cmbRol.setRequired(!soloLectura);
        cmbRol.setReadOnly(soloLectura);

        Checkbox chkHabilitado = new Checkbox("Habilitado");
        chkHabilitado.setReadOnly(soloLectura);

        if (!esNuevo) {
            txtUsername.setValue(usuarioEditar.getUsername());
            txtEmail.setValue(usuarioEditar.getEmail());
            cmbRol.setValue(usuarioEditar.getRol());
            chkHabilitado.setValue(usuarioEditar.isHabilitado());
        } else {
            cmbRol.setValue("USER");
            chkHabilitado.setValue(true);
        }

        Span errorMsg = new Span();
        errorMsg.getStyle().set("color", "#dc2626").set("font-size", "0.85rem");
        errorMsg.setVisible(false);

        VerticalLayout contenido = new VerticalLayout(
                tituloDialog, txtUsername, txtEmail, txtPassword, cmbRol, chkHabilitado, errorMsg);
        contenido.setPadding(false);
        contenido.setSpacing(false);
        contenido.getStyle().set("gap", "12px");
        dialog.add(contenido);

        if (soloLectura) {
            Button btnCerrar = new Button("Cerrar");
            btnCerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnCerrar.addClickListener(e -> dialog.close());
            dialog.getFooter().add(btnCerrar);
        } else {
            Button btnGuardar = new Button(esNuevo ? "Crear usuario" : "Guardar cambios");
            btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btnGuardar.getStyle().set("background-color", "#002060").set("color", "white");

            Button btnCancelar = new Button("Cancelar");
            btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnCancelar.addClickListener(e -> dialog.close());

            btnGuardar.addClickListener(e -> {
                errorMsg.setVisible(false);
                String username = txtUsername.getValue().trim();
                String email    = txtEmail.getValue().trim();
                String password = txtPassword.getValue();

                if (username.isBlank() || email.isBlank() || cmbRol.isEmpty()) {
                    errorMsg.setText("Completá todos los campos obligatorios.");
                    errorMsg.setVisible(true);
                    return;
                }
                if (esNuevo && password.isBlank()) {
                    errorMsg.setText("La contraseña es obligatoria para un nuevo usuario.");
                    errorMsg.setVisible(true);
                    return;
                }
                if (esNuevo && usuarioService.existeUsername(username)) {
                    errorMsg.setText("El nombre de usuario ya existe.");
                    errorMsg.setVisible(true);
                    return;
                }
                if (esNuevo && usuarioService.existeEmail(email)) {
                    errorMsg.setText("El email ya está registrado.");
                    errorMsg.setVisible(true);
                    return;
                }
                if (!esNuevo && usuarioService.existeUsernameParaOtroUsuario(username, usuarioEditar.getId())) {
                    errorMsg.setText("El nombre de usuario ya existe.");
                    errorMsg.setVisible(true);
                    return;
                }
                if (!esNuevo && usuarioService.existeEmailParaOtroUsuario(email, usuarioEditar.getId())) {
                    errorMsg.setText("El email ya está registrado.");
                    errorMsg.setVisible(true);
                    return;
                }

                if (esNuevo) {
                    Usuario nuevo = new Usuario();
                    nuevo.setUsername(username);
                    nuevo.setEmail(email);
                    nuevo.setPassword(password);
                    nuevo.setRol(cmbRol.getValue());
                    nuevo.setHabilitado(chkHabilitado.getValue());
                    usuarioService.guardar(nuevo);
                    Toast.success("Usuario creado exitosamente");
                } else {
                    usuarioEditar.setUsername(username);
                    usuarioEditar.setEmail(email);
                    usuarioEditar.setRol(cmbRol.getValue());
                    usuarioEditar.setHabilitado(chkHabilitado.getValue());
                    usuarioService.actualizar(usuarioEditar, password.isBlank() ? null : password);
                    Toast.success("Usuario actualizado");
                }
                dialog.close();
                actualizarLista();
            });

            dialog.getFooter().add(btnCancelar, btnGuardar);
        }

        dialog.open();
    }
}
