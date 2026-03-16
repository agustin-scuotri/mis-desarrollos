package com.desarrollos.views;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;

/**
 * Configuración global de la shell de la aplicación Vaadin.
 * @Push debe estar aquí (no en AppLayout) para habilitar server push
 * y permitir UI.access() desde hilos de fondo.
 * Lumo es el tema por defecto en Vaadin 25, no requiere @Theme explícito.
 * El modo oscuro se maneja inyectando las variables CSS de Lumo desde MainLayout.
 */
@Push
public class AppShell implements AppShellConfigurator {
}
