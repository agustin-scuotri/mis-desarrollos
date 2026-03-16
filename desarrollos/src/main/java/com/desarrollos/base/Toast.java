package com.desarrollos.base;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Toasts no-intrusivos en la esquina inferior derecha.
 * Reemplaza los Notification.show() genéricos que aparecían en el centro de pantalla.
 */
public final class Toast {

    private Toast() {}

    public static void success(String mensaje) {
        show(mensaje, VaadinIcon.CHECK_CIRCLE, NotificationVariant.LUMO_SUCCESS, 3000, false);
    }

    public static void error(String mensaje) {
        show(mensaje, VaadinIcon.EXCLAMATION_CIRCLE_O, NotificationVariant.LUMO_ERROR, 7000, true);
    }

    public static void warning(String mensaje) {
        show(mensaje, VaadinIcon.WARNING, NotificationVariant.LUMO_WARNING, 5000, false);
    }

    public static void info(String mensaje) {
        show(mensaje, VaadinIcon.INFO_CIRCLE_O, NotificationVariant.LUMO_PRIMARY, 3000, false);
    }

    // ── Implementación ────────────────────────────────────────────────────────
    private static void show(String mensaje, VaadinIcon iconoTipo,
                             NotificationVariant variante, int duracionMs, boolean conBotonCerrar) {

        Notification notif = new Notification();
        notif.addThemeVariants(variante);
        notif.setDuration(duracionMs);
        notif.setPosition(Notification.Position.BOTTOM_END);

        Icon icono = iconoTipo.create();
        icono.setSize("18px");
        icono.getStyle().set("flex-shrink", "0");

        Span texto = new Span(mensaje);
        texto.getStyle()
                .set("font-size", "0.875rem")
                .set("font-weight", "500")
                .set("line-height", "1.4");

        HorizontalLayout layout = new HorizontalLayout(icono, texto);
        layout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        layout.setSpacing(false);
        layout.getStyle()
                .set("gap", "10px")
                .set("padding", "2px 0");

        if (conBotonCerrar) {
            Icon closeIcon = VaadinIcon.CLOSE_SMALL.create();
            closeIcon.setSize("16px");
            Button btnCerrar = new Button(closeIcon, e -> notif.close());
            btnCerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_ICON);
            btnCerrar.getStyle()
                    .set("margin-left", "8px")
                    .set("padding", "0")
                    .set("min-width", "unset");
            layout.add(btnCerrar);
        }

        notif.add(layout);
        notif.open();
    }
}
