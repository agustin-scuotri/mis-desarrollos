package com.desarrollos.base;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;

public abstract class FormView extends VerticalLayout {

    protected H2 tituloFormulario;
    protected VerticalLayout contenidoPrincipal;
    protected Button btnGuardar;
    protected Button btnCancelar;
    protected HorizontalLayout barraBotones;
    protected HorizontalLayout cabecera;

    public FormView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("background-color", "var(--lumo-contrast-5pct, #f8fafc)");

        configurarEstructuraBase();
    }

    private void configurarEstructuraBase() {
        // 1. Título
        tituloFormulario = new H2();
        tituloFormulario.getStyle()
            .set("color", "var(--lumo-header-text-color, #1e293b)")
            .set("font-size", "1.4rem")
            .set("font-weight", "700")
            .set("letter-spacing", "-0.3px")
            .set("margin", "0");

        // 2. Botones
        btnGuardar = new Button(getTranslation("app.guardar"), VaadinIcon.CHECK.create());
        btnGuardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        btnGuardar.addClickListener(e -> accionGuardar());

        btnCancelar = new Button(getTranslation("app.cancelar"), VaadinIcon.ARROW_LEFT.create());
        btnCancelar.addClickListener(e -> accionCancelar());

        // 3. Barra de botones
        barraBotones = new HorizontalLayout(btnCancelar, btnGuardar);
        barraBotones.setSpacing(true);

        // 4. Cabecera: título (izquierda) + botones (derecha)
        cabecera = new HorizontalLayout(tituloFormulario, barraBotones);
        cabecera.setWidthFull();
        cabecera.setAlignItems(FlexComponent.Alignment.CENTER);
        cabecera.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        cabecera.setPadding(false);

        // 5. Contenido principal con estilo de card
        contenidoPrincipal = new VerticalLayout();
        contenidoPrincipal.setPadding(false);
        contenidoPrincipal.setSpacing(true);
        contenidoPrincipal.setWidthFull();
        contenidoPrincipal.getStyle()
                .set("background", "var(--lumo-base-color, white)")
                .set("border-radius", "14px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.05)")
                .set("padding", "24px");

        add(cabecera, contenidoPrincipal);
    }

    protected void setTitulo(String texto) {
        tituloFormulario.setText(texto);
    }

    protected abstract void configurarCampos();
    protected abstract void accionGuardar();
    protected abstract void accionCancelar();
}
