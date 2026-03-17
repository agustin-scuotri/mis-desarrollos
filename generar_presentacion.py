from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
from pptx.util import Inches, Pt
import pptx.oxml.ns as nsmap
from lxml import etree

# ── Paleta de colores ────────────────────────────────────────────────────────
DARK_BG     = RGBColor(0x0F, 0x17, 0x2A)   # azul muy oscuro
CARD_BG     = RGBColor(0x1A, 0x27, 0x44)   # azul oscuro (cards)
ACCENT      = RGBColor(0x3B, 0x82, 0xF6)   # azul vibrante
ACCENT2     = RGBColor(0x10, 0xB9, 0x81)   # verde esmeralda
ACCENT3     = RGBColor(0xF5, 0x9E, 0x0B)   # ámbar
TEXT_WHITE  = RGBColor(0xFF, 0xFF, 0xFF)
TEXT_LIGHT  = RGBColor(0xCB, 0xD5, 0xE1)
TEXT_MUTED  = RGBColor(0x64, 0x74, 0x8B)

W = Inches(13.33)   # widescreen 16:9
H = Inches(7.5)


def new_prs():
    prs = Presentation()
    prs.slide_width  = W
    prs.slide_height = H
    return prs


def blank_slide(prs):
    blank_layout = prs.slide_layouts[6]   # completamente en blanco
    return prs.slides.add_slide(blank_layout)


# ── helpers de formas ────────────────────────────────────────────────────────
def add_rect(slide, x, y, w, h, fill_color, alpha=None):
    shape = slide.shapes.add_shape(1, x, y, w, h)   # MSO_SHAPE_TYPE.RECTANGLE
    shape.line.fill.background()                      # sin borde
    if fill_color is None:
        shape.fill.background()
    else:
        shape.fill.solid()
        shape.fill.fore_color.rgb = fill_color
    return shape


def add_textbox(slide, text, x, y, w, h,
                font_size=18, bold=False, italic=False,
                color=TEXT_WHITE, align=PP_ALIGN.LEFT,
                wrap=True, font_name="Calibri"):
    txBox = slide.shapes.add_textbox(x, y, w, h)
    tf = txBox.text_frame
    tf.word_wrap = wrap
    p = tf.paragraphs[0]
    p.alignment = align
    run = p.add_run()
    run.text = text
    run.font.size = Pt(font_size)
    run.font.bold = bold
    run.font.italic = italic
    run.font.color.rgb = color
    run.font.name = font_name
    return txBox


def add_bullet_list(slide, items, x, y, w, h,
                    font_size=16, color=TEXT_LIGHT,
                    bullet_color=ACCENT, font_name="Calibri"):
    txBox = slide.shapes.add_textbox(x, y, w, h)
    tf = txBox.text_frame
    tf.word_wrap = True
    first = True
    for item in items:
        if first:
            p = tf.paragraphs[0]
            first = False
        else:
            p = tf.add_paragraph()
        p.space_before = Pt(4)
        run = p.add_run()
        run.text = f"  •  {item}"
        run.font.size = Pt(font_size)
        run.font.color.rgb = color
        run.font.name = font_name
    return txBox


def add_divider(slide, y, color=ACCENT):
    line = slide.shapes.add_shape(1, Inches(0.6), y, Inches(12.1), Pt(2))
    line.fill.solid()
    line.fill.fore_color.rgb = color
    line.line.fill.background()


def bg(slide, color=DARK_BG):
    add_rect(slide, 0, 0, W, H, color)


def accent_bar(slide, w=Inches(0.12)):
    """Barra vertical de acento a la izquierda."""
    add_rect(slide, 0, 0, w, H, ACCENT)


# ── SLIDE 1 – Portada ────────────────────────────────────────────────────────
def slide_portada(prs):
    s = blank_slide(prs)
    bg(s)

    # Gradiente simulado – rectángulo derecho más claro
    add_rect(s, Inches(7), 0, Inches(6.33), H, CARD_BG)

    # Barra superior de acento
    add_rect(s, 0, 0, W, Inches(0.08), ACCENT)

    # Barra inferior
    add_rect(s, 0, H - Inches(0.08), W, Inches(0.08), ACCENT2)

    # Círculo decorativo (simulado con cuadrado redondeado grande)
    circ = s.shapes.add_shape(9, Inches(8.5), Inches(1.2), Inches(3.8), Inches(3.8))
    circ.fill.solid()
    circ.fill.fore_color.rgb = RGBColor(0x1E, 0x3A, 0x5F)
    circ.line.fill.background()

    # Ícono texto grande
    add_textbox(s, "🧾", Inches(9.5), Inches(1.6), Inches(2), Inches(2),
                font_size=72, align=PP_ALIGN.CENTER)

    # Tag tecnología
    tag = add_rect(s, Inches(0.6), Inches(1.4), Inches(3.4), Inches(0.38), ACCENT)
    add_textbox(s, "SPRING BOOT  •  VAADIN  •  CLAUDE AI",
                Inches(0.6), Inches(1.4), Inches(3.4), Inches(0.38),
                font_size=9, bold=True, color=TEXT_WHITE, align=PP_ALIGN.CENTER)

    # Título principal
    add_textbox(s, "Sistema de Conversión\nde Facturas con IA",
                Inches(0.6), Inches(1.9), Inches(6.2), Inches(2.0),
                font_size=40, bold=True, color=TEXT_WHITE, align=PP_ALIGN.LEFT)

    # Subtítulo
    add_textbox(s,
                "Extracción automática de datos de facturas\nmediante Inteligencia Artificial",
                Inches(0.6), Inches(4.0), Inches(6.2), Inches(1.0),
                font_size=17, color=TEXT_LIGHT, align=PP_ALIGN.LEFT)

    # Línea divisora
    add_divider(s, Inches(5.2), ACCENT2)

    # Autor / fecha
    add_textbox(s, "Agustín Scuotri   |   Marzo 2026",
                Inches(0.6), Inches(5.35), Inches(6), Inches(0.5),
                font_size=13, color=TEXT_MUTED, align=PP_ALIGN.LEFT)


# ── SLIDE 2 – El problema ────────────────────────────────────────────────────
def slide_problema(prs):
    s = blank_slide(prs)
    bg(s)
    accent_bar(s)

    add_textbox(s, "EL PROBLEMA", Inches(0.9), Inches(0.35), Inches(11), Inches(0.5),
                font_size=11, bold=True, color=ACCENT, align=PP_ALIGN.LEFT)
    add_textbox(s, "Procesar facturas manualmente es lento y propenso a errores",
                Inches(0.9), Inches(0.75), Inches(11.5), Inches(0.8),
                font_size=26, bold=True, color=TEXT_WHITE, align=PP_ALIGN.LEFT)
    add_divider(s, Inches(1.65), ACCENT)

    # Tres cards de problemas
    problems = [
        ("⏱", "Tiempo",    "Cargar datos de facturas a mano puede\ntomar minutos por documento."),
        ("❌", "Errores",   "La carga manual genera errores de tipeo,\nomisiones y datos inconsistentes."),
        ("📂", "Volumen",   "Empresas manejan decenas o cientos\nde facturas por mes."),
    ]
    for i, (icon, title, desc) in enumerate(problems):
        x = Inches(0.9 + i * 4.1)
        card = add_rect(s, x, Inches(2.1), Inches(3.8), Inches(3.6), CARD_BG)
        add_rect(s, x, Inches(2.1), Inches(3.8), Inches(0.06), ACCENT3)
        add_textbox(s, icon, x + Inches(0.2), Inches(2.3), Inches(1), Inches(0.8),
                    font_size=34, align=PP_ALIGN.LEFT)
        add_textbox(s, title, x + Inches(0.2), Inches(3.15), Inches(3.4), Inches(0.45),
                    font_size=18, bold=True, color=TEXT_WHITE)
        add_textbox(s, desc, x + Inches(0.2), Inches(3.6), Inches(3.4), Inches(1.6),
                    font_size=14, color=TEXT_LIGHT)


# ── SLIDE 3 – La solución ────────────────────────────────────────────────────
def slide_solucion(prs):
    s = blank_slide(prs)
    bg(s)
    accent_bar(s)

    add_textbox(s, "LA SOLUCIÓN", Inches(0.9), Inches(0.35), Inches(11), Inches(0.5),
                font_size=11, bold=True, color=ACCENT2, align=PP_ALIGN.LEFT)
    add_textbox(s, "Automatizar la extracción con Inteligencia Artificial",
                Inches(0.9), Inches(0.75), Inches(11.5), Inches(0.8),
                font_size=26, bold=True, color=TEXT_WHITE, align=PP_ALIGN.LEFT)
    add_divider(s, Inches(1.65), ACCENT2)

    # Flecha de flujo
    steps = [
        ("📄", "Subir\nfactura",   "PDF, PNG\no JPG"),
        ("🤖", "Claude AI\nprocesa", "Vision API\nde Anthropic"),
        ("📋", "JSON\nestructurado", "Datos listos\npara usar"),
        ("📊", "Dashboard\ny métricas", "Estado y\nresultados"),
    ]
    for i, (icon, title, sub) in enumerate(steps):
        x = Inches(0.75 + i * 3.1)
        add_rect(s, x, Inches(2.2), Inches(2.7), Inches(3.2), CARD_BG)
        add_rect(s, x, Inches(2.2), Inches(2.7), Inches(0.06), ACCENT2)
        add_textbox(s, icon, x + Inches(0.15), Inches(2.4), Inches(2.4), Inches(0.8),
                    font_size=32, align=PP_ALIGN.CENTER)
        add_textbox(s, title, x + Inches(0.15), Inches(3.25), Inches(2.4), Inches(0.7),
                    font_size=16, bold=True, color=TEXT_WHITE, align=PP_ALIGN.CENTER)
        add_textbox(s, sub, x + Inches(0.15), Inches(3.95), Inches(2.4), Inches(0.8),
                    font_size=12, color=TEXT_MUTED, align=PP_ALIGN.CENTER)
        if i < 3:
            add_textbox(s, "→", x + Inches(2.75), Inches(3.35), Inches(0.5), Inches(0.5),
                        font_size=22, bold=True, color=ACCENT2, align=PP_ALIGN.CENTER)

    # Highlight resultado
    add_rect(s, Inches(0.75), Inches(5.8), Inches(11.8), Inches(0.8), CARD_BG)
    add_rect(s, Inches(0.75), Inches(5.8), Inches(0.06), Inches(0.8), ACCENT2)
    add_textbox(s,
                "✅  De minutos de carga manual  →  segundos de procesamiento automático",
                Inches(1.0), Inches(5.85), Inches(11.5), Inches(0.6),
                font_size=15, bold=True, color=ACCENT2, align=PP_ALIGN.LEFT)


# ── SLIDE 4 – Stack tecnológico ──────────────────────────────────────────────
def slide_stack(prs):
    s = blank_slide(prs)
    bg(s)
    accent_bar(s)

    add_textbox(s, "TECNOLOGÍAS", Inches(0.9), Inches(0.35), Inches(11), Inches(0.5),
                font_size=11, bold=True, color=ACCENT, align=PP_ALIGN.LEFT)
    add_textbox(s, "Stack tecnológico del proyecto",
                Inches(0.9), Inches(0.75), Inches(11.5), Inches(0.8),
                font_size=26, bold=True, color=TEXT_WHITE, align=PP_ALIGN.LEFT)
    add_divider(s, Inches(1.65), ACCENT)

    techs = [
        (ACCENT,  "⚙️",  "Spring Boot 4",     "Backend / API REST\nJava 21 + Maven"),
        (ACCENT2, "🖥️",  "Vaadin 25",          "Frontend full-stack\nComponentes Java"),
        (ACCENT3, "🤖",  "Claude Vision API",  "IA de Anthropic\nExtracción de texto"),
        (RGBColor(0xA7,0x8B,0xFA), "🗄️", "PostgreSQL",    "Base de datos\nrelacional"),
    ]
    for i, (color, icon, title, desc) in enumerate(techs):
        x = Inches(0.75 + i * 3.1)
        add_rect(s, x, Inches(2.1), Inches(2.8), Inches(3.5), CARD_BG)
        add_rect(s, x, Inches(2.1), Inches(2.8), Inches(0.06), color)
        add_textbox(s, icon, x + Inches(0.2), Inches(2.3), Inches(2.4), Inches(0.7),
                    font_size=30, align=PP_ALIGN.CENTER)
        add_textbox(s, title, x + Inches(0.1), Inches(3.05), Inches(2.6), Inches(0.5),
                    font_size=15, bold=True, color=TEXT_WHITE, align=PP_ALIGN.CENTER)
        add_textbox(s, desc, x + Inches(0.1), Inches(3.55), Inches(2.6), Inches(1.0),
                    font_size=12, color=TEXT_MUTED, align=PP_ALIGN.CENTER)

    # Fila extra: complementos
    add_textbox(s, "También:", Inches(0.9), Inches(5.85), Inches(2), Inches(0.4),
                font_size=12, bold=True, color=TEXT_MUTED)
    extras = ["Spring Security (auth)", "Caffeine Cache", "Spring WebFlux", "JWT Sessions"]
    for i, e in enumerate(extras):
        x = Inches(1.9 + i * 2.8)
        add_rect(s, x, Inches(5.8), Inches(2.5), Inches(0.42), RGBColor(0x1E,0x3A,0x5F))
        add_textbox(s, e, x + Inches(0.1), Inches(5.82), Inches(2.3), Inches(0.38),
                    font_size=11, color=ACCENT, align=PP_ALIGN.CENTER)


# ── SLIDE 5 – Arquitectura ───────────────────────────────────────────────────
def slide_arquitectura(prs):
    s = blank_slide(prs)
    bg(s)
    accent_bar(s)

    add_textbox(s, "ARQUITECTURA", Inches(0.9), Inches(0.35), Inches(11), Inches(0.5),
                font_size=11, bold=True, color=ACCENT, align=PP_ALIGN.LEFT)
    add_textbox(s, "Arquitectura en capas del sistema",
                Inches(0.9), Inches(0.75), Inches(11.5), Inches(0.8),
                font_size=26, bold=True, color=TEXT_WHITE, align=PP_ALIGN.LEFT)
    add_divider(s, Inches(1.65), ACCENT)

    layers = [
        (ACCENT,  "CAPA DE PRESENTACIÓN",  "Vaadin Views: Login · Inicio · Conversor · Archivos · Usuarios"),
        (ACCENT2, "CAPA DE SERVICIOS",      "ArchivoService · DocumentoConvertidoService · ClaudeVisionService · UsuarioService"),
        (ACCENT3, "CAPA DE ACCESO A DATOS", "10 Repositorios JPA · 10 Entidades (Archivo, DocumentoConvertido, Productos…)"),
        (RGBColor(0xA7,0x8B,0xFA), "BASE DE DATOS",  "PostgreSQL · Hibernate DDL auto-update · Caffeine Cache (30s TTL, 500 items)"),
    ]
    for i, (color, title, desc) in enumerate(layers):
        y = Inches(1.9 + i * 1.28)
        add_rect(s, Inches(0.9), y, Inches(11.8), Inches(1.1), CARD_BG)
        add_rect(s, Inches(0.9), y, Inches(0.06), Inches(1.1), color)
        add_textbox(s, title, Inches(1.1), y + Inches(0.07), Inches(3.0), Inches(0.42),
                    font_size=10, bold=True, color=color)
        add_textbox(s, desc, Inches(1.1), y + Inches(0.48), Inches(11.4), Inches(0.55),
                    font_size=13, color=TEXT_LIGHT)
        if i < 3:
            add_textbox(s, "▼", Inches(6.4), y + Inches(1.12), Inches(0.5), Inches(0.3),
                        font_size=10, color=TEXT_MUTED, align=PP_ALIGN.CENTER)

    # Componente externo
    add_rect(s, Inches(9.5), Inches(1.85), Inches(3.2), Inches(0.5),
             RGBColor(0x1E, 0x3A, 0x5F))
    add_textbox(s, "☁️  Claude Vision API (Anthropic)",
                Inches(9.5), Inches(1.85), Inches(3.2), Inches(0.5),
                font_size=11, color=ACCENT2, align=PP_ALIGN.CENTER)
    add_textbox(s, "↕  HTTPS", Inches(10.5), Inches(2.35), Inches(1.2), Inches(0.3),
                font_size=9, color=TEXT_MUTED, align=PP_ALIGN.CENTER)


# ── SLIDE 6 – Funcionalidades ────────────────────────────────────────────────
def slide_funcionalidades(prs):
    s = blank_slide(prs)
    bg(s)
    accent_bar(s)

    add_textbox(s, "FUNCIONALIDADES", Inches(0.9), Inches(0.35), Inches(11), Inches(0.5),
                font_size=11, bold=True, color=ACCENT, align=PP_ALIGN.LEFT)
    add_textbox(s, "¿Qué puede hacer el sistema?",
                Inches(0.9), Inches(0.75), Inches(11.5), Inches(0.8),
                font_size=26, bold=True, color=TEXT_WHITE, align=PP_ALIGN.LEFT)
    add_divider(s, Inches(1.65), ACCENT)

    features = [
        ("📤", "Gestión de Archivos",
         ["Subir PDFs, PNG y JPG (hasta 10 MB)",
          "Detección de duplicados por hash",
          "Estado de conversión: pendiente / procesado / error"]),
        ("🤖", "Conversor IA",
         ["Envía el archivo a Claude Vision API",
          "Extrae: emisor, número, fecha, CAE, ítems, impuestos",
          "Manejo de errores y rate-limiting"]),
        ("📊", "Dashboard",
         ["Métricas en tiempo real: total, procesados, pendientes",
          "Gráficos de distribución por estado",
          "Filtro por rango de fechas"]),
        ("👥", "Usuarios (Admin)",
         ["Crear / editar / eliminar usuarios",
          "Roles: USER y ADMIN",
          "Activar / desactivar cuentas"]),
    ]
    for i, (icon, title, bullets) in enumerate(features):
        col = i % 2
        row = i // 2
        x = Inches(0.75 + col * 6.3)
        y = Inches(2.0 + row * 2.55)
        add_rect(s, x, y, Inches(5.9), Inches(2.35), CARD_BG)
        add_rect(s, x, y, Inches(0.06), Inches(2.35), ACCENT)
        add_textbox(s, icon + "  " + title, x + Inches(0.2), y + Inches(0.12),
                    Inches(5.5), Inches(0.45),
                    font_size=15, bold=True, color=TEXT_WHITE)
        add_bullet_list(s, bullets,
                        x + Inches(0.2), y + Inches(0.55),
                        Inches(5.5), Inches(1.6),
                        font_size=12)


# ── SLIDE 7 – Flujo de conversión ────────────────────────────────────────────
def slide_flujo(prs):
    s = blank_slide(prs)
    bg(s)
    accent_bar(s)

    add_textbox(s, "FLUJO DE TRABAJO", Inches(0.9), Inches(0.35), Inches(11), Inches(0.5),
                font_size=11, bold=True, color=ACCENT2, align=PP_ALIGN.LEFT)
    add_textbox(s, "Cómo se convierte una factura paso a paso",
                Inches(0.9), Inches(0.75), Inches(11.5), Inches(0.8),
                font_size=26, bold=True, color=TEXT_WHITE, align=PP_ALIGN.LEFT)
    add_divider(s, Inches(1.65), ACCENT2)

    steps = [
        ("1", ACCENT,  "Usuario sube\nel archivo",      "PDF / PNG / JPG\nhasta 10 MB"),
        ("2", ACCENT2, "Sistema valida\ny almacena",    "Hash anti-duplicado\nPostgreSQL"),
        ("3", ACCENT3, "Conversor envía\na Claude AI",  "Base64 + prompt\nestructurado"),
        ("4", ACCENT,  "Claude devuelve\nel JSON",       "Emisor, ítems,\nimpuestos, CAE…"),
        ("5", ACCENT2, "Sistema persiste\nlos datos",   "10 entidades JPA\nrelacionadas"),
    ]

    for i, (num, color, title, desc) in enumerate(steps):
        x = Inches(0.7 + i * 2.45)
        # línea conectora
        if i < 4:
            add_rect(s, x + Inches(1.65), Inches(3.15), Inches(0.8), Inches(0.06), TEXT_MUTED)

        # círculo numerado (simulado)
        circ = s.shapes.add_shape(9, x + Inches(0.35), Inches(2.2), Inches(1.2), Inches(1.2))
        circ.fill.solid(); circ.fill.fore_color.rgb = color
        circ.line.fill.background()
        add_textbox(s, num, x + Inches(0.35), Inches(2.3), Inches(1.2), Inches(0.8),
                    font_size=26, bold=True, color=DARK_BG, align=PP_ALIGN.CENTER)

        add_textbox(s, title, x, Inches(3.55), Inches(2.0), Inches(0.8),
                    font_size=13, bold=True, color=TEXT_WHITE, align=PP_ALIGN.CENTER)
        add_textbox(s, desc, x, Inches(4.35), Inches(2.0), Inches(0.8),
                    font_size=11, color=TEXT_MUTED, align=PP_ALIGN.CENTER)

    # Resultado final
    add_rect(s, Inches(0.75), Inches(5.5), Inches(11.8), Inches(1.15), CARD_BG)
    add_rect(s, Inches(0.75), Inches(5.5), Inches(11.8), Inches(0.05), ACCENT2)
    add_textbox(s,
                "Resultado: factura disponible en el listado con todos sus datos estructurados, "
                "lista para consultar, exportar o integrar con otros sistemas.",
                Inches(1.0), Inches(5.6), Inches(11.3), Inches(0.9),
                font_size=14, color=TEXT_LIGHT, align=PP_ALIGN.LEFT)


# ── SLIDE 8 – Datos extraídos ────────────────────────────────────────────────
def slide_datos(prs):
    s = blank_slide(prs)
    bg(s)
    accent_bar(s)

    add_textbox(s, "DATOS EXTRAÍDOS", Inches(0.9), Inches(0.35), Inches(11), Inches(0.5),
                font_size=11, bold=True, color=ACCENT, align=PP_ALIGN.LEFT)
    add_textbox(s, "Estructura de datos que genera el sistema",
                Inches(0.9), Inches(0.75), Inches(11.5), Inches(0.8),
                font_size=26, bold=True, color=TEXT_WHITE, align=PP_ALIGN.LEFT)
    add_divider(s, Inches(1.65), ACCENT)

    groups = [
        ("📋 Encabezado",     ["Número de factura", "Tipo (A/B/C)", "Fecha emisión", "CAE y vencimiento CAE"]),
        ("🏢 Emisor",         ["CUIT / razón social", "Dirección fiscal", "Teléfono / email", "Condición IVA"]),
        ("🛒 Ítems",          ["Código y descripción", "Cantidad y unidad", "Precio unitario", "Subtotales"]),
        ("💰 Impuestos",      ["IVA por alícuota", "Percepciones IIBB", "Percepciones IVA", "Descuentos/recargos"]),
        ("📆 Vencimientos",   ["Fecha de pago", "Monto por cuota", "Moneda", "Condición de pago"]),
        ("🔢 Totales",        ["Neto gravado", "IVA total", "Percepciones totales", "Total final factura"]),
    ]

    for i, (title, items) in enumerate(groups):
        col = i % 3
        row = i // 3
        x = Inches(0.75 + col * 4.2)
        y = Inches(2.0 + row * 2.55)
        add_rect(s, x, y, Inches(3.9), Inches(2.35), CARD_BG)
        add_textbox(s, title, x + Inches(0.15), y + Inches(0.1), Inches(3.6), Inches(0.4),
                    font_size=13, bold=True, color=ACCENT)
        add_bullet_list(s, items, x + Inches(0.1), y + Inches(0.5), Inches(3.7), Inches(1.75),
                        font_size=11)


# ── SLIDE 9 – Interfaz de usuario ────────────────────────────────────────────
def slide_ui(prs):
    s = blank_slide(prs)
    bg(s)
    accent_bar(s)

    add_textbox(s, "INTERFAZ DE USUARIO", Inches(0.9), Inches(0.35), Inches(11), Inches(0.5),
                font_size=11, bold=True, color=ACCENT, align=PP_ALIGN.LEFT)
    add_textbox(s, "Pantallas principales de la aplicación",
                Inches(0.9), Inches(0.75), Inches(11.5), Inches(0.8),
                font_size=26, bold=True, color=TEXT_WHITE, align=PP_ALIGN.LEFT)
    add_divider(s, Inches(1.65), ACCENT)

    screens = [
        ("🔐 Login",          "Autenticación con usuario y contraseña.\nProtegido con Spring Security."),
        ("📊 Dashboard",      "Métricas en tiempo real: total, procesados,\npendientes y errores. Gráficos de barras."),
        ("📤 Archivos",       "Subida y gestión de documentos. Estado\nde conversión visible en la grilla."),
        ("🤖 Conversor",      "Selección de archivo → envío a Claude AI\n→ visualización del JSON extraído."),
        ("📋 Lista de JSONs", "Detalle completo de cada factura con todos\nsus campos y relaciones."),
        ("👥 Usuarios",       "ABM de usuarios (solo ADMIN). Asignación\nde roles y activación de cuentas."),
    ]

    for i, (title, desc) in enumerate(screens):
        col = i % 3
        row = i // 3
        x = Inches(0.75 + col * 4.2)
        y = Inches(2.0 + row * 2.6)
        add_rect(s, x, y, Inches(3.9), Inches(2.4), CARD_BG)
        add_rect(s, x, y, Inches(3.9), Inches(0.06), ACCENT)

        # Simulación de ventana
        add_rect(s, x + Inches(0.15), y + Inches(0.2), Inches(3.6), Inches(1.1),
                 RGBColor(0x0F, 0x17, 0x2A))
        add_textbox(s, title,
                    x + Inches(0.15), y + Inches(0.35), Inches(3.6), Inches(0.5),
                    font_size=14, bold=True, color=ACCENT, align=PP_ALIGN.CENTER)

        add_textbox(s, desc,
                    x + Inches(0.1), y + Inches(1.4), Inches(3.7), Inches(0.9),
                    font_size=11, color=TEXT_LIGHT)


# ── SLIDE 10 – Decisiones técnicas ───────────────────────────────────────────
def slide_decisiones(prs):
    s = blank_slide(prs)
    bg(s)
    accent_bar(s)

    add_textbox(s, "DECISIONES TÉCNICAS", Inches(0.9), Inches(0.35), Inches(11), Inches(0.5),
                font_size=11, bold=True, color=ACCENT3, align=PP_ALIGN.LEFT)
    add_textbox(s, "Por qué elegí cada tecnología",
                Inches(0.9), Inches(0.75), Inches(11.5), Inches(0.8),
                font_size=26, bold=True, color=TEXT_WHITE, align=PP_ALIGN.LEFT)
    add_divider(s, Inches(1.65), ACCENT3)

    decisions = [
        ("Vaadin 25",
         "Frontend en Java puro, sin necesidad de mantener un proyecto separado "
         "React/Angular. Todo el código en el mismo repositorio."),
        ("Claude Vision API",
         "Capacidad multimodal para interpretar tanto texto como imágenes escaneadas. "
         "Permite un prompt estructurado que garantiza salida JSON consistente."),
        ("Caffeine Cache",
         "Reduce las consultas repetidas a la base de datos en operaciones de lectura "
         "frecuentes (listados, conteos). TTL de 30 segundos."),
        ("Hash de contenido",
         "Detección de duplicados al momento de la subida sin comparar byte a byte en "
         "la DB. Evita procesamiento doble de la misma factura."),
        ("Roles USER / ADMIN",
         "Separación de responsabilidades: solo el admin gestiona usuarios. "
         "Escalable a más roles en el futuro."),
    ]

    for i, (title, desc) in enumerate(decisions):
        y = Inches(2.0 + i * 1.02)
        add_rect(s, Inches(0.9), y, Inches(11.8), Inches(0.92), CARD_BG)
        add_rect(s, Inches(0.9), y, Inches(0.06), Inches(0.92), ACCENT3)
        add_textbox(s, title, Inches(1.1), y + Inches(0.06), Inches(2.5), Inches(0.35),
                    font_size=12, bold=True, color=ACCENT3)
        add_textbox(s, desc,  Inches(3.7), y + Inches(0.06), Inches(8.8), Inches(0.78),
                    font_size=12, color=TEXT_LIGHT)


# ── SLIDE 11 – Próximos pasos ────────────────────────────────────────────────
def slide_proximos(prs):
    s = blank_slide(prs)
    bg(s)
    accent_bar(s)

    add_textbox(s, "PRÓXIMOS PASOS", Inches(0.9), Inches(0.35), Inches(11), Inches(0.5),
                font_size=11, bold=True, color=ACCENT2, align=PP_ALIGN.LEFT)
    add_textbox(s, "Mejoras y funcionalidades planificadas",
                Inches(0.9), Inches(0.75), Inches(11.5), Inches(0.8),
                font_size=26, bold=True, color=TEXT_WHITE, align=PP_ALIGN.LEFT)
    add_divider(s, Inches(1.65), ACCENT2)

    items_left = [
        "Exportar facturas a Excel / CSV",
        "API REST para integración con otros sistemas",
        "Procesamiento por lote (múltiples archivos)",
        "Notificaciones por email al completar conversión",
    ]
    items_right = [
        "Soporte multi-empresa / multi-tenant",
        "Auditoría de cambios (log de acciones)",
        "Mejoras en el parsing de facturas complejas",
        "Despliegue en la nube (Docker + CI/CD)",
    ]

    add_textbox(s, "Funcionales", Inches(0.9), Inches(2.0), Inches(5.5), Inches(0.4),
                font_size=13, bold=True, color=ACCENT2)
    add_bullet_list(s, items_left, Inches(0.9), Inches(2.45), Inches(5.5), Inches(3.0),
                    font_size=14)

    add_textbox(s, "Técnicas / Infraestructura", Inches(6.8), Inches(2.0), Inches(5.9), Inches(0.4),
                font_size=13, bold=True, color=ACCENT2)
    add_bullet_list(s, items_right, Inches(6.8), Inches(2.45), Inches(5.9), Inches(3.0),
                    font_size=14)

    add_divider(s, Inches(5.7), RGBColor(0x1E,0x3A,0x5F))
    add_textbox(s,
                "El sistema ya funciona end-to-end. "
                "El foco de los próximos sprints es robustez, integración y escalabilidad.",
                Inches(0.9), Inches(5.85), Inches(11.5), Inches(0.75),
                font_size=14, color=TEXT_MUTED, align=PP_ALIGN.LEFT, italic=True)


# ── SLIDE 12 – Cierre ────────────────────────────────────────────────────────
def slide_cierre(prs):
    s = blank_slide(prs)
    bg(s)

    add_rect(s, 0, 0, W, Inches(0.08), ACCENT)
    add_rect(s, 0, H - Inches(0.08), W, Inches(0.08), ACCENT2)

    # Mitad derecha decorativa
    add_rect(s, Inches(7), 0, Inches(6.33), H, CARD_BG)
    circ = s.shapes.add_shape(9, Inches(8.8), Inches(1.5), Inches(3.2), Inches(3.2))
    circ.fill.solid(); circ.fill.fore_color.rgb = RGBColor(0x1E,0x3A,0x5F)
    circ.line.fill.background()
    add_textbox(s, "🙏", Inches(9.6), Inches(1.9), Inches(1.8), Inches(1.8),
                font_size=64, align=PP_ALIGN.CENTER)

    add_textbox(s, "GRACIAS", Inches(0.9), Inches(1.5), Inches(5.5), Inches(1.0),
                font_size=52, bold=True, color=TEXT_WHITE, align=PP_ALIGN.LEFT)

    add_divider(s, Inches(2.7), ACCENT)

    add_textbox(s, "Sistema de Conversión de Facturas con IA",
                Inches(0.9), Inches(2.9), Inches(5.8), Inches(0.6),
                font_size=18, color=TEXT_LIGHT, align=PP_ALIGN.LEFT)

    add_textbox(s,
                "Spring Boot 4  ·  Vaadin 25  ·  Claude Vision API  ·  PostgreSQL",
                Inches(0.9), Inches(3.55), Inches(5.8), Inches(0.45),
                font_size=12, color=TEXT_MUTED, align=PP_ALIGN.LEFT)

    add_divider(s, Inches(4.3), RGBColor(0x1E,0x3A,0x5F))

    add_textbox(s, "Agustín Scuotri   |   Marzo 2026",
                Inches(0.9), Inches(4.5), Inches(5.8), Inches(0.5),
                font_size=14, color=TEXT_MUTED, align=PP_ALIGN.LEFT)

    add_textbox(s, "¿Preguntas?",
                Inches(0.9), Inches(5.4), Inches(5.8), Inches(0.7),
                font_size=22, bold=True, color=ACCENT, align=PP_ALIGN.LEFT)


# ── main ─────────────────────────────────────────────────────────────────────
def main():
    prs = new_prs()
    slide_portada(prs)
    slide_problema(prs)
    slide_solucion(prs)
    slide_stack(prs)
    slide_arquitectura(prs)
    slide_funcionalidades(prs)
    slide_flujo(prs)
    slide_datos(prs)
    slide_ui(prs)
    slide_decisiones(prs)
    slide_proximos(prs)
    slide_cierre(prs)

    out = "/home/user/mis-desarrollos/presentacion_proyecto.pptx"
    prs.save(out)
    print(f"✅  Presentación guardada en: {out}")
    print(f"    Slides generadas: {len(prs.slides)}")


if __name__ == "__main__":
    main()
