from PIL import Image, ImageDraw
import os

ZOOM = 0.9
SRC_IMAGE = r"C:\Users\hugo\Desktop\LIT icone MAt.png"

densities = {
    "mdpi":    48,
    "hdpi":    72,
    "xhdpi":   96,
    "xxhdpi":  144,
    "xxxhdpi": 192,
}

adaptive_sizes = {
    "mdpi":    108,
    "hdpi":    162,
    "xhdpi":   216,
    "xxhdpi":  324,
    "xxxhdpi": 432,
}

icon_sets = ["icone", "icone_emp", "icone_mat"]
base = r"F:\LIT_ANDROID\aplicacaoMenuAutomatico\app\src\main\res"
orig = Image.open(SRC_IMAGE).convert("RGBA")
orig_w, orig_h = orig.size

def compor(src, canvas_size, circular=False):
    """Redimensiona mantendo proporção original — preenche a largura com zoom."""
    src_w, src_h = src.size
    # escala pela largura do canvas com zoom aplicado
    target_w = int(canvas_size * ZOOM)
    target_h = int(target_w * src_h / src_w)  # mantém proporção

    fg = src.resize((target_w, target_h), Image.LANCZOS)

    bg = Image.new("RGBA", (canvas_size, canvas_size), (255, 255, 255, 255))
    # centraliza horizontal e verticalmente
    x = (canvas_size - target_w) // 2
    y = (canvas_size - target_h) // 2
    bg.paste(fg, (x, y), fg)

    if circular:
        mask = Image.new("L", (canvas_size, canvas_size), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, canvas_size - 1, canvas_size - 1), fill=255)
        result = Image.new("RGBA", (canvas_size, canvas_size), (255, 255, 255, 255))
        result.paste(bg, (0, 0))
        result.putalpha(mask)
        return result

    return bg.convert("RGB")

for icon in icon_sets:
    for density, size in densities.items():
        folder = os.path.join(base, f"mipmap-{density}")
        adaptive_size = adaptive_sizes[density]

        # _src — salva original na resolução do canvas adaptativo
        orig.save(os.path.join(folder, f"{icon}_foreground_src.png"), "PNG")

        # foreground — adaptive icon (Android 8+)
        fg_result = compor(orig, adaptive_size)
        fg_result.save(os.path.join(folder, f"{icon}_foreground.png"), "PNG")

        # legado (Android < 8)
        compor(orig, size).save(os.path.join(folder, f"{icon}.png"), "PNG")
        compor(orig, size, circular=True).save(os.path.join(folder, f"{icon}_round.png"), "PNG")

    print(f"{icon} OK")
