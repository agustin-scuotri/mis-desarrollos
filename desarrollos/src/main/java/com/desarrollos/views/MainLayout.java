package com.desarrollos.views;

import com.desarrollos.services.ArchivoService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.RouterLink;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class MainLayout extends AppLayout {

	private final ArchivoService archivoService;
	private Span badgePendientes;

	@Autowired
	public MainLayout(ArchivoService archivoService) {
		this.archivoService = archivoService;
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

			// 3. Sidebar: ítem activo (pill resaltado) - modo claro
			"  a[highlight] {" +
			"    background-color: rgba(0, 32, 96, 0.10) !important;" +
			"    color: #002060 !important;" +
			"    font-weight: 700 !important;" +
			"  }" +
			"  a[router-link]:hover:not([highlight]) {" +
			"    background-color: rgba(0, 32, 96, 0.06) !important;" +
			"    color: #1e3a6e !important;" +
			"  }" +

			// 3b. Sidebar: ítem activo - modo oscuro
			"  [theme~='dark'] a[highlight] {" +
			"    background-color: rgba(255,255,255,0.12) !important;" +
			"    color: rgba(255,255,255,0.95) !important;" +
			"  }" +
			"  [theme~='dark'] a[router-link]:hover:not([highlight]) {" +
			"    background-color: rgba(255,255,255,0.07) !important;" +
			"    color: rgba(255,255,255,0.75) !important;" +
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
			// 5b. Indicador requerido en modo oscuro
			"  [theme~='dark'] [required]::part(required-indicator)::after { " +
			"    color: #60a5fa !important; " +
			"  } " +

			// 6. Campos: ancho máximo 100%
			"  vaadin-text-field, vaadin-password-field, vaadin-combo-box, " +
			"  vaadin-date-picker, vaadin-number-field, vaadin-text-area { max-width: 100%; } " +
			"`;" +
			"document.head.appendChild(style);";

		getElement().executeJs(cssGlobal);

		// Refresh badge on every navigation
		addAttachListener(e -> actualizarBadgePendientes());
	}

	private void actualizarBadgePendientes() {
		if (badgePendientes != null) {
			long count = archivoService.contarFiltrado("", "", "PENDIENTE");
			badgePendientes.setText(String.valueOf(count));
			badgePendientes.setVisible(count > 0);
		}
	}

	private void crearCabecera() {
		H1 logo = new H1("Mi Sistema");
		logo.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0 0 0 10px")
				.set("color", "white").set("font-weight", "600").set("letter-spacing", "-0.3px");

		DrawerToggle toggle = new DrawerToggle();
		toggle.getStyle().set("color", "white");
		toggle.getElement().executeJs("this.shadowRoot.querySelector('[part~=\"icon\"]').style.color = '#001030';");

		// ── Toggle Modo Oscuro (100% client-side: sin roundtrip al servidor) ──
		// Sin addClickListener → Vaadin NO re-renderiza el DOM al hacer click
		// → el menú lateral no pierde el item activo
		Button btnTema = new Button();
		btnTema.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		btnTema.getStyle()
				.set("cursor", "pointer")
				.set("color", "rgba(255,255,255,0.85)")
				.set("min-width", "36px")
				.set("min-height", "36px");

		addAttachListener((AttachEvent ae) -> {
			btnTema.getElement().executeJs(
				"var btn = this;" +
				"var moonSvg = '<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"18\" height=\"18\" fill=\"currentColor\" viewBox=\"0 0 16 16\"><path d=\"M6 .278a.768.768 0 0 1 .08.858 7.208 7.208 0 0 0-.878 3.46c0 4.021 3.278 7.277 7.318 7.277.527 0 1.04-.055 1.533-.16a.787.787 0 0 1 .81.316.733.733 0 0 1-.031.893A8.349 8.349 0 0 1 8.344 16C3.734 16 0 12.286 0 7.71 0 4.266 2.114 1.312 5.124.06A.752.752 0 0 1 6 .278z\"/></svg>';" +
				"var sunSvg = '<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"18\" height=\"18\" fill=\"currentColor\" viewBox=\"0 0 16 16\"><path d=\"M8 11a3 3 0 1 1 0-6 3 3 0 0 1 0 6zm0 1a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM8 0a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-1 0v-2A.5.5 0 0 1 8 0zm0 13a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-1 0v-2A.5.5 0 0 1 8 13zm8-5a.5.5 0 0 1-.5.5h-2a.5.5 0 0 1 0-1h2a.5.5 0 0 1 .5.5zM3 8a.5.5 0 0 1-.5.5h-2a.5.5 0 0 1 0-1h2A.5.5 0 0 1 3 8zm10.657-5.657a.5.5 0 0 1 0 .707l-1.414 1.415a.5.5 0 1 1-.707-.708l1.414-1.414a.5.5 0 0 1 .707 0zm-9.193 9.193a.5.5 0 0 1 0 .707L3.05 13.657a.5.5 0 0 1-.707-.707l1.414-1.414a.5.5 0 1 1 .707.707zm9.193 2.121a.5.5 0 0 1-.707 0l-1.414-1.414a.5.5 0 0 1 .707-.707l1.414 1.414a.5.5 0 0 1 0 .707zM4.464 4.465a.5.5 0 0 1-.707 0L2.343 3.05a.5.5 0 1 1 .707-.707l1.414 1.414a.5.5 0 0 1 0 .707z\"/></svg>';" +

				// CSS con todas las variables de color Lumo para dark mode
				// Se inyecta como <style> al activar y se elimina al desactivar.
				// Esto garantiza el dark mode aunque Vaadin no haya bundleado el CSS oscuro.
				"var DARK_CSS = " +
				"  'html[theme~=\"dark\"] {' +" +
				"  '  color-scheme: dark;' +" +
				"  '  --lumo-base-color: hsl(214,35%,15%);' +" +
				"  '  --lumo-tint-5pct: hsla(214,65%,85%,0.06);' +" +
				"  '  --lumo-tint-10pct: hsla(214,60%,80%,0.14);' +" +
				"  '  --lumo-tint-20pct: hsla(214,64%,82%,0.23);' +" +
				"  '  --lumo-tint-30pct: hsla(214,69%,84%,0.32);' +" +
				"  '  --lumo-tint-40pct: hsla(214,73%,86%,0.41);' +" +
				"  '  --lumo-tint-50pct: hsla(214,78%,88%,0.50);' +" +
				"  '  --lumo-tint-60pct: hsla(214,82%,90%,0.60);' +" +
				"  '  --lumo-tint-70pct: hsla(214,87%,92%,0.70);' +" +
				"  '  --lumo-tint-80pct: hsla(214,91%,94%,0.80);' +" +
				"  '  --lumo-tint-90pct: hsla(214,96%,96%,0.90);' +" +
				"  '  --lumo-tint: hsl(214,100%,98%);' +" +
				"  '  --lumo-shade-5pct: hsla(214,0%,0%,0.07);' +" +
				"  '  --lumo-shade-10pct: hsla(214,4%,2%,0.15);' +" +
				"  '  --lumo-shade-20pct: hsla(214,8%,4%,0.23);' +" +
				"  '  --lumo-shade-30pct: hsla(214,12%,6%,0.32);' +" +
				"  '  --lumo-shade-40pct: hsla(214,16%,8%,0.41);' +" +
				"  '  --lumo-shade-50pct: hsla(214,20%,10%,0.50);' +" +
				"  '  --lumo-shade-60pct: hsla(214,24%,12%,0.60);' +" +
				"  '  --lumo-shade-70pct: hsla(214,28%,13%,0.70);' +" +
				"  '  --lumo-shade-80pct: hsla(214,32%,13%,0.80);' +" +
				"  '  --lumo-shade-90pct: hsla(214,33%,13%,0.90);' +" +
				"  '  --lumo-shade: hsl(214,33%,13%);' +" +
				"  '  --lumo-contrast-5pct: var(--lumo-tint-5pct);' +" +
				"  '  --lumo-contrast-10pct: var(--lumo-tint-10pct);' +" +
				"  '  --lumo-contrast-20pct: var(--lumo-tint-20pct);' +" +
				"  '  --lumo-contrast-30pct: var(--lumo-tint-30pct);' +" +
				"  '  --lumo-contrast-40pct: var(--lumo-tint-40pct);' +" +
				"  '  --lumo-contrast-50pct: var(--lumo-tint-50pct);' +" +
				"  '  --lumo-contrast-60pct: var(--lumo-tint-60pct);' +" +
				"  '  --lumo-contrast-70pct: var(--lumo-tint-70pct);' +" +
				"  '  --lumo-contrast-80pct: var(--lumo-tint-80pct);' +" +
				"  '  --lumo-contrast-90pct: var(--lumo-tint-90pct);' +" +
				"  '  --lumo-contrast: var(--lumo-tint);' +" +
				"  '  --lumo-header-text-color: var(--lumo-contrast-90pct);' +" +
				"  '  --lumo-body-text-color: var(--lumo-contrast-80pct);' +" +
				"  '  --lumo-secondary-text-color: var(--lumo-contrast-60pct);' +" +
				"  '  --lumo-tertiary-text-color: var(--lumo-contrast-50pct);' +" +
				"  '  --lumo-disabled-text-color: var(--lumo-contrast-30pct);' +" +
				"  '  --lumo-primary-text-color: hsl(214,90%,77%);' +" +
				"  '  --lumo-error-text-color: hsl(3,90%,75%);' +" +
				"  '  --lumo-success-text-color: hsl(145,65%,58%);' +" +
				"  '  --lumo-link-color: hsl(214,90%,77%);' +" +
				"  '}' +" +
				// Fondo del body y partes del app-layout via ::part() para shadow DOM
				"  'html[theme~=\"dark\"] body { background: hsl(214,35%,15%) !important; color: hsla(214,96%,96%,0.80) !important; }' +" +
				"  'html[theme~=\"dark\"] vaadin-app-layout::part(content) { background: hsl(214,35%,15%); }' +" +
				"  'html[theme~=\"dark\"] vaadin-app-layout::part(drawer) { background: hsl(214,33%,13%); }' +" +
				// Grid: las cabeceras usan --vaadin-grid-cell-background dentro del shadow DOM.
				// Se sobreescribe via ::part() y via la variable CSS custom en el elemento raíz.
				"  'html[theme~=\"dark\"] vaadin-grid { --vaadin-grid-cell-background: hsl(214,35%,15%); }' +" +
				"  'html[theme~=\"dark\"] vaadin-grid::part(header-cell) { background-color: hsl(214,28%,18%) !important; color: hsla(214,96%,96%,0.70) !important; }' +" +
				"  'html[theme~=\"dark\"] vaadin-grid::part(footer-cell) { background-color: hsl(214,28%,18%) !important; }' +" +
				// Scrollbar global en dark mode
				"  'html[theme~=\"dark\"] ::-webkit-scrollbar { width: 8px; height: 8px; }' +" +
				"  'html[theme~=\"dark\"] ::-webkit-scrollbar-track { background: hsl(214,35%,11%); border-radius: 4px; }' +" +
				"  'html[theme~=\"dark\"] ::-webkit-scrollbar-thumb { background: hsl(214,20%,32%); border-radius: 4px; border: 2px solid hsl(214,35%,11%); }' +" +
				"  'html[theme~=\"dark\"] ::-webkit-scrollbar-thumb:hover { background: hsl(214,20%,46%); }' +" +
				"  'html[theme~=\"dark\"] { scrollbar-color: hsl(214,20%,32%) hsl(214,35%,11%); scrollbar-width: thin; }';" +

				// Inyección directa en shadowRoot de cada vaadin-grid (más confiable que ::part())
				// :host sobreescribe las vars Lumo DENTRO del shadow DOM (donde el componente las redefine).
				// background-image:none neutraliza el linear-gradient que Lumo usa para el hover.
				"var GRID_DARK_CSS = " +
				"':host {' +" +
				"'  --lumo-base-color: hsl(214,35%,21%);' +" +
				"'  --lumo-contrast-5pct:  hsla(214,60%,80%,0.06);' +" +
				"'  --lumo-contrast-10pct: hsla(214,60%,80%,0.14);' +" +
				"'  --lumo-contrast-20pct: hsla(214,64%,82%,0.23);' +" +
				"'  --lumo-contrast-30pct: hsla(214,69%,84%,0.32);' +" +
				"'  --lumo-contrast-40pct: hsla(214,73%,86%,0.41);' +" +
				"'  --lumo-contrast-50pct: hsla(214,78%,88%,0.50);' +" +
				"'  --lumo-contrast-60pct: hsla(214,82%,90%,0.60);' +" +
				"'  --lumo-contrast-70pct: hsla(214,87%,92%,0.70);' +" +
				"'  --lumo-contrast-80pct: hsla(214,91%,94%,0.80);' +" +
				"'  --lumo-contrast-90pct: hsla(214,96%,96%,0.90);' +" +
				"'  --lumo-contrast:       hsl(214,100%,98%);' +" +
				"'  --lumo-tint-5pct:  hsla(214,65%,85%,0.06);' +" +
				"'  --lumo-tint-10pct: hsla(214,60%,80%,0.14);' +" +
				"'  --lumo-shade: hsl(214,33%,13%);' +" +
				"'  --vaadin-grid-cell-background: hsl(214,35%,15%);' +" +
				"'  --lumo-body-text-color:      hsla(214,96%,96%,0.80);' +" +
				"'  --lumo-secondary-text-color: hsla(214,96%,96%,0.60);' +" +
				// Variables usadas por CrudView para los colores hardcodeados convertidos a var()
				"'  --grid-header-bg:      hsl(214,28%,18%);' +" +
				"'  --grid-frozen-bg:      hsl(214,35%,15%);' +" +
				"'  --grid-hover-bg:       hsl(214,35%,22%);' +" +
				"'  --grid-frozen-hover-bg: hsl(214,35%,22%);' +" +
				"'} ' +" +
				"'[part~=\"header-cell\"] { background-color: hsl(214,28%,18%) !important; background-image: none !important; color: hsla(214,96%,96%,0.70) !important; } ' +" +
				"'[part~=\"footer-cell\"] { background-color: hsl(214,28%,18%) !important; background-image: none !important; } ' +" +
				"'[part~=\"body-cell\"]   { background-color: hsl(214,35%,15%) !important; background-image: none !important; color: hsla(214,96%,96%,0.80) !important; transition: background-color 120ms ease !important; } ' +" +
				"'[part~=\"body-cell\"]:hover, tr:hover [part~=\"body-cell\"] { background-color: hsl(214,35%,22%) !important; background-image: none !important; } ' +" +
				// Scrollbar interno del grid en dark mode
				"'::-webkit-scrollbar { width: 8px; height: 8px; } ' +" +
				"'::-webkit-scrollbar-track { background: hsl(214,35%,11%); border-radius: 4px; } ' +" +
				"'::-webkit-scrollbar-thumb { background: hsl(214,20%,32%); border-radius: 4px; border: 2px solid hsl(214,35%,11%); } ' +" +
				"'::-webkit-scrollbar-thumb:hover { background: hsl(214,20%,46%); } ' +" +
				"'* { scrollbar-color: hsl(214,20%,32%) hsl(214,35%,11%); scrollbar-width: thin; }';" +
				"function aplicarTemaGrid(grid, dark) {" +
				"  var ex = grid.shadowRoot && grid.shadowRoot.getElementById('lumo-dark-grid');" +
				"  if (dark && !ex && grid.shadowRoot) {" +
				"    var s = document.createElement('style');" +
				"    s.id = 'lumo-dark-grid';" +
				"    s.textContent = GRID_DARK_CSS;" +
				"    grid.shadowRoot.appendChild(s);" +
				"  } else if (!dark && ex) {" +
				"    ex.remove();" +
				"  }" +
				"}" +
				"function aplicarTemaGrids(dark) {" +
				"  document.querySelectorAll('vaadin-grid').forEach(function(g) { aplicarTemaGrid(g, dark); });" +
				"}" +
				// MutationObserver para grids cargados después de la carga inicial (navegación entre vistas)
				"var gridObserver = new MutationObserver(function(mutations) {" +
				"  if (document.documentElement.getAttribute('theme') !== 'dark') return;" +
				"  mutations.forEach(function(m) {" +
				"    m.addedNodes.forEach(function(n) {" +
				"      if (n.nodeType !== 1) return;" +
				"      var gs = [];" +
				"      if (n.tagName && n.tagName.toLowerCase() === 'vaadin-grid') gs.push(n);" +
				"      if (n.querySelectorAll) n.querySelectorAll('vaadin-grid').forEach(function(g) { gs.push(g); });" +
				"      gs.forEach(function(g) { aplicarTemaGrid(g, true); });" +
				"    });" +
				"  });" +
				"});" +
				"gridObserver.observe(document.body, { childList: true, subtree: true });" +

				// Estilo de transición: se activa solo durante el toggle, no en carga inicial
				"if (!document.getElementById('lumo-trans-base')) {" +
				"  var _ts = document.createElement('style');" +
				"  _ts.id = 'lumo-trans-base';" +
				"  _ts.textContent = 'html.theme-transitioning * { transition: background-color 320ms ease, color 280ms ease, border-color 280ms ease !important; }';" +
				"  document.head.appendChild(_ts);" +
				"}" +

				"function aplicarTema(dark, animate) {" +
				"  if (animate) document.documentElement.classList.add('theme-transitioning');" +
				"  document.documentElement[dark ? 'setAttribute' : 'removeAttribute']('theme', 'dark');" +
				"  document.body[dark ? 'setAttribute' : 'removeAttribute']('theme', 'dark');" +
				"  var existing = document.getElementById('lumo-dark-vars');" +
				"  if (dark && !existing) {" +
				"    var s = document.createElement('style');" +
				"    s.id = 'lumo-dark-vars';" +
				"    s.textContent = DARK_CSS;" +
				"    document.head.appendChild(s);" +
				"  } else if (!dark && existing) {" +
				"    existing.remove();" +
				"  }" +
				"  aplicarTemaGrids(dark);" +
				"  localStorage[dark ? 'setItem' : 'removeItem']('dark-mode', '1');" +
				"  btn.innerHTML = dark ? sunSvg : moonSvg;" +
				"  btn.title = dark ? 'Activar modo claro' : 'Activar modo oscuro';" +
				"  if (animate) setTimeout(function() { document.documentElement.classList.remove('theme-transitioning'); }, 400);" +
				"}" +
				"aplicarTema(localStorage.getItem('dark-mode') === '1', false);" +
				"btn.addEventListener('click', function() {" +
				"  aplicarTema(document.documentElement.getAttribute('theme') !== 'dark', true);" +
				"});"
			);
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
		buscadorMenu.setValueChangeMode(ValueChangeMode.EAGER);
		buscadorMenu.setClearButtonVisible(true);

		VerticalLayout opcionesContainer = new VerticalLayout();
		opcionesContainer.setPadding(false);
		opcionesContainer.setSpacing(false);

		RouterLink linkInicio    = crearItemMenu(getTranslation("app.inicio"),     VaadinIcon.HOME,         InicioView.class);
		RouterLink linkArchivos  = crearItemMenu(getTranslation("app.archivos"),   VaadinIcon.FILE_PROCESS, AbmArchivosView.class);
		RouterLink linkConversor = crearItemMenu(getTranslation("app.conversor"),  VaadinIcon.EXCHANGE,     ConversorView.class);
		RouterLink linkDocumentos= crearItemMenu("Lista de JSONs",                 VaadinIcon.FILE_TABLE,   AbmDocumentosConvertidosView.class);

		// Badge de pendientes en el ítem Archivos
		badgePendientes = new Span("0");
		badgePendientes.getStyle()
				.set("background-color", "#dc2626")
				.set("color", "white")
				.set("border-radius", "10px")
				.set("padding", "2px 7px")
				.set("font-size", "0.68rem")
				.set("font-weight", "700")
				.set("margin-left", "auto")
				.set("min-width", "18px")
				.set("text-align", "center")
				.set("line-height", "1.5");
		badgePendientes.setVisible(false);
		linkArchivos.add(badgePendientes);

		Map<RouterLink, String> itemsMenu = new LinkedHashMap<>();
		itemsMenu.put(linkInicio,     getTranslation("app.inicio").toLowerCase());
		itemsMenu.put(linkArchivos,   getTranslation("app.archivos").toLowerCase());
		itemsMenu.put(linkConversor,  getTranslation("app.conversor").toLowerCase());
		itemsMenu.put(linkDocumentos, "lista de jsons");

		buscadorMenu.addValueChangeListener(e -> {
			String filtro = e.getValue().trim().toLowerCase();
			itemsMenu.forEach((link, label) -> link.setVisible(filtro.isEmpty() || label.contains(filtro)));
		});

		opcionesContainer.add(linkInicio, linkArchivos, linkConversor, linkDocumentos);

		VerticalLayout menuCompleto = new VerticalLayout(buscadorMenu, opcionesContainer);
		menuCompleto.setPadding(false);
		menuCompleto.setSpacing(false);
		menuCompleto.setHeightFull();
		menuCompleto.getStyle().set("background-color", "var(--lumo-base-color, #fcfcfc)");

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
				.set("color", "var(--lumo-body-text-color, #475569)")
				.set("transition", "all 0.15s ease")
				.set("box-sizing", "border-box");

		// Hover gestionado por CSS global (a[router-link]:hover)
		// Estado activo gestionado por CSS global (a[highlight])

		return link;
	}
}
