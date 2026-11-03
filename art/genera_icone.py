"""Genera icona e banner dell'app Telecamere (PC Web Agency).

Uso: python3 art/genera_icone.py   (dalla cartella del progetto, richiede Pillow)
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
SS = 4  # supersampling per bordi lisci

NAVY, BLUE, CYAN = (10, 25, 60), (21, 101, 192), (0, 188, 212)


def gradient(w, h, c1, c2):
    """Sfumatura diagonale da c1 (alto-sinistra) a c2 (basso-destra)."""
    g = Image.new("RGB", (w, h))
    px = g.load()
    for y in range(h):
        for x in range(w):
            t = (x / w + y / h) / 2
            px[x, y] = tuple(int(a + (b - a) * t) for a, b in zip(c1, c2))
    return g


def lens(size):
    """Lente di telecamera con anelli, vetro sfumato, riflesso e spia LIVE."""
    s = size * SS
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = s / 2

    def circle(r, **kw):
        d.ellipse([c - r, c - r, c + r, c + r], **kw)

    # ombra morbida
    shadow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).ellipse([c - s * .46, c - s * .42, c + s * .46, c + s * .50], fill=(0, 0, 0, 110))
    img.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(s * .03)))

    circle(s * .45, fill=(236, 240, 245))            # corpo bianco
    circle(s * .37, fill=(38, 50, 66))               # anello scuro
    circle(s * .30, fill=(20, 28, 40))
    # vetro: sfumatura radiale blu
    for i in range(60, 0, -1):
        t = i / 60
        col = tuple(int(a + (b - a) * (1 - t)) for a, b in zip((13, 71, 161), (79, 195, 247)))
        circle(s * .24 * t, fill=col + (255,))
    circle(s * .09, fill=(8, 16, 30))                # pupilla
    # riflessi
    d.ellipse([c - s * .17, c - s * .19, c - s * .05, c - s * .07], fill=(255, 255, 255, 170))
    d.ellipse([c + s * .06, c + s * .07, c + s * .11, c + s * .12], fill=(255, 255, 255, 110))
    # spia LIVE
    r = s * .055
    lx, ly = c + s * .31, c - s * .31
    d.ellipse([lx - r * 1.5, ly - r * 1.5, lx + r * 1.5, ly + r * 1.5], fill=(236, 240, 245))
    d.ellipse([lx - r, ly - r, lx + r, ly + r], fill=(229, 57, 53))
    return img.resize((size, size), Image.LANCZOS)


def rounded_mask(w, h, radius):
    m = Image.new("L", (w * SS, h * SS), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, w * SS - 1, h * SS - 1], radius * SS, fill=255)
    return m.resize((w, h), Image.LANCZOS)


def icon(size):
    bg = gradient(size, size, BLUE, NAVY).convert("RGBA")
    bg.alpha_composite(lens(int(size * .78)), (int(size * .11), int(size * .11)))
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(bg, (0, 0), rounded_mask(size, size, int(size * .22)))
    return out


def banner(w=640, h=360):
    bg = gradient(w, h, NAVY, BLUE).convert("RGBA")
    # alone ciano dietro la lente
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([10, 30, 330, 350], fill=CYAN + (90,))
    bg.alpha_composite(glow.filter(ImageFilter.GaussianBlur(40)))
    ls = 250
    bg.alpha_composite(lens(ls), (45, (h - ls) // 2))
    d = ImageDraw.Draw(bg)
    title = ImageFont.truetype("/System/Library/Fonts/Avenir Next.ttc", 54, index=2)   # Demi Bold
    sub = ImageFont.truetype("/System/Library/Fonts/Avenir Next.ttc", 26, index=5)     # Medium
    d.text((318, 112), "Telecamere", font=title, fill="white")
    d.text((324, 184), "Live · Dahua · RTSP", font=sub, fill=(178, 235, 242))
    return bg


if __name__ == "__main__":
    for folder, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        p = RES / f"mipmap-{folder}"
        p.mkdir(parents=True, exist_ok=True)
        icon(px).save(p / "ic_launcher.png")
    (RES / "drawable-xhdpi").mkdir(parents=True, exist_ok=True)
    banner().save(RES / "drawable-xhdpi/banner.png")
    icon(512).save(ROOT / "art/icon-512.png")
    banner().save(ROOT / "art/banner.png")
    print("icone generate")
