package com.desarrollos.base;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent; // Necesario para alineación

public abstract class FormView extends VerticalLayout {

    protected H2 tituloFormulario;
    protected VerticalLayout contenidoPrincipal;
    protected Button btnGuardar;
    protected Button btnCancelar;
    protected HorizontalLayout barraBotones;
    protected HorizontalLayout cabecera; // Nuevo contenedor superior

    public FormView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("font-family", "Verdana, sans-serif");

        configurarEstructuraBase();
    }

    private void configurarEstructuraBase() {
        // 1. Configurar Título
        tituloFormulario = new H2();
        tituloFormulario.getStyle()
            .set("color", "#002060")
            .set("margin", "0"); // Quitamos margen para que alinee bien

        // 2. Configurar Botones
        btnGuardar = new Button(getTranslation("app.guardar"), VaadinIcon.CHECK.create());
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        btnGuardar.getStyle().set("font-family", "Verdana, sans-serif");
        btnGuardar.addClickListener(e -> accionGuardar());

        btnCancelar = new Button(getTranslation("app.cancelar"), VaadinIcon.ARROW_LEFT.create());
        btnCancelar.getStyle().set("font-family", "Verdana, sans-serif");
        btnCancelar.addClickListener(e -> accionCancelar());

        // 3. Barra de botones (ahora sin ancho completo para que quepa al lado del título)
        barraBotones = new HorizontalLayout(btnCancelar, btnGuardar);
        barraBotones.setSpacing(true);

        // 4. CABECERA: Aquí unimos título (izquierda) y botones (derecha)
        cabecera = new HorizontalLayout(tituloFormulario, barraBotones);
        cabecera.setWidthFull();
        cabecera.setAlignItems(FlexComponent.Alignment.CENTER); // Alineación vertical centrada
        cabecera.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN); // Título a la izq, Botones a la der
        cabecera.setPadding(false);

        // 5. Contenido Principal (el centro del formulario)
        contenidoPrincipal = new VerticalLayout();
        contenidoPrincipal.setPadding(false);
        contenidoPrincipal.setSpacing(true);
        contenidoPrincipal.setWidthFull();

        // Agregamos a la vista en orden: Cabecera primero, luego contenido
        add(cabecera, contenidoPrincipal);
    }

    protected void setTitulo(String texto) {
        tituloFormulario.setText(texto);
    }

    protected abstract void configurarCampos();
    protected abstract void accionGuardar();
    protected abstract void accionCancelar();
}