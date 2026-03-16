package com.desarrollos.views;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;

/**
 * Configuración global de la shell de la aplicación Vaadin.
 * @Push debe estar aquí (no en AppLayout) para habilitar server push
 * y permitir UI.access() desde hilos de fondo.
 * @Theme asegura que Lumo (incluyendo el CSS de modo oscuro) se incluya en el bundle.
 */
@Push
@Theme("lumo")
public class AppShell implements AppShellConfigurator {
}
