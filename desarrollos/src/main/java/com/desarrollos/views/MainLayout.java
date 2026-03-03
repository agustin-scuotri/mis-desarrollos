package com.desarrollos.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {

	public MainLayout() {
		crearCabecera();
		crearMenuLateral();

		// ── CSS global: Inter font + required indicator + sidebar pills ───────
		String cssGlobal =
			// 1. Importar fuente Inter desde Google Fonts
			"const fontLink = document.createElement('link');" +
			"fontLink.rel = 'stylesheet';" +
			"fontLink.href = 'https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap';" +
			"document.head.appendChild(fontLink);" +

			"const style = document.createElement('style');" +
			"style.textContent = `" +

			// 2. Fuente global Inter
			"  *, body, html { font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif !important; }" +

			// 3. Sidebar: ítem activo (pill resaltado)
			"  a[highlight] {" +
			"    background-color: rgba(0, 32, 96, 0.10) !important;" +
			"    color: #002060 !important;" +
			"    font-weight: 700 !important;" +
			"  }" +
			"  a[router-link]:hover:not([highlight]) {" +
			"    background-color: rgba(0, 32, 96, 0.06) !important;" +
			"    color: #1e3a6e !important;" +
			"  }" +

			// 4. Etiquetas de campos: alineación en fila (mantener comportamiento existente)
			"  vaadin-text-field::part(label), " +
			"  vaadin-password-field::part(label), " +
			"  vaadin-combo-box::part(label), " +
			"  vaadin-date-picker::part(label), " +
			"  vaadin-text-area::part(label), " +
			"  vaadin-number-field::part(label) { " +
			"    display: inline-flex !important; " +
			"    align-items: center !important; " +
			"    flex-direction: row !important; " +
			"  } " +

			// 5. Indicador requerido: reemplazar asterisco por punto azul ●
			"  [required]::part(required-indicator) { " +
			"    font-size: 0px !important; " +
			"    position: static !important; " +
			"    line-height: 0 !important; " +
			"  } " +
			"  [required]::part(required-indicator)::after { " +
			"    content: '●' !important; " +
			"    color: #002060 !important; " +
			"    font-size: 10px !important; " +
			"    margin-left: 8px !important; " +
			"    visibility: visible !important; " +
			"    display: inline-block !important; " +
			"    vertical-align: middle !important; " +
			"    transform: translateY(1px) !important; " +
			"  } " +

			// 6. Campos: ancho máximo 100%
			"  vaadin-text-field, vaadin-password-field, vaadin-combo-box, " +
			"  vaadin-date-picker, vaadin-number-field, vaadin-text-area { max-width: 100%; } " +
			"`;" +
			"document.head.appendChild(style);";

		getElement().executeJs(cssGlobal);
	}

	private void crearCabecera() {
		H1 logo = new H1("Mi Sistema");
		logo.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0 0 0 10px")
				.set("color", "white").set("font-weight", "600").set("letter-spacing", "-0.3px");

		DrawerToggle toggle = new DrawerToggle();
		toggle.getStyle().set("color", "white");
		toggle.getElement().executeJs("this.shadowRoot.querySelector('[part~=\"icon\"]').style.color = '#001030';");

		// ── Toggle Modo Oscuro ────────────────────────────────────────────────
		final boolean[] isDark = {false};

		Icon iconoInicial = VaadinIcon.MOON.create();
		iconoInicial.setSize("18px");
		iconoInicial.getStyle().set("color", "rgba(255,255,255,0.85)");

		Button btnTema = new Button(iconoInicial);
		btnTema.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		btnTema.getElement().setAttribute("title", "Activar modo oscuro");
		btnTema.getStyle().set("cursor", "pointer");

		// ── Toggle manejado 100% client-side (sin round-trip) ────────────────
		// Aplica preferencia de localStorage y adjunta el toggle al botón
		addAttachListener((AttachEvent ae) -> {
			ae.getUI().getPage().executeJs(
				"if (localStorage.getItem('dark-mode') === '1') {" +
				"  document.documentElement.setAttribute('theme','dark');" +
				"}" +
				"return localStorage.getItem('dark-mode') === '1';")
			.then(Boolean.class, dark -> {
				if (Boolean.TRUE.equals(dark)) {
					isDark[0] = true;
					Icon ic = VaadinIcon.SUN_O.create();
					ic.setSize("18px");
					ic.getStyle().set("color", "rgba(255,255,255,0.85)");
					btnTema.setIcon(ic);
					btnTema.getElement().setAttribute("title", "Activar modo claro");
				}
			});
			// Listener JS nativo: cambia el tema sin esperar al servidor
			btnTema.getElement().executeJs(
				"this.addEventListener('click', () => {" +
				"  const html = document.documentElement;" +
				"  const dark = html.getAttribute('theme') !== 'dark';" +
				"  if (dark) {" +
				"    html.setAttribute('theme','dark');" +
				"    localStorage.setItem('dark-mode','1');" +
				"  } else {" +
				"    html.removeAttribute('theme');" +
				"    localStorage.removeItem('dark-mode');" +
				"  }" +
				"}, true);");
		});

		// Listener servidor: solo actualiza el ícono
		btnTema.addClickListener(e -> {
			isDark[0] = !isDark[0];
			Icon ic = isDark[0] ? VaadinIcon.SUN_O.create() : VaadinIcon.MOON.create();
			ic.setSize("18px");
			ic.getStyle().set("color", "rgba(255,255,255,0.85)");
			btnTema.setIcon(ic);
			btnTema.getElement().setAttribute("title",
				isDark[0] ? "Activar modo claro" : "Activar modo oscuro");
		});

		// Spacer para empujar el botón a la derecha
		HorizontalLayout spacer = new HorizontalLayout();
		spacer.setFlexGrow(1, spacer);

		HorizontalLayout header = new HorizontalLayout(toggle, logo, spacer, btnTema);
		header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		header.setWidthFull();
		header.setHeight("60px");
		header.setPadding(true);
		header.setSpacing(false);
		header.getStyle()
				.set("background-color", "#002060")
				.set("box-shadow", "0 2px 5px rgba(0,0,0,0.3)");

		addToNavbar(header);
	}

	private void crearMenuLateral() {
		TextField buscadorMenu = new TextField();
		buscadorMenu.setPlaceholder(getTranslation("app.buscar"));
		buscadorMenu.setPrefixComponent(VaadinIcon.SEARCH.create());
		buscadorMenu.setWidth("90%");
		buscadorMenu.getStyle().set("margin", "15px auto").set("display", "block");

		VerticalLayout opcionesContainer = new VerticalLayout();
		opcionesContainer.setPadding(false);
		opcionesContainer.setSpacing(false);

		RouterLink linkInicio    = crearItemMenu(getTranslation("app.inicio"),     VaadinIcon.HOME,         InicioView.class);
		RouterLink linkArchivos  = crearItemMenu(getTranslation("app.archivos"),   VaadinIcon.FILE_PROCESS, AbmArchivosView.class);
		RouterLink linkConversor = crearItemMenu(getTranslation("app.conversor"),  VaadinIcon.EXCHANGE,     ConversorView.class);
		RouterLink linkDocumentos= crearItemMenu("Lista de JSONs",                 VaadinIcon.FILE_TABLE,   AbmDocumentosConvertidosView.class);

		opcionesContainer.add(linkInicio, linkArchivos, linkConversor, linkDocumentos);

		VerticalLayout menuCompleto = new VerticalLayout(buscadorMenu, opcionesContainer);
		menuCompleto.setPadding(false);
		menuCompleto.setSpacing(false);
		menuCompleto.setHeightFull();
		menuCompleto.getStyle().set("background-color", "#fcfcfc");

		getElement().executeJs(
			"this.style.setProperty('--vaadin-app-layout-drawer-width', '280px');" +
			"this.shadowRoot.querySelector('[part~=\"drawer\"]').style.boxShadow = '4px 0 12px rgba(0,32,96,0.15)';" +
			"this.shadowRoot.querySelector('[part~=\"drawer\"]').style.border = 'none';"
		);

		addToDrawer(menuCompleto);
	}

	private RouterLink crearItemMenu(String nombre, VaadinIcon icono, Class vistaDestino) {
		RouterLink link = new RouterLink(nombre, vistaDestino);

		Icon icon = icono.create();
		icon.setSize("17px");
		link.addComponentAsFirst(icon);

		// Estilo pill: border-radius redondeado, sin borde, margen lateral
		link.getStyle()
				.set("display", "flex")
				.set("align-items", "center")
				.set("gap", "12px")
				.set("padding", "10px 16px")
				.set("margin", "2px 10px")
				.set("width", "calc(100% - 20px)")
				.set("border-radius", "10px")
				.set("text-decoration", "none")
				.set("font-weight", "500")
				.set("font-size", "0.875rem")
				.set("color", "#475569")
				.set("transition", "all 0.15s ease")
				.set("box-sizing", "border-box");

		// Hover gestionado por CSS global (a[router-link]:hover)
		// Estado activo gestionado por CSS global (a[highlight])

		return link;
	}
}
