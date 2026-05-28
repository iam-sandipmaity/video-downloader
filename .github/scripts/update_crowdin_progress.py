import json
import os
import hashlib
import re
import urllib.parse
import urllib.request
from html import escape


BADGE_PATH = ".github/badges/crowdin.json"
SVG_PATH = ".github/badges/crowdin-progress.svg"
README_PATH = "README.md"


def as_number(value):
    return value if isinstance(value, (int, float)) else None


def progress_color(progress):
    if progress >= 90:
        return "#1f9d7a"
    if progress >= 70:
        return "#2775c9"
    if progress >= 40:
        return "#df5a32"
    return "#a73c3c"


def badge_color(progress):
    if progress >= 90:
        return "brightgreen"
    if progress >= 75:
        return "green"
    if progress >= 50:
        return "yellow"
    if progress >= 25:
        return "orange"
    return "red"


def compact_count(total, unit):
    if total is None:
        return ""
    if total >= 1000:
        value = total / 1000
        return f"{value:.1f}k {unit}" if total % 1000 else f"{int(value)}k {unit}"
    return f"{int(total)} {unit}"


def fetch_crowdin_progress():
    project_id = os.environ["CROWDIN_PROJECT_ID"]
    token = os.environ["CROWDIN_PERSONAL_TOKEN"]
    query = urllib.parse.urlencode({"limit": 500})
    url = f"https://api.crowdin.com/api/v2/projects/{project_id}/languages/progress?{query}"
    request = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/json",
        },
    )

    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def parse_progress_items(payload):
    progress_items = []
    for item in payload.get("data", []):
        data = item.get("data", item)
        progress = as_number(data.get("translationProgress"))
        if progress is None:
            continue

        language = data.get("language") or {}
        words = data.get("words") or {}
        phrases = data.get("phrases") or {}
        total_words = as_number(words.get("total"))
        translated_words = as_number(words.get("translated"))
        total_phrases = as_number(phrases.get("total"))
        words_label = (
            compact_count(total_words, "words")
            if total_words is not None
            else compact_count(total_phrases, "strings")
        )

        progress_items.append(
            {
                "language": language.get("name") or data.get("languageId") or "Unknown",
                "progress": float(progress),
                "approval": float(as_number(data.get("approvalProgress")) or 0),
                "total_words": total_words,
                "translated_words": translated_words,
                "words_label": words_label,
            }
        )

    if not progress_items:
        raise RuntimeError("Crowdin returned no language progress entries")

    return sorted(progress_items, key=lambda item: item["language"].casefold())


def overall_progress(progress_items):
    weighted_total = sum(item["total_words"] for item in progress_items if item["total_words"] is not None)
    weighted_translated = sum(
        item["translated_words"]
        for item in progress_items
        if item["translated_words"] is not None and item["total_words"] is not None
    )
    if weighted_total > 0 and weighted_translated >= 0:
        return (weighted_translated / weighted_total) * 100
    return sum(item["progress"] for item in progress_items) / len(progress_items)


def write_badge(progress):
    rounded = round(progress, 1)
    badge = {
        "schemaVersion": 1,
        "label": "translations",
        "message": f"{rounded:.1f}%",
        "color": badge_color(rounded),
        "namedLogo": "crowdin",
        "logoColor": "white",
    }

    os.makedirs(os.path.dirname(BADGE_PATH), exist_ok=True)
    with open(BADGE_PATH, "w", encoding="utf-8") as output:
        json.dump(badge, output, indent=2)
        output.write("\n")


def svg_text(x, y, text, size=14, fill="#cbd5e1", anchor="start", weight=400):
    return (
        f'<text x="{x}" y="{y}" font-family="Inter,Segoe UI,Arial,sans-serif" '
        f'font-size="{size}" font-weight="{weight}" fill="{fill}" text-anchor="{anchor}">'
        f"{escape(text)}</text>"
    )


def write_progress_svg(progress_items, progress):
    width = 760
    row_height = 34
    top = 86
    height = top + (len(progress_items) * row_height) + 38
    bar_x = 300
    bar_width = 260
    bar_height = 8

    lines = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" role="img" aria-label="Crowdin translation progress">',
        "<title>Crowdin translation progress</title>",
        f'<rect width="{width}" height="{height}" rx="14" fill="#0f141b"/>',
        '<rect x="0" y="0" width="760" height="58" fill="#142a2a"/>',
        '<path d="M20 0 78 58M112 0l58 58M204 0l58 58M650 0l58 58" stroke="#1f6f66" stroke-width="8" opacity=".28"/>',
        svg_text(28, 36, "Crowdin Translation Progress", 20, "#f8fafc", weight=700),
        svg_text(732, 36, f"{progress:.1f}%", 18, "#f8fafc", anchor="end", weight=700),
        svg_text(28, 72, "Language", 12, "#94a3b8", weight=700),
        svg_text(bar_x, 72, "Translated", 12, "#94a3b8", weight=700),
        svg_text(628, 72, "Approved", 12, "#94a3b8", anchor="end", weight=700),
        svg_text(728, 72, "Words", 12, "#94a3b8", anchor="end", weight=700),
    ]

    for index, item in enumerate(progress_items):
        y = top + (index * row_height)
        progress_value = max(0, min(item["progress"], 100))
        approval_value = max(0, min(item["approval"], 100))
        fill_width = round((progress_value / 100) * bar_width, 2)
        approval_width = round((approval_value / 100) * bar_width, 2)
        color = progress_color(progress_value)

        if index % 2 == 1:
            lines.append(f'<rect x="16" y="{y - 18}" width="728" height="28" rx="6" fill="#111923"/>')

        lines.extend(
            [
                svg_text(28, y, item["language"], 14, "#dbeafe"),
                f'<rect x="{bar_x}" y="{y - 10}" width="{bar_width}" height="{bar_height}" rx="4" fill="#38424f"/>',
                f'<rect x="{bar_x}" y="{y - 10}" width="{fill_width}" height="{bar_height}" rx="4" fill="{color}"/>',
                f'<rect x="{bar_x}" y="{y + 2}" width="{bar_width}" height="3" rx="1.5" fill="#25303b"/>',
                f'<rect x="{bar_x}" y="{y + 2}" width="{approval_width}" height="3" rx="1.5" fill="#6ee7b7" opacity=".85"/>',
                svg_text(612, y, f"{progress_value:.0f}% / {approval_value:.0f}%", 13, "#cbd5e1", anchor="end", weight=700),
                svg_text(728, y, item["words_label"], 13, "#cbd5e1", anchor="end"),
            ]
        )

    lines.append("</svg>")

    os.makedirs(os.path.dirname(SVG_PATH), exist_ok=True)
    with open(SVG_PATH, "w", encoding="utf-8") as output:
        output.write("\n".join(lines))
        output.write("\n")


def asset_cache_key():
    digest = hashlib.sha256()
    for path in (BADGE_PATH, SVG_PATH):
        with open(path, "rb") as input_file:
            digest.update(input_file.read())
    return digest.hexdigest()[:12]


def update_readme_cache_keys(cache_key):
    badge_json_url = (
        "https://raw.githubusercontent.com/iam-sandipmaity/video-downloader/"
        f"main/.github/badges/crowdin.json?v={cache_key}"
    )
    badge_url = (
        "https://img.shields.io/endpoint?"
        f"url={urllib.parse.quote(badge_json_url, safe='')}&cacheSeconds=300"
    )
    progress_url = (
        "https://raw.githubusercontent.com/iam-sandipmaity/video-downloader/"
        f"main/.github/badges/crowdin-progress.svg?v={cache_key}"
    )

    with open(README_PATH, "r", encoding="utf-8") as input_file:
        readme = input_file.read()

    readme = re.sub(
        r"https://img\.shields\.io/endpoint\?url=[^\"]*crowdin\.json[^\"]*",
        badge_url,
        readme,
        count=1,
    )
    readme = re.sub(
        r"(?:\.github/badges/crowdin-progress\.svg|https://raw\.githubusercontent\.com/iam-sandipmaity/video-downloader/main/\.github/badges/crowdin-progress\.svg[^\"]*)",
        progress_url,
        readme,
        count=1,
    )

    with open(README_PATH, "w", encoding="utf-8") as output:
        output.write(readme)


def main():
    payload = fetch_crowdin_progress()
    progress_items = parse_progress_items(payload)
    progress = overall_progress(progress_items)
    write_badge(progress)
    write_progress_svg(progress_items, progress)
    update_readme_cache_keys(asset_cache_key())


if __name__ == "__main__":
    main()
