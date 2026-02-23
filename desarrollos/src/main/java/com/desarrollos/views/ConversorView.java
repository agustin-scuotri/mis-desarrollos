package com.desarrollos.views;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.desarrollos.base.FormView;
import com.desarrollos.combos.ArchivoCombo;
import com.desarrollos.entities.Archivo;
import com.desarrollos.entities.DocumentoConvertido;
import com.desarrollos.services.ArchivoService;
import com.desarrollos.services.DocumentoConvertidoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

@Route(value = "conversor", layout = MainLayout.class)
@PageTitle("Conversor de Facturas")
public class ConversorView extends FormView {

	private final ArchivoService archivoService;
	private final DocumentoConvertidoService documentoConvertidoService;
	private ArchivoCombo archivoCombo;
	private Archivo archivoSeleccionado;

	// ── Botón convertir ───────────────────────────────────────────────────────
	private final Button btnConvertir = new Button("Convertir", VaadinIcon.BOLT.create());

	// ── Progreso ──────────────────────────────────────────────────────────────
	private final ProgressBar progressBar = new ProgressBar();
	private final VerticalLayout panelProgreso = new VerticalLayout();

	// ── Panel resultado ───────────────────────────────────────────────────────
	private final VerticalLayout panelResultado = new VerticalLayout();
	private final Pre jsonViewer = new Pre();
	private final HorizontalLayout barraDescarga = new HorizontalLayout();

	private final Paragraph parrafoNotas = new Paragraph();

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
			if (e.getValue() != null) {
				panelResultado.setVisible(false);
				panelProgreso.setVisible(false);
				jsonViewer.setText("");
				barraDescarga.removeAll();
			}
		});

		// ── Botón convertir ───────────────────────────────────────────────────
		btnConvertir.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		btnConvertir.getStyle().set("background-color", "#002060").set("color", "white").set("margin-top", "15px");
		btnConvertir.addClickListener(e -> ejecutarConversion());

		// ── Panel progreso ────────────────────────────────────────────────────
		progressBar.setIndeterminate(true);
		progressBar.getStyle().set("width", "400px");

		H3 mensajeProcesando = new H3("Procesando imagen con IA...");
		mensajeProcesando.getStyle().set("color", "#002060").set("font-size", "1rem").set("margin", "0");

		panelProgreso.add(mensajeProcesando, progressBar);
		panelProgreso.setPadding(false);
		panelProgreso.setSpacing(true);
		panelProgreso.setVisible(false);
		panelProgreso.getStyle().set("margin-top", "20px");

		// ── Panel resultado ───────────────────────────────────────────────────
		H3 tituloResultado = new H3("Resultado");
		tituloResultado.getStyle().set("color", "#002060").set("margin", "0");

		jsonViewer.getStyle().set("background-color", "#f0f4ff").set("border", "1px solid rgba(0,32,96,0.2)")
				.set("border-radius", "8px").set("padding", "15px").set("font-family", "monospace")
				.set("font-size", "14px").set("width", "500px").set("white-space", "pre-wrap")
				.set("word-break", "break-word");

		parrafoNotas.getStyle().set("color", "#cc6600").set("font-family", "Verdana, sans-serif")
				.set("font-size", "0.9rem").set("margin-top", "10px").set("white-space", "pre-line");
		parrafoNotas.setVisible(false);

		panelResultado.add(tituloResultado, jsonViewer, parrafoNotas, barraDescarga);
		panelResultado.setPadding(false);
		panelResultado.setSpacing(true);
		panelResultado.setVisible(false);
		panelResultado.getStyle().set("margin-top", "20px");

		contenidoPrincipal.add(archivoCombo, btnConvertir, panelProgreso, panelResultado);
	}

	private void ejecutarConversion() {
		if (archivoSeleccionado == null) {
			Notification.show("Seleccioná un archivo antes de convertir")
					.addThemeVariants(NotificationVariant.LUMO_WARNING);
			return;
		}

		// Mostramos progreso y deshabilitamos botones
		panelProgreso.setVisible(true);
		panelResultado.setVisible(false);
		btnConvertir.setEnabled(false);
		btnGuardar.setEnabled(false);

		try {
			DocumentoConvertido resultado = documentoConvertidoService.convertir(archivoSeleccionado);

			// Ocultamos progreso
			panelProgreso.setVisible(false);

			// Mostramos JSON
			jsonViewer.setText(resultado.getJsonResultado());

			// Generamos botón de descarga
			generarBotonDescarga(resultado);

			StringBuilder notas = new StringBuilder();
			if (estaVacio(resultado.getCuit()))
			    notas.append("• No se encontró el CUIT.\n");
			if (estaVacio(resultado.getRazonSocial()))
			    notas.append("• No se encontró la razón social.\n");
			if (estaVacio(resultado.getSituacionIva()))
			    notas.append("• No se encontró la situación ante IVA.\n");
			if (estaVacio(resultado.getDireccion()))
			    notas.append("• No se encontró la dirección.\n");
			if (estaVacio(resultado.getCiudad()))
			    notas.append("• No se encontró la ciudad/localidad.\n");
			if (estaVacio(resultado.getCodigoPostal()))
			    notas.append("• No se encontró el código postal.\n");
			if (estaVacio(resultado.getProvincia()))
			    notas.append("• No se encontró la provincia.\n");
			if (estaVacio(resultado.getPais()))
			    notas.append("• No se encontró el país.\n");
			if (estaVacio(resultado.getCodigoArca()))
			    notas.append("• No se encontró el código ARCA.\n");
			if (estaVacio(resultado.getLetra()))
			    notas.append("• No se encontró la letra del comprobante.\n");
			if (estaVacio(resultado.getCentroEmision()))
			    notas.append("• No se encontró el centro de emisión (punto de venta).\n");
			if (estaVacio(resultado.getNumeroComprobante()))
			    notas.append("• No se encontró el número de comprobante.\n");
			if (estaVacio(resultado.getFechaEmision()))
			    notas.append("• No se encontró la fecha de emisión.\n");
			if (estaVacio(resultado.getCae()))
			    notas.append("• No se encontró el CAE.\n");
			if (estaVacio(resultado.getFechaVencimientoCae()))
			    notas.append("• No se encontró la fecha de vencimiento del CAE.\n");
			if (estaVacio(resultado.getMoneda()))
			    notas.append("• No se encontró la moneda.\n");
			if (estaVacio(resultado.getCotizacion()))
			    notas.append("• No se encontró la cotización.\n");

			if (notas.length() > 0) {
			    parrafoNotas.setText("⚠ Campos no encontrados:\n" + notas.toString());
			    parrafoNotas.setVisible(true);
			} else {
			    parrafoNotas.setVisible(false);
			}
			
			panelResultado.setVisible(true);

			// Limpiamos combo
			archivoCombo.limpiar();
			archivoSeleccionado = null;

			Notification.show("¡Archivo convertido exitosamente!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);

		} catch (Exception e) {
			panelProgreso.setVisible(false);
			Notification.show("Error al convertir: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
		} finally {
			btnConvertir.setEnabled(true);
			btnGuardar.setEnabled(true);
		}
	}

	private void generarBotonDescarga(DocumentoConvertido resultado) {
		barraDescarga.removeAll();

		String nombreArchivo = resultado.getArchivo().getNombre().replaceAll("\\s+", "_") + ".json";

		StreamResource resource = new StreamResource(nombreArchivo,
				() -> new ByteArrayInputStream(resultado.getJsonResultado().getBytes(StandardCharsets.UTF_8)));

		Anchor btnDescargar = new Anchor(resource, "");
		btnDescargar.getElement().setAttribute("download", true);

		Button botonDescarga = new Button("Descargar JSON", VaadinIcon.DOWNLOAD.create());
		// DESPUÉS
		botonDescarga.getStyle().set("background-color", "#002060").set("color", "white");

		btnDescargar.add(botonDescarga);
		barraDescarga.add(btnDescargar);
	}

	@Override
	protected void accionGuardar() {
		// El guardar está integrado dentro de ejecutarConversion()
	}

	private boolean estaVacio(String valor) {
		return valor == null || valor.isEmpty() || valor.equals("null");
	}

	@Override
	protected void accionCancelar() {
		archivoCombo.limpiar();
		archivoSeleccionado = null;
		panelResultado.setVisible(false);
		panelProgreso.setVisible(false);
		jsonViewer.setText("");
		barraDescarga.removeAll();
		parrafoNotas.setVisible(false);
		parrafoNotas.setText("");
	}
}