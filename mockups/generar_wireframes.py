#!/usr/bin/env python3
"""Genera los wireframes de Belmonte SpA en HTML y SVG desde una sola descripción.

Uso, desde la raíz del repositorio:

    python3 mockups/generar_wireframes.py

Escribe mockups/wireframes-belmonte.html y mockups/wireframes-belmonte.svg (el que muestra el
README). Para cambiar una pantalla se edita GRUPOS, más abajo, y se vuelve a ejecutar: los dos
archivos generados no se editan a mano. Solo usa la librería estándar.

Las pantallas siguen la especificación del README ("Navegación y pantallas", "Máquinas de
estado"). Todos los datos son ficticios (RNF09).
"""
from html import escape
from pathlib import Path

DIR = Path(__file__).resolve().parent
AVISOS = []  # textos que no caben en el SVG; se informan al final

# ----------------------------------------------------------------------------------- paleta
CLARO = {
    'paper': '#f3f2ee', 'ink': '#211f1c', 'ink_soft': '#696864', 'line_strong': '#4a5560',
    'line_soft': '#d8d6d0', 'surface': '#ffffff', 'surface_tint': '#eceae3', 'accent': '#2f6f5e',
    'accent_soft': '#d9ebe6', 'on_accent': '#ffffff', 'info': '#37689c', 'info_soft': '#dde8f2',
    'danger': '#a3402f', 'danger_soft': '#f5e1dc', 'on_danger': '#ffffff', 'warn': '#8a6514',
    'warn_soft': '#f6ecd2',
}
OSCURO = {
    'paper': '#15171a', 'ink': '#eef0ee', 'ink_soft': '#9ea3a1', 'line_strong': '#7c8790',
    'line_soft': '#2c2f33', 'surface': '#1d2023', 'surface_tint': '#262a2d', 'accent': '#4fae95',
    'accent_soft': '#1f3630', 'on_accent': '#08201b', 'info': '#7fb0e0', 'info_soft': '#1f2c37',
    'danger': '#e08a78', 'danger_soft': '#3a221d', 'on_danger': '#2a0f0a', 'warn': '#d9b45a',
    'warn_soft': '#352c17',
}
C = CLARO  # el SVG usa la paleta clara

# Estado → (etiqueta, estilo). Los estilos son los mismos en HTML (clase chip-<estilo>) y SVG.
ESTADOS = {
    'PENDIENTE': ('Pendiente', 'neutro'), 'ASIGNADO': ('Asignado', 'info'),
    'EN_CURSO': ('En curso', 'acento'), 'FINALIZADO': ('Finalizado', 'tenue'),
    'CANCELADO': ('Cancelado', 'peligro'),
    'PAGO_PENDIENTE': ('Pendiente', 'neutro'), 'PAGADO': ('Pagado', 'ok'),
    'RECHAZADO': ('Rechazado', 'peligro'), 'ANULADO': ('Anulado', 'peligro_borde'),
    'REEMBOLSADO': ('Reembolsado', 'aviso'),
    'DISPONIBLE': ('Disponible', 'ok'), 'OCUPADO': ('Ocupado', 'acento'),
    'EN_USO': ('En uso', 'acento'),
    'SYNC': ('Pendiente de sincronizar', 'aviso'),
    'ATRASADO': ('Atrasado', 'aviso'), 'VENCIDO': ('Vencido', 'peligro_borde'),
    'INACTIVO': ('Inactivo', 'tenue'), 'SOL_PENDIENTE': ('Por aprobar', 'aviso'),
}
ESTILOS = {  # estilo → (texto, borde, fondo)
    'neutro': ('ink_soft', 'line_strong', None), 'info': ('info', 'info', 'info_soft'),
    'acento': ('on_accent', 'accent', 'accent'), 'tenue': ('ink_soft', 'line_soft', 'surface_tint'),
    'peligro': ('danger', 'danger', 'danger_soft'), 'peligro_borde': ('danger', 'danger', None),
    'ok': ('accent', 'accent', 'accent_soft'), 'aviso': ('warn', 'warn', 'warn_soft'),
}

# ----------------------------------------------------------------------------------- iconos
ICONOS = {  # trazos en una grilla de 24 × 24
    'atras': 'M15 18l-6-6 6-6',
    'pin': 'M12 21s-6-5.7-6-10a6 6 0 1 1 12 0c0 4.3-6 10-6 10Z M14 11a2 2 0 1 1-4 0a2 2 0 1 1 4 0',
    'select': 'M6 9l6 6 6-6',
    'fecha': 'M4 6h16v14H4z M4 10h16 M8 3v4 M16 3v4',
    'hora': 'M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18 M12 7v5l3 2',
    'brujula': 'M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18 M15 9l-4 2-2 4 4-2 2-4Z',
    'usuario': 'M12 4a4 4 0 1 0 0 8a4 4 0 1 0 0-8 M4.5 20c1.3-3.3 4-5 7.5-5s6.2 1.7 7.5 5',
    'usuarios': 'M9 5a3 3 0 1 0 0 6a3 3 0 1 0 0-6 M3 19c.8-3 3-4.5 6-4.5s5.2 1.5 6 4.5 '
                'M16 6a2.5 2.5 0 1 1 0 5 M17 14.6c2 .4 3.4 1.8 4 4.4',
    'auto': 'M4 16v-4l2-5h12l2 5v4z M4 16v2 M20 16v2 M7.5 13h.01 M16.5 13h.01',
    'lista': 'M9 6h11 M9 12h11 M9 18h11 M4.5 6h.01 M4.5 12h.01 M4.5 18h.01',
    'calendario': 'M4 6h16v14H4z M4 10h16 M8 3v4 M16 3v4 M8 14h.01 M12 14h.01 M16 14h.01',
    'mas': 'M12 5v14 M5 12h14',
    'editar': 'M4 20h4L19 9l-4-4L4 16z',
    'quitar': 'M6 6l12 12 M18 6L6 18',
    'check': 'M5 12l5 5 9-10',
    'sin_red': 'M3 3l18 18 M8.5 16a5 5 0 0 1 7 0 M5 12.5a10 10 0 0 1 4-2 M19 12.5a10 10 0 0 0-3-1.6 '
               'M2 8.5a15 15 0 0 1 5-2.7 M22 8.5A15 15 0 0 0 12 5 M12 19.5h.01',
    'campana': 'M6 16v-5a6 6 0 1 1 12 0v5l2 2H4z M10 20a2 2 0 0 0 4 0',
    'carga': 'M12 3a9 9 0 1 1-9 9',
    'llave': 'M14 10a4 4 0 1 0-4 4 M12.8 12.8L20 20 M17 17l2-2',
    'salir': 'M10 5H5v14h5 M14 8l4 4-4 4 M18 12H9',
}


def icono_svg(out, nombre, x, y, tam, color, grosor=2):
    escala = tam / 24
    out.append(f'<g transform="translate({x:.1f},{y:.1f}) scale({escala:.4f})" fill="none" '
               f'stroke="{color}" stroke-width="{grosor}" stroke-linecap="round" '
               f'stroke-linejoin="round"><path d="{ICONOS[nombre]}"/></g>')


def icono_html(nombre, tam=14):
    return (f'<svg viewBox="0 0 24 24" width="{tam}" height="{tam}" fill="none" stroke="currentColor" '
            f'stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'
            f'<path d="{ICONOS[nombre]}"/></svg>')


# ----------------------------------------------------------------------------- texto en SVG
ESTRECHAS = set("iljtfr.,:;·'|!()[]-")
ANCHAS = set('mwMW@%')


def medir(texto, tam, mono=False, negrita=False, espaciado=0.0):
    if mono:
        return len(texto) * (tam * 0.6 + espaciado)
    total = 0.0
    for c in texto:
        if c == ' ':
            f = 0.28
        elif c in ESTRECHAS:
            f = 0.3
        elif c in ANCHAS:
            f = 0.82
        elif c.isdigit():
            f = 0.57
        elif c.isupper() or c in '$#+':
            f = 0.64
        else:
            f = 0.52
        total += f
    return total * tam * (1.07 if negrita else 1.0)


def partir(texto, tam, ancho, mono=False, negrita=False, espaciado=0.0, donde=''):
    """Ajuste de línea por palabras; avisa si una palabra sola no cabe."""
    lineas, actual = [], ''
    for palabra in texto.split(' '):
        if medir(palabra, tam, mono, negrita, espaciado) > ancho:
            AVISOS.append(f'{donde}: "{palabra}" no cabe en {ancho:.0f}px')
        prueba = palabra if not actual else actual + ' ' + palabra
        if not actual or medir(prueba, tam, mono, negrita, espaciado) <= ancho:
            actual = prueba
        else:
            lineas.append(actual)
            actual = palabra
    lineas.append(actual)
    return lineas


def cabe(texto, tam, ancho, donde, mono=False, negrita=False, espaciado=0.0):
    if medir(texto, tam, mono, negrita, espaciado) > ancho:
        AVISOS.append(f'{donde}: "{texto}" no cabe en {ancho:.0f}px')


def texto(out, x, y, contenido, tam, color, mono=False, peso=None, ancla=None, espaciado=None):
    extra = ''
    if peso:
        extra += f' font-weight="{peso}"'
    if ancla:
        extra += f' text-anchor="{ancla}"'
    if espaciado:
        extra += f' letter-spacing="{espaciado}"'
    out.append(f'<text x="{x:.1f}" y="{y:.1f}" class="{"m" if mono else "s"}" font-size="{tam}" '
               f'fill="{color}"{extra}>{escape(contenido)}</text>')


def rect(out, x, y, w, h, relleno='none', borde=None, rx=0, grosor=1, guiones=None, opacidad=None):
    extra = f' stroke="{borde}" stroke-width="{grosor}"' if borde else ''
    if guiones:
        extra += f' stroke-dasharray="{guiones}"'
    if opacidad is not None:
        extra += f' opacity="{opacidad}"'
    out.append(f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{rx}" '
               f'fill="{relleno}"{extra}/>')


def linea(out, x1, y1, x2, y2, color, grosor=1, guiones=None):
    extra = f' stroke-dasharray="{guiones}"' if guiones else ''
    out.append(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{color}" '
               f'stroke-width="{grosor}"{extra}/>')


def h(s):
    return escape(s)


# ------------------------------------------------------------------------------ componentes
# Cada componente sabe su alto en el SVG (alto), dibujarse en el SVG (svg) y emitir su HTML (html).
GAP = 11  # separación vertical entre componentes


class Chip:
    def __init__(self, estado, grande=False):
        self.etiqueta, self.estilo = ESTADOS[estado]
        self.grande = grande

    def ancho(self):
        tam = 10 if self.grande else 8.5
        return medir(self.etiqueta.upper(), tam, mono=True, espaciado=0.3) + (22 if self.grande else 14)

    def alto_propio(self):
        return 22 if self.grande else 17

    def dibujar(self, out, x, y):
        tc, bc, fc = ESTILOS[self.estilo]
        w, hh = self.ancho(), self.alto_propio()
        rect(out, x, y, w, hh, C[fc] if fc else 'none', C[bc], rx=hh / 2)
        tam = 10 if self.grande else 8.5
        texto(out, x + w / 2, y + hh / 2 + tam * 0.36, self.etiqueta.upper(), tam, C[tc], mono=True,
              ancla='middle', espaciado=0.3)

    # como componente de cuerpo: centrado
    def alto(self, w):
        return self.alto_propio()

    def svg(self, out, x, y, w):
        self.dibujar(out, x + (w - self.ancho()) / 2, y)

    def html(self):
        return f'<span class="chip chip-{self.estilo}{" chip-lg" if self.grande else ""}">{h(self.etiqueta)}</span>'


class Campo:
    def __init__(self, etiqueta, valor, icono=None, vacio=False):
        self.etiqueta, self.valor, self.icono, self.vacio = etiqueta, valor, icono, vacio

    def alto(self, w):
        return 45

    def svg(self, out, x, y, w):
        texto(out, x, y + 9, self.etiqueta.upper(), 8.5, C['ink_soft'], mono=True, espaciado=0.4)
        cabe(self.etiqueta.upper(), 8.5, w, 'etiqueta', mono=True, espaciado=0.4)
        rect(out, x, y + 15, w, 30, C['surface'], C['line_soft'], rx=7)
        tx, disponible = x + 10, w - 20
        if self.icono in ('pin', 'fecha', 'hora'):
            icono_svg(out, self.icono, x + 8, y + 23.5, 13, C['ink_soft'])
            tx, disponible = x + 26, w - 36
        if self.icono == 'select':
            icono_svg(out, 'select', x + w - 21, y + 23.5, 13, C['ink_soft'])
            disponible -= 16
        cabe(self.valor, 12, disponible, f'campo "{self.etiqueta}"')
        texto(out, tx, y + 34, self.valor, 12, C['ink_soft'] if self.vacio else C['ink'])

    def html(self):
        izq = icono_html(self.icono, 13) if self.icono in ('pin', 'fecha', 'hora') else ''
        der = f'<span class="sel">{icono_html("select", 13)}</span>' if self.icono == 'select' else ''
        cls = ' is-empty' if self.vacio else ''
        return (f'<div class="wf-field"><label>{h(self.etiqueta)}</label>'
                f'<div class="wf-input{cls}">{izq}<span>{h(self.valor)}</span>{der}</div></div>')


class Fila:
    def __init__(self, *campos, pesos=None):
        self.campos = campos
        self.pesos = pesos or [1] * len(campos)

    def alto(self, w):
        return max(c.alto(w) for c in self.campos)

    def svg(self, out, x, y, w):
        libre = w - 10 * (len(self.campos) - 1)
        for c, peso in zip(self.campos, self.pesos):
            cw = libre * peso / sum(self.pesos)
            c.svg(out, x, y, cw)
            x += cw + 10

    def html(self):
        return '<div class="field-row">' + ''.join(
            f'<div class="grow" style="flex:{peso}">{c.html()}</div>'
            for c, peso in zip(self.campos, self.pesos)) + '</div>'


class Boton:
    COLORES = {  # tipo → (texto, borde, fondo)
        'primario': ('on_accent', 'accent', 'accent'), 'secundario': ('ink', 'line_strong', None),
        'fantasma': ('accent', None, None), 'peligro': ('on_danger', 'danger', 'danger'),
        'peligro_sec': ('danger', 'danger', None),
    }

    def __init__(self, etiqueta, tipo='primario', deshabilitado=False, icono=None, chico=False, multilinea=False):
        self.etiqueta, self.tipo, self.deshabilitado = etiqueta, tipo, deshabilitado
        self.icono, self.chico, self.multilinea = icono, chico, multilinea

    def ancho_propio(self):
        return medir(self.etiqueta, 11 if self.chico else 12.5, negrita=True) + (18 if self.chico else 28) + \
            (18 if self.icono else 0)

    def lineas(self, w):
        return partir(self.etiqueta, 12.5, w - 24, negrita=True, donde=f'botón "{self.etiqueta}"')

    def alto(self, w):
        if self.multilinea:
            return 34 + 15 * (len(self.lineas(w)) - 1)
        return 26 if self.chico else 34

    def svg(self, out, x, y, w):
        tc, bc, fc = self.COLORES[self.tipo]
        if self.chico:
            w = self.ancho_propio()
        hh, tam = self.alto(w), (11 if self.chico else 12.5)
        grupo = []
        rect(grupo, x, y, w, hh, C[fc] if fc else 'none', C[bc] if bc else None, rx=8)
        if self.multilinea:
            ls = self.lineas(w)
            by = y + hh / 2 + tam * 0.36 - 7.5 * (len(ls) - 1)
            for i, l in enumerate(ls):
                texto(grupo, x + w / 2, by + 15 * i, l, tam, C[tc], peso='500', ancla='middle')
            out.extend(grupo)
            return
        tw = medir(self.etiqueta, tam, negrita=True) + (18 if self.icono else 0)
        cabe(self.etiqueta, tam, w - 12, f'botón "{self.etiqueta}"', negrita=True)
        inicio = x + (w - tw) / 2
        if self.icono:
            icono_svg(grupo, self.icono, inicio, y + hh / 2 - 7, 14, C[tc])
            inicio += 18
        texto(grupo, inicio, y + hh / 2 + tam * 0.36, self.etiqueta, tam, C[tc], peso='500')
        if self.deshabilitado:
            out.append('<g opacity="0.42">' + ''.join(grupo) + '</g>')
        else:
            out.extend(grupo)

    def html(self):
        cls = f'wf-btn wf-btn-{self.tipo}' + (' wf-btn-sm' if self.chico else '') + \
              (' is-disabled' if self.deshabilitado else '')
        ic = icono_html(self.icono, 14) if self.icono else ''
        return f'<div class="{cls}">{ic}{h(self.etiqueta)}</div>'


class Botones:
    def __init__(self, *botones):
        self.botones = botones

    def alto(self, w):
        return 34

    def svg(self, out, x, y, w):
        bw = (w - 8 * (len(self.botones) - 1)) / len(self.botones)
        for i, b in enumerate(self.botones):
            b.svg(out, x + i * (bw + 8), y, bw)

    def html(self):
        return '<div class="btn-row">' + ''.join(b.html() for b in self.botones) + '</div>'


class Nota:
    def __init__(self, contenido, centro=False):
        self.contenido, self.centro = contenido, centro

    def lineas(self, w):
        return partir(self.contenido, 10.5, w, donde='nota')

    def alto(self, w):
        return len(self.lineas(w)) * 15 - 3

    def svg(self, out, x, y, w):
        for i, l in enumerate(self.lineas(w)):
            if self.centro:
                texto(out, x + w / 2, y + 10 + i * 15, l, 10.5, C['ink_soft'], ancla='middle')
            else:
                texto(out, x, y + 10 + i * 15, l, 10.5, C['ink_soft'])

    def html(self):
        return f'<p class="note{" note-center" if self.centro else ""}">{h(self.contenido)}</p>'


class Aviso:
    """Recuadro de mensaje: info (genérico, confirmación), aviso (falta algo) o peligro (conflicto)."""

    TONOS = {'info': ('info', 'info_soft'), 'aviso': ('warn', 'warn_soft'), 'peligro': ('danger', 'danger_soft')}

    def __init__(self, contenido, tono='info'):
        self.contenido, self.tono = contenido, tono

    def lineas(self, w):
        return partir(self.contenido, 11, w - 20, donde='aviso')

    def alto(self, w):
        return len(self.lineas(w)) * 15 + 16

    def svg(self, out, x, y, w):
        tinta, fondo = self.TONOS[self.tono]
        rect(out, x, y, w, self.alto(w), C[fondo], C[tinta], rx=8)
        for i, l in enumerate(self.lineas(w)):
            texto(out, x + 10, y + 19 + i * 15, l, 11, C[tinta])

    def html(self):
        cls = '' if self.tono == 'info' else f' callout-{self.tono}'
        return f'<div class="callout{cls}">{h(self.contenido)}</div>'


class Seccion:
    def __init__(self, contenido):
        self.contenido = contenido

    def alto(self, w):
        return 10

    def svg(self, out, x, y, w):
        cabe(self.contenido.upper(), 8.5, w, 'sección', mono=True, espaciado=0.4)
        texto(out, x, y + 9, self.contenido.upper(), 8.5, C['ink_soft'], mono=True, espaciado=0.4)

    def html(self):
        return f'<p class="section-label">{h(self.contenido)}</p>'


class KV:
    def __init__(self, clave, valor=None, chip=None):
        self.clave, self.valor, self.chip = clave, valor, chip

    def ancho_valor(self, w):
        return w - medir(self.clave, 12) - 12

    def lineas(self, w):
        if self.chip:
            return ['']
        return partir(self.valor, 12, self.ancho_valor(w), negrita=True, donde=f'valor "{self.clave}"')

    def alto(self, w):
        base = 19 if self.chip else len(self.lineas(w)) * 15
        return base + 12

    def svg(self, out, x, y, w):
        cabe(self.clave, 12, w * 0.5, 'clave')
        texto(out, x, y + 17, self.clave, 12, C['ink_soft'])
        if self.chip:
            self.chip.dibujar(out, x + w - self.chip.ancho(), y + 5)
        else:
            for i, l in enumerate(self.lineas(w)):
                texto(out, x + w, y + 17 + i * 15, l, 12, C['ink'], peso='500', ancla='end')
        linea(out, x, y + self.alto(w), x + w, y + self.alto(w), C['line_soft'])

    def html(self):
        v = self.chip.html() if self.chip else h(self.valor)
        return f'<div class="kv-row"><span class="k">{h(self.clave)}</span><span class="v">{v}</span></div>'


class Item:
    """Fila de lista: título, sublíneas y, a la derecha, un chip o un botón chico."""

    def __init__(self, titulo, *subs, derecha=None):
        self.titulo, self.subs, self.derecha = titulo, subs, derecha

    def _der_w(self):
        if isinstance(self.derecha, Chip):
            return self.derecha.ancho()
        if isinstance(self.derecha, Boton):
            return self.derecha.ancho_propio()
        return 0

    def partes(self, w):
        tl = partir(self.titulo, 12.5, w - self._der_w() - 8, negrita=True, donde='título de fila')
        sl = [l for s in self.subs for l in partir(s, 11, w, donde='sub de fila')]
        return tl, sl

    def alto(self, w):
        tl, sl = self.partes(w)
        der = 26 if isinstance(self.derecha, Boton) else 17
        return 8 + max(len(tl) * 16, der) + len(sl) * 15 + 8

    def svg(self, out, x, y, w):
        tl, sl = self.partes(w)
        for i, l in enumerate(tl):
            texto(out, x, y + 21 + i * 16, l, 12.5, C['ink'], peso='500')
        if isinstance(self.derecha, Chip):
            self.derecha.dibujar(out, x + w - self.derecha.ancho(), y + 8)
        elif isinstance(self.derecha, Boton):
            self.derecha.svg(out, x + w - self.derecha.ancho_propio(), y + 6, 0)
        der = 26 if isinstance(self.derecha, Boton) else 17
        sy = y + 8 + max(len(tl) * 16, der)
        for i, l in enumerate(sl):
            texto(out, x, sy + 11 + i * 15, l, 11, C['ink_soft'])
        linea(out, x, y + self.alto(w), x + w, y + self.alto(w), C['line_soft'])

    def html(self):
        subs = ''.join(f'<span class="li-sub">{h(s)}</span>' for s in self.subs)
        der = self.derecha.html() if self.derecha else ''
        return (f'<div class="wf-list-item"><div class="li-main"><span class="li-title">{h(self.titulo)}</span>'
                f'{subs}</div>{der}</div>')


class Opcion:
    """Fila con radio: conductor o vehículo a elegir."""

    def __init__(self, titulo, chip, sub=None, elegido=False, deshabilitado=False):
        self.titulo, self.chip, self.sub = titulo, chip, sub
        self.elegido, self.deshabilitado = elegido, deshabilitado

    def alto(self, w):
        return 34 if self.sub else 30

    def svg(self, out, x, y, w):
        g = []
        hh = self.alto(w)
        cy = y + hh / 2
        g.append(f'<circle cx="{x + 7:.1f}" cy="{cy:.1f}" r="6.5" fill="none" stroke="{C["line_strong"]}" '
                 f'stroke-width="1.5"/>')
        if self.elegido:
            g.append(f'<circle cx="{x + 7:.1f}" cy="{cy:.1f}" r="3.5" fill="{C["accent"]}"/>')
        disponible = w - 22 - self.chip.ancho() - 6
        cabe(self.titulo, 12.5, disponible, 'opción', negrita=True)
        if self.sub:
            texto(g, x + 22, cy - 2, self.titulo, 12.5, C['ink'], peso='500')
            cabe(self.sub, 10.5, disponible, 'sub de opción')
            texto(g, x + 22, cy + 11, self.sub, 10.5, C['ink_soft'])
        else:
            texto(g, x + 22, cy + 4.5, self.titulo, 12.5, C['ink'], peso='500')
        self.chip.dibujar(g, x + w - self.chip.ancho(), cy - 8.5)
        linea(g, x, y + hh, x + w, y + hh, C['line_soft'])
        out.append(f'<g opacity="0.45">{"".join(g)}</g>' if self.deshabilitado else ''.join(g))

    def html(self):
        cls = 'driver-row' + (' is-disabled' if self.deshabilitado else '')
        radio = 'radio radio-selected' if self.elegido else 'radio'
        sub = f'<span class="li-sub">{h(self.sub)}</span>' if self.sub else ''
        return (f'<div class="{cls}"><span class="{radio}"></span><div class="li-main driver-name">'
                f'<span class="li-title">{h(self.titulo)}</span>{sub}</div>{self.chip.html()}</div>')


class Casilla:
    def __init__(self, titulo, sub, marcada=False, bloqueada=False):
        self.titulo, self.sub, self.marcada, self.bloqueada = titulo, sub, marcada, bloqueada

    def alto(self, w):
        return 36

    def svg(self, out, x, y, w):
        g = []
        relleno = C['accent'] if self.marcada else 'none'
        rect(g, x, y + 11, 14, 14, relleno, C['accent'] if self.marcada else C['line_strong'], rx=3, grosor=1.5)
        if self.marcada:
            icono_svg(g, 'check', x + 1.5, y + 12.5, 11, C['on_accent'], grosor=3)
        texto(g, x + 24, y + 15, self.titulo, 12.5, C['ink'], peso='500')
        texto(g, x + 24, y + 29, self.sub, 10.5, C['ink_soft'])
        linea(g, x, y + 36, x + w, y + 36, C['line_soft'])
        out.append(f'<g opacity="0.55">{"".join(g)}</g>' if self.bloqueada else ''.join(g))

    def html(self):
        cls = 'check-row' + (' is-locked' if self.bloqueada else '')
        caja = 'box is-checked' if self.marcada else 'box'
        marca = icono_html('check', 11) if self.marcada else ''
        return (f'<div class="{cls}"><span class="{caja}">{marca}</span><div class="li-main">'
                f'<span class="li-title">{h(self.titulo)}</span><span class="li-sub">{h(self.sub)}</span></div></div>')


class Viaje:
    """Tarjeta de la agenda del Conductor."""
    BARRA = {'PENDIENTE': 'line_strong', 'ASIGNADO': 'info', 'EN_CURSO': 'accent',
             'FINALIZADO': 'ink_soft', 'CANCELADO': 'danger'}

    def __init__(self, hora, estado, nombre, ruta, sync=False):
        self.hora, self.estado, self.nombre, self.ruta, self.sync = hora, estado, nombre, ruta, sync

    def alto(self, w):
        return 64 + (24 if self.sync else 0)

    def svg(self, out, x, y, w):
        hh = self.alto(w)
        rect(out, x, y, w, hh, C['surface'], C['line_soft'], rx=10)
        rect(out, x, y, 4, hh, C[self.BARRA[self.estado]], rx=2)
        texto(out, x + 14, y + 20, self.hora, 12, C['ink'], mono=True, peso='600')
        chip = Chip(self.estado)
        chip.dibujar(out, x + w - chip.ancho() - 10, y + 8)
        cabe(self.nombre, 12.5, w - 24, 'tarjeta', negrita=True)
        texto(out, x + 14, y + 39, self.nombre, 12.5, C['ink'], peso='500')
        cabe(self.ruta, 11, w - 24, 'tarjeta')
        texto(out, x + 14, y + 55, self.ruta, 11, C['ink_soft'])
        if self.sync:
            Chip('SYNC').dibujar(out, x + 14, y + 63)

    def html(self):
        sync = Chip('SYNC').html() if self.sync else ''
        cls = {'ASIGNADO': 'st-asignado', 'EN_CURSO': 'st-encurso', 'FINALIZADO': 'st-finalizado',
               'CANCELADO': 'st-cancelado'}.get(self.estado, '')
        return (f'<div class="trip-card {cls}"><div class="trip-top"><span class="trip-time">{h(self.hora)}</span>'
                f'{Chip(self.estado).html()}</div><span class="trip-name">{h(self.nombre)}</span>'
                f'<span class="trip-route">{h(self.ruta)}</span>{sync}</div>')


class Filtros:
    def __init__(self, opciones, activa):
        self.opciones, self.activa = opciones, activa

    def _pos(self, w):
        pos, cx, fila = [], 0.0, 0
        for o in self.opciones:
            ow = medir(o, 10.5, negrita=True) + 18
            if cx and cx + ow > w:
                cx, fila = 0.0, fila + 1
            pos.append((o, cx, fila, ow))
            cx += ow + 6
        return pos

    def alto(self, w):
        return (self._pos(w)[-1][2] + 1) * 28 - 6

    def svg(self, out, x, y, w):
        for o, cx, fila, ow in self._pos(w):
            activa = o == self.activa
            rect(out, x + cx, y + fila * 28, ow, 22, C['ink'] if activa else 'none',
                 None if activa else C['line_strong'], rx=11)
            texto(out, x + cx + ow / 2, y + fila * 28 + 15, o, 10.5, C['surface'] if activa else C['ink'],
                  peso='500', ancla='middle')

    def html(self):
        return '<div class="filters">' + ''.join(
            f'<span class="filter{" is-active" if o == self.activa else ""}">{h(o)}</span>'
            for o in self.opciones) + '</div>'


class Banner:
    def __init__(self, contenido):
        self.contenido = contenido

    def lineas(self, w):
        return partir(self.contenido, 10.5, w - 34, donde='banner')

    def alto(self, w):
        return len(self.lineas(w)) * 14 + 14

    def svg(self, out, x, y, w):
        rect(out, x, y, w, self.alto(w), C['warn_soft'], C['warn'], rx=8)
        icono_svg(out, 'sin_red', x + 9, y + 7, 15, C['warn'])
        for i, l in enumerate(self.lineas(w)):
            texto(out, x + 30, y + 18 + i * 14, l, 10.5, C['warn'], peso='500')

    def html(self):
        return f'<div class="banner">{icono_html("sin_red", 15)}<span>{h(self.contenido)}</span></div>'


class SelectorFecha:
    def __init__(self, contenido):
        self.contenido = contenido

    def alto(self, w):
        return 32

    def svg(self, out, x, y, w):
        rect(out, x, y, w, 32, C['surface_tint'], C['line_soft'], rx=8)
        icono_svg(out, 'calendario', x + 10, y + 9, 14, C['ink_soft'])
        texto(out, x + 31, y + 20.5, self.contenido, 12, C['ink'], peso='500')
        icono_svg(out, 'select', x + w - 24, y + 9, 14, C['ink_soft'])

    def html(self):
        return (f'<div class="date-sel">{icono_html("calendario", 14)}<span>{h(self.contenido)}</span>'
                f'<span class="sel">{icono_html("select", 14)}</span></div>')


class Persona:
    """Avatar con nombre y una línea secundaria."""

    def __init__(self, nombre, sub):
        self.nombre, self.sub = nombre, sub

    def alto(self, w):
        return 38

    def svg(self, out, x, y, w):
        out.append(f'<circle cx="{x + 18:.1f}" cy="{y + 19:.1f}" r="17" fill="{C["surface_tint"]}" '
                   f'stroke="{C["line_strong"]}" stroke-width="1.4"/>')
        icono_svg(out, 'usuario', x + 8, y + 9, 20, C['ink_soft'], grosor=1.6)
        cabe(self.nombre, 13, w - 46, 'persona', negrita=True)
        cabe(self.sub, 10.5, w - 46, 'persona')
        texto(out, x + 46, y + 16, self.nombre, 13, C['ink'], peso='600')
        texto(out, x + 46, y + 31, self.sub, 10.5, C['ink_soft'])

    def html(self):
        return (f'<div class="driver-block"><span class="avatar">{icono_html("usuario", 18)}</span>'
                f'<div class="li-main"><span class="li-title">{h(self.nombre)}</span>'
                f'<span class="li-sub">{h(self.sub)}</span></div></div>')


class Pasajero:
    """Pasajero en el formulario de solicitud, con editar y quitar."""

    def __init__(self, nombre, documento):
        self.nombre, self.documento = nombre, documento

    def alto(self, w):
        return 40

    def svg(self, out, x, y, w):
        rect(out, x, y, w, 40, C['surface'], C['line_soft'], rx=8)
        texto(out, x + 10, y + 17, self.nombre, 12, C['ink'], peso='500')
        texto(out, x + 10, y + 31, self.documento, 10.5, C['ink_soft'])
        icono_svg(out, 'editar', x + w - 44, y + 13, 14, C['ink_soft'])
        icono_svg(out, 'quitar', x + w - 22, y + 13, 14, C['ink_soft'])

    def html(self):
        return (f'<div class="pax-row"><div class="li-main"><span class="li-title">{h(self.nombre)}</span>'
                f'<span class="li-sub">{h(self.documento)}</span></div>'
                f'<span class="pax-actions">{icono_html("editar", 14)}{icono_html("quitar", 14)}</span></div>')


class Marca:
    def alto(self, w):
        return 66

    def svg(self, out, x, y, w):
        icono_svg(out, 'brujula', x + w / 2 - 15, y, 30, C['accent'], grosor=1.6)
        texto(out, x + w / 2, y + 48, 'BELMONTE', 16, C['ink'], mono=True, peso='700', ancla='middle',
              espaciado=1)
        texto(out, x + w / 2, y + 64, 'Traslados corporativos', 11, C['ink_soft'], ancla='middle')

    def html(self):
        return (f'<div class="brand">{icono_html("brujula", 30)}<p class="brand-word">BELMONTE</p>'
                f'<p class="brand-tag">Traslados corporativos</p></div>')


class Carga:
    def __init__(self, contenido):
        self.contenido = contenido

    def alto(self, w):
        return 54

    def svg(self, out, x, y, w):
        icono_svg(out, 'carga', x + w / 2 - 13, y, 26, C['accent'], grosor=2.2)
        texto(out, x + w / 2, y + 48, self.contenido, 12, C['ink_soft'], ancla='middle')

    def html(self):
        return f'<div class="loading">{icono_html("carga", 26)}<span>{h(self.contenido)}</span></div>'


class Reloj:
    def __init__(self, hora, fecha):
        self.hora, self.fecha = hora, fecha

    def alto(self, w):
        return 64

    def svg(self, out, x, y, w):
        texto(out, x + w / 2, y + 40, self.hora, 40, C['ink'], peso='300', ancla='middle')
        texto(out, x + w / 2, y + 60, self.fecha, 11.5, C['ink_soft'], ancla='middle')

    def html(self):
        return f'<div class="lock"><span class="lock-time">{h(self.hora)}</span><span class="lock-date">{h(self.fecha)}</span></div>'


class Notificacion:
    def __init__(self, titulo, contenido):
        self.titulo, self.contenido = titulo, contenido

    def lineas(self, w):
        return partir(self.contenido, 11, w - 20, donde='notificación')

    def alto(self, w):
        return 48 + len(self.lineas(w)) * 14

    def svg(self, out, x, y, w):
        rect(out, x, y, w, self.alto(w), C['surface'], C['line_soft'], rx=12)
        icono_svg(out, 'campana', x + 10, y + 9, 12, C['accent'])
        texto(out, x + 27, y + 19, 'BELMONTE · ahora', 8.5, C['ink_soft'], mono=True, espaciado=0.3)
        texto(out, x + 10, y + 37, self.titulo, 12.5, C['ink'], peso='600')
        for i, l in enumerate(self.lineas(w)):
            texto(out, x + 10, y + 53 + i * 14, l, 11, C['ink_soft'])

    def html(self):
        return (f'<div class="notif"><span class="notif-app">{icono_html("campana", 12)}BELMONTE · ahora</span>'
                f'<span class="notif-title">{h(self.titulo)}</span><span class="li-sub">{h(self.contenido)}</span></div>')


class Lista:
    """Lista simple de nombres (pasajeros en un detalle)."""

    def __init__(self, *elementos):
        self.elementos = elementos

    def alto(self, w):
        return len(self.elementos) * 22

    def svg(self, out, x, y, w):
        for i, e in enumerate(self.elementos):
            icono_svg(out, 'usuario', x, y + 4 + i * 22, 13, C['ink_soft'])
            cabe(e, 12, w - 20, 'lista')
            texto(out, x + 20, y + 15 + i * 22, e, 12, C['ink'])

    def html(self):
        return '<ul class="plain-list">' + ''.join(
            f'<li>{icono_html("usuario", 13)}<span>{h(e)}</span></li>' for e in self.elementos) + '</ul>'


class Dialogo:
    """Diálogo de confirmación sobre la pantalla (overlay)."""

    def __init__(self, titulo, contenido, botones, apilados=False):
        self.titulo, self.contenido, self.botones = titulo, contenido, botones
        self.apilados = apilados

    def partes(self, w):
        return (partir(self.titulo, 13.5, w - 28, negrita=True, donde='diálogo'),
                partir(self.contenido, 11, w - 28, donde='diálogo'))

    def alto_botones(self, w):
        if not self.apilados:
            return 32
        return sum(b.alto(w - 28) for b in self.botones) + 8 * (len(self.botones) - 1)

    def alto(self, w):
        tl, cl = self.partes(w)
        return 16 + len(tl) * 18 + 6 + len(cl) * 15 + 14 + self.alto_botones(w) + 14

    def svg(self, out, x, y, w):
        tl, cl = self.partes(w)
        rect(out, x, y, w, self.alto(w), C['surface'], C['line_soft'], rx=12)
        cy = y + 16
        for l in tl:
            texto(out, x + 14, cy + 13, l, 13.5, C['ink'], peso='600')
            cy += 18
        cy += 6
        for l in cl:
            texto(out, x + 14, cy + 11, l, 11, C['ink_soft'])
            cy += 15
        if not self.apilados:
            Botones(*self.botones).svg(out, x + 14, cy + 14, w - 28)
            return
        by = cy + 14
        for b in self.botones:
            b.svg(out, x + 14, by, w - 28)
            by += b.alto(w - 28) + 8

    def html(self):
        botones = ''.join(b.html() for b in self.botones) if self.apilados else Botones(*self.botones).html()
        return (f'<div class="scrim"></div><div class="dialog"><h5>{h(self.titulo)}</h5>'
                f'<p class="note">{h(self.contenido)}</p>{botones}</div>')


class Hoja:
    """Hoja inferior (overlay): opciones (elegir app de navegación) o un formulario corto (cuerpo)."""

    def __init__(self, titulo, opciones, nota, cuerpo=None):
        self.titulo, self.opciones, self.nota, self.cuerpo = titulo, opciones, nota, cuerpo

    def medio(self, w):
        if self.cuerpo:
            return sum(c.alto(w - 28) for c in self.cuerpo) + GAP * (len(self.cuerpo) - 1) + 8
        return len(self.opciones) * 40

    def alto(self, w):
        return 16 + 18 + self.medio(w) + 8 + len(partir(self.nota, 10.5, w - 28)) * 15 + 18

    def svg(self, out, x, y, w):
        out.append(f'<path d="M{x:.1f},{y + 16:.1f} a16,16 0 0 1 16,-16 h{w - 32:.1f} a16,16 0 0 1 16,16 '
                   f'v{self.alto(w) - 16:.1f} h{-w:.1f} z" fill="{C["surface"]}" stroke="{C["line_soft"]}"/>')
        rect(out, x + w / 2 - 18, y + 7, 36, 4, C['line_strong'], rx=2, opacidad=0.5)
        texto(out, x + 14, y + 30, self.titulo, 13, C['ink'], peso='600')
        cy = y + 40
        if self.cuerpo:
            by = cy + 4
            for c in self.cuerpo:
                c.svg(out, x + 14, by, w - 28)
                by += c.alto(w - 28) + GAP
            cy += self.medio(w)
        for ic, etiqueta in self.opciones:
            rect(out, x + 14, cy + 6, 26, 26, C['accent_soft'], rx=7)
            icono_svg(out, ic, x + 19, cy + 11, 16, C['accent'])
            texto(out, x + 50, cy + 23, etiqueta, 12.5, C['ink'], peso='500')
            cy += 40
        for i, l in enumerate(partir(self.nota, 10.5, w - 28, donde='hoja')):
            texto(out, x + 14, cy + 16 + i * 15, l, 10.5, C['ink_soft'])

    def html(self):
        ops = ''.join(f'<div class="sheet-opt"><span class="sheet-ic">{icono_html(ic, 16)}</span>{h(t)}</div>'
                      for ic, t in self.opciones)
        if self.cuerpo:
            ops += ''.join(c.html() for c in self.cuerpo)
        return (f'<div class="scrim"></div><div class="sheet"><span class="grabber"></span><h5>{h(self.titulo)}</h5>'
                f'{ops}<p class="note">{h(self.nota)}</p></div>')


# --------------------------------------------------------------------------------- pantallas
NAV_SOL = (('Mis solicitudes', 'lista'), ('Perfil', 'usuario'))
NAV_ADM = (('Solicitudes', 'lista'), ('Conductores', 'usuarios'), ('Vehículos', 'auto'), ('Perfil', 'usuario'))
NAV_CON = (('Mis viajes', 'calendario'), ('Perfil', 'usuario'))


class Pantalla:
    def __init__(self, titulo, traza, cuerpo, barra=None, sub=None, atras=False, badge=None,
                 nav=None, activa=0, overlay=None, centro=False, bloqueo=False):
        self.titulo, self.traza, self.cuerpo = titulo, traza, cuerpo
        self.barra, self.sub, self.atras, self.badge = barra, sub, atras, badge
        self.nav, self.activa, self.overlay = nav, activa, overlay
        self.centro, self.bloqueo = centro, bloqueo
        self.num = 0
        self.rol = ''


NUEVA_SOLICITUD_FONDO = [Chip('ASIGNADO', grande=True), KV('Horario', '24-09-2026 · 14:30'),
                         KV('Origen', 'Aeropuerto A. Merino Benítez'), KV('Destino', 'Av. Apoquindo 5400')]

DATOS_LOCALES_FONDO = [
    Marca(), Aviso('No pudimos abrir los datos locales. Inténtalo de nuevo.', tono='peligro'),
    Boton('Reintentar'), Boton('Cerrar sesión', 'secundario', icono='salir'),
    Boton('Restablecer datos locales', 'peligro_sec')]

SOLICITUD_SIN_PASAJEROS = [
    Chip('PENDIENTE', grande=True), Seccion('Viaje'), KV('Horario', '26-09-2026 · 10:00'),
    KV('Origen', 'Av. Vitacura 2939'), KV('Destino', 'Aeropuerto A. Merino Benítez'),
    Aviso('Faltan pasajeros: el viaje no se puede asignar hasta agregar al menos uno.', tono='aviso'),
    Seccion('Pasajeros (0)'), Boton('Agregar pasajero', icono='mas'),
    Boton('Cancelar solicitud', 'peligro_sec')]

GRUPOS = [
    ('Transversal', 'acceso', [
        Pantalla('Inicio de aplicación', ['CU-14'], centro=True, cuerpo=[
            Marca(), Carga('Comprobando sesión…'),
            Nota('Revisa mantenimiento local y cierre pendiente; con sesión guardada abre el inicio de su rol '
                 'sin esperar la red; sin sesión, el login.', centro=True)]),
        Pantalla('Login', ['CU-14', 'RF01', 'RF06', 'HU01', 'HU06'], centro=True, cuerpo=[
            Marca(), Campo('Correo electrónico', 'nombre@empresa.cl', vacio=True),
            Campo('Contraseña', '••••••••', vacio=True), Boton('Iniciar sesión'),
            Boton('Crear cuenta solicitante', 'fantasma'), Boton('Recuperar contraseña', 'fantasma'),
            Nota('Conductores: usen las credenciales que entrega el Administrador. El rol lo define el '
                 'backend; no hay selector de rol.', centro=True)]),
        Pantalla('Registro de solicitante', ['RF01', 'HU01'], barra='Crear cuenta solicitante', atras=True, cuerpo=[
            Campo('Nombre de contacto', 'Constanza Ríos'),
            Campo('Correo', 'crios@andesconsult.cl'),
            Campo('Teléfono (opcional)', '+56 9 5555 0101'), Campo('Contraseña', '••••••••'),
            Seccion('Empresa'), Campo('Razón social', 'Andes Consultores SpA'),
            Campo('RUT de la empresa', '76.543.210-K'), Boton('Crear cuenta'),
            Nota('Al terminar vuelve al login con el correo precargado. Si el RUT ya existe, la cuenta '
                 'espera la aprobación de un Administrador.')]),
        Pantalla('Recuperar contraseña', ['CU-13', 'RF21', 'HU22'], barra='Recuperar contraseña', atras=True, cuerpo=[
            Nota('Te enviaremos una contraseña temporal al correo registrado. Tu contraseña actual sigue '
                 'funcionando; la temporal vence en 30 minutos.'),
            Campo('Correo', 'crios@andesconsult.cl'), Boton('Enviar contraseña temporal'),
            Aviso('Si el correo pertenece a una cuenta habilitada, recibirás la contraseña temporal.'),
            Nota('Solo Solicitante y Administrador. Conductores: el Administrador reemplaza su contraseña.')]),
        Pantalla('Cambio obligatorio de contraseña', ['CU-13', 'CU-14', 'RF21'], barra='Cambiar contraseña',
                 cuerpo=[
            Aviso('Debes cambiar la contraseña: entraste con una contraseña temporal.', tono='aviso'),
            Campo('Contraseña actual (temporal)', '••••••••••••'), Campo('Nueva contraseña', '••••••••'),
            Campo('Confirmar contraseña', '••••••••'), Boton('Guardar'),
            Boton('Cerrar sesión', 'fantasma', icono='salir'),
            Nota('Sin flecha atrás. Con la sesión temporal solo se cambia la contraseña o se cierra sesión; al '
                 'guardar, la app inicia sesión con la nueva.')]),
        Pantalla('Sesión por renovar', ['CU-14', 'RF11', 'HU11'], centro=True, cuerpo=[
            Marca(), Aviso('Tu sesión terminó. Inicia sesión de nuevo.', tono='aviso'),
            Campo('Correo electrónico', 'ireyes@belmonte.cl'),
            Campo('Contraseña', '••••••••', vacio=True), Boton('Iniciar sesión'),
            Aviso('Tus cambios pendientes se enviarán al volver a entrar.'),
            Nota('No se pudo renovar la sesión: la app conserva datos y pendientes de la cuenta. Entrar con '
                 'otra cuenta pide antes confirmar su descarte.', centro=True)]),
        Pantalla('Datos locales no disponibles', ['RF11', 'HU11'], centro=True, cuerpo=DATOS_LOCALES_FONDO + [
            Nota('La base local no abre. Reintentar no borra nada; Cerrar sesión deja la limpieza de esta cuenta '
                 'pendiente hasta que la base abra.', centro=True)]),
        Pantalla('Restablecer datos locales', ['RF11', 'HU11'], centro=True, cuerpo=DATOS_LOCALES_FONDO,
                 overlay=Dialogo('¿Restablecer los datos locales?',
                                 'Se borrarán los datos guardados de todas las cuentas en este dispositivo, los '
                                 'cambios sin sincronizar y los registros de solicitudes interrumpidas. Se cerrará '
                                 'tu sesión. Los cambios que ya recibió el servidor no se desharán. Si había un '
                                 'envío sin confirmar, contacta al Administrador antes de repetirlo.',
                                 [Boton('Cancelar', 'secundario'),
                                  Boton('Borrar datos locales y cerrar sesión', 'peligro', multilinea=True)],
                                 apilados=True)),
        Pantalla('Cierre de sesión pendiente', ['CU-14', 'RF11'], centro=True, cuerpo=[
            Marca(), Aviso('Estamos terminando el cierre de sesión. Inténtalo de nuevo.', tono='aviso'),
            Boton('Reintentar cierre'),
            Nota('El cierre ya se aceptó: el acceso protegido sigue bloqueado y no se permite otro login hasta '
                 'terminar la limpieza local.', centro=True)]),
    ]),
    ('Solicitante', 'cliente corporativo', [
        Pantalla('Mis solicitudes', ['RF03', 'HU03'], barra='Mis solicitudes', nav=NAV_SOL, cuerpo=[
            Boton('Nueva solicitud', icono='mas'),
            Item('TR-1051', 'Marcela Soto', '25-09 · 09:00 · Vitacura → Aeropuerto', derecha=Chip('PENDIENTE')),
            Item('TR-1042', 'Constanza Ríos +1', '24-09 · 14:30 · Aeropuerto → Las Condes', derecha=Chip('ASIGNADO')),
            Item('TR-1039', 'Roberto Aguilar', '24-09 · 07:15 · Apoquindo → Aeropuerto', derecha=Chip('FINALIZADO')),
            Item('TR-1033', 'Paula Lagos', '23-09 · 18:00 · Las Condes → Aeropuerto', derecha=Chip('CANCELADO'))]),
        Pantalla('Solicitud sin terminar', ['CU-01', 'RF02', 'HU02'], barra='Mis solicitudes', nav=NAV_SOL, cuerpo=[
            Aviso('Una solicitud no terminó de enviarse.', tono='aviso'),
            Boton('Continuar envío'), Boton('Descartar', 'secundario'),
            Item('TR-1051', 'Marcela Soto', '25-09 · 09:00 · Vitacura → Aeropuerto', derecha=Chip('PENDIENTE')),
            Item('TR-1042', 'Constanza Ríos +1', '24-09 · 14:30 · Aeropuerto → Las Condes', derecha=Chip('ASIGNADO')),
            Nota('Continuar envío repite cada paso pendiente con su clave original y no crea otro viaje. '
                 'Descartar solo borra el registro local.')]),
        Pantalla('Revisión requerida', ['CU-01', 'RF02', 'HU02'], barra='Mis solicitudes', nav=NAV_SOL, cuerpo=[
            Aviso('No pudimos confirmar el resultado. Contacta al Administrador antes de crear otra solicitud.',
                  tono='peligro'),
            KV('Folio', 'TR-1056'), Boton('Descartar registro', 'peligro_sec'),
            Item('TR-1051', 'Marcela Soto', '25-09 · 09:00 · Vitacura → Aeropuerto', derecha=Chip('PENDIENTE')),
            Nota('Pasaron 24 horas sin confirmar el envío. Descartar no cancela el viaje: con folio, abre su '
                 'detalle.')]),
        Pantalla('Nueva solicitud', ['CU-01', 'RF02', 'HU02', 'V1'], barra='Nueva solicitud', atras=True, cuerpo=[
            Fila(Campo('Fecha', '24-09-2026', 'fecha'), Campo('Hora', '14:30', 'hora'), pesos=(3, 2)),
            Campo('Origen', 'Aeropuerto A. Merino B.', 'pin'),
            Campo('Destino', 'Av. Apoquindo 5400', 'pin'),
            Nota('Origen y destino solo desde las sugerencias de Google Places, con coordenadas.'),
            Seccion('Pasajeros (2)'), Pasajero('Constanza Ríos', 'RUT 15.234.567-8'),
            Pasajero('Tomás Vidal', 'Pasaporte P4471023'), Boton('Agregar pasajero', 'fantasma', icono='mas'),
            Campo('N.º de vuelo (opcional)', 'LA 1502'), Boton('Enviar solicitud')]),
        Pantalla('Detalle de solicitud', ['CU-01', 'RF03', 'RF20', 'HU03', 'HU21'], barra='Solicitud TR-1042', atras=True,
                 cuerpo=[
            Chip('ASIGNADO', grande=True), Seccion('Viaje'), KV('Horario', '24-09-2026 · 14:30'),
            KV('Origen', 'Aeropuerto A. Merino Benítez'), KV('Destino', 'Av. Apoquindo 5400'),
            KV('Vuelo', 'LA 1502'), Seccion('Pasajeros'), Lista('Constanza Ríos', 'Tomás Vidal'),
            Boton('Agregar pasajero', 'fantasma', icono='mas'),
            Seccion('Asignación'), KV('Conductor', 'Iván Reyes'), KV('Vehículo', 'KXJR42'),
            Seccion('Pago'), KV('Estado', chip=Chip('PAGADO')), KV('Monto', '$48.000'),
            Boton('Cancelar solicitud', 'peligro_sec')]),
        Pantalla('Solicitud sin pasajeros', ['CU-01', 'HU02', 'V2'], barra='Solicitud TR-1055', atras=True,
                 cuerpo=SOLICITUD_SIN_PASAJEROS),
        Pantalla('Agregar pasajero', ['CU-01', 'HU02'], barra='Solicitud TR-1055', atras=True,
                 cuerpo=SOLICITUD_SIN_PASAJEROS,
                 overlay=Hoja('Agregar pasajero', [],
                              'Se registra y se asocia al viaje. Con vehículo asignado, no más pasajeros que '
                              'su capacidad.',
                              cuerpo=[Campo('Nombre', 'Marcela Soto'),
                                      Fila(Campo('Documento', 'RUT', 'select'), Campo('Número', '16.789.012-3'),
                                           pesos=(2, 3)),
                                      Botones(Boton('Volver', 'secundario'), Boton('Agregar'))])),
        Pantalla('Confirmar cancelación', ['CU-08', 'RF15', 'HU16', 'V5', 'P4', 'P5'], barra='Solicitud TR-1042',
                 atras=True, cuerpo=NUEVA_SOLICITUD_FONDO,
                 overlay=Dialogo('¿Cancelar la solicitud TR-1042?',
                                 'Se avisará al conductor y el viaje saldrá de su agenda. Un pago Pagado se '
                                 'reembolsa al 50 %; uno Pendiente se anula.',
                                 [Boton('Volver', 'secundario'), Boton('Cancelar', 'peligro')])),
        Pantalla('Solicitud cancelada', ['CU-08', 'RF15', 'V5', 'P4'], barra='Solicitud TR-1042', atras=True,
                 cuerpo=[
            Chip('CANCELADO', grande=True), KV('Horario', '24-09-2026 · 14:30'),
            KV('Origen', 'Aeropuerto A. Merino Benítez'), KV('Destino', 'Av. Apoquindo 5400'),
            Seccion('Pago'), KV('Estado', chip=Chip('REEMBOLSADO')), KV('Monto', '$48.000'),
            KV('Devuelto 50 %', '$24.000'),
            Nota('Tras cancelar se queda en el detalle con el nuevo estado. Ya no se ofrece "Cancelar".')]),
        Pantalla('Perfil', ['RF01'], barra='Perfil', nav=NAV_SOL, activa=1, cuerpo=[
            Persona('Constanza Ríos', 'crios@andesconsult.cl'), KV('Teléfono', '+56 9 5555 0101'),
            Seccion('Empresa'), KV('Razón social', 'Andes Consultores SpA'), KV('RUT', '76.543.210-K'),
            Boton('Cambiar contraseña', 'secundario', icono='llave'),
            Boton('Cerrar sesión', 'fantasma', icono='salir')]),
        Pantalla('Cambiar contraseña', ['RF21'], barra='Cambiar contraseña', atras=True, cuerpo=[
            Campo('Contraseña actual', '••••••••'), Campo('Nueva contraseña', '••••••••'),
            Campo('Confirmar contraseña', '••••••••'),
            Boton('Guardar'),
            Nota('Solo Solicitante y Administrador. Si entraste con una contraseña temporal, debes cambiarla '
                 'antes de seguir.')]),
    ]),
    ('Administrador', 'recepcionista', [
        Pantalla('Solicitudes', ['CU-04', 'RF04', 'HU04'], barra='Solicitudes', nav=NAV_ADM, cuerpo=[
            Boton('Cuentas por aprobar (1)', 'fantasma', icono='usuarios'),
            Filtros(['Pendientes', 'Asignado', 'En curso', 'Finalizado', 'Cancelado', 'Todas'], 'Pendientes'),
            Item('TR-1038', 'Luis Pino', '24-09 · 06:30 · Ñuñoa → Aeropuerto', derecha=Chip('VENCIDO')),
            Item('TR-1051', 'Marcela Soto', '25-09 · 09:00 · Vitacura → Aeropuerto',
                 derecha=Boton('Asignar', 'secundario', chico=True)),
            Item('TR-1047', 'Diego Fuentes', '25-09 · 11:45 · Providencia → Aeropuerto',
                 derecha=Boton('Asignar', 'secundario', chico=True)),
            Nota('Cada fila abre el detalle; en las pendientes, "Asignar" abre directo la asignación. '
                 'Vencido: pasó una hora de su horario sin iniciar.')]),
        Pantalla('Lista por actualizar', ['RF04', 'HU04', 'HU05'], barra='Solicitudes', nav=NAV_ADM, cuerpo=[
            Boton('Cuentas por aprobar (1)', 'fantasma', icono='usuarios'),
            Filtros(['Pendientes', 'Asignado', 'En curso', 'Finalizado', 'Cancelado', 'Todas'], 'Asignado'),
            Aviso('La lista necesita actualizarse.', tono='aviso'), Boton('Actualizar', 'secundario'),
            Nota('TR-1042 se asignó desde Pendientes: esta lista guardada aún no lo incluye. No es una lista '
                 'vacía confirmada; se completa con la respuesta del servidor.')]),
        Pantalla('Cuentas por aprobar', ['CU-17', 'RF26', 'HU27'], barra='Cuentas por aprobar', atras=True,
                 cuerpo=[
            Item('Rodrigo Pérez', 'rperez@andesconsult.cl', 'Andes Consultores SpA · 76.543.210-K',
                 derecha=Chip('SOL_PENDIENTE')),
            Botones(Boton('Rechazar', 'secundario'), Boton('Aprobar')),
            Nota('Se registró con el RUT de una empresa existente. Aprobar o rechazar le avisa por correo; '
                 'hasta entonces no puede iniciar sesión.')]),
        Pantalla('Detalle de solicitud', ['CU-04', 'RF22', 'HU23', 'V2'], barra='Solicitud TR-1042',
                 sub='Andes Consultores SpA', atras=True, cuerpo=[
            Chip('ASIGNADO', grande=True), KV('Horario', '24-09-2026 · 14:30'),
            KV('Origen', 'Aeropuerto A. Merino Benítez'), KV('Destino', 'Av. Apoquindo 5400'),
            KV('Pasajeros', '2'), Seccion('Asignación'), KV('Conductor', 'Iván Reyes'),
            KV('Vehículo', 'KXJR42'), Boton('Reasignar', 'secundario'),
            Seccion('Pago'), KV('Estado', chip=Chip('PAGADO')), Boton('Ver pago', 'fantasma'),
            Boton('Cancelar viaje', 'peligro_sec'),
            Nota('Reasignar solo en Pendiente o Asignado sin vencer; cancelar, en Pendiente o Asignado. En En '
                 'curso o Finalizado no se ofrecen.')]),
        Pantalla('Solicitud sin pasajeros', ['CU-04', 'HU05', 'V2'], barra='Solicitud TR-1055',
                 sub='Andes Consultores SpA', atras=True, cuerpo=[
            Chip('PENDIENTE', grande=True), KV('Horario', '26-09-2026 · 10:00'),
            KV('Origen', 'Av. Vitacura 2939'), KV('Destino', 'Aeropuerto A. Merino Benítez'),
            KV('Pasajeros', '0'), Aviso('Sin pasajeros: no se puede asignar.', tono='aviso'),
            Boton('Registrar pago', 'secundario'), Boton('Cancelar viaje', 'peligro_sec'),
            Nota('"Asignar" aparece cuando el Solicitante agrega al menos un pasajero.')]),
        Pantalla('Cancelar viaje', ['CU-15', 'RF23', 'HU24', 'V7', 'P4', 'P5'], barra='Cancelar viaje',
                 sub='TR-1038 · Luis Pino', atras=True, cuerpo=[
            Chip('VENCIDO', grande=True), KV('Horario', '24-09-2026 · 06:30'),
            KV('Pago', chip=Chip('PAGADO')), KV('Monto', '$52.000'),
            Campo('Motivo', 'Conductor sin disponibilidad'),
            Campo('Monto a reembolsar', '$52.000'),
            Botones(Boton('Volver', 'secundario'), Boton('Confirmar', 'peligro')),
            Nota('Vencido: el reembolso lo decide el Administrador, de $0 al total. Si no está vencido, se '
                 'reembolsa el 100 % sin preguntar.')]),
        Pantalla('Asignar conductor y vehículo', ['CU-04', 'RF05', 'RF22', 'HU05', 'HU23', 'V2'],
                 barra='Asignar', sub='TR-1051 · 2 pasajeros', atras=True, cuerpo=[
            Seccion('Conductor'),
            Opcion('Iván Reyes', Chip('DISPONIBLE'), sub='1 viaje asignado', elegido=True),
            Opcion('Paula Muñoz', Chip('DISPONIBLE'), sub='Sin viajes'),
            Opcion('Jorge Lara', Chip('OCUPADO'), sub='TR-1045 en curso', deshabilitado=True),
            Seccion('Vehículo habilitado para Iván'),
            Opcion('KXJR42', Chip('DISPONIBLE'), sub='4 pasajeros', elegido=True),
            Opcion('LPHT17', Chip('DISPONIBLE'), sub='6 pasajeros'),
            Seccion('Resumen'), KV('Conductor', 'Iván Reyes'), KV('Vehículo', 'KXJR42'),
            Boton('Confirmar asignación'),
            Nota('Disponible = sin viaje en curso. Ni conductor ni vehículo pueden tener otro viaje a menos '
                 'de 2 h. La capacidad debe cubrir a los pasajeros.')]),
        Pantalla('Conductores', ['CU-07'], barra='Conductores', nav=NAV_ADM, activa=1, cuerpo=[
            Boton('Registrar conductor', icono='mas'),
            Item('Iván Reyes', 'ireyes@belmonte.cl', derecha=Chip('DISPONIBLE')),
            Item('Paula Muñoz', 'pmunoz@belmonte.cl', derecha=Chip('DISPONIBLE')),
            Item('Jorge Lara', 'jlara@belmonte.cl', derecha=Chip('OCUPADO')),
            Item('Marta Soto', 'msoto@belmonte.cl', derecha=Chip('INACTIVO')),
            Nota('Ocupado solo mientras tiene un viaje en curso. Inactivo: dado de baja; no inicia sesión ni '
                 'se asigna.')]),
        Pantalla('Registrar conductor', ['CU-07', 'RF14', 'HU15'], barra='Registrar conductor', atras=True, cuerpo=[
            Campo('Nombre', 'Paula Muñoz'), Campo('Correo', 'pmunoz@belmonte.cl'),
            Campo('Teléfono (opcional)', '+56 9 5555 0202'), Campo('Contraseña inicial', '••••••••'),
            Boton('Registrar conductor'),
            Nota('La contraseña la define el Administrador (no se genera sola) y se guarda como hash. El '
                 'Conductor no tiene autorregistro.')]),
        Pantalla('Detalle de conductor', ['CU-07', 'CU-10', 'RF14'], barra='Iván Reyes', atras=True, cuerpo=[
            Chip('DISPONIBLE', grande=True), KV('Correo', 'ireyes@belmonte.cl'),
            KV('Teléfono', '+56 9 5555 0303'), Seccion('Vehículos habilitados'),
            Item('KXJR42', '4 pasajeros', derecha=Chip('DISPONIBLE')),
            Item('LPHT17', '6 pasajeros', derecha=Chip('DISPONIBLE')),
            Boton('Gestionar habilitaciones', 'secundario'), Boton('Editar datos', 'secundario', icono='editar'),
            Boton('Reemplazar contraseña', 'secundario', icono='llave'),
            Boton('Desactivar', 'peligro_sec'),
            Nota('Solo sin viajes asignados ni en curso.')]),
        Pantalla('Vehículos', ['CU-09'], barra='Vehículos', nav=NAV_ADM, activa=2, cuerpo=[
            Boton('Registrar vehículo', icono='mas'),
            Item('KXJR42', '4 pasajeros · GPS 50231', derecha=Chip('DISPONIBLE')),
            Item('LPHT17', '6 pasajeros · sin GPS', derecha=Chip('DISPONIBLE')),
            Item('GZRW90', '4 pasajeros · GPS 50477', derecha=Chip('EN_USO')),
            Item('HJKL21', '4 pasajeros · en taller', derecha=Chip('INACTIVO')),
            Nota('En uso solo mientras tiene un viaje en curso. Inactivo: no se asigna.')]),
        Pantalla('Registrar vehículo', ['CU-09', 'RF16', 'HU17'], barra='Registrar vehículo', atras=True, cuerpo=[
            Campo('Patente', 'LPHT17'), Campo('Capacidad (pasajeros)', '6'),
            Campo('Serial GPS (opcional)', 'Opcional', vacio=True), Boton('Registrar vehículo'),
            Nota('Patente única y capacidad mayor que 0. El serial GPS es un dato de flota. Nace Disponible '
                 'y activo.')]),
        Pantalla('Detalle de vehículo', ['CU-09', 'CU-10'], barra='KXJR42', atras=True, cuerpo=[
            Chip('DISPONIBLE', grande=True), KV('Capacidad', '4 pasajeros'), KV('Serial GPS', '50231'),
            Seccion('Conductores habilitados'), Lista('Iván Reyes', 'Paula Muñoz'),
            Boton('Gestionar habilitaciones', 'secundario'), Boton('Desactivar', 'peligro_sec')]),
        Pantalla('Gestionar habilitaciones', ['CU-10', 'RF17', 'HU18'], barra='Habilitaciones',
                 sub='Vehículo KXJR42', atras=True, cuerpo=[
            Casilla('Iván Reyes', 'Ya habilitado', marcada=True, bloqueada=True),
            Casilla('Paula Muñoz', 'Nueva habilitación', marcada=True),
            Casilla('Jorge Lara', 'Sin habilitar'), Boton('Agregar habilitaciones'),
            Nota('Solo se agregan; una relación existente no se duplica. No se eliminan en el MVP.')]),
        Pantalla('Registrar pago', ['CU-11', 'RF18', 'HU19', 'P1'], barra='Registrar pago', sub='Viaje TR-1042',
                 atras=True, cuerpo=[
            Campo('Monto', '$48.000'), Campo('Banco', 'BancoEstado', 'select'),
            Campo('Medio de pago', 'Transferencia', 'select'), Campo('Tipo de recibo', 'Factura', 'select'),
            Boton('Registrar pago'),
            Nota('Solo si el viaje no está Cancelado y no tiene pago. Nace Pendiente. Un pago rechazado o '
                 'anulado no se reemplaza.')]),
        Pantalla('Registrar pago sin catálogos', ['CU-11', 'RF18', 'HU19'], barra='Registrar pago',
                 sub='Viaje TR-1042', atras=True, cuerpo=[
            Aviso('No pudimos cargar los datos del formulario.', tono='peligro'),
            Boton('Reintentar', 'secundario'),
            Campo('Monto', '$48.000'), Campo('Banco', 'Sin datos', 'select', vacio=True),
            Campo('Medio de pago', 'Sin datos', 'select', vacio=True),
            Campo('Tipo de recibo', 'Sin datos', 'select', vacio=True),
            Boton('Registrar pago', deshabilitado=True),
            Nota('Selectores y envío quedan deshabilitados hasta recibir los catálogos; la app no inventa '
                 'opciones.')]),
        Pantalla('Detalle de pago', ['CU-12', 'RF19', 'HU20', 'P2', 'P3', 'V6'], barra='Pago',
                 sub='Viaje TR-1042', atras=True, cuerpo=[
            Chip('PAGO_PENDIENTE', grande=True), KV('Monto', '$48.000'), KV('Banco', 'BancoEstado'),
            KV('Medio de pago', 'Transferencia'), KV('Recibo', 'Factura'), KV('Fecha de cobro', '—'),
            Boton('Marcar pagado'), Botones(Boton('Rechazar', 'secundario'), Boton('Anular', 'peligro_sec')),
            Nota('Desde Pagado solo se puede anular. Al cancelar el viaje, Pagado pasa a Reembolsado y '
                 'Pendiente a Anulado. Anular cancela el viaje si está Pendiente o Asignado.')]),
        Pantalla('Perfil', ['RF21'], barra='Perfil', nav=NAV_ADM, activa=3, cuerpo=[
            Persona('Andrea Castro', 'recepcion@belmonte.cl'), KV('Rol', 'Administrador'),
            Boton('Cambiar contraseña', 'secundario', icono='llave'),
            Boton('Registrar administrador', 'secundario', icono='mas'),
            Boton('Cerrar sesión', 'fantasma', icono='salir')]),
        Pantalla('Registrar administrador', ['CU-18', 'RF27', 'HU28'], barra='Registrar administrador',
                 atras=True, cuerpo=[
            Campo('Nombre', 'Tomás Ibarra'), Campo('Correo', 'tibarra@belmonte.cl'),
            Campo('Teléfono (opcional)', 'Opcional', vacio=True), Campo('Contraseña inicial', '••••••••'),
            Boton('Registrar administrador'),
            Nota('La contraseña inicial es temporal: vence en 72 horas y debe cambiarse al entrar.')]),
    ]),
    ('Conductor', 'en terreno', [
        Pantalla('Notificación push', ['RF12', 'HU13'], bloqueo=True, cuerpo=[
            Reloj('08:12', 'jueves 24 de septiembre'),
            Notificacion('Nuevo viaje asignado', 'Viaje TR-1051 para 25-09-2026 · 09:00'),
            Nota('Al tocarla, la app comprueba sesión, rol y acceso y abre el detalle del viaje. El push '
                 'lleva folio y fecha, sin nombres ni direcciones (RNF08).', centro=True)]),
        Pantalla('Mis viajes', ['CU-02', 'RF07', 'HU07'], barra='Mis viajes', sub='Iván Reyes', nav=NAV_CON,
                 cuerpo=[
            SelectorFecha('Hoy · 24-09-2026'),
            Viaje('07:15', 'FINALIZADO', 'Roberto Aguilar', 'Apoquindo → Aeropuerto'),
            Viaje('14:30', 'ASIGNADO', 'Constanza Ríos +1', 'Aeropuerto → Las Condes'),
            Viaje('18:45', 'ASIGNADO', 'Felipe Mora', 'Las Condes → Aeropuerto'),
            Nota('Varios viajes asignados por día. El selector de fecha muestra la agenda de otro día.')]),
        Pantalla('Detalle de viaje', ['CU-03', 'RF08', 'RF09', 'HU08', 'HU09', 'V3', 'V4'], barra='Viaje TR-1042',
                 atras=True, cuerpo=[
            Chip('ASIGNADO', grande=True), KV('Horario', '24-09-2026 · 14:30'), KV('Vuelo', 'LA 1502'),
            KV('Solicitante', 'Constanza Ríos'), KV('Teléfono', '+56 9 5555 0101'),
            KV('Origen', 'Aeropuerto A. Merino Benítez'), KV('Destino', 'Av. Apoquindo 5400'),
            KV('Vehículo', 'KXJR42'), Seccion('Pasajeros'), Lista('Constanza Ríos', 'Tomás Vidal'),
            Boton('Iniciar viaje'), Boton('Finalizar viaje', 'secundario', deshabilitado=True),
            Boton('Navegar al origen', 'secundario', icono='brujula'), Boton('Rechazar', 'peligro_sec'),
            Nota('Iniciar desde 1 h antes hasta 1 h después del horario. Rechazar, con motivo y conexión, '
                 'devuelve el viaje a Pendiente.')]),
        Pantalla('Antes de la ventana', ['RF09', 'HU09', 'V3'], barra='Viaje TR-1049', atras=True, cuerpo=[
            Chip('ASIGNADO', grande=True), KV('Horario', '24-09-2026 · 18:45'),
            KV('Origen', 'Av. Isidora Goyenechea 3000'), KV('Destino', 'Aeropuerto A. Merino Benítez'),
            Seccion('Pasajeros'), Lista('Felipe Mora'),
            Boton('Iniciar viaje', deshabilitado=True), Boton('Finalizar viaje', 'secundario', deshabilitado=True),
            Nota('Disponible desde las 17:45.', centro=True),
            Boton('Navegar al origen', 'secundario', icono='brujula'), Boton('Rechazar', 'peligro_sec')]),
        Pantalla('Confirmar inicio', ['RF09', 'HU09', 'V3'], barra='Viaje TR-1042', atras=True,
                 cuerpo=NUEVA_SOLICITUD_FONDO,
                 overlay=Dialogo('¿Iniciar el viaje TR-1042?',
                                 'Pasará a En curso. Quedarás Ocupado y el vehículo KXJR42 En uso hasta '
                                 'finalizarlo.',
                                 [Boton('Volver', 'secundario'), Boton('Iniciar')])),
        Pantalla('Sin conexión', ['RF11', 'HU11', 'V3'], barra='Viaje TR-1042', atras=True, cuerpo=[
            Banner('Sin conexión. Los cambios se enviarán al reconectar.'),
            Chip('ASIGNADO', grande=True), Chip('SYNC'),
            Nota('En curso, pendiente de sincronizar: no se muestra como confirmado hasta que responda el '
                 'servidor.', centro=True),
            KV('Horario', '24-09-2026 · 14:30'), KV('Destino', 'Av. Apoquindo 5400'),
            Boton('Iniciar viaje', deshabilitado=True), Boton('Finalizar viaje', 'secundario'),
            Nota('La cola respeta el orden por viaje: Iniciar y luego Finalizar.')]),
        Pantalla('Cambio en conflicto', ['RF11', 'HU11', 'V3'], barra='Viaje TR-1044', atras=True, cuerpo=[
            Chip('ASIGNADO', grande=True), Chip('VENCIDO'),
            Aviso('No se pudo sincronizar «Iniciar viaje»: el viaje está fuera de la ventana de inicio.',
                  tono='peligro'),
            KV('Horario', '24-09-2026 · 07:30'), KV('Destino', 'Aeropuerto A. Merino Benítez'),
            Boton('Iniciar viaje', deshabilitado=True), Boton('Finalizar viaje', 'secundario', deshabilitado=True),
            Boton('Descartar cambio', 'peligro_sec'),
            Nota('Vencido: avisa al Administrador. Descartar borra el cambio pendiente y vuelve a cargar el '
                 'viaje.')]),
        Pantalla('Viaje no disponible', ['RF07', 'RF11', 'HU11'], barra='Mis viajes', sub='Iván Reyes',
                 nav=NAV_CON, cuerpo=[
            Aviso('El viaje ya no está disponible para tu cuenta. Los cambios pendientes no se enviaron.',
                  tono='peligro'),
            SelectorFecha('Hoy · 24-09-2026'),
            Viaje('07:15', 'FINALIZADO', 'Roberto Aguilar', 'Apoquindo → Aeropuerto'),
            Viaje('18:45', 'ASIGNADO', 'Felipe Mora', 'Las Condes → Aeropuerto'),
            Nota('El viaje se reasignó a otro conductor: su consulta respondió 403 y la app borró sus datos '
                 'y su cola.')]),
        Pantalla('Abrir navegación', ['CU-03', 'RF10', 'HU10'], barra='Viaje TR-1042', atras=True,
                 cuerpo=NUEVA_SOLICITUD_FONDO,
                 overlay=Hoja('Abrir con', [('pin', 'Google Maps'), ('brujula', 'Waze')],
                              'Asignado: navega al origen; En curso: al destino. Solo apps instaladas; sin '
                              'ninguna, aviso.')),
        Pantalla('Perfil', ['RF06'], barra='Perfil', nav=NAV_CON, activa=1, cuerpo=[
            Persona('Iván Reyes', 'ireyes@belmonte.cl'), KV('Teléfono', '+56 9 5555 0303'),
            Nota('Tu contraseña la gestiona el Administrador; no hay cambio ni recuperación por autoservicio.'),
            Boton('Cerrar sesión', 'fantasma', icono='salir')]),
    ]),
]

n = 0
for rol, _, pantallas in GRUPOS:
    for p in pantallas:
        n += 1
        p.num, p.rol = n, rol
TOTAL = n

# ------------------------------------------------------------------------------------ SVG
ANCHO = 1062
MARGEN = 40
COLS = 4
COL_GAP = 20
COL_W = (ANCHO - 2 * MARGEN - (COLS - 1) * COL_GAP) / COLS
DEV_W = 226
SCR_W = DEV_W - 2 * 2 - 2 * 7  # borde 2 y relleno 7 por lado
PAD = 13
CUERPO_W = SCR_W - 2 * PAD
NAV_H = 48
SANS = "-apple-system, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif"
MONO = "ui-monospace, SFMono-Regular, Menlo, Consolas, 'Liberation Mono', monospace"


def barra_partes(p):
    if not p.barra:
        return [], []
    disponible = SCR_W - 28 - (22 if p.atras else 0) - (28 if p.badge else 0)
    return (partir(p.barra, 14, disponible, negrita=True, donde=f'{p.num:02d} barra'),
            partir(p.sub, 10, disponible, mono=True, donde=f'{p.num:02d} sub') if p.sub else [])


def alto_barra(p):
    tl, sl = barra_partes(p)
    return 0 if not p.barra else 26 + len(tl) * 17 + len(sl) * 13


def alto_cuerpo(p):
    comps = p.cuerpo
    return 2 * PAD + sum(c.alto(CUERPO_W) for c in comps) + GAP * (len(comps) - 1)


def alto_pantalla(p):
    return alto_barra(p) + alto_cuerpo(p) + (NAV_H if p.nav else 0)


def meta_partes(p):
    tl = partir(p.titulo, 12.5, COL_W - 26, mono=True, negrita=True, donde=f'{p.num:02d} título')
    traza = ' · '.join([p.rol] + p.traza)
    return tl, partir(traza, 9.5, COL_W - 26, mono=True, donde=f'{p.num:02d} traza')


def alto_meta(p):
    tl, ml = meta_partes(p)
    return 12 + len(tl) * 16 + len(ml) * 13 + 12


def dibujar_pantalla(out, p, x, y, alto_meta_fila, alto_scr):
    # metadatos sobre el dispositivo
    linea(out, x, y, x + COL_W, y, C['line_soft'], guiones='2,3')
    texto(out, x, y + 21, f'{p.num:02d}', 11, C['accent'], mono=True, peso='600')
    tl, ml = meta_partes(p)
    for i, l in enumerate(tl):
        texto(out, x + 26, y + 21 + i * 16, l, 12.5, C['ink'], mono=True, peso='600')
    for i, l in enumerate(ml):
        texto(out, x + 26, y + 21 + len(tl) * 16 + i * 13, l, 9.5, C['ink_soft'], mono=True)
    # dispositivo
    dx, dy = x + (COL_W - DEV_W) / 2, y + alto_meta_fila
    dev_h = 22 + alto_scr + 22
    rect(out, dx, dy, DEV_W, dev_h, C['paper'], C['line_strong'], rx=26, grosor=2)
    rect(out, dx + DEV_W / 2 - 28, dy + 9, 56, 5, C['line_strong'], rx=2.5, opacidad=0.8)
    sx, sy = dx + 9, dy + 22
    rect(out, sx, sy, SCR_W, alto_scr, C['surface_tint'] if p.bloqueo else C['surface'], C['line_soft'], rx=18)
    rect(out, dx + DEV_W / 2 - 32, sy + alto_scr + 9, 64, 4, C['line_strong'], rx=2, opacidad=0.5)
    cy = sy
    if p.barra:
        tl, sl = barra_partes(p)
        tx = sx + 14
        if p.atras:
            icono_svg(out, 'atras', sx + 12, sy + 15, 16, C['ink_soft'])
            tx = sx + 34
        for i, l in enumerate(tl):
            texto(out, tx, sy + 28 + i * 17, l, 14, C['ink'], peso='700')
        for i, l in enumerate(sl):
            texto(out, tx, sy + 28 + len(tl) * 17 + i * 13 - 2, l, 10, C['ink_soft'], mono=True)
        if p.badge:
            rect(out, sx + SCR_W - 38, sy + 15, 24, 17, C['accent'], rx=8.5)
            texto(out, sx + SCR_W - 26, sy + 27, p.badge, 10, C['on_accent'], mono=True, ancla='middle')
        cy = sy + alto_barra(p)
        linea(out, sx, cy, sx + SCR_W, cy, C['line_soft'])
    # cuerpo
    by = cy + PAD
    libre = alto_scr - alto_barra(p) - (NAV_H if p.nav else 0) - alto_cuerpo(p)
    if p.centro:
        by += libre / 2
    for c in p.cuerpo:
        c.svg(out, sx + PAD, by, CUERPO_W)
        by += c.alto(CUERPO_W) + GAP
    # barra de navegación inferior
    if p.nav:
        ny = sy + alto_scr - NAV_H
        linea(out, sx, ny, sx + SCR_W, ny, C['line_soft'])
        tw = SCR_W / len(p.nav)
        for i, (etiqueta, ic) in enumerate(p.nav):
            color = C['accent'] if i == p.activa else C['ink_soft']
            cx = sx + tw * i + tw / 2
            if i == p.activa:
                rect(out, cx - 18, ny + 6, 36, 22, C['accent_soft'], rx=11)
            icono_svg(out, ic, cx - 8, ny + 9, 16, color)
            cabe(etiqueta, 8.5, tw - 2, 'pestaña', negrita=True)
            texto(out, cx, ny + 41, etiqueta, 8.5, color, peso='600' if i == p.activa else '500', ancla='middle')
    # superpuesto
    if p.overlay:
        rect(out, sx, sy, SCR_W, alto_scr, C['ink'], rx=18, opacidad=0.38)
        if isinstance(p.overlay, Dialogo):
            dw = SCR_W - 24
            dh = p.overlay.alto(dw)
            p.overlay.svg(out, sx + 12, sy + (alto_scr - dh) / 2, dw)
        else:
            hh = p.overlay.alto(SCR_W)
            p.overlay.svg(out, sx, sy + alto_scr - hh, SCR_W)
    return dev_h


def leyenda_svg(out, y):
    filas = [('Viaje', ['PENDIENTE', 'ASIGNADO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO']),
               ('Retraso', ['ATRASADO', 'VENCIDO']),
             ('Pago', ['PAGO_PENDIENTE', 'PAGADO', 'RECHAZADO', 'ANULADO', 'REEMBOLSADO']),
             ('Conductor / vehículo', ['DISPONIBLE', 'OCUPADO', 'EN_USO', 'INACTIVO']),
             ('Sin conexión', ['SYNC'])]
    for nombre, estados in filas:
        texto(out, MARGEN, y + 13, nombre.upper(), 10, C['ink_soft'], mono=True, espaciado=0.6)
        x = MARGEN + 170
        for e in estados:
            chip = Chip(e)
            chip.dibujar(out, x, y + 1)
            x += chip.ancho() + 10
        y += 27
    return y


def generar_svg():
    out = []
    y = 42
    texto(out, MARGEN, y, 'BELMONTE SPA · MVP TRASLADOS CORPORATIVOS', 11, C['accent'], mono=True, peso='600',
          espaciado=1.4)
    texto(out, MARGEN, y + 34, 'Wireframes de baja fidelidad', 27, C['ink'], mono=True, peso='700')
    lede = (f'Las {TOTAL} pantallas del MVP descritas en el README ("Navegación y pantallas"), agrupadas por rol '
            'y trazadas a casos de uso (CU), requerimientos (RF), historias (HU) y transiciones de estado (V/P). '
            'Nombres, RUT, patentes, direcciones y vuelos son ficticios (RNF09).')
    ly = y + 58
    for l in partir(lede, 13, ANCHO - 2 * MARGEN, donde='lede'):
        texto(out, MARGEN, ly, l, 13, C['ink_soft'])
        ly += 19
    linea(out, MARGEN, ly, ANCHO - MARGEN, ly, C['line_soft'], guiones='2,3')
    y = leyenda_svg(out, ly + 14) + 10
    for rol, detalle, pantallas in GRUPOS:
        y += 30
        texto(out, MARGEN, y, rol.upper(), 13, C['ink'], mono=True, peso='700', espaciado=0.4)
        texto(out, MARGEN + medir(rol.upper(), 13, mono=True, espaciado=0.4) + 12, y, f'· {detalle}', 11,
              C['ink_soft'], mono=True)
        texto(out, ANCHO - MARGEN, y, f'{len(pantallas)} pantallas', 11, C['ink_soft'], mono=True, ancla='end')
        linea(out, MARGEN, y + 10, ANCHO - MARGEN, y + 10, C['line_soft'])
        y += 26
        for i in range(0, len(pantallas), COLS):
            fila = pantallas[i:i + COLS]
            am = max(alto_meta(p) for p in fila)
            ascr = max(max(alto_pantalla(p) for p in fila), 380)
            alturas = [dibujar_pantalla(out, p, MARGEN + j * (COL_W + COL_GAP), y, am, ascr)
                       for j, p in enumerate(fila)]
            y += am + max(alturas) + 34
    y += 6
    linea(out, MARGEN, y, ANCHO - MARGEN, y, C['line_soft'], guiones='2,3')
    texto(out, MARGEN, y + 22, 'Fuente: README.md (especificación) y anexo de requerimiento', 10.5, C['ink_soft'],
          mono=True)
    texto(out, ANCHO / 2 + 60, y + 22, 'Generado por mockups/generar_wireframes.py', 10.5, C['ink_soft'], mono=True,
          ancla='middle')
    texto(out, ANCHO - MARGEN, y + 22, 'Datos ficticios', 10.5, C['ink_soft'], mono=True, ancla='end')
    alto_total = y + 46
    cabecera = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {ANCHO} {alto_total:.0f}" width="{ANCHO}" '
                f'height="{alto_total:.0f}">\n'
                '<!-- Generado por mockups/generar_wireframes.py; no editar a mano. -->\n'
                f'<style>.s{{font-family:{SANS}}}.m{{font-family:{MONO}}}</style>\n'
                f'<rect x="0" y="0" width="{ANCHO}" height="{alto_total:.0f}" fill="{C["paper"]}"/>\n')
    return cabecera + '\n'.join(out) + '\n</svg>\n'


# ------------------------------------------------------------------------------------ HTML
def variables(paleta):
    return ''.join(f'--{k.replace("_", "-")}:{v};' for k, v in paleta.items())


CSS = """
*{box-sizing:border-box;}
body{margin:0;background:var(--paper);color:var(--ink);font-family:'Roboto','Helvetica Neue',Arial,sans-serif;}
.wrap{max-width:1180px;margin:0 auto;padding:48px 16px 64px;}
.eyebrow{font-family:'IBM Plex Mono','Courier New',monospace;font-size:11px;font-weight:600;letter-spacing:.14em;
  text-transform:uppercase;color:var(--accent);margin:0;}
h1{font-family:'IBM Plex Mono','Courier New',monospace;font-weight:700;font-size:clamp(26px,4vw,38px);
  letter-spacing:-0.01em;text-wrap:balance;margin:10px 0 0;}
.lede{font-size:15px;line-height:1.65;color:var(--ink-soft);max-width:72ch;margin:14px 0 0;}
.legend{display:flex;flex-direction:column;gap:10px;margin-top:22px;padding-top:18px;
  border-top:1px dashed var(--line-soft);font-family:'IBM Plex Mono',monospace;font-size:11px;color:var(--ink-soft);}
.legend-row{display:flex;flex-wrap:wrap;align-items:center;gap:8px 10px;}
.legend-title{text-transform:uppercase;letter-spacing:.08em;min-width:170px;}
.group{display:flex;flex-direction:column;gap:20px;margin-top:44px;}
.group-title{display:flex;align-items:baseline;flex-wrap:wrap;gap:8px;font-family:'IBM Plex Mono',monospace;
  font-size:12px;letter-spacing:.09em;text-transform:uppercase;color:var(--ink-soft);
  border-bottom:1px solid var(--line-soft);padding-bottom:10px;}
.group-title strong{color:var(--ink);font-size:14px;letter-spacing:.02em;}
.group-count{margin-left:auto;font-weight:400;}
.frames{display:grid;grid-template-columns:repeat(auto-fill,minmax(244px,1fr));gap:34px 20px;align-items:start;}
.frame-card{display:flex;flex-direction:column;gap:12px;}
.frame-meta{display:flex;gap:10px;align-items:baseline;border-top:1px dashed var(--line-soft);padding-top:9px;}
.frame-num{font-family:'IBM Plex Mono',monospace;font-size:11px;font-weight:600;color:var(--accent);flex:none;}
.frame-meta-text h3{margin:0;font-family:'IBM Plex Mono',monospace;font-size:13px;font-weight:600;color:var(--ink);}
.frame-tag{margin:3px 0 0;font-family:'IBM Plex Mono',monospace;font-size:10px;letter-spacing:.02em;color:var(--ink-soft);}
.device{width:min(250px,100%);margin:0 auto;border:2px solid var(--line-strong);border-radius:26px;padding:8px;
  background:var(--paper);}
.device .notch{width:56px;height:5px;background:var(--line-strong);border-radius:3px;margin:0 auto 8px;opacity:.8;}
.device .home-indicator{width:64px;height:4px;background:var(--line-strong);border-radius:3px;margin:9px auto 0;opacity:.5;}
.screen{position:relative;background:var(--surface);border-radius:18px;overflow:hidden;display:flex;
  flex-direction:column;min-height:420px;border:1px solid var(--line-soft);}
.screen.is-lock{background:var(--surface-tint);}
.appbar{display:flex;align-items:center;gap:8px;padding:13px 14px;border-bottom:1px solid var(--line-soft);}
.appbar .back{color:var(--ink-soft);display:flex;flex:none;}
.appbar-text{display:flex;flex-direction:column;gap:1px;min-width:0;}
.appbar-text h4{margin:0;font-size:14px;font-weight:700;color:var(--ink);}
.appbar-text .sub{font-size:10.5px;color:var(--ink-soft);font-family:'IBM Plex Mono',monospace;}
.appbar-badge{margin-left:auto;font-family:'IBM Plex Mono',monospace;font-size:10px;color:var(--on-accent);
  background:var(--accent);border-radius:999px;padding:2px 7px;flex:none;}
.screen-body{padding:14px;display:flex;flex-direction:column;gap:11px;flex:1;}
.screen-center{justify-content:center;text-align:center;}
.brand{display:flex;flex-direction:column;align-items:center;gap:6px;color:var(--accent);margin-top:6px;}
.brand-word{margin:2px 0 0;font-family:'IBM Plex Mono',monospace;font-weight:700;font-size:16px;letter-spacing:.06em;color:var(--ink);}
.brand-tag{margin:0;font-size:11px;color:var(--ink-soft);}
.loading{display:flex;flex-direction:column;align-items:center;gap:8px;color:var(--accent);font-size:12px;}
.loading span{color:var(--ink-soft);}
.wf-field{display:flex;flex-direction:column;gap:4px;text-align:left;}
.wf-field label{font-family:'IBM Plex Mono',monospace;font-size:9.5px;letter-spacing:.05em;text-transform:uppercase;color:var(--ink-soft);}
.wf-input{border:1px solid var(--line-soft);border-radius:7px;padding:8px 10px;font-size:12.5px;color:var(--ink);
  background:var(--surface);display:flex;align-items:center;gap:6px;}
.wf-input.is-empty{color:var(--ink-soft);}
.wf-input svg{flex:none;color:var(--ink-soft);}
.wf-input .sel,.date-sel .sel{margin-left:auto;display:flex;color:var(--ink-soft);}
.field-row{display:flex;gap:10px;}
.field-row .grow{min-width:0;}
.wf-btn{display:flex;align-items:center;justify-content:center;gap:6px;border-radius:8px;padding:9px 14px;
  font-weight:500;font-size:12.5px;border:1px solid transparent;}
.wf-btn-primario{background:var(--accent);color:var(--on-accent);}
.wf-btn-secundario{background:transparent;border-color:var(--line-strong);color:var(--ink);}
.wf-btn-fantasma{background:transparent;color:var(--accent);}
.wf-btn-peligro{background:var(--danger);color:var(--on-danger);}
.wf-btn-peligro_sec{background:transparent;border-color:var(--danger);color:var(--danger);}
.wf-btn-sm{padding:5px 10px;font-size:11px;flex:none;}
.wf-btn.is-disabled{opacity:.42;}
.btn-row{display:flex;gap:8px;}
.btn-row .wf-btn{flex:1;}
.note{font-size:10.5px;line-height:1.5;color:var(--ink-soft);margin:0;}
.note-center{text-align:center;}
.callout{border:1px solid var(--info);background:var(--info-soft);color:var(--info);border-radius:8px;
  padding:8px 10px;font-size:11px;line-height:1.45;}
.callout-aviso{border-color:var(--warn);background:var(--warn-soft);color:var(--warn);}
.callout-peligro{border-color:var(--danger);background:var(--danger-soft);color:var(--danger);}
.chip{font-family:'IBM Plex Mono',monospace;font-size:9px;letter-spacing:.03em;text-transform:uppercase;
  padding:3px 8px;border-radius:999px;border:1px solid;white-space:nowrap;flex:none;align-self:center;}
.chip-lg{font-size:10.5px;padding:4px 12px;}
.chip-neutro{color:var(--ink-soft);border-color:var(--line-strong);background:transparent;}
.chip-info{color:var(--info);border-color:var(--info);background:var(--info-soft);}
.chip-acento{color:var(--on-accent);border-color:var(--accent);background:var(--accent);}
.chip-tenue{color:var(--ink-soft);border-color:var(--line-soft);background:var(--surface-tint);}
.chip-peligro{color:var(--danger);border-color:var(--danger);background:var(--danger-soft);}
.chip-peligro_borde{color:var(--danger);border-color:var(--danger);background:transparent;}
.chip-ok{color:var(--accent);border-color:var(--accent);background:var(--accent-soft);}
.chip-aviso{color:var(--warn);border-color:var(--warn);background:var(--warn-soft);}
.wf-list-item{display:flex;justify-content:space-between;align-items:flex-start;gap:10px;padding:8px 0;
  border-bottom:1px solid var(--line-soft);}
.li-main{display:flex;flex-direction:column;gap:2px;min-width:0;text-align:left;}
.li-title{font-size:12.5px;font-weight:500;color:var(--ink);}
.li-sub{font-size:11px;color:var(--ink-soft);}
.section-label{font-family:'IBM Plex Mono',monospace;font-size:9.5px;text-transform:uppercase;letter-spacing:.05em;
  color:var(--ink-soft);margin:4px 0 0;}
.kv-row{display:flex;justify-content:space-between;align-items:center;gap:10px;font-size:12px;padding:5px 0;
  border-bottom:1px solid var(--line-soft);}
.kv-row .k{color:var(--ink-soft);flex:none;}
.kv-row .v{color:var(--ink);font-weight:500;text-align:right;}
.driver-row{display:flex;align-items:center;gap:10px;padding:6px 0;border-bottom:1px solid var(--line-soft);}
.driver-row.is-disabled{opacity:.45;}
.driver-name{flex:1;}
.radio{width:14px;height:14px;border-radius:50%;border:1.5px solid var(--line-strong);flex:none;display:flex;
  align-items:center;justify-content:center;}
.radio-selected::after{content:"";width:7px;height:7px;border-radius:50%;background:var(--accent);}
.check-row{display:flex;align-items:center;gap:10px;padding:6px 0;border-bottom:1px solid var(--line-soft);}
.check-row.is-locked{opacity:.55;}
.box{width:14px;height:14px;border-radius:3px;border:1.5px solid var(--line-strong);flex:none;display:flex;
  align-items:center;justify-content:center;color:var(--on-accent);}
.box.is-checked{background:var(--accent);border-color:var(--accent);}
.avatar{width:34px;height:34px;border-radius:50%;border:1.4px solid var(--line-strong);color:var(--ink-soft);
  display:flex;align-items:center;justify-content:center;flex:none;background:var(--surface-tint);}
.driver-block{display:flex;align-items:center;gap:10px;}
.pax-row{display:flex;align-items:center;justify-content:space-between;gap:8px;border:1px solid var(--line-soft);
  border-radius:8px;padding:6px 10px;}
.pax-actions{display:flex;gap:8px;color:var(--ink-soft);}
.plain-list{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:6px;font-size:12px;}
.plain-list li{display:flex;align-items:center;gap:7px;color:var(--ink);}
.plain-list svg{color:var(--ink-soft);}
.trip-card{border:1px solid var(--line-soft);border-left:4px solid var(--line-strong);border-radius:10px;
  padding:9px 12px;display:flex;flex-direction:column;gap:4px;}
.trip-card.st-asignado{border-left-color:var(--info);}
.trip-card.st-encurso{border-left-color:var(--accent);}
.trip-card.st-finalizado{border-left-color:var(--ink-soft);}
.trip-card.st-cancelado{border-left-color:var(--danger);}
.trip-card .chip{align-self:flex-start;}
.trip-top{display:flex;align-items:center;justify-content:space-between;gap:8px;}
.trip-time{font-family:'IBM Plex Mono',monospace;font-size:12px;font-weight:600;color:var(--ink);}
.trip-name{font-size:12.5px;font-weight:500;}
.trip-route{font-size:11px;color:var(--ink-soft);}
.filters{display:flex;flex-wrap:wrap;gap:6px;}
.filter{font-size:10.5px;font-weight:500;border:1px solid var(--line-strong);border-radius:999px;padding:3px 9px;}
.filter.is-active{background:var(--ink);color:var(--surface);border-color:var(--ink);}
.banner{display:flex;align-items:center;gap:8px;border:1px solid var(--warn);background:var(--warn-soft);
  color:var(--warn);border-radius:8px;padding:7px 9px;font-size:10.5px;font-weight:500;}
.banner svg{flex:none;}
.date-sel{display:flex;align-items:center;gap:8px;border:1px solid var(--line-soft);background:var(--surface-tint);
  border-radius:8px;padding:8px 10px;font-size:12px;font-weight:500;}
.date-sel svg{color:var(--ink-soft);}
.lock{display:flex;flex-direction:column;align-items:center;gap:2px;margin-top:10px;}
.lock-time{font-size:40px;font-weight:300;}
.lock-date{font-size:11.5px;color:var(--ink-soft);}
.notif{display:flex;flex-direction:column;gap:4px;text-align:left;background:var(--surface);
  border:1px solid var(--line-soft);border-radius:12px;padding:10px;}
.notif-app{display:flex;align-items:center;gap:6px;font-family:'IBM Plex Mono',monospace;font-size:9px;
  letter-spacing:.03em;color:var(--ink-soft);}
.notif-app svg{color:var(--accent);}
.notif-title{font-size:12.5px;font-weight:600;}
.navbar{display:flex;border-top:1px solid var(--line-soft);padding:6px 4px 7px;}
.tab{flex:1;display:flex;flex-direction:column;align-items:center;gap:3px;font-size:9px;font-weight:500;color:var(--ink-soft);}
.tab .tab-ic{display:flex;padding:3px 10px;border-radius:999px;}
.tab.is-active{color:var(--accent);font-weight:600;}
.tab.is-active .tab-ic{background:var(--accent-soft);}
.scrim{position:absolute;inset:0;background:var(--ink);opacity:.38;}
.dialog{position:absolute;left:12px;right:12px;top:50%;transform:translateY(-50%);background:var(--surface);
  border:1px solid var(--line-soft);border-radius:12px;padding:14px;display:flex;flex-direction:column;gap:10px;}
.dialog h5,.sheet h5{margin:0;font-size:13.5px;font-weight:600;}
.sheet{position:absolute;left:0;right:0;bottom:0;background:var(--surface);border:1px solid var(--line-soft);
  border-radius:16px 16px 0 0;padding:10px 14px 16px;display:flex;flex-direction:column;gap:10px;}
.grabber{width:36px;height:4px;border-radius:2px;background:var(--line-strong);opacity:.5;align-self:center;}
.sheet-opt{display:flex;align-items:center;gap:10px;font-size:12.5px;font-weight:500;}
.sheet-ic{display:flex;padding:5px;border-radius:7px;background:var(--accent-soft);color:var(--accent);}
footer{margin-top:60px;padding-top:20px;border-top:1px dashed var(--line-soft);display:flex;flex-wrap:wrap;
  gap:8px 24px;justify-content:space-between;font-family:'IBM Plex Mono',monospace;font-size:10.5px;color:var(--ink-soft);}
"""


def pantalla_html(p):
    partes = [f'<article class="frame-card"><header class="frame-meta"><span class="frame-num">{p.num:02d}</span>'
              f'<div class="frame-meta-text"><h3>{h(p.titulo)}</h3>'
              f'<p class="frame-tag">{h(" · ".join([p.rol] + p.traza))}</p></div></header>'
              '<div class="device"><div class="notch"></div>'
              f'<div class="screen{" is-lock" if p.bloqueo else ""}">']
    if p.barra:
        atras = f'<span class="back">{icono_html("atras", 16)}</span>' if p.atras else ''
        sub = f'<span class="sub">{h(p.sub)}</span>' if p.sub else ''
        badge = f'<span class="appbar-badge">{h(p.badge)}</span>' if p.badge else ''
        partes.append(f'<div class="appbar">{atras}<div class="appbar-text"><h4>{h(p.barra)}</h4>{sub}</div>'
                      f'{badge}</div>')
    partes.append(f'<div class="screen-body{" screen-center" if p.centro or p.bloqueo else ""}">' +
                  ''.join(c.html() for c in p.cuerpo) + '</div>')
    if p.nav:
        partes.append('<nav class="navbar">' + ''.join(
            f'<span class="tab{" is-active" if i == p.activa else ""}"><span class="tab-ic">{icono_html(ic, 16)}'
            f'</span>{h(etiqueta)}</span>' for i, (etiqueta, ic) in enumerate(p.nav)) + '</nav>')
    if p.overlay:
        partes.append(p.overlay.html())
    partes.append('</div><div class="home-indicator"></div></div></article>')
    return ''.join(partes)


def generar_html():
    leyenda = [('Viaje', ['PENDIENTE', 'ASIGNADO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO']),
               ('Retraso', ['ATRASADO', 'VENCIDO']),
               ('Pago', ['PAGO_PENDIENTE', 'PAGADO', 'RECHAZADO', 'ANULADO', 'REEMBOLSADO']),
               ('Conductor / vehículo', ['DISPONIBLE', 'OCUPADO', 'EN_USO', 'INACTIVO']),
               ('Sin conexión', ['SYNC'])]
    filas = ''.join(f'<div class="legend-row"><span class="legend-title">{h(n)}</span>' +
                    ''.join(Chip(e).html() for e in es) + '</div>' for n, es in leyenda)
    grupos = ''.join(
        f'<section class="group"><div class="group-title"><strong>{h(rol)}</strong><span>&middot; {h(det)}</span>'
        f'<span class="group-count">{len(ps)} pantallas</span></div><div class="frames">' +
        ''.join(pantalla_html(p) for p in ps) + '</div></section>'
        for rol, det, ps in GRUPOS)
    claro, oscuro = variables(CLARO), variables(OSCURO)
    return f"""<!doctype html>
<!-- Generado por mockups/generar_wireframes.py; no editar a mano. -->
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Wireframes Belmonte</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;500;600;700&family=Roboto:wght@300;400;500;700&display=swap">
<style>
:root{{{claro}}}
@media (prefers-color-scheme: dark){{:root:not([data-theme="light"]){{{oscuro}}}}}
:root[data-theme="dark"]{{{oscuro}}}
{CSS}</style>
</head>
<body>
<div class="wrap">
<header>
<p class="eyebrow">Belmonte SpA &middot; MVP traslados corporativos</p>
<h1>Wireframes de baja fidelidad</h1>
<p class="lede">Las {TOTAL} pantallas del MVP descritas en el README («Navegación y pantallas»), agrupadas por rol y
trazadas a casos de uso (CU), requerimientos (RF), historias (HU) y transiciones de estado (V/P). Nombres, RUT,
patentes, direcciones y vuelos son ficticios (RNF09).</p>
<div class="legend">{filas}</div>
</header>
{grupos}
<footer><span>Fuente: README.md (especificación) y anexo de requerimiento</span>
<span>Generado por mockups/generar_wireframes.py</span><span>Datos ficticios con fines académicos</span></footer>
</div>
</body>
</html>
"""


def main():
    svg = generar_svg()
    html_txt = generar_html()
    (DIR / 'wireframes-belmonte.svg').write_text(svg, encoding='utf-8')
    (DIR / 'wireframes-belmonte.html').write_text(html_txt, encoding='utf-8')
    print(f'{TOTAL} pantallas → wireframes-belmonte.html, wireframes-belmonte.svg')
    if AVISOS:
        print(f'\n{len(set(AVISOS))} textos no caben en el SVG:')
        for a in sorted(set(AVISOS)):
            print('  -', a)
        raise SystemExit(1)


if __name__ == '__main__':
    main()
