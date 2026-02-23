package com.desarrollos.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
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

		String cssGlobal = "const style = document.createElement('style');" + "style.textContent = ` "
				+ "  /* 1. Seleccionamos el label de todos los componentes de entrada */ "
				+ "  vaadin-text-field::part(label), " + "  vaadin-password-field::part(label), "
				+ "  vaadin-combo-box::part(label), " + "  vaadin-date-picker::part(label), "
				+ "  vaadin-text-area::part(label), " + "  vaadin-number-field::part(label) { "
				+ "    display: inline-flex !important; " + "    align-items: center !important; "
				+ "    flex-direction: row !important; " + "  } " +

				"  /* 2. Quitamos el asterisco original y su posición de exponente */ "
				+ "  [required]::part(required-indicator) { " + "    font-size: 0px !important; "
				+ "    position: static !important; " + "    line-height: 0 !important; " + "  } " +

				"  /* 3. Creamos el punto relleno ● alineado */ " + "  [required]::part(required-indicator)::after { "
				+ "    content: '●' !important; " + "    color: #002060 !important; "
				+ "    font-size: 10px !important; " + "    margin-left: 8px !important; "
				+ "    visibility: visible !important; " + "    display: inline-block !important; "
				+ "    vertical-align: middle !important; "
				+ "    transform: translateY(1px) !important; /* Ajuste fino según la fuente */ " + "  } " +

				"  vaadin-text-field, vaadin-password-field, vaadin-combo-box, "
				+ "  vaadin-date-picker, vaadin-number-field, vaadin-text-area { " + "    max-width: 100%; " + "  } `;"
				+ "document.head.appendChild(style);";

		getElement().executeJs(cssGlobal);
	}

	private void crearCabecera() {
		H1 logo = new H1("Mi Sistema");
		logo.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0 0 0 10px")
				.set("color", "white");

		DrawerToggle toggle = new DrawerToggle();
		toggle.getStyle().set("color", "white");
		toggle.getElement().executeJs("this.shadowRoot.querySelector('[part~=\"icon\"]').style.color = '#001030';");
		HorizontalLayout header = new HorizontalLayout(toggle, logo);
		header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		header.setWidthFull();
		header.setHeight("60px");
		header.setPadding(true);

		header.getStyle().set("background-color", "#002060")
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
		opcionesContainer.setSpacing(true);

		RouterLink linkInicio = crearItemMenu(getTranslation("app.inicio"), VaadinIcon.HOME, InicioView.class);
		RouterLink linkArchivos = crearItemMenu(getTranslation("app.archivos"), VaadinIcon.FILE_PROCESS, AbmArchivosView.class);
		// ─── NUEVO: Conversor de facturas a JSON ───────────────────────────────────
		RouterLink linkConversor = crearItemMenu(getTranslation("app.conversor"), VaadinIcon.EXCHANGE, ConversorView.class);

		opcionesContainer.add(linkInicio, linkArchivos, linkConversor);

		VerticalLayout menuCompleto = new VerticalLayout(buscadorMenu, opcionesContainer);
		menuCompleto.setPadding(false);
		menuCompleto.setSpacing(false);
		menuCompleto.setHeightFull();
		menuCompleto.getStyle().set("background-color", "#fcfcfc");

		getElement().executeJs("this.style.setProperty('--vaadin-app-layout-drawer-width', '320px');"
				+ "this.shadowRoot.querySelector('[part~=\"drawer\"]').style.boxShadow = '4px 0 12px rgba(0, 32, 96, 0.2)';"
				+ "this.shadowRoot.querySelector('[part~=\"drawer\"]').style.border = 'none';");

		addToDrawer(menuCompleto);
	}

	private RouterLink crearItemMenu(String nombre, VaadinIcon icono, Class vistaDestino) {
		RouterLink link = new RouterLink(nombre, vistaDestino);
		link.addComponentAsFirst(icono.create());

		link.getStyle().set("display", "flex").set("align-items", "center").set("gap", "15px")
				.set("padding", "15px 25px")
				.set("margin", "0")
				.set("width", "100%")
				.set("border-radius", "0")
				.set("text-decoration", "none").set("font-weight", "600").set("color", "#002060")
				.set("transition", "background-color 0.2s");

		link.getElement().addEventListener("mouseover",
				e -> link.getStyle().set("background-color", "rgba(0, 32, 96, 0.1)"));
		link.getElement().addEventListener("mouseout", e -> link.getStyle().set("background-color", "transparent"));

		return link;
	}
}