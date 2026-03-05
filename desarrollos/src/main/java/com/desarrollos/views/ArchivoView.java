package com.desarrollos.views;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import com.desarrollos.base.FormView;
import com.desarrollos.entities.Archivo;
import com.desarrollos.services.ArchivoService;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;

@PageTitle("Detalle de Archivo")
@Route(value = "archivo-detalle", layout = MainLayout.class)
public class ArchivoView extends FormView implements HasUrlParameter<String> {

	private final ArchivoService service;

	private TextField codigo = new TextField();
	private TextField nombre = new TextField();
	private MemoryBuffer buffer = new MemoryBuffer();
	private Upload upload = new Upload(buffer);
	private Div galeriaContainer = new Div();
	private VerticalLayout zonaVisual = new VerticalLayout();
	
	private Span nombreArchivoLabel = new Span();
	private VerticalLayout estadoCargado = new VerticalLayout();
	private VerticalLayout estadoVacio = new VerticalLayout();

	private Span errorArchivo = new Span("Debe adjuntar un archivo");

	private Binder<Archivo> binder = new BeanValidationBinder<>(Archivo.class);
	private Archivo archivoActual;

	@Autowired
	public ArchivoView(ArchivoService service) {
		this.service = service;
		setTitulo(getTranslation("archivo.nuevo.titulo"));
		configurarCampos();
		configurarBinder();
	}

	@Override
    public void setParameter(BeforeEvent event, @WildcardParameter String parameter) {
        resetearInterfaz(); // Tu método de limpieza

        if (parameter != null && !parameter.isEmpty()) {
            // Limpiamos posibles barras al inicio o final y separamos
            String path = parameter.startsWith("/") ? parameter.substring(1) : parameter;
            String[] partes = path.split("/");
            
            try {
                // El primer segmento SIEMPRE es el ID
                Long id = Long.parseLong(partes[0]);
                
                // El segundo segmento (si existe) define si es lectura
                boolean esLectura = partes.length > 1 && partes[1].equalsIgnoreCase("read");

                this.archivoActual = service.buscarPorId(id);
                if (archivoActual != null && archivoActual.getId() != null) {
                    binder.readBean(archivoActual);
                    actualizarInterfazCargaExistente();

                    if (esLectura || "PROCESADO".equals(archivoActual.getEstadoConversion())) {
                        aplicarModoLectura();
                    } else {
                        setTitulo(getTranslation("archivo.editar.titulo"));
                    }
                }
            } catch (NumberFormatException e) {
                Notification.show("Error: Formato de ID incorrecto en la URL");
            }
        } else {
            // MODO NUEVO
            configurarNuevoRegistro();
        }
    }
	
	private void configurarNuevoRegistro() {
		this.archivoActual = new Archivo();
		this.archivoActual.setCodigo(service.obtenerProximoCodigo());
		binder.readBean(archivoActual);
		setTitulo(getTranslation("archivo.nuevo.titulo"));
		codigo.setReadOnly(true);
	}

	private void resetearInterfaz() {
		binder.getFields().forEach(f -> {
			if (f instanceof HasEnabled) {
				((HasEnabled) f).setEnabled(true);
			}
		});
		
		upload.setVisible(true);
		btnGuardar.setVisible(true);
		btnCancelar.setText(getTranslation("app.cancelar"));

		errorArchivo.setVisible(false);
		galeriaContainer.setVisible(false);
		estadoCargado.setVisible(false);
		estadoVacio.setVisible(true);
		upload.getStyle().set("z-index", "10");
		zonaVisual.getStyle().set("border-style", "dashed").set("border-color", "#00206033");
	}

	private void aplicarModoLectura() {
		setTitulo(getTranslation("archivo.visualizar.titulo"));
		
		binder.getFields().forEach(f -> {
			if (f instanceof HasEnabled) {
				((HasEnabled) f).setEnabled(false);
			}
		});
		
		upload.setVisible(false);
		btnGuardar.setVisible(false);
		btnCancelar.setText(getTranslation("app.volver"));

		if (archivoActual.getContenido() != null) {
			mostrarPrevisualizacion(archivoActual.getContenido());
		}
	}

	private void actualizarInterfazCargaExistente() {
		if (archivoActual.getNombreOriginal() != null) {
			nombreArchivoLabel.setText(archivoActual.getNombreOriginal());
			estadoVacio.setVisible(false);
			estadoCargado.setVisible(true);
			
			upload.getStyle().set("z-index", "0");
			zonaVisual.getStyle().set("border-style", "solid").set("border-color", "#002060");
		}
	}

	private void mostrarPrevisualizacion(byte[] datos) {
		String nombreFile = archivoActual.getNombreOriginal().toLowerCase();
		if (nombreFile.endsWith(".jpg") || nombreFile.endsWith(".png") || nombreFile.endsWith(".jpeg")) {
			galeriaContainer.removeAll();
			StreamResource res = new StreamResource("preview", () -> new ByteArrayInputStream(datos));
			Image img = new Image(res, "Preview");
			img.setWidth("100%");
			img.getStyle().set("border-radius", "8px").set("box-shadow", "0 2px 8px rgba(0,0,0,0.15)");
			
			galeriaContainer.add(new H3("Vista Previa del Documento"), img);
			galeriaContainer.setVisible(true);
		}
	}

	@Override
	protected void configurarCampos() {
		VerticalLayout colIzquierda = new VerticalLayout();
		colIzquierda.setWidth("40%");
		colIzquierda.setPadding(false);
		
		codigo.setLabel(getTranslation("archivo.codigo"));
		codigo.setWidthFull();

		nombre.setLabel(getTranslation("archivo.nombre"));
		nombre.setRequired(true);
		nombre.setWidthFull();
		// Forzar mensaje de error en rojo y sin negrita (sobrescribe estilo del tema)
		nombre.getElement().executeJs(
			"const s = document.createElement('style');" +
			"s.textContent = ':host([invalid]) [part=error-message] { color: #dc2626 !important; font-weight: 400 !important; }';" +
			"this.shadowRoot.appendChild(s);"
		);

		Span nota = new Span("NOTA IMPORTANTE: El archivo que debe cargar puede tener hasta 5 facturas como máximo.");
		nota.getStyle()
				.set("font-size", "0.8rem")
				.set("color", "#b45309")
				.set("background-color", "#fffbeb")
				.set("border", "1px solid #fcd34d")
				.set("border-radius", "6px")
				.set("padding", "8px 10px")
				.set("display", "block")
				.set("line-height", "1.4");

		colIzquierda.add(codigo, nombre, nota);

		VerticalLayout colDerecha = new VerticalLayout();
		colDerecha.setWidth("60%");
		colDerecha.setAlignItems(Alignment.CENTER);

		Span labelArchivo = new Span("Archivo adjunto");
		Span asterisco = new Span(" \u2022");
		asterisco.getStyle().set("color", "var(--lumo-primary-color)");
		HorizontalLayout headerArchivo = new HorizontalLayout(labelArchivo, asterisco);
		headerArchivo.setSpacing(false);

		errorArchivo.getStyle().set("color", "#dc2626").set("font-size", "0.75rem");
		errorArchivo.setVisible(false);

		colDerecha.add(headerArchivo, configurarZonaUpload(), errorArchivo, galeriaContainer);

		HorizontalLayout mainLayout = new HorizontalLayout(colIzquierda, colDerecha);
		mainLayout.setWidthFull();
		mainLayout.setSpacing(true);

		contenidoPrincipal.removeAll();
		contenidoPrincipal.add(mainLayout);
	}

	private VerticalLayout configurarZonaUpload() {
		zonaVisual.setWidth("500px");
		zonaVisual.setHeight("250px");
		zonaVisual.setAlignItems(Alignment.CENTER);
		zonaVisual.setJustifyContentMode(JustifyContentMode.CENTER);
		zonaVisual.getStyle().set("border", "2px dashed #00206033").set("border-radius", "12px").set("position", "relative");

		Icon fileIcon = VaadinIcon.FILE_TEXT.create();
		fileIcon.setSize("60px");
		fileIcon.setColor("#002060");
		
		Button btnVer = new Button("Descargar / Abrir", VaadinIcon.EYE.create(), e -> {
			if (archivoActual.getContenido() != null) {
				StreamResource res = new StreamResource(archivoActual.getNombreOriginal(), 
						() -> new ByteArrayInputStream(archivoActual.getContenido()));
				final StreamRegistration reg = VaadinSession.getCurrent().getResourceRegistry().registerResource(res);
				UI.getCurrent().getPage().open(reg.getResourceUri().toString(), "_blank");
			}
		});
		btnVer.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		btnVer.getStyle().set("z-index", "20");
		
		estadoCargado.add(fileIcon, nombreArchivoLabel, btnVer);
		estadoCargado.setAlignItems(Alignment.CENTER);
		estadoCargado.setVisible(false);

		Icon nube = VaadinIcon.CLOUD_UPLOAD_O.create();
		nube.setSize("60px");
		nube.setColor("#002060");
		estadoVacio.add(nube, new Span("Arrastre o haga click para subir"));
		estadoVacio.setAlignItems(Alignment.CENTER);

		upload.setAcceptedFileTypes("image/*", "application/pdf");
		upload.getStyle().set("position", "absolute").set("inset", "0").set("z-index", "10");
		
		upload.getElement().executeJs("const style = document.createElement('style');" 
				+ "style.textContent = ` :host { position: absolute; inset: 0; padding: 0; margin: 0; } "
				+ "vaadin-upload-file-list { display: none !important; } "
				+ "vaadin-upload::part(upload-button) { position: absolute; inset: 0; width: 100%; height: 100%; opacity: 0; cursor: pointer; } `;"
				+ "this.shadowRoot.appendChild(style);");

		upload.addSucceededListener(event -> {
			try {
				byte[] bytes = buffer.getInputStream().readAllBytes();
				archivoActual.setContenido(bytes);
				archivoActual.setNombreOriginal(event.getFileName());

				// Si el archivo tenía error y se reemplaza el contenido, volver a PENDIENTE
				if ("PROCESADO_ERROR".equals(archivoActual.getEstadoConversion())) {
					archivoActual.setEstadoConversion("PENDIENTE");
					archivoActual.setConvertido(false);
				}

				nombre.setValue(event.getFileName());
				nombreArchivoLabel.setText(event.getFileName());
				
				estadoVacio.setVisible(false);
				estadoCargado.setVisible(true);
				errorArchivo.setVisible(false);
				upload.getStyle().set("z-index", "0");
				zonaVisual.getStyle().set("border-style", "solid").set("border-color", "#002060");
				
				mostrarPrevisualizacion(bytes);
				upload.getElement().executeJs("this.files = [];");
			} catch (IOException e) {
				Notification.show("Error de lectura");
			}
		});

		zonaVisual.add(estadoVacio, estadoCargado, upload);
		return zonaVisual;
	}

	private void configurarBinder() {
		binder.bindInstanceFields(this);
	}

	@Override
	protected void accionGuardar() {
		if (archivoActual.getContenido() == null || archivoActual.getContenido().length == 0) {
			errorArchivo.setVisible(true);
			return;
		}
		errorArchivo.setVisible(false);

		if (binder.writeBeanIfValid(archivoActual)) {
			try {
				service.guardar(archivoActual);
				Notification.show(getTranslation("app.guardar.exito")).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
				getUI().ifPresent(ui -> ui.navigate(AbmArchivosView.class));
			} catch (Exception e) {
				Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
						.addThemeVariants(NotificationVariant.LUMO_ERROR);
			}
		}
	}

	@Override
	protected void accionCancelar() {
		getUI().ifPresent(ui -> ui.navigate(AbmArchivosView.class));
	}
}