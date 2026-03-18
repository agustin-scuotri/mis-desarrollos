package com.desarrollos.views;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import com.desarrollos.base.FormView;
import com.desarrollos.base.Toast;
import com.desarrollos.entities.Archivo;
import com.desarrollos.services.ArchivoService;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.WildcardParameter;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
@PageTitle("Detalle de Archivo")
@Route(value = "archivo-detalle", layout = MainLayout.class)
public class ArchivoView extends FormView implements HasUrlParameter<String>, BeforeLeaveObserver {

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
	private boolean dirty = false;
	private Span breadcrumbDerecha = new Span("Nuevo");

	@Autowired
	public ArchivoView(ArchivoService service) {
		this.service = service;
		setTitulo(getTranslation("archivo.nuevo.titulo"));
		agregarBreadcrumb();
		configurarCampos();
		configurarBinder();

		// Ctrl+S para guardar
		Shortcuts.addShortcutListener(this, this::accionGuardar, Key.KEY_S, KeyModifier.CONTROL);
	}

	private void agregarBreadcrumb() {
		RouterLink linkInicio = new RouterLink("Inicio", InicioView.class);
		RouterLink linkArchivos = new RouterLink("Archivos", AbmArchivosView.class);

		Span sep1 = new Span(" › ");
		Span sep2 = new Span(" › ");
		sep1.getStyle().set("color", "var(--lumo-tertiary-text-color, #94a3b8)").set("margin", "0 2px");
		sep2.getStyle().set("color", "var(--lumo-tertiary-text-color, #94a3b8)").set("margin", "0 2px");

		String linkStyle = "color: var(--lumo-secondary-text-color, #64748b); font-size: 0.8rem; text-decoration: none;";
		linkInicio.getStyle().set("color", "var(--lumo-secondary-text-color, #64748b)").set("font-size", "0.8rem").set("text-decoration", "none");
		linkArchivos.getStyle().set("color", "var(--lumo-secondary-text-color, #64748b)").set("font-size", "0.8rem").set("text-decoration", "none");

		breadcrumbDerecha.getStyle()
				.set("font-size", "0.8rem")
				.set("font-weight", "600")
				.set("color", "var(--lumo-header-text-color, #1e293b)");

		HorizontalLayout breadcrumb = new HorizontalLayout(linkInicio, sep1, linkArchivos, sep2, breadcrumbDerecha);
		breadcrumb.setSpacing(false);
		breadcrumb.setAlignItems(FlexComponent.Alignment.CENTER);
		breadcrumb.getStyle()
				.set("padding", "4px 0 0 0")
				.set("margin-bottom", "-8px");

		addComponentAtIndex(0, breadcrumb);
	}

	@Override
    public void setParameter(BeforeEvent event, @WildcardParameter String parameter) {
        dirty = false;
        resetearInterfaz();

        if (parameter != null && !parameter.isEmpty()) {
            String path = parameter.startsWith("/") ? parameter.substring(1) : parameter;
            String[] partes = path.split("/");

            try {
                Long id = Long.parseLong(partes[0]);
                boolean esLectura = partes.length > 1 && partes[1].equalsIgnoreCase("read");

                this.archivoActual = service.buscarPorId(id);
                if (archivoActual != null && archivoActual.getId() != null) {
                    binder.readBean(archivoActual);
                    actualizarInterfazCargaExistente();

                    if (esLectura || "PROCESADO".equals(archivoActual.getEstadoConversion())) {
                        aplicarModoLectura();
                        breadcrumbDerecha.setText("Viendo: " + archivoActual.getCodigo());
                    } else {
                        setTitulo(getTranslation("archivo.editar.titulo"));
                        breadcrumbDerecha.setText("Editando: " + archivoActual.getCodigo());
                    }
                }
            } catch (NumberFormatException e) {
                Toast.error("Error: Formato de ID incorrecto en la URL");
            }
        } else {
            configurarNuevoRegistro();
            breadcrumbDerecha.setText("Nuevo");
        }
    }

	@Override
	public void beforeLeave(BeforeLeaveEvent event) {
		if (dirty) {
			BeforeLeaveEvent.ContinueNavigationAction action = event.postpone();
			ConfirmDialog dialog = new ConfirmDialog();
			dialog.setHeader("¿Salir sin guardar?");
			dialog.setText("Tenés cambios sin guardar. ¿Querés salir igual?");
			dialog.setCancelable(true);
			dialog.setCancelText("Quedarme aquí");
			dialog.setConfirmText("Salir sin guardar");
			dialog.setConfirmButtonTheme("error primary");
			dialog.addConfirmListener(e -> {
				dirty = false;
				action.proceed();
			});
			dialog.open();
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
			nombreArchivoLabel.getElement().setAttribute("title", archivoActual.getNombreOriginal());
			estadoVacio.setVisible(false);
			estadoCargado.setVisible(true);
			
			upload.getStyle().set("z-index", "0");
			zonaVisual.getStyle().set("border-style", "solid").set("border-color", "#002060");
		}
	}

	private void mostrarPrevisualizacion(byte[] datos) {
		String nombreFile = archivoActual.getNombreOriginal().toLowerCase();
		galeriaContainer.removeAll();

		if (nombreFile.endsWith(".jpg") || nombreFile.endsWith(".png") || nombreFile.endsWith(".jpeg")) {
			StreamResource res = new StreamResource("preview", () -> new ByteArrayInputStream(datos));
			Image img = new Image(res, "Preview");
			img.setWidth("100%");
			img.getStyle()
					.set("border-radius", "8px")
					.set("box-shadow", "0 2px 8px rgba(0,0,0,0.15)")
					.set("cursor", "zoom-in")
					.set("transition", "opacity 0.15s ease");
			img.getElement().addEventListener("mouseenter",
					e -> img.getStyle().set("opacity", "0.88"));
			img.getElement().addEventListener("mouseleave",
					e -> img.getStyle().set("opacity", "1"));
			img.addClickListener(e -> abrirLightbox(datos));

			Span hint = new Span("Click para ampliar");
			hint.getStyle()
					.set("font-size", "0.75rem").set("color", "var(--lumo-tertiary-text-color, #94a3b8)")
					.set("margin-top", "4px").set("display", "block");

			galeriaContainer.add(new H3("Vista Previa del Documento"), img, hint);
			galeriaContainer.setVisible(true);

		} else if (nombreFile.endsWith(".pdf")) {
			StreamResource res = new StreamResource(archivoActual.getNombreOriginal(),
					() -> new ByteArrayInputStream(datos));
			final StreamRegistration reg = VaadinSession.getCurrent().getResourceRegistry().registerResource(res);
			String pdfUrl = reg.getResourceUri().toString();

			com.vaadin.flow.dom.Element iframe = new com.vaadin.flow.dom.Element("iframe");
			iframe.setAttribute("src", pdfUrl);
			iframe.getStyle()
					.set("width", "100%")
					.set("height", "500px")
					.set("border", "none")
					.set("border-radius", "8px");
			Div pdfEmbed = new Div();
			pdfEmbed.setWidthFull();
			pdfEmbed.getElement().appendChild(iframe);

			galeriaContainer.add(new H3("Vista Previa del PDF"), pdfEmbed);
			galeriaContainer.setVisible(true);
		}
	}

	private void abrirLightbox(byte[] datos) {
		Dialog lightbox = new Dialog();
		lightbox.setModal(true);
		lightbox.setWidth("90vw");
		lightbox.setHeight("90vh");
		lightbox.getElement().setAttribute("aria-label", "Vista ampliada");

		StreamResource resLB = new StreamResource("preview-full", () -> new ByteArrayInputStream(datos));
		Image imgLB = new Image(resLB, "Vista completa");
		imgLB.getStyle()
				.set("max-width", "100%")
				.set("max-height", "calc(90vh - 80px)")
				.set("object-fit", "contain")
				.set("display", "block")
				.set("margin", "auto")
				.set("border-radius", "8px");

		Button btnCerrar = new Button("Cerrar", VaadinIcon.CLOSE.create(), e -> lightbox.close());
		btnCerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

		lightbox.add(imgLB);
		lightbox.getFooter().add(btnCerrar);
		lightbox.open();
	}

	@Override
	protected void configurarCampos() {
		VerticalLayout colIzquierda = new VerticalLayout();
		colIzquierda.setWidth("40%");
		colIzquierda.setPadding(false);
		
		codigo.setLabel(getTranslation("archivo.codigo"));
		codigo.setWidthFull();
		codigo.setTooltipText("Código autogenerado por el sistema. No editable.");

		nombre.setLabel(getTranslation("archivo.nombre"));
		nombre.setRequired(true);
		nombre.setWidthFull();
		nombre.setTooltipText("Nombre descriptivo del archivo o factura. Puede editarse.");
		nombre.addValueChangeListener(e -> { if (e.isFromClient()) dirty = true; });
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
		
		nombreArchivoLabel.getStyle()
				.set("max-width", "420px")
				.set("overflow", "hidden")
				.set("text-overflow", "ellipsis")
				.set("white-space", "nowrap")
				.set("display", "block");

		estadoCargado.add(fileIcon, nombreArchivoLabel, btnVer);
		estadoCargado.setAlignItems(Alignment.CENTER);
		estadoCargado.setVisible(false);

		Icon nube = VaadinIcon.CLOUD_UPLOAD_O.create();
		nube.setSize("60px");
		nube.setColor("#002060");
		estadoVacio.add(nube, new Span("Arrastre o haga click para subir"));
		estadoVacio.setAlignItems(Alignment.CENTER);

		upload.setAcceptedFileTypes("image/*", "application/pdf");
		upload.setMaxFileSize(20 * 1024 * 1024); // 20 MB
		upload.addFileRejectedListener(event -> {
			long limiteMB = 20;
			errorArchivo.setText("El archivo supera el límite de " + limiteMB + " MB permitido.");
			errorArchivo.setVisible(true);
		});
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
				dirty = true;

				// Si el archivo tenía error y se reemplaza el contenido, volver a PENDIENTE
				if ("PROCESADO_ERROR".equals(archivoActual.getEstadoConversion())) {
					archivoActual.setEstadoConversion("PENDIENTE");
					archivoActual.setConvertido(false);
				}

				nombre.setValue(event.getFileName());
				nombreArchivoLabel.setText(event.getFileName());
				nombreArchivoLabel.getElement().setAttribute("title", event.getFileName());
				
				estadoVacio.setVisible(false);
				estadoCargado.setVisible(true);
				errorArchivo.setVisible(false);
				upload.getStyle().set("z-index", "0");
				zonaVisual.getStyle().set("border-style", "solid").set("border-color", "#002060");
				
				mostrarPrevisualizacion(bytes);
				upload.getElement().executeJs("this.files = [];");
			} catch (IOException e) {
				Toast.error("Error de lectura del archivo");
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
				dirty = false;

				// Notificación con botón de acción
				Notification notif = new Notification();
				notif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
				notif.setDuration(5000);
				notif.setPosition(Notification.Position.BOTTOM_START);

				Span msg = new Span(getTranslation("app.guardar.exito") + " · ");
				msg.getStyle().set("font-weight", "500");

				Button btnVerLista = new Button("Ver en lista →", e -> {
					notif.close();
					getUI().ifPresent(ui -> ui.navigate(AbmArchivosView.class));
				});
				btnVerLista.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
				btnVerLista.getStyle().set("color", "white").set("text-decoration", "underline");

				HorizontalLayout notifContent = new HorizontalLayout(msg, btnVerLista);
				notifContent.setAlignItems(FlexComponent.Alignment.CENTER);
				notifContent.setSpacing(false);
				notif.add(notifContent);
				notif.open();

				getUI().ifPresent(ui -> ui.navigate(AbmArchivosView.class));
			} catch (Exception e) {
				Toast.error("Error: " + e.getMessage());
			}
		}
	}

	@Override
	protected void accionCancelar() {
		getUI().ifPresent(ui -> ui.navigate(AbmArchivosView.class));
	}
}