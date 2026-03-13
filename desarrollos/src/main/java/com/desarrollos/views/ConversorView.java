package com.desarrollos.views;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.desarrollos.base.FormView;
import com.desarrollos.combos.ArchivoCombo;
import com.desarrollos.entities.Archivo;
import com.desarrollos.services.ApiSaturadaException;
import com.desarrollos.services.ArchivoDuplicadoException;
import com.desarrollos.services.ErrorFactura;
import com.desarrollos.services.FacturaDuplicadaException;
import com.desarrollos.services.ResultadoConversion;
import com.desarrollos.services.TotalNegativoException;
import com.desarrollos.entities.DocumentoConvertido;
import com.desarrollos.entities.NetoGravado;
import com.desarrollos.entities.PercepcionIIBB;
import com.desarrollos.entities.PercepcionIVA;
import com.desarrollos.entities.ProductoConcepto;
import com.desarrollos.entities.Vencimiento;
import com.desarrollos.services.ArchivoService;
import com.desarrollos.services.DocumentoConvertidoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;

@Route(value = "conversor", layout = MainLayout.class)
@PageTitle("Conversor de Facturas")
public class ConversorView extends FormView {

	private final ArchivoService archivoService;
	private final DocumentoConvertidoService documentoConvertidoService;
	private ArchivoCombo archivoCombo;
	private Archivo archivoSeleccionado;

	// ── Botones principales ───────────────────────────────────────────────────
	private final Button btnConvertir  = new Button("Convertir",   VaadinIcon.BOLT.create());
	private final Button btnVerArchivo = new Button("Ver Archivo", VaadinIcon.EYE.create());

	// ── Progreso ──────────────────────────────────────────────────────────────
	private final ProgressBar    progressBar   = new ProgressBar();
	private final VerticalLayout panelProgreso = new VerticalLayout();

	// ── Panel resultado (contenedor dinámico) ─────────────────────────────────
	private final VerticalLayout panelResultado = new VerticalLayout();

	// ── Mensaje de error inline (debajo del combo) ──────────────────────────
	private final HorizontalLayout mensajeErrorArchivo = new HorizontalLayout();

	// ── Mensaje de progreso (reutilizado en el hilo para countdown) ───────────
	private final H3 mensajeProcesando = new H3("Procesando imagen con IA...");

	// ── Stepper visual ────────────────────────────────────────────────────────
	private final HorizontalLayout stepperLayout = new HorizontalLayout();
	private Span circulo1, circulo2, circulo3;
	private Span label1, label2, label3;

	public ConversorView(ArchivoService archivoService, DocumentoConvertidoService documentoConvertidoService) {
		this.archivoService = archivoService;
		this.documentoConvertidoService = documentoConvertidoService;
		setTitulo("Conversor de Facturas");
		configurarCampos();
	}

	@Override
	protected void configurarCampos() {

		// ── Combo de archivos ─────────────────────────────────────────────────
		archivoCombo = new ArchivoCombo(archivoService);
		archivoCombo.setLabel("Archivo");
		archivoCombo.setPlaceholder(getTranslation("conversor.seleccione.archivo"));
		archivoCombo.getCombo().addValueChangeListener(e -> {
			archivoSeleccionado = e.getValue();
			boolean hayArchivo = archivoSeleccionado != null;
			btnVerArchivo.setEnabled(hayArchivo);
			if (hayArchivo) {
				mensajeErrorArchivo.setVisible(false);
				panelResultado.removeAll();
				panelResultado.setVisible(false);
				panelProgreso.setVisible(false);
				actualizarStepper("completado", "pendiente", "pendiente");
			} else {
				actualizarStepper("activo", "pendiente", "pendiente");
			}
		});

		// ── Mensaje de error inline ──────────────────────────────────────────────
		Icon iconoError = VaadinIcon.EXCLAMATION_CIRCLE_O.create();
		iconoError.setSize("16px");
		iconoError.setColor("#dc2626");
		Span textoError = new Span("Seleccioná un archivo antes de convertir");
		textoError.getStyle().set("color", "#dc2626").set("font-size", "0.85rem").set("font-weight", "500");
		mensajeErrorArchivo.add(iconoError, textoError);
		mensajeErrorArchivo.setAlignItems(FlexComponent.Alignment.CENTER);
		mensajeErrorArchivo.setSpacing(false);
		mensajeErrorArchivo.setPadding(false);
		mensajeErrorArchivo.getStyle().set("gap", "6px").set("margin-top", "2px");
		mensajeErrorArchivo.setVisible(false);

		// ── Botón convertir ───────────────────────────────────────────────────
		btnConvertir.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		btnConvertir.getStyle()
				.set("background-color", "#2563eb").set("color", "white")
				.set("margin-top", "15px").set("font-weight", "600");
		btnConvertir.addClickListener(e -> ejecutarConversion());

		// ── Botón ver archivo (deshabilitado hasta seleccionar) ───────────────
		btnVerArchivo.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		btnVerArchivo.setEnabled(false);
		btnVerArchivo.getStyle().set("margin-top", "15px");
		btnVerArchivo.addClickListener(e -> abrirArchivo());

		HorizontalLayout botones = new HorizontalLayout(btnConvertir, btnVerArchivo);
		botones.setAlignItems(FlexComponent.Alignment.CENTER);
		botones.setSpacing(true);
		botones.setPadding(false);

		// ── Nota: calidad del archivo ─────────────────────────────────────────
		Icon iconoInfo = VaadinIcon.INFO_CIRCLE_O.create();
		iconoInfo.setSize("16px");
		iconoInfo.getStyle().set("flex-shrink", "0").set("margin-top", "2px").set("color", "#1d4ed8");
		Span textoNota = new Span(
				"La calidad del archivo es clave para una conversión correcta. " +
				"Escaneá o fotografiá el documento con buena iluminación, sin bordes cortados ni texto borroso. " +
				"Si algún dato queda vacío o incorrecto (como el CUIT), probablemente el archivo " +
				"no era legible en esa zona — revisá y corregí manualmente desde el ABM.");
		textoNota.getStyle().set("font-size", "0.82rem").set("line-height", "1.4").set("color", "#1e3a5f");
		HorizontalLayout notaCalidad = new HorizontalLayout(iconoInfo, textoNota);
		notaCalidad.setAlignItems(FlexComponent.Alignment.START);
		notaCalidad.setSpacing(false);
		notaCalidad.getStyle()
				.set("gap", "8px")
				.set("background", "#eff6ff")
				.set("border", "1px solid #bfdbfe")
				.set("border-radius", "6px")
				.set("padding", "10px 14px")
				.set("margin-top", "12px");

		// ── Panel progreso ────────────────────────────────────────────────────
		progressBar.setIndeterminate(true);
		progressBar.getStyle().set("width", "400px")
				.set("--vaadin-progress-value-background", "#2563eb");

		mensajeProcesando.getStyle()
				.set("color", "#1e293b").set("font-size", "1rem")
				.set("margin", "0").set("font-weight", "600");

		panelProgreso.add(mensajeProcesando, progressBar);
		panelProgreso.setPadding(false);
		panelProgreso.setSpacing(true);
		panelProgreso.setVisible(false);
		panelProgreso.getStyle().set("margin-top", "20px");

		// ── Panel resultado (contenedor dinámico, se llena en ejecutarConversion) ──
		panelResultado.setPadding(false);
		panelResultado.setSpacing(true);
		panelResultado.setVisible(false);
		panelResultado.getStyle().set("margin-top", "20px");

		// ── Stepper ───────────────────────────────────────────────────────────
		construirStepper();
		stepperLayout.getStyle().set("margin-top", "18px");

		contenidoPrincipal.add(archivoCombo, mensajeErrorArchivo, botones, notaCalidad,
				stepperLayout, panelProgreso, panelResultado);
		barraBotones.setVisible(false);
	}

	// ── Stepper: construcción ────────────────────────────────────────────────
	private void construirStepper() {
		circulo1 = crearCirculo("1", "activo");
		circulo2 = crearCirculo("2", "pendiente");
		circulo3 = crearCirculo("3", "pendiente");
		label1   = crearLabelPaso("Seleccionar", "activo");
		label2   = crearLabelPaso("Procesando", "pendiente");
		label3   = crearLabelPaso("Resultado", "pendiente");

		stepperLayout.removeAll();
		stepperLayout.setSpacing(false);
		stepperLayout.setPadding(false);
		stepperLayout.setAlignItems(Alignment.CENTER);
		stepperLayout.getStyle().set("gap", "0");

		stepperLayout.add(
				crearPasoContainer(circulo1, label1),
				crearConector(),
				crearPasoContainer(circulo2, label2),
				crearConector(),
				crearPasoContainer(circulo3, label3)
		);
	}

	private Span crearCirculo(String numero, String estado) {
		Span circulo = new Span(numero);
		circulo.getStyle()
				.set("width", "32px").set("height", "32px")
				.set("border-radius", "50%").set("display", "inline-flex")
				.set("align-items", "center").set("justify-content", "center")
				.set("font-weight", "700").set("font-size", "0.85rem")
				.set("transition", "all 0.25s ease")
				.set("flex-shrink", "0");
		aplicarEstadoCirculo(circulo, estado);
		return circulo;
	}

	private Span crearLabelPaso(String texto, String estado) {
		Span label = new Span(texto);
		label.getStyle()
				.set("font-size", "0.78rem").set("font-weight", "500")
				.set("margin-top", "5px").set("transition", "all 0.25s ease");
		aplicarEstadoLabel(label, estado);
		return label;
	}

	private com.vaadin.flow.component.html.Div crearPasoContainer(Span circulo, Span label) {
		com.vaadin.flow.component.html.Div paso = new com.vaadin.flow.component.html.Div(circulo, label);
		paso.getStyle()
				.set("display", "flex").set("flex-direction", "column")
				.set("align-items", "center").set("gap", "4px")
				.set("min-width", "80px");
		return paso;
	}

	private com.vaadin.flow.component.html.Div crearConector() {
		com.vaadin.flow.component.html.Div linea = new com.vaadin.flow.component.html.Div();
		linea.getStyle()
				.set("flex", "1").set("height", "2px")
				.set("background", "#e2e8f0").set("margin", "0 4px")
				.set("margin-bottom", "18px").set("min-width", "20px");
		return linea;
	}

	private void aplicarEstadoCirculo(Span circulo, String estado) {
		switch (estado) {
			case "activo"    -> circulo.getStyle()
					.set("background", "#2563eb").set("color", "white")
					.set("box-shadow", "0 0 0 3px rgba(37,99,235,0.2)");
			case "completado" -> circulo.getStyle()
					.set("background", "#16a34a").set("color", "white")
					.set("box-shadow", "none");
			case "error"     -> circulo.getStyle()
					.set("background", "#dc2626").set("color", "white")
					.set("box-shadow", "none");
			default          -> circulo.getStyle()
					.set("background", "#e2e8f0").set("color", "#94a3b8")
					.set("box-shadow", "none");
		}
	}

	private void aplicarEstadoLabel(Span label, String estado) {
		switch (estado) {
			case "activo"    -> label.getStyle().set("color", "#2563eb").set("font-weight", "600");
			case "completado" -> label.getStyle().set("color", "#16a34a").set("font-weight", "500");
			case "error"     -> label.getStyle().set("color", "#dc2626").set("font-weight", "500");
			default          -> label.getStyle().set("color", "#94a3b8").set("font-weight", "400");
		}
	}

	private void actualizarStepper(String est1, String est2, String est3) {
		aplicarEstadoCirculo(circulo1, est1);
		aplicarEstadoCirculo(circulo2, est2);
		aplicarEstadoCirculo(circulo3, est3);
		// Actualizar íconos: completado → checkmark, error → X
		actualizarIconoCirculo(circulo1, est1);
		actualizarIconoCirculo(circulo2, est2);
		actualizarIconoCirculo(circulo3, est3);
		aplicarEstadoLabel(label1, est1);
		aplicarEstadoLabel(label2, est2);
		aplicarEstadoLabel(label3, est3);
	}

	private void actualizarIconoCirculo(Span circulo, String estado) {
		if ("completado".equals(estado)) {
			circulo.setText("✓");
		} else if ("error".equals(estado)) {
			circulo.setText("✗");
		} else if ("1".equals(circulo.getText()) || "2".equals(circulo.getText()) || "3".equals(circulo.getText())) {
			// ya tiene número, no tocar
		}
		// Si ya fue cambiado a ✓ o ✗ y ahora es activo/pendiente, restaurar número
		String t = circulo.getText();
		if (!estado.equals("completado") && !estado.equals("error") && (t.equals("✓") || t.equals("✗"))) {
			// number restore: no se necesita en flujo normal
		}
	}

	// ── Contenido completo de una factura ─────────────────────────────────────
	private VerticalLayout crearContenidoFactura(DocumentoConvertido doc) {
		VerticalLayout contenido = new VerticalLayout();
		contenido.setPadding(false);
		contenido.setSpacing(true);

		// Panel datos clave
		HorizontalLayout panelDatos = new HorizontalLayout();
		panelDatos.setWidthFull();
		panelDatos.setSpacing(false);
		panelDatos.getStyle()
				.set("background-color", "#f0f9ff")
				.set("border", "1px solid #bae6fd")
				.set("border-radius", "10px")
				.set("padding", "14px 18px")
				.set("display", "flex")
				.set("flex-direction", "row")
				.set("align-items", "flex-start")
				.set("gap", "40px");
		panelDatos.add(
			crearCampoInfo("CUIT del Emisor",  estaVacio(doc.getCuit())              ? "—" : doc.getCuit()),
			crearCampoInfo("N° Comprobante",    estaVacio(doc.getNumeroComprobante()) ? "—" : doc.getNumeroComprobante()),
			crearCampoInfo("Centro de Emisión", estaVacio(doc.getCentroEmision())     ? "—" : doc.getCentroEmision()),
			crearCampoInfo("Total",             doc.getTotal() == null               ? "—" : formatImporte(doc.getTotal()))
		);

		// Resultado JSON (colapsable)
		H3 tituloResultado = new H3("Resultado JSON");
		tituloResultado.getStyle()
				.set("color", "#1e293b").set("margin", "0").set("font-weight", "700")
				.set("font-size", "1rem").set("letter-spacing", "-0.2px");

		Pre jsonViewer = new Pre();
		jsonViewer.getStyle()
				.set("background-color", "#1e1e2e").set("color", "#cdd6f4").set("border", "none")
				.set("border-radius", "12px").set("padding", "20px")
				.set("font-family", "'JetBrains Mono', 'Fira Code', 'Courier New', monospace")
				.set("font-size", "13px").set("width", "100%").set("white-space", "pre-wrap")
				.set("word-break", "break-word").set("box-shadow", "inset 0 2px 8px rgba(0,0,0,0.4)")
				.set("line-height", "1.6");
		jsonViewer.setText(doc.getJsonResultado());
		jsonViewer.setVisible(false);

		Button btnVerMas   = new Button("Ver más",   VaadinIcon.CHEVRON_DOWN.create());
		Button btnVerMenos = new Button("Ver menos", VaadinIcon.CHEVRON_UP.create());
		btnVerMas.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		btnVerMas.getStyle().set("align-self", "flex-start");
		btnVerMenos.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		btnVerMenos.getStyle().set("align-self", "flex-start");
		btnVerMenos.setVisible(false);
		btnVerMas.addClickListener(e -> { jsonViewer.setVisible(true);  btnVerMas.setVisible(false); btnVerMenos.setVisible(true); });
		btnVerMenos.addClickListener(e -> { jsonViewer.setVisible(false); btnVerMas.setVisible(true); btnVerMenos.setVisible(false); });

		// ── Grid productos/conceptos ──────────────────────────────────────────
		H4 tituloProductos = new H4("Productos / Conceptos");
		tituloProductos.getStyle().set("color", "#002060").set("margin", "16px 0 4px 0");
		Grid<ProductoConcepto> gridProductos = new Grid<>(ProductoConcepto.class, false);
		gridProductos.addColumn(ProductoConcepto::getSku).setHeader("SKU").setWidth("130px").setFlexGrow(0);
		gridProductos.addColumn(ProductoConcepto::getDescripcion).setHeader("Descripción").setFlexGrow(1);
		gridProductos.addColumn(p -> formatImporte(p.getCantidad())).setHeader("Cant.").setWidth("70px").setFlexGrow(0);
		gridProductos.addColumn(p -> formatImporte(p.getPrecioUnitario())).setHeader("P. Unit.").setWidth("95px").setFlexGrow(0);
		gridProductos.addColumn(p -> formatImporte(p.getDescuento())).setHeader("Desc.").setWidth("80px").setFlexGrow(0);
		gridProductos.addColumn(p -> formatImporte(p.getSubTotal())).setHeader("Subtotal").setWidth("95px").setFlexGrow(0);
		gridProductos.addColumn(ProductoConcepto::getAlicuotaIva).setHeader("IVA").setWidth("75px").setFlexGrow(0);
		gridProductos.addColumn(ProductoConcepto::getOrdenCompra).setHeader("OC").setWidth("90px").setFlexGrow(0);
		gridProductos.addColumn(ProductoConcepto::getRemito).setHeader("Remito").setWidth("130px").setFlexGrow(0);
		gridProductos.addColumn(ProductoConcepto::getNumeroDespacho).setHeader("Nro. Despacho").setWidth("170px").setFlexGrow(0);
		gridProductos.addColumn(ProductoConcepto::getFechaDespacho).setHeader("Fecha Despacho").setWidth("130px").setFlexGrow(0);
		gridProductos.addColumn(ProductoConcepto::getRegistroOficializacion).setHeader("Reg. Ofic.").setWidth("100px").setFlexGrow(0);
		gridProductos.setAllRowsVisible(true);
		gridProductos.getStyle().set("margin-top", "4px");
		boolean hayProductos = !doc.getProductosConceptos().isEmpty();
		if (hayProductos) gridProductos.setItems(doc.getProductosConceptos());
		tituloProductos.setVisible(hayProductos);
		gridProductos.setVisible(hayProductos);

		// ── Grid netos gravados ───────────────────────────────────────────────
		H4 tituloNetos = new H4("Netos Gravados e IVA");
		tituloNetos.getStyle().set("color", "#002060").set("margin", "16px 0 4px 0");
		Grid<NetoGravado> gridNetosGravados = new Grid<>(NetoGravado.class, false);
		gridNetosGravados.addColumn(NetoGravado::getAlicuota).setHeader("Alícuota").setWidth("110px").setFlexGrow(0);
		gridNetosGravados.addColumn(n -> formatImporte(n.getImporteNetoGravado())).setHeader("Importe Neto Gravado").setFlexGrow(1);
		gridNetosGravados.addColumn(n -> formatImporte(n.getIva())).setHeader("IVA").setWidth("130px").setFlexGrow(0);
		gridNetosGravados.setAllRowsVisible(true);
		gridNetosGravados.getStyle().set("margin-top", "4px");
		boolean hayNetos = !doc.getNetosGravados().isEmpty();
		if (hayNetos) gridNetosGravados.setItems(doc.getNetosGravados());
		tituloNetos.setVisible(hayNetos);
		gridNetosGravados.setVisible(hayNetos);

		// ── Grid percepciones IIBB ────────────────────────────────────────────
		H4 tituloPercepcionesIIBB = new H4("Percepciones IIBB");
		tituloPercepcionesIIBB.getStyle().set("color", "#002060").set("margin", "16px 0 4px 0");
		Grid<PercepcionIIBB> gridPercepcionesIIBB = new Grid<>(PercepcionIIBB.class, false);
		gridPercepcionesIIBB.addColumn(PercepcionIIBB::getProvincia).setHeader("Provincia").setFlexGrow(1);
		gridPercepcionesIIBB.addColumn(PercepcionIIBB::getAlicuota).setHeader("Alícuota").setWidth("110px").setFlexGrow(0);
		gridPercepcionesIIBB.addColumn(p -> formatImporte(p.getImporte())).setHeader("Importe").setWidth("130px").setFlexGrow(0);
		gridPercepcionesIIBB.setAllRowsVisible(true);
		gridPercepcionesIIBB.getStyle().set("margin-top", "4px");
		boolean hayIIBB = !doc.getPercepcionesIIBB().isEmpty();
		if (hayIIBB) gridPercepcionesIIBB.setItems(doc.getPercepcionesIIBB());
		tituloPercepcionesIIBB.setVisible(hayIIBB);
		gridPercepcionesIIBB.setVisible(hayIIBB);

		// ── Grid percepciones IVA ─────────────────────────────────────────────
		H4 tituloPercepcionesIVA = new H4("Percepciones IVA");
		tituloPercepcionesIVA.getStyle().set("color", "#002060").set("margin", "16px 0 4px 0");
		Grid<PercepcionIVA> gridPercepcionesIVA = new Grid<>(PercepcionIVA.class, false);
		gridPercepcionesIVA.addColumn(PercepcionIVA::getAlicuota).setHeader("Alícuota").setWidth("150px").setFlexGrow(0);
		gridPercepcionesIVA.addColumn(p -> formatImporte(p.getImporte())).setHeader("Importe").setFlexGrow(1);
		gridPercepcionesIVA.setAllRowsVisible(true);
		gridPercepcionesIVA.getStyle().set("margin-top", "4px");
		boolean hayIVA = !doc.getPercepcionesIVA().isEmpty();
		if (hayIVA) gridPercepcionesIVA.setItems(doc.getPercepcionesIVA());
		tituloPercepcionesIVA.setVisible(hayIVA);
		gridPercepcionesIVA.setVisible(hayIVA);

		// ── Grid vencimientos ─────────────────────────────────────────────────
		H4 tituloVencimientos = new H4("Vencimientos");
		tituloVencimientos.getStyle().set("color", "#002060").set("margin", "16px 0 4px 0");
		Grid<Vencimiento> gridVencimientos = new Grid<>(Vencimiento.class, false);
		gridVencimientos.addColumn(Vencimiento::getFecha).setHeader("Fecha").setWidth("150px").setFlexGrow(0);
		gridVencimientos.addColumn(v -> formatImporte(v.getImporte())).setHeader("Importe").setFlexGrow(1);
		gridVencimientos.setAllRowsVisible(true);
		gridVencimientos.getStyle().set("margin-top", "4px");
		boolean hayVencimientos = !doc.getVencimientos().isEmpty();
		if (hayVencimientos) gridVencimientos.setItems(doc.getVencimientos());
		tituloVencimientos.setVisible(hayVencimientos);
		gridVencimientos.setVisible(hayVencimientos);

		// ── Notas campos opcionales faltantes ────────────────────────────────
		Paragraph parrafoNotas = new Paragraph();
		parrafoNotas.getStyle()
				.set("color", "#92400e").set("font-size", "0.875rem").set("margin-top", "10px")
				.set("white-space", "pre-line").set("background-color", "#fffbeb")
				.set("border", "1px solid #fde68a").set("border-radius", "8px").set("padding", "12px");
		parrafoNotas.setVisible(false);

		StringBuilder notas = new StringBuilder();
		if (estaVacio(doc.getRazonSocial()))          notas.append("• Razón social\n");
		if (estaVacio(doc.getSituacionIva()))         notas.append("• Situación ante IVA\n");
		if (estaVacio(doc.getDireccion()))            notas.append("• Dirección\n");
		if (estaVacio(doc.getCiudad()))               notas.append("• Ciudad/Localidad\n");
		if (estaVacio(doc.getCodigoPostal()))         notas.append("• Código postal\n");
		if (estaVacio(doc.getProvincia()))            notas.append("• Provincia\n");
		if (estaVacio(doc.getPais()))                 notas.append("• País\n");
		if (estaVacio(doc.getTelefono()))             notas.append("• Teléfono\n");
		if (estaVacio(doc.getMail()))                 notas.append("• Mail\n");
		if (estaVacio(doc.getLetra()))                notas.append("• Letra del comprobante\n");
		if (estaVacio(doc.getCae()))                  notas.append("• CAE\n");
		if (doc.getFechaVencimientoCae() == null)     notas.append("• Fecha venc. CAE\n");
		if (doc.getCotizacion() == null)              notas.append("• Cotización\n");
		if (estaVacio(doc.getOrdenCompra()))          notas.append("• Orden de compra\n");
		if (doc.getProductosConceptos().isEmpty())    notas.append("• Productos/Conceptos\n");
		if (doc.getNetosGravados().isEmpty())         notas.append("• Netos gravados e IVA\n");
		if (doc.getSubTotalNoGravado() == null)       notas.append("• Importe neto no gravado\n");
		if (doc.getPercepcionesIIBB().isEmpty())      notas.append("• Percepciones IIBB\n");
		if (doc.getPercepcionesIVA().isEmpty())       notas.append("• Percepciones IVA\n");
		if (doc.getVencimientos().isEmpty())          notas.append("• Vencimientos\n");
		if (notas.length() > 0) {
			parrafoNotas.setText("⚠ Campos no encontrados:\n" + notas);
			parrafoNotas.setVisible(true);
		}

		contenido.add(panelDatos,
				tituloResultado, btnVerMas, jsonViewer, btnVerMenos,
				tituloProductos, gridProductos,
				tituloNetos, gridNetosGravados,
				tituloPercepcionesIIBB, gridPercepcionesIIBB,
				tituloPercepcionesIVA, gridPercepcionesIVA,
				tituloVencimientos, gridVencimientos,
				parrafoNotas,
				crearBotonDescarga(doc));
		return contenido;
	}

	// ── Panel inline para facturas que no pudieron convertirse ────────────────
	private Component crearPanelFallas(List<String> mensajes) {
		VerticalLayout panel = new VerticalLayout();
		panel.setPadding(false);
		panel.setSpacing(false);
		panel.setWidthFull();
		panel.getStyle()
				.set("background-color", "#fef2f2")
				.set("border", "1px solid #fecaca")
				.set("border-radius", "8px")
				.set("padding", "12px")
				.set("gap", "4px");

		Span titulo = new Span("⚠  Facturas no convertidas:");
		titulo.getStyle().set("font-weight", "600").set("color", "#dc2626").set("font-size", "0.85rem");
		panel.add(titulo);

		for (String msg : mensajes) {
			Span item = new Span("• " + msg);
			item.getStyle().set("color", "#991b1b").set("font-size", "0.82rem");
			panel.add(item);
		}
		return panel;
	}

	// ── Abrir archivo en nueva pestaña ────────────────────────────────────────
	private void abrirArchivo() {
		if (archivoSeleccionado == null) return;
		try {
			Archivo completo = archivoService.buscarPorIdConContenido(archivoSeleccionado.getId());
			if (completo.getContenido() == null) {
				Notification.show("El archivo no tiene contenido adjunto")
						.addThemeVariants(NotificationVariant.LUMO_WARNING);
				return;
			}
			StreamResource res = new StreamResource(
					completo.getNombreOriginal(),
					() -> new ByteArrayInputStream(completo.getContenido()));
			final StreamRegistration reg = VaadinSession.getCurrent()
					.getResourceRegistry().registerResource(res);
			UI.getCurrent().getPage().open(reg.getResourceUri().toString(), "_blank");
		} catch (Exception ex) {
			Notification.show("Error al abrir el archivo: " + ex.getMessage())
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
		}
	}

	// ── Conversión en hilo de fondo (evita Connection Reset) ─────────────────
	private void ejecutarConversion() {
		if (archivoSeleccionado == null) {
			mensajeErrorArchivo.setVisible(true);
			return;
		}

		panelProgreso.setVisible(true);
		panelResultado.setVisible(false);
		btnConvertir.setEnabled(false);
		btnVerArchivo.setEnabled(false);
		mensajeProcesando.setText("Procesando imagen con IA...");
		actualizarStepper("completado", "activo", "pendiente");

		final Archivo archivoAConvertir = archivoSeleccionado;
		archivoSeleccionado = null;
		archivoCombo.limpiar();
		archivoCombo.refrescar();

		final UI ui = UI.getCurrent();

		new Thread(() -> {
			final java.util.concurrent.atomic.AtomicBoolean procesando = new java.util.concurrent.atomic.AtomicBoolean(true);
			final Thread rotador = new Thread(() -> {
				final String[] mensajes = { "Procesando imagen con IA...", "Esto puede demorar algunos minutos..." };
				int idx = 0;
				while (procesando.get()) {
					try { Thread.sleep(5_000); } catch (InterruptedException e) { break; }
					if (!procesando.get()) break;
					final String msg = mensajes[idx % 2];
					idx++;
					ui.access(() -> mensajeProcesando.setText(msg));
				}
			});
			rotador.setDaemon(true);
			rotador.start();

			try {
				final ResultadoConversion resultados = documentoConvertidoService.convertir(archivoAConvertir);
				final List<DocumentoConvertido> exitosos = resultados.getExitosos();
				final List<ErrorFactura>        errores  = resultados.getErrores();

				procesando.set(false);
				rotador.interrupt();
				ui.access(() -> {
					mensajeProcesando.setText("Procesando imagen con IA...");
					panelProgreso.setVisible(false);
					btnConvertir.setEnabled(true);

					// El servicio ya valida campos antes de guardar: exitosos siempre son válidos
					List<DocumentoConvertido> tabWorthy      = new ArrayList<>(exitosos);
					actualizarStepper("completado", tabWorthy.isEmpty() ? "error" : "completado", "activo");
					List<String>             mensajesFallas = new ArrayList<>();

					// Caso: documento único que no es una factura (ningún campo obligatorio presente)
					if (exitosos.isEmpty() && errores.size() == 1
							&& errores.get(0).getTipo() == ErrorFactura.Tipo.CAMPOS_FALTANTES) {
						mostrarDialogoNoFactura(errores.get(0).getCamposFaltantes());
						return;
					}

					for (ErrorFactura e : errores) {
						mensajesFallas.add("Factura " + e.getNumero() + " — " + e.getDetalle());
					}

					panelResultado.removeAll();

					if (!mensajesFallas.isEmpty()) {
						panelResultado.add(crearPanelFallas(mensajesFallas));
					}

					if (tabWorthy.size() == 1) {
						panelResultado.add(crearContenidoFactura(tabWorthy.get(0)));
					} else if (tabWorthy.size() > 1) {
						TabSheet tabs = new TabSheet();
						tabs.setWidthFull();
						for (int i = 0; i < tabWorthy.size(); i++) {
							DocumentoConvertido doc = tabWorthy.get(i);
							String nro   = doc.getNumeroComprobante();
							String label = estaVacio(nro) ? "Factura " + (i + 1) : "Nro " + nro;
							tabs.add(label, crearContenidoFactura(doc));
						}
						panelResultado.add(tabs);
					}

					if (!tabWorthy.isEmpty() || !mensajesFallas.isEmpty()) {
						panelResultado.setVisible(true);
					}

					archivoCombo.refrescar();
					if (!tabWorthy.isEmpty() || !mensajesFallas.isEmpty()) {
						mostrarDialogoResultado(tabWorthy.size(), mensajesFallas);
					}
				});

			} catch (ApiSaturadaException ex) {
				procesando.set(false);
				rotador.interrupt();
				ui.access(() -> {
					mensajeProcesando.setText("Procesando imagen con IA...");
					panelProgreso.setVisible(false);
					btnConvertir.setEnabled(true);
					actualizarStepper("completado", "error", "pendiente");
					Notification.show("La API está saturada. Esperá unos minutos e intentá de nuevo.")
							.addThemeVariants(NotificationVariant.LUMO_WARNING);
				});

			} catch (ArchivoDuplicadoException ex) {
				procesando.set(false);
				rotador.interrupt();
				final ArchivoDuplicadoException dup = ex;
				try { archivoService.actualizarEstado(archivoAConvertir, "PENDIENTE"); } catch (Exception ignored) {}
				ui.access(() -> {
					mensajeProcesando.setText("Procesando imagen con IA...");
					panelProgreso.setVisible(false);
					btnConvertir.setEnabled(true);
					archivoCombo.refrescar();
					mostrarDialogoArchivoDuplicado(dup);
				});

			} catch (FacturaDuplicadaException ex) {
				procesando.set(false);
				rotador.interrupt();
				final FacturaDuplicadaException dup = ex;
				try { archivoService.actualizarEstado(archivoAConvertir, "PROCESADO_ERROR", dup.getMessage()); } catch (Exception ignored) {}
				ui.access(() -> {
					mensajeProcesando.setText("Procesando imagen con IA...");
					panelProgreso.setVisible(false);
					btnConvertir.setEnabled(true);
					archivoCombo.refrescar();
					mostrarDialogoDuplicada(dup);
				});

			} catch (TotalNegativoException ex) {
				procesando.set(false);
				rotador.interrupt();
				final TotalNegativoException neg = ex;
				try { archivoService.actualizarEstado(archivoAConvertir, "PROCESADO_ERROR", neg.getMessage()); } catch (Exception ignored) {}
				ui.access(() -> {
					mensajeProcesando.setText("Procesando imagen con IA...");
					panelProgreso.setVisible(false);
					btnConvertir.setEnabled(true);
					archivoCombo.refrescar();
					mostrarDialogoTotalNegativo(neg);
				});

			} catch (Exception ex) {
				procesando.set(false);
				rotador.interrupt();
				if (esErrorDeConexion(ex)) {
					try { archivoService.actualizarEstado(archivoAConvertir, "PENDIENTE"); } catch (Exception ignored) {}
					ui.access(() -> {
						mensajeProcesando.setText("Procesando imagen con IA...");
						panelProgreso.setVisible(false);
						btnConvertir.setEnabled(true);
						archivoCombo.refrescar();
						mostrarDialogoErrorConexion();
					});
				} else {
					try { archivoService.actualizarEstado(archivoAConvertir, "PROCESADO_ERROR", ex.getMessage()); } catch (Exception ignored) {}
					ui.access(() -> {
						mensajeProcesando.setText("Procesando imagen con IA...");
						panelProgreso.setVisible(false);
						btnConvertir.setEnabled(true);
						archivoCombo.refrescar();
						Notification.show("Error al convertir: " + ex.getMessage())
								.addThemeVariants(NotificationVariant.LUMO_ERROR);
					});
				}
			}
		}).start();
	}
	// ── Verificar campos obligatorios de una factura ──────────────────────────
	private List<String> verificarCamposObligatorios(DocumentoConvertido doc) {
		List<String> faltantes = new ArrayList<>();
		if (estaVacio(doc.getCuit()))              faltantes.add("CUIT del Emisor");
		if (estaVacio(doc.getCodigoArca()))        faltantes.add("Código ARCA");
		if (estaVacio(doc.getCentroEmision()))     faltantes.add("Centro de Emisión (Punto de Venta)");
		if (estaVacio(doc.getNumeroComprobante())) faltantes.add("N° Comprobante");
		if (doc.getFechaEmision() == null)         faltantes.add("Fecha de Emisión");
		if (estaVacio(doc.getMoneda()))            faltantes.add("Moneda");
		if (doc.getTotal() == null)               faltantes.add("Total");
		return faltantes;
	}

	// ── Diálogo de error con lista de campos faltantes ────────────────────────
	private void mostrarDialogoNoFactura(List<String> camposFaltantes) {
		Dialog dialog = new Dialog();
		dialog.setModal(true);
		dialog.setWidth("500px");

		Icon icono = VaadinIcon.WARNING.create();
		icono.setSize("48px");
		icono.setColor("#dc2626");

		H3 titulo = new H3("Documento no válido");
		titulo.getStyle().set("color", "#dc2626").set("margin", "8px 0 0 0").set("font-weight", "700");

		Paragraph mensaje = new Paragraph(
				"El documento procesado no es una factura válida o le faltan campos obligatorios.");
		mensaje.getStyle()
				.set("text-align", "center").set("color", "#475569")
				.set("font-size", "0.875rem").set("white-space", "pre-line").set("margin", "0");

		VerticalLayout listaCampos = new VerticalLayout();
		listaCampos.setPadding(false);
		listaCampos.setSpacing(false);
		listaCampos.getStyle()
				.set("background-color", "#fef2f2").set("border", "1px solid #fecaca")
				.set("border-radius", "8px").set("padding", "12px").set("gap", "4px")
				.set("width", "100%").set("margin-top", "8px");

		Span tituloLista = new Span("Campos obligatorios no encontrados:");
		tituloLista.getStyle().set("font-weight", "600").set("color", "#dc2626").set("font-size", "0.8rem");
		listaCampos.add(tituloLista);
		for (String campo : camposFaltantes) {
			Span item = new Span("• " + campo);
			item.getStyle().set("color", "#991b1b").set("font-size", "0.8rem");
			listaCampos.add(item);
		}

		VerticalLayout contenido = new VerticalLayout(icono, titulo, mensaje, listaCampos);
		contenido.setAlignItems(FlexComponent.Alignment.CENTER);
		contenido.setPadding(true);
		contenido.setSpacing(true);

		Button btnAceptar = new Button("Aceptar", e -> dialog.close());
		btnAceptar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		btnAceptar.getStyle().set("background-color", "#dc2626").set("color", "white");

		dialog.add(contenido);
		dialog.getFooter().add(btnAceptar);
		dialog.open();
	}

	// ── Diálogo: factura duplicada ────────────────────────────────────────────
	private void mostrarDialogoDuplicada(FacturaDuplicadaException ex) {
		Dialog dialog = new Dialog();
		dialog.setModal(true);
		dialog.setWidth("520px");

		Icon icono = VaadinIcon.WARNING.create();
		icono.setSize("48px");
		icono.setColor("#dc2626");

		H3 titulo = new H3("Factura ya procesada");
		titulo.getStyle().set("color", "#dc2626").set("margin", "8px 0 0 0").set("font-weight", "700");

		Paragraph mensaje = new Paragraph(
				"Ya existe una factura registrada con la misma combinación de datos.\n"
				+ "El archivo fue marcado como procesado con error y no volverá a aparecer en el selector.");
		mensaje.getStyle()
				.set("text-align", "center").set("color", "#475569")
				.set("font-size", "0.875rem").set("white-space", "pre-line").set("margin", "0");

		VerticalLayout detalle = new VerticalLayout();
		detalle.setPadding(false);
		detalle.setSpacing(false);
		detalle.getStyle()
				.set("background-color", "#fef2f2").set("border", "1px solid #fecaca")
				.set("border-radius", "8px").set("padding", "12px").set("gap", "6px")
				.set("width", "100%").set("margin-top", "8px");

		Span tituloDetalle = new Span("Clave de la factura duplicada:");
		tituloDetalle.getStyle().set("font-weight", "600").set("color", "#dc2626").set("font-size", "0.8rem");
		detalle.add(tituloDetalle);

		String[][] campos = {
			{ "CUIT del Emisor",   ex.getCuit() },
			{ "Código ARCA",       ex.getCodigoArca() },
			{ "Centro de Emisión", ex.getCentroEmision() },
			{ "N° Comprobante",    ex.getNumeroComprobante() }
		};
		for (String[] par : campos) {
			Span item = new Span(par[0] + ": " + par[1]);
			item.getStyle().set("color", "#991b1b").set("font-size", "0.82rem");
			detalle.add(item);
		}

		VerticalLayout contenido = new VerticalLayout(icono, titulo, mensaje, detalle);
		contenido.setAlignItems(FlexComponent.Alignment.CENTER);
		contenido.setPadding(true);
		contenido.setSpacing(true);

		Button btnAceptar = new Button("Aceptar", e -> dialog.close());
		btnAceptar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		btnAceptar.getStyle().set("background-color", "#dc2626").set("color", "white");

		dialog.add(contenido);
		dialog.getFooter().add(btnAceptar);
		dialog.open();
	}

	// ── Diálogo: total negativo ───────────────────────────────────────────────
	private void mostrarDialogoTotalNegativo(TotalNegativoException ex) {
		Dialog dialog = new Dialog();
		dialog.setModal(true);
		dialog.setWidth("480px");

		Icon icono = VaadinIcon.WARNING.create();
		icono.setSize("48px");
		icono.setColor("#dc2626");

		H3 titulo = new H3("Total inválido");
		titulo.getStyle().set("color", "#dc2626").set("margin", "8px 0 0 0").set("font-weight", "700");

		Paragraph mensaje = new Paragraph(
				"No se permiten facturas con subtotal negativo.\n"
				+ "Revisá el documento e intentá nuevamente.");
		mensaje.getStyle()
				.set("text-align", "center").set("color", "#475569")
				.set("font-size", "0.875rem").set("white-space", "pre-line").set("margin", "0");

		VerticalLayout detalle = new VerticalLayout();
		detalle.setPadding(false);
		detalle.setSpacing(false);
		detalle.getStyle()
				.set("background-color", "#fef2f2").set("border", "1px solid #fecaca")
				.set("border-radius", "8px").set("padding", "12px")
				.set("width", "100%").set("margin-top", "8px");

		Span itemTotal = new Span("Total detectado: " + ex.getTotal());
		itemTotal.getStyle().set("color", "#991b1b").set("font-size", "0.82rem").set("font-weight", "600");
		detalle.add(itemTotal);

		VerticalLayout contenido = new VerticalLayout(icono, titulo, mensaje, detalle);
		contenido.setAlignItems(FlexComponent.Alignment.CENTER);
		contenido.setPadding(true);
		contenido.setSpacing(true);

		Button btnAceptar = new Button("Aceptar", e -> dialog.close());
		btnAceptar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		btnAceptar.getStyle().set("background-color", "#dc2626").set("color", "white");

		dialog.add(contenido);
		dialog.getFooter().add(btnAceptar);
		dialog.open();
	}

	// ── Diálogo de resultado de conversión ───────────────────────────────────
	private void mostrarDialogoResultado(int exitosas, List<String> fallas) {
		Dialog dialog = new Dialog();
		dialog.setModal(true);
		dialog.setWidth("480px");

		boolean hayFallas = !fallas.isEmpty();

		Icon icono = hayFallas
				? VaadinIcon.WARNING.create()
				: VaadinIcon.CHECK_CIRCLE.create();
		icono.setSize("48px");
		icono.setColor(hayFallas ? "#d97706" : "#16a34a");

		String titulotxt = hayFallas
				? "Conversión finalizada con observaciones"
				: (exitosas == 1 ? "¡Factura convertida exitosamente!" : "¡Facturas convertidas exitosamente!");

		H3 titulo = new H3(titulotxt);
		titulo.getStyle()
				.set("color", hayFallas ? "#92400e" : "#14532d")
				.set("margin", "8px 0 0 0")
				.set("font-weight", "700")
				.set("text-align", "center");

		VerticalLayout contenido = new VerticalLayout(icono, titulo);
		contenido.setAlignItems(FlexComponent.Alignment.CENTER);
		contenido.setPadding(true);
		contenido.setSpacing(true);

		if (exitosas > 0) {
			Span linea = new Span("✔  " + exitosas + (exitosas == 1 ? " factura convertida." : " facturas convertidas."));
			linea.getStyle().set("color", "#15803d").set("font-weight", "600").set("font-size", "0.95rem");
			contenido.add(linea);
		}

		if (hayFallas) {
			VerticalLayout panelFallas = new VerticalLayout();
			panelFallas.setPadding(false);
			panelFallas.setSpacing(false);
			panelFallas.setWidthFull();
			panelFallas.getStyle()
					.set("background-color", "#fffbeb").set("border", "1px solid #fde68a")
					.set("border-radius", "8px").set("padding", "12px").set("gap", "4px")
					.set("margin-top", "4px");

			Span tituloFallas = new Span("Facturas con errores:");
			tituloFallas.getStyle().set("font-weight", "600").set("color", "#92400e").set("font-size", "0.82rem");
			panelFallas.add(tituloFallas);

			for (String msg : fallas) {
				Span item = new Span("• " + msg);
				item.getStyle().set("color", "#78350f").set("font-size", "0.82rem");
				panelFallas.add(item);
			}
			contenido.add(panelFallas);
		}

		Button btnAceptar = new Button("Aceptar", e -> dialog.close());
		btnAceptar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		btnAceptar.getStyle()
				.set("background-color", hayFallas ? "#d97706" : "#16a34a")
				.set("color", "white");

		dialog.add(contenido);
		dialog.getFooter().add(btnAceptar);
		dialog.open();
	}

	private Component crearBotonDescarga(DocumentoConvertido resultado) {
		String nombreArchivo = resultado.getArchivo().getNombre().replaceAll("\\s+", "_") + ".json";
		StreamResource resource = new StreamResource(nombreArchivo,
				() -> new ByteArrayInputStream(resultado.getJsonResultado().getBytes(StandardCharsets.UTF_8)));
		Anchor btnDescargar = new Anchor(resource, "");
		btnDescargar.getElement().setAttribute("download", true);
		Button botonDescarga = new Button("Descargar JSON", VaadinIcon.DOWNLOAD.create());
		botonDescarga.getStyle().set("background-color", "#2563eb").set("color", "white");
		btnDescargar.add(botonDescarga);
		return btnDescargar;
	}

	@Override
	protected void accionGuardar() { /* integrado en ejecutarConversion() */ }

	private VerticalLayout crearCampoInfo(String etiqueta, String valor) {
		Span lbl = new Span(etiqueta);
		lbl.getStyle()
				.set("font-size", "0.72rem").set("font-weight", "600")
				.set("color", "#0369a1").set("text-transform", "uppercase")
				.set("letter-spacing", "0.05em");
		Span val = new Span(valor);
		val.getStyle()
				.set("font-size", "1rem").set("font-weight", "700")
				.set("color", "#0c4a6e");
		VerticalLayout campo = new VerticalLayout(lbl, val);
		campo.setPadding(false);
		campo.setSpacing(false);
		campo.setWidth("auto");
		campo.getStyle().set("gap", "2px").set("flex-shrink", "0");
		return campo;
	}

	private boolean estaVacio(String valor) {
		return valor == null || valor.isEmpty() || valor.equals("null");
	}

	private static String formatImporte(BigDecimal valor) {
		if (valor == null) return "";
		return NumberFormat.getNumberInstance(new Locale("es", "AR")).format(valor);
	}

	@Override
	protected void accionCancelar() {
		archivoCombo.limpiar();
		archivoSeleccionado = null;
		btnVerArchivo.setEnabled(false);
		panelResultado.removeAll();
		panelResultado.setVisible(false);
		panelProgreso.setVisible(false);
		actualizarStepper("activo", "pendiente", "pendiente");
	}

	// ── Detecta errores de red/conexión (Connection reset, timeout, etc.) ──────
	private boolean esErrorDeConexion(Throwable ex) {
		Throwable actual = ex;
		while (actual != null) {
			String msg = actual.getMessage();
			if (msg != null) {
				String msgLower = msg.toLowerCase();
				if (msgLower.contains("connection reset")
						|| msgLower.contains("connection refused")
						|| msgLower.contains("connection timed out")
						|| msgLower.contains("broken pipe")
						|| msgLower.contains("remotely closed")) {
					return true;
				}
			}
			actual = actual.getCause();
		}
		return false;
	}

	// ── Diálogo: archivo duplicado (mismo contenido ya procesado) ──────────────
	private void mostrarDialogoArchivoDuplicado(ArchivoDuplicadoException ex) {
		Dialog dialog = new Dialog();
		dialog.setModal(true);
		dialog.setWidth("500px");

		Icon icono = VaadinIcon.WARNING.create();
		icono.setSize("48px");
		icono.setColor("#d97706");

		H3 titulo = new H3("Archivo ya procesado");
		titulo.getStyle().set("color", "#d97706").set("margin", "8px 0 0 0").set("font-weight", "700");

		Paragraph mensaje = new Paragraph(
				"El contenido de este archivo es idéntico a uno procesado anteriormente.\n"
				+ "El archivo volvió a estado Pendiente.");
		mensaje.getStyle()
				.set("text-align", "center").set("color", "#475569")
				.set("font-size", "0.875rem").set("white-space", "pre-line").set("margin", "0");

		VerticalLayout detalle = new VerticalLayout();
		detalle.setPadding(false);
		detalle.setSpacing(false);
		detalle.getStyle()
				.set("background-color", "#fffbeb").set("border", "1px solid #fde68a")
				.set("border-radius", "8px").set("padding", "12px").set("gap", "6px")
				.set("width", "100%").set("margin-top", "8px");
		Span tituloDetalle = new Span("Archivo original:");
		tituloDetalle.getStyle().set("font-weight", "600").set("color", "#92400e").set("font-size", "0.8rem");
		detalle.add(tituloDetalle);
		detalle.add(new Span("Código: " + ex.getCodigoOriginal()));
		detalle.add(new Span("Nombre: " + ex.getNombreOriginal()));

		VerticalLayout contenido = new VerticalLayout(icono, titulo, mensaje, detalle);
		contenido.setAlignItems(FlexComponent.Alignment.CENTER);
		contenido.setPadding(true);
		contenido.setSpacing(true);

		Button btnAceptar = new Button("Aceptar", e -> dialog.close());
		btnAceptar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		btnAceptar.getStyle().set("background-color", "#d97706").set("color", "white");

		dialog.add(contenido);
		dialog.getFooter().add(btnAceptar);
		dialog.open();
	}

	// ── Diálogo de error de conexión (el archivo queda en PENDIENTE) ───────────
	private void mostrarDialogoErrorConexion() {
		Dialog dialog = new Dialog();
		dialog.setModal(true);
		dialog.setWidth("420px");

		Icon icono = VaadinIcon.WARNING.create();
		icono.setSize("48px");
		icono.setColor("#d97706");

		H3 titulo = new H3("Error de conexión");
		titulo.getStyle().set("color", "#d97706").set("margin", "8px 0 0 0").set("font-weight", "700");

		Paragraph mensaje = new Paragraph(
				"Ocurrió un error de conexión al procesar el archivo.\n"
				+ "El archivo quedó en estado Pendiente. Por favor, volvé a intentarlo.");
		mensaje.getStyle()
				.set("text-align", "center").set("color", "#475569")
				.set("font-size", "0.875rem").set("white-space", "pre-line").set("margin", "0");

		VerticalLayout contenido = new VerticalLayout(icono, titulo, mensaje);
		contenido.setAlignItems(FlexComponent.Alignment.CENTER);
		contenido.setPadding(true);
		contenido.setSpacing(true);

		Button btnAceptar = new Button("Aceptar", e -> dialog.close());
		btnAceptar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		btnAceptar.getStyle().set("background-color", "#d97706").set("color", "white");

		dialog.add(contenido);
		dialog.getFooter().add(btnAceptar);
		dialog.open();
	}
}
