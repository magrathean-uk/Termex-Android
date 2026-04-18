#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFilter, ImageFont
except ImportError as exc:  # pragma: no cover
    raise SystemExit("Pillow is required. Install with: python3 -m pip install pillow") from exc


SCREENSHOT_ALTS = {
    "01-onboarding-welcome.png": "Welcome screen introducing guided SSH setup in Termex.",
    "02-onboarding-review.png": "Review step showing jump host, forwarding, and persistent tmux options.",
    "03-servers-overview.png": "Saved server list with a production workspace and multiple hosts.",
    "04-settings-overview.png": "Settings screen showing security, backup, themes, and terminal options.",
    "05-extra-keys.png": "Extra keys editor with presets and ordered terminal key controls.",
}


def font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    for candidate in (
        "/System/Library/Fonts/Helvetica.ttc",
        "/System/Library/Fonts/Supplemental/Arial.ttf",
    ):
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size)
    return ImageFont.load_default()


def resize_icon(icon_src: Path, out_path: Path) -> None:
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with Image.open(icon_src).convert("RGBA") as icon:
        icon.resize((512, 512), Image.Resampling.LANCZOS).save(out_path, format="PNG")


def rounded_mask(size: tuple[int, int], radius: int) -> Image.Image:
    mask = Image.new("L", size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle((0, 0, size[0], size[1]), radius=radius, fill=255)
    return mask


def add_card(base: Image.Image, screenshot: Path, box: tuple[int, int, int, int]) -> None:
    x, y, w, h = box
    with Image.open(screenshot).convert("RGB") as shot:
        thumb = shot.resize((w, h), Image.Resampling.LANCZOS)
    shadow = Image.new("RGBA", (w + 24, h + 24), (0, 0, 0, 0))
    shadow_draw = ImageDraw.Draw(shadow)
    shadow_draw.rounded_rectangle((12, 12, w + 12, h + 12), radius=28, fill=(8, 19, 40, 120))
    shadow = shadow.filter(ImageFilter.GaussianBlur(10))
    base.alpha_composite(shadow, (x - 12, y - 6))

    card = Image.new("RGBA", (w, h), (255, 255, 255, 0))
    mask = rounded_mask((w, h), 24)
    card.paste(thumb, (0, 0))
    base.paste(card, (x, y), mask)


def build_feature_graphic(phone_dir: Path, out_path: Path) -> None:
    out_path.parent.mkdir(parents=True, exist_ok=True)
    canvas = Image.new("RGBA", (1024, 500), "#06254a")
    draw = ImageDraw.Draw(canvas)
    for y in range(500):
        ratio = y / 499
        r = int(6 + (34 - 6) * ratio)
        g = int(37 + (103 - 37) * ratio)
        b = int(74 + (187 - 74) * ratio)
        draw.line((0, y, 1024, y), fill=(r, g, b, 255))

    accent = Image.new("RGBA", (1024, 500), (0, 0, 0, 0))
    accent_draw = ImageDraw.Draw(accent)
    accent_draw.ellipse((640, -80, 1180, 460), fill=(66, 186, 255, 80))
    accent_draw.ellipse((-120, 320, 380, 760), fill=(11, 85, 255, 70))
    accent = accent.filter(ImageFilter.GaussianBlur(30))
    canvas.alpha_composite(accent)

    screenshots = [
        phone_dir / "02-onboarding-review.png",
        phone_dir / "03-servers-overview.png",
        phone_dir / "05-extra-keys.png",
    ]
    positions = [(460, 56, 150, 332), (635, 30, 170, 378), (826, 70, 150, 332)]
    for screenshot, position in zip(screenshots, positions):
        add_card(canvas, screenshot, position)

    title_font = font(54)
    body_font = font(22)
    small_font = font(18)
    draw = ImageDraw.Draw(canvas)
    draw.text((60, 110), "Termex", fill="white", font=title_font)
    draw.text((60, 180), "SSH client for Android", fill=(232, 241, 255), font=body_font)
    draw.text((60, 224), "Keys, jump hosts, tunnels, tmux restore", fill=(204, 224, 248), font=body_font)

    chip_fill = (15, 46, 94, 210)
    for box, label in (
        ((60, 312, 220, 352), "Android 10+"),
        ((236, 312, 430, 352), "Biometric lock"),
        ((60, 368, 360, 408), "Local, remote, dynamic forwards"),
    ):
        draw.rounded_rectangle(box, radius=18, fill=chip_fill)
        draw.text((box[0] + 18, box[1] + 11), label, fill="white", font=small_font)

    canvas.convert("RGB").save(out_path, format="PNG")


def write_alt_text(phone_dir: Path, out_path: Path) -> None:
    lines = ["# Alt text", ""]
    for name in sorted(SCREENSHOT_ALTS):
        if (phone_dir / name).exists():
            lines.append(f"- `{name}`: {SCREENSHOT_ALTS[name]}")
    lines.append("- `feature-graphic.png`: Blue feature graphic with Termex title and three app screenshots.")
    out_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_manifest(pack_dir: Path) -> None:
    assets = {
        "icon": "assets/icon/play-icon-512.png",
        "featureGraphic": "assets/feature-graphic.png",
        "phoneScreenshots": [f"assets/phone/{name}" for name in sorted(SCREENSHOT_ALTS)],
        "altText": "assets/alt-text.md",
    }
    (pack_dir / "assets-manifest.json").write_text(json.dumps(assets, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", required=True)
    parser.add_argument("--pack-dir", required=True)
    args = parser.parse_args()

    repo_root = Path(args.repo_root)
    pack_dir = Path(args.pack_dir)
    phone_dir = pack_dir / "assets" / "phone"
    icon_src = repo_root / "termex.icon" / "Assets" / "Untitled-1.png"

    resize_icon(icon_src, pack_dir / "assets" / "icon" / "play-icon-512.png")
    build_feature_graphic(phone_dir, pack_dir / "assets" / "feature-graphic.png")
    write_alt_text(phone_dir, pack_dir / "assets" / "alt-text.md")
    write_manifest(pack_dir)


if __name__ == "__main__":
    main()
