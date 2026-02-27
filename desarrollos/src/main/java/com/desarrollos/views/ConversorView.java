package com.desarrollos.views;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.desarrollos.base.FormView;
import com.desarrollos.combos.ArchivoCombo;
import com.desarrollos.entities.Archivo;
import com.desarrollos.entities.DocumentoConvertido;
import com.desarrollos.entities.NetoGravado;
import com.desarrollos.entities.PercepcionIIBB;
import com.desarrollos.entities.PercepcionIVA;
import com.desarrollos.entities.ProductoConcepto;
import com.desarrollos.entities.Vencimiento;
import com.desarrollos.services.ArchivoService;
import com.desarrollos.services.DocumentoConvertidoService;
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
	private final ProgressBar progressBar      = new ProgressBar();
	private final VerticalLayout panelProgreso = new VerticalLayout();

	// ── Panel resultado ───────────────────────────────────────────────────────
	private final VerticalLayout panelResultado  = new VerticalLayout();
	private final Pre jsonViewer                 = new Pre();
	private final Button btnVerMas   = new Button("Ver más",   VaadinIcon.CHEVRON_DOWN.create());
	private final Button btnVerMenos = new Button("Ver menos", VaadinIcon.CHEVRON_UP.create());
	private final HorizontalLayout barraDescarga = new HorizontalLayout();
	private final Paragraph parrafoNotas         = new Paragraph();

	private final Grid<ProductoConcepto> gridProductos        = new Grid<>(ProductoConcepto.class, false);
	private final Grid<NetoGravado>      gridNetosGravados    = new Grid<>(NetoGravado.class, false);
	private final Grid<PercepcionIIBB>   gridPercepcionesIIBB = new Grid<>(PercepcionIIBB.class, false);
	private final Grid<PercepcionIVA>    gridPercepcionesIVA  = new Grid<>(PercepcionIVA.class, false);
	private final Grid<Vencimiento>      gridVencimientos     = new Grid<>(Vencimiento.class, false);

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
				panelResultado.setVisible(false);
				panelProgreso.setVisible(false);
				jsonViewer.setText("");
				barraDescarga.removeAll();
			}
		});

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

		// ── Panel progreso ────────────────────────────────────────────────────
		progressBar.setIndeterminate(true);
		progressBar.getStyle().set("width", "400px")
				.set("--vaadin-progress-value-background", "#2563eb");

		H3 mensajeProcesando = new H3("Procesando imagen con IA...");
		mensajeProcesando.getStyle()
				.set("color", "#1e293b").set("font-size", "1rem")
				.set("margin", "0").set("font-weight", "600");

		panelProgreso.add(mensajeProcesando, progressBar);
		panelProgreso.setPadding(false);
		panelProgreso.setSpacing(true);
		panelProgreso.setVisible(false);
		panelProgreso.getStyle().set("margin-top", "20px");

		// ── Panel resultado ───────────────────────────────────────────────────
		H3 tituloResultado = new H3("Resultado JSON");
		tituloResultado.getStyle()
				.set("color", "#1e293b").set("margin", "0").set("font-weight", "700")
				.set("font-size", "1rem").set("letter-spacing", "-0.2px");

		jsonViewer.getStyle()
				.set("background-color", "#1e1e2e").set("color", "#cdd6f4").set("border", "none")
				.set("border-radius", "12px").set("padding", "20px")
				.set("font-family", "'JetBrains Mono', 'Fira Code', 'Courier New', monospace")
				.set("font-size", "13px").set("width", "100%").set("white-space", "pre-wrap")
				.set("word-break", "break-word").set("box-shadow", "inset 0 2px 8px rgba(0,0,0,0.4)")
				.set("line-height", "1.6");
		jsonViewer.setVisible(false);

		btnVerMas.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		btnVerMas.getStyle().set("align-self", "flex-start");
		btnVerMas.addClickListener(e -> {
			jsonViewer.setVisible(true);
			btnVerMas.setVisible(false);
			btnVerMenos.setVisible(true);
		});

		btnVerMenos.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		btnVerMenos.getStyle().set("align-self", "flex-start");
		btnVerMenos.setVisible(false);
		btnVerMenos.addClickListener(e -> {
			jsonViewer.setVisible(false);
			btnVerMas.setVisible(true);
			btnVerMenos.setVisible(false);
		});

		parrafoNotas.getStyle()
				.set("color", "#92400e").set("font-size", "0.875rem").set("margin-top", "10px")
				.set("white-space", "pre-line").set("background-color", "#fffbeb")
				.set("border", "1px solid #fde68a").set("border-radius", "8px").set("padding", "12px");
		parrafoNotas.setVisible(false);

		// ── Grid productos/conceptos ──────────────────────────────────────────
		H4 tituloProductos = new H4("Productos / Conceptos");
		tituloProductos.getStyle().set("color", "#002060").set("margin", "16px 0 4px 0");
		gridProductos.addColumn(ProductoConcepto::getSku).setHeader("SKU").setWidth("130px").setFlexGrow(0);
		gridProductos.addColumn(ProductoConcepto::getDescripcion).setHeader("Descripción").setFlexGrow(1);
		gridProductos.addColumn(ProductoConcepto::getCantidad).setHeader("Cant.").setWidth("70px").setFlexGrow(0);
		gridProductos.addColumn(ProductoConcepto::getPrecioUnitario).setHeader("P. Unit.").setWidth("95px").setFlexGrow(0);
		gridProductos.addColumn(ProductoConcepto::getDescuento).setHeader("Desc.").setWidth("80px").setFlexGrow(0);
		gridProductos.addColumn(ProductoConcepto::getSubTotal).setHeader("Subtotal").setWidth("95px").setFlexGrow(0);
		gridProductos.addColumn(ProductoConcepto::getAlicuotaIva).setHeader("IVA").setWidth("75px").setFlexGrow(0);
		gridProductos.addColumn(ProductoConcepto::getOrdenCompra).setHeader("OC").setWidth("90px").setFlexGrow(0);
		gridProductos.addColumn(ProductoConcepto::getRemito).setHeader("Remito").setWidth("130px").setFlexGrow(0);
		gridProductos.setAllRowsVisible(true);
		gridProductos.getStyle().set("margin-top", "4px");
		gridProductos.setVisible(false);

		// ── Grid netos gravados ───────────────────────────────────────────────
		H4 tituloNetos = new H4("Netos Gravados e IVA");
		tituloNetos.getStyle().set("color", "#002060").set("margin", "16px 0 4px 0");
		gridNetosGravados.addColumn(NetoGravado::getAlicuota).setHeader("Alícuota").setWidth("110px").setFlexGrow(0);
		gridNetosGravados.addColumn(NetoGravado::getImporteNetoGravado).setHeader("Importe Neto Gravado").setFlexGrow(1);
		gridNetosGravados.addColumn(NetoGravado::getIva).setHeader("IVA").setWidth("130px").setFlexGrow(0);
		gridNetosGravados.setAllRowsVisible(true);
		gridNetosGravados.getStyle().set("margin-top", "4px");
		gridNetosGravados.setVisible(false);

		// ── Grid percepciones IIBB ────────────────────────────────────────────
		H4 tituloPercepcionesIIBB = new H4("Percepciones IIBB");
		tituloPercepcionesIIBB.getStyle().set("color", "#002060").set("margin", "16px 0 4px 0");
		gridPercepcionesIIBB.addColumn(PercepcionIIBB::getProvincia).setHeader("Provincia").setFlexGrow(1);
		gridPercepcionesIIBB.addColumn(PercepcionIIBB::getAlicuota).setHeader("Alícuota").setWidth("110px").setFlexGrow(0);
		gridPercepcionesIIBB.addColumn(PercepcionIIBB::getImporte).setHeader("Importe").setWidth("130px").setFlexGrow(0);
		gridPercepcionesIIBB.setAllRowsVisible(true);
		gridPercepcionesIIBB.getStyle().set("margin-top", "4px");
		gridPercepcionesIIBB.setVisible(false);

		// ── Grid percepciones IVA ─────────────────────────────────────────────
		H4 tituloPercepcionesIVA = new H4("Percepciones IVA");
		tituloPercepcionesIVA.getStyle().set("color", "#002060").set("margin", "16px 0 4px 0");
		gridPercepcionesIVA.addColumn(PercepcionIVA::getAlicuota).setHeader("Alícuota").setWidth("150px").setFlexGrow(0);
		gridPercepcionesIVA.addColumn(PercepcionIVA::getImporte).setHeader("Importe").setFlexGrow(1);
		gridPercepcionesIVA.setAllRowsVisible(true);
		gridPercepcionesIVA.getStyle().set("margin-top", "4px");
		gridPercepcionesIVA.setVisible(false);

		// ── Grid vencimientos ─────────────────────────────────────────────────
		H4 tituloVencimientos = new H4("Vencimientos");
		tituloVencimientos.getStyle().set("color", "#002060").set("margin", "16px 0 4px 0");
		gridVencimientos.addColumn(Vencimiento::getFecha).setHeader("Fecha").setWidth("150px").setFlexGrow(0);
		gridVencimientos.addColumn(Vencimiento::getImporte).setHeader("Importe").setFlexGrow(1);
		gridVencimientos.setAllRowsVisible(true);
		gridVencimientos.getStyle().set("margin-top", "4px");
		gridVencimientos.setVisible(false);

		panelResultado.add(tituloResultado, btnVerMas, jsonViewer, btnVerMenos,
				tituloProductos, gridProductos,
				tituloNetos, gridNetosGravados,
				tituloPercepcionesIIBB, gridPercepcionesIIBB,
				tituloPercepcionesIVA, gridPercepcionesIVA,
				tituloVencimientos, gridVencimientos,
				parrafoNotas, barraDescarga);
		panelResultado.setPadding(false);
		panelResultado.setSpacing(true);
		panelResultado.setVisible(false);
		panelResultado.getStyle().set("margin-top", "20px");

		contenidoPrincipal.add(archivoCombo, botones, panelProgreso, panelResultado);
		barraBotones.setVisible(false);
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
			Notification.show("Seleccioná un archivo antes de convertir")
					.addThemeVariants(NotificationVariant.LUMO_WARNING);
			return;
		}

		panelProgreso.setVisible(true);
		panelResultado.setVisible(false);
		btnConvertir.setEnabled(false);
		btnVerArchivo.setEnabled(false);

		final Archivo archivoAConvertir = archivoSeleccionado;
		// Limpiar selección antes de iniciar para que desaparezca del combo
		archivoSeleccionado = null;
		archivoCombo.limpiar();
		archivoCombo.refrescar();

		final UI ui = UI.getCurrent();

		new Thread(() -> {
			try {
				final List<DocumentoConvertido> resultados = documentoConvertidoService.convertir(archivoAConvertir);
				final DocumentoConvertido resultado = resultados.get(0);
				final int totalFacturas = resultados.size();

				// Detectar campos obligatorios faltantes en la primera factura (para mostrar al usuario)
				final List<String> camposFaltantes = new ArrayList<>();
				if (estaVacio(resultado.getCuit()))              camposFaltantes.add("CUIT del Emisor");
				if (estaVacio(resultado.getCodigoArca()))        camposFaltantes.add("Código ARCA");
				if (estaVacio(resultado.getCentroEmision()))     camposFaltantes.add("Centro de Emisión (Punto de Venta)");
				if (estaVacio(resultado.getNumeroComprobante())) camposFaltantes.add("N° Comprobante");
				if (estaVacio(resultado.getFechaEmision()))      camposFaltantes.add("Fecha de Emisión");
				if (estaVacio(resultado.getMoneda()))            camposFaltantes.add("Moneda");
				if (estaVacio(resultado.getTotal()))             camposFaltantes.add("Total");

				ui.access(() -> {
					panelProgreso.setVisible(false);
					btnConvertir.setEnabled(true);

					if (!camposFaltantes.isEmpty()) {
						mostrarDialogoNoFactura(camposFaltantes);
						return;
					}

					// Si hay múltiples facturas, mostrar banner informativo
					if (totalFacturas > 1) {
						Notification notifMulti = new Notification(
								totalFacturas + " facturas distintas detectadas y guardadas. " +
								"Se muestra la primera — ver todas en Lista de JSONs.",
								6000, Notification.Position.TOP_CENTER);
						notifMulti.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
						notifMulti.open();
					}

					// Mostrar JSON de la primera factura (colapsado por defecto)
					jsonViewer.setText(resultado.getJsonResultado());
					jsonViewer.setVisible(false);
					btnVerMas.setVisible(true);
					btnVerMenos.setVisible(false);

					// Cargar grids
					if (!resultado.getProductosConceptos().isEmpty()) {
						gridProductos.setItems(resultado.getProductosConceptos());
						gridProductos.setVisible(true);
					} else { gridProductos.setVisible(false); }

					if (!resultado.getNetosGravados().isEmpty()) {
						gridNetosGravados.setItems(resultado.getNetosGravados());
						gridNetosGravados.setVisible(true);
					} else { gridNetosGravados.setVisible(false); }

					if (!resultado.getPercepcionesIIBB().isEmpty()) {
						gridPercepcionesIIBB.setItems(resultado.getPercepcionesIIBB());
						gridPercepcionesIIBB.setVisible(true);
					} else { gridPercepcionesIIBB.setVisible(false); }

					if (!resultado.getPercepcionesIVA().isEmpty()) {
						gridPercepcionesIVA.setItems(resultado.getPercepcionesIVA());
						gridPercepcionesIVA.setVisible(true);
					} else { gridPercepcionesIVA.setVisible(false); }

					if (!resultado.getVencimientos().isEmpty()) {
						gridVencimientos.setItems(resultado.getVencimientos());
						gridVencimientos.setVisible(true);
					} else { gridVencimientos.setVisible(false); }

					generarBotonDescarga(resultado);

					// Notas de campos opcionales faltantes
					StringBuilder notas = new StringBuilder();
					if (estaVacio(resultado.getRazonSocial()))          notas.append("• Razón social\n");
					if (estaVacio(resultado.getSituacionIva()))         notas.append("• Situación ante IVA\n");
					if (estaVacio(resultado.getDireccion()))            notas.append("• Dirección\n");
					if (estaVacio(resultado.getCiudad()))               notas.append("• Ciudad/Localidad\n");
					if (estaVacio(resultado.getCodigoPostal()))         notas.append("• Código postal\n");
					if (estaVacio(resultado.getProvincia()))            notas.append("• Provincia\n");
					if (estaVacio(resultado.getPais()))                 notas.append("• País\n");
					if (estaVacio(resultado.getTelefono()))             notas.append("• Teléfono\n");
					if (estaVacio(resultado.getMail()))                 notas.append("• Mail\n");
					if (estaVacio(resultado.getLetra()))                notas.append("• Letra del comprobante\n");
					if (estaVacio(resultado.getCae()))                  notas.append("• CAE\n");
					if (estaVacio(resultado.getFechaVencimientoCae()))  notas.append("• Fecha venc. CAE\n");
					if (estaVacio(resultado.getCotizacion()))           notas.append("• Cotización\n");
					if (estaVacio(resultado.getOrdenCompra()))          notas.append("• Orden de compra\n");
					if (resultado.getProductosConceptos().isEmpty())    notas.append("• Productos/Conceptos\n");
					if (resultado.getNetosGravados().isEmpty())         notas.append("• Netos gravados e IVA\n");
					if (estaVacio(resultado.getSubTotalNoGravado()))    notas.append("• Importe neto no gravado\n");
					if (resultado.getPercepcionesIIBB().isEmpty())      notas.append("• Percepciones IIBB\n");
					if (resultado.getPercepcionesIVA().isEmpty())       notas.append("• Percepciones IVA\n");
					if (resultado.getVencimientos().isEmpty())          notas.append("• Vencimientos\n");

					if (notas.length() > 0) {
						parrafoNotas.setText("⚠ Campos no encontrados:\n" + notas);
						parrafoNotas.setVisible(true);
					} else {
						parrafoNotas.setVisible(false);
					}

					panelResultado.setVisible(true);
					if (totalFacturas == 1) {
						Notification.show("¡Archivo convertido exitosamente!")
								.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
					}
				});

			} catch (Exception ex) {
				ui.access(() -> {
					panelProgreso.setVisible(false);
					btnConvertir.setEnabled(true);
					Notification.show("Error al convertir: " + ex.getMessage())
							.addThemeVariants(NotificationVariant.LUMO_ERROR);
				});
				// Marcar el archivo como error para que no vuelva a aparecer en el combo
				try { archivoService.actualizarEstado(archivoAConvertir, "PROCESADO_ERROR"); } catch (Exception ignored) {}
			}
		}).start();
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
				"El documento procesado no es una factura válida o le faltan campos obligatorios.\n"
				+ "Se guardó con estado 'Procesado error' y no volverá a aparecer en el selector de archivos.");
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

	private void generarBotonDescarga(DocumentoConvertido resultado) {
		barraDescarga.removeAll();
		String nombreArchivo = resultado.getArchivo().getNombre().replaceAll("\\s+", "_") + ".json";
		StreamResource resource = new StreamResource(nombreArchivo,
				() -> new ByteArrayInputStream(resultado.getJsonResultado().getBytes(StandardCharsets.UTF_8)));
		Anchor btnDescargar = new Anchor(resource, "");
		btnDescargar.getElement().setAttribute("download", true);
		Button botonDescarga = new Button("Descargar JSON", VaadinIcon.DOWNLOAD.create());
		botonDescarga.getStyle().set("background-color", "#2563eb").set("color", "white");
		btnDescargar.add(botonDescarga);
		barraDescarga.add(btnDescargar);
	}

	@Override
	protected void accionGuardar() { /* integrado en ejecutarConversion() */ }

	private boolean estaVacio(String valor) {
		return valor == null || valor.isEmpty() || valor.equals("null");
	}

	@Override
	protected void accionCancelar() {
		archivoCombo.limpiar();
		archivoSeleccionado = null;
		btnVerArchivo.setEnabled(false);
		panelResultado.setVisible(false);
		panelProgreso.setVisible(false);
		jsonViewer.setText("");
		jsonViewer.setVisible(false);
		btnVerMas.setVisible(true);
		btnVerMenos.setVisible(false);
		gridProductos.setItems(Collections.emptyList());    gridProductos.setVisible(false);
		gridNetosGravados.setItems(Collections.emptyList()); gridNetosGravados.setVisible(false);
		gridPercepcionesIIBB.setItems(Collections.emptyList()); gridPercepcionesIIBB.setVisible(false);
		gridPercepcionesIVA.setItems(Collections.emptyList());  gridPercepcionesIVA.setVisible(false);
		gridVencimientos.setItems(Collections.emptyList());  gridVencimientos.setVisible(false);
		barraDescarga.removeAll();
		parrafoNotas.setVisible(false);
		parrafoNotas.setText("");
	}
}
