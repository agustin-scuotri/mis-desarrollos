package com.desarrollos.views;

import com.desarrollos.base.Toast;
import com.desarrollos.entities.Usuario;
import com.desarrollos.services.UsuarioService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
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

@PageTitle("Usuarios")
@Route(value = "usuarios", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class AbmUsuariosView extends VerticalLayout {

    private final UsuarioService usuarioService;
    private final Grid<Usuario> grid = new Grid<>(Usuario.class, false);

    public AbmUsuariosView(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);
        getStyle().set("background-color", "var(--lumo-contrast-5pct, #f8fafc)");

        // ── Cabecera ──────────────────────────────────────────────────────────
        H2 titulo = new H2("Gestión de Usuarios");
        titulo.getStyle()
                .set("margin", "0")
                .set("font-size", "1.4rem")
                .set("font-weight", "700")
                .set("color", "var(--lumo-header-text-color, #1e293b)")
                .set("letter-spacing", "-0.3px");

        Button btnNuevo = new Button("Nuevo usuario", VaadinIcon.PLUS.create());
        btnNuevo.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNuevo.getStyle().set("background-color", "#002060").set("color", "white");
        btnNuevo.addClickListener(e -> abrirDialogo(null));

        HorizontalLayout barra = new HorizontalLayout(titulo, btnNuevo);
        barra.setWidthFull();
        barra.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        barra.setFlexGrow(1, titulo);
        barra.getStyle()
                .set("background", "var(--lumo-base-color, white)")
                .set("border-radius", "12px")
                .set("padding", "16px 20px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.08)");

        // ── Grid ──────────────────────────────────────────────────────────────
        configurarGrid();

        add(barra, grid);
        setFlexGrow(1, grid);
        cargarDatos();
    }

    private void configurarGrid() {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        grid.getStyle()
                .set("border-radius", "12px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.08)")
                .set("background", "var(--lumo-base-color, white)");

        grid.addColumn(Usuario::getUsername).setHeader("Usuario").setAutoWidth(true).setSortable(true);
        grid.addColumn(Usuario::getEmail).setHeader("Email").setAutoWidth(true).setSortable(true);
        grid.addComponentColumn(u -> {
            Span badge = new Span(u.getRol());
            badge.getStyle()
                    .set("background-color", "ADMIN".equals(u.getRol()) ? "#002060" : "#e2e8f0")
                    .set("color", "ADMIN".equals(u.getRol()) ? "white" : "#475569")
                    .set("border-radius", "8px")
                    .set("padding", "2px 10px")
                    .set("font-size", "0.75rem")
                    .set("font-weight", "600");
            return badge;
        }).setHeader("Rol").setAutoWidth(true);
        grid.addComponentColumn(u -> {
            Span estado = new Span(u.isHabilitado() ? "Activo" : "Inactivo");
            estado.getStyle()
                    .set("color", u.isHabilitado() ? "#16a34a" : "#dc2626")
                    .set("font-weight", "600")
                    .set("font-size", "0.85rem");
            return estado;
        }).setHeader("Estado").setAutoWidth(true);
        grid.addComponentColumn(u -> crearBotonesAccion(u)).setHeader("Acciones").setAutoWidth(true).setFlexGrow(0);
    }

    private HorizontalLayout crearBotonesAccion(Usuario u) {
        Button btnEditar = new Button(VaadinIcon.EDIT.create());
        btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        btnEditar.setTooltipText("Editar");
        btnEditar.addClickListener(e -> abrirDialogo(u));

        Button btnEliminar = new Button(VaadinIcon.TRASH.create());
        btnEliminar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        btnEliminar.setTooltipText("Eliminar");

        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        btnEliminar.setEnabled(!u.getUsername().equals(usuarioActual));

        btnEliminar.addClickListener(e -> {
            ConfirmDialog confirm = new ConfirmDialog();
            confirm.setHeader("Eliminar usuario");
            confirm.setText("¿Eliminar al usuario \"" + u.getUsername() + "\"? Esta acción no se puede deshacer.");
            confirm.setCancelable(true);
            confirm.setCancelText("Cancelar");
            confirm.setConfirmText("Eliminar");
            confirm.setConfirmButtonTheme("error primary");
            confirm.addConfirmListener(ev -> {
                usuarioService.eliminar(u.getId());
                Toast.show(getUI().orElse(null), "Usuario eliminado");
                cargarDatos();
            });
            confirm.open();
        });

        HorizontalLayout acciones = new HorizontalLayout(btnEditar, btnEliminar);
        acciones.setSpacing(false);
        acciones.getStyle().set("gap", "4px");
        return acciones;
    }

    private void abrirDialogo(Usuario usuarioEditar) {
        boolean esNuevo = (usuarioEditar == null);
        Dialog dialog = new Dialog();
        dialog.setWidth("440px");
        dialog.setCloseOnOutsideClick(false);

        H3 tituloDialog = new H3(esNuevo ? "Nuevo usuario" : "Editar usuario");
        tituloDialog.getStyle().set("margin", "0 0 16px 0").set("font-size", "1.1rem");

        TextField txtUsername = new TextField("Usuario");
        txtUsername.setWidthFull();
        txtUsername.setRequired(true);

        EmailField txtEmail = new EmailField("Email");
        txtEmail.setWidthFull();
        txtEmail.setRequired(true);

        PasswordField txtPassword = new PasswordField(esNuevo ? "Contraseña" : "Nueva contraseña (dejar vacío para no cambiar)");
        txtPassword.setWidthFull();
        if (esNuevo) txtPassword.setRequired(true);

        ComboBox<String> cmbRol = new ComboBox<>("Rol");
        cmbRol.setItems("USER", "ADMIN");
        cmbRol.setWidthFull();
        cmbRol.setRequired(true);

        Checkbox chkHabilitado = new Checkbox("Habilitado");

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

        Button btnGuardar = new Button(esNuevo ? "Crear usuario" : "Guardar cambios");
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnGuardar.getStyle().set("background-color", "#002060").set("color", "white");

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnCancelar.addClickListener(e -> dialog.close());

        btnGuardar.addClickListener(e -> {
            errorMsg.setVisible(false);
            String username = txtUsername.getValue().trim();
            String email = txtEmail.getValue().trim();
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
                Toast.show(getUI().orElse(null), "Usuario creado exitosamente");
            } else {
                usuarioEditar.setUsername(username);
                usuarioEditar.setEmail(email);
                usuarioEditar.setRol(cmbRol.getValue());
                usuarioEditar.setHabilitado(chkHabilitado.getValue());
                usuarioService.actualizar(usuarioEditar, password.isBlank() ? null : password);
                Toast.show(getUI().orElse(null), "Usuario actualizado");
            }
            dialog.close();
            cargarDatos();
        });

        HorizontalLayout botones = new HorizontalLayout(btnGuardar, btnCancelar);
        botones.getStyle().set("margin-top", "8px");

        VerticalLayout contenido = new VerticalLayout(
                tituloDialog, txtUsername, txtEmail, txtPassword, cmbRol, chkHabilitado, errorMsg, botones);
        contenido.setPadding(false);
        contenido.setSpacing(false);
        contenido.getStyle().set("gap", "12px");

        dialog.add(contenido);
        dialog.open();
    }

    private void cargarDatos() {
        List<Usuario> usuarios = usuarioService.listarTodos();
        grid.setItems(usuarios);
    }
}
