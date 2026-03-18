package com.desarrollos.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Pantalla de login.
 * @AnonymousAllowed: permite el acceso sin autenticación (es la puerta de entrada).
 * No usa MainLayout (sin sidebar ni cabecera).
 */
@Route("login")
@PageTitle("Iniciar Sesión")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background-color", "#f0f4f8");

        // ── Inyectar Inter font (mismo que MainLayout) ────────────────────────
        getElement().executeJs(
            "if (!document.getElementById('inter-font')) {" +
            "  const link = document.createElement('link');" +
            "  link.id = 'inter-font';" +
            "  link.rel = 'stylesheet';" +
            "  link.href = 'https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap';" +
            "  document.head.appendChild(link);" +
            "}" +
            "document.body.style.fontFamily = \"'Inter', -apple-system, sans-serif\";"
        );

        // ── Card contenedor ───────────────────────────────────────────────────
        Div card = new Div();
        card.getStyle()
                .set("background", "white")
                .set("border-radius", "16px")
                .set("box-shadow", "0 4px 24px rgba(0,32,96,0.12), 0 1px 4px rgba(0,32,96,0.08)")
                .set("padding", "40px 48px")
                .set("width", "100%")
                .set("max-width", "420px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("gap", "8px");

        // ── Logo / Título ─────────────────────────────────────────────────────
        Div logoBadge = new Div();
        logoBadge.getStyle()
                .set("background-color", "#002060")
                .set("border-radius", "12px")
                .set("width", "52px")
                .set("height", "52px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("margin-bottom", "4px");
        Span logoIcon = new Span("🔐");
        logoIcon.getStyle().set("font-size", "24px");
        logoBadge.add(logoIcon);

        H2 titulo = new H2("Mi Sistema");
        titulo.getStyle()
                .set("font-family", "'Inter', sans-serif")
                .set("font-size", "1.5rem")
                .set("font-weight", "700")
                .set("color", "#1e293b")
                .set("margin", "0");

        Paragraph subtitulo = new Paragraph("Ingresá tus credenciales para continuar");
        subtitulo.getStyle()
                .set("font-size", "0.875rem")
                .set("color", "#64748b")
                .set("margin", "0 0 16px 0")
                .set("text-align", "center");

        // ── LoginForm de Vaadin ───────────────────────────────────────────────
        // El action apunta al endpoint de Spring Security (POST /api/auth/login)
        loginForm.setAction("api/auth/login");
        loginForm.setForgotPasswordButtonVisible(false);
        loginForm.getStyle()
                .set("width", "100%")
                .set("padding", "0");

        // ── Pie de card ───────────────────────────────────────────────────────
        Div footer = new Div();
        footer.getStyle()
                .set("margin-top", "8px")
                .set("text-align", "center");
        Span footerText = new Span("Sistema de Conversión de Facturas · v1.0");
        footerText.getStyle()
                .set("font-size", "0.75rem")
                .set("color", "#94a3b8");
        footer.add(footerText);

        card.add(logoBadge, titulo, subtitulo, loginForm, footer);
        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Si viene ?error en la URL, Spring Security falló el login → mostrar error
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            loginForm.setError(true);
        }
    }
}
