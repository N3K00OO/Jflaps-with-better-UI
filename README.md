# JFLAP 7.1 Better UI (Unofficial)

This repo builds a more modern, themeable UI for the original **JFLAP 7.1** by wrapping it with a small launcher (Swing + FlatLaf) and applying a few runtime/bytecode tweaks for dark-mode readability.

The primary distributables in this folder are:

- `JFLAP7.1.5.1-better-ui.jar` (slim build)
- `JFLAP7.1.5.1-better-ui-fat.jar` (optional, build with `-Fat`)

## Run

```bash
java -jar JFLAP7.1.5.1-better-ui.jar
```

Launcher options:

```bash
java -jar JFLAP7.1.5.1-better-ui.jar --help
```

- `--theme=light|dark|intellij|darcula`
- `--uiScale=<number>` (example: `--uiScale=1.25`)
- Short flags: `--light`, `--dark`, `--intellij`, `--darcula`
- `--selftest` (diagnose crashes / UI creation)
- Also supported: `-Djflap.theme=<theme>` or env var `JFLAP_THEME=<theme>`

## What Changed

- Modern look & feel via **FlatLaf** (defaults to **Darcula**).
- `View > Theme` menu: IntelliJ / Darcula / Light / Dark (saved).
- `View > Customize Theme...`: RGB overrides for:
  - Accent color
  - App background
  - Canvas background (optional checkbox)
  - Selection box color (optional checkbox)
  - Node color (optional checkbox)
  - Arrow color (optional checkbox)
  - Final-state ring color (optional checkbox)
  - Start triangle color (optional checkbox)
  - Tip: you can also paste/type a hex color like `#1e1e1e`
- `Ctrl+K`: Command Palette for quick actions.
- `Ctrl+C` / `Ctrl+V`: canvas copy/paste for selected states/transitions (marquee selection supported, deterministic rename on paste).
- `Delete` / `Backspace`: delete selected states/transitions.
- `Ctrl+Z` / `Ctrl+Y` (or `Ctrl+Shift+Z`): undo/redo on the canvas.
- `Ctrl+Mouse Wheel` or `Ctrl+Plus` / `Ctrl+Minus`: zoom the canvas (auto-zoom is disabled when you manually zoom).
- `Ctrl+Shift+R`: Fast Run shortcut with an optional rerun prompt for another input.
- `File > Export PNG...` / `File > Export SVG...`: export the current automaton using the canvas background color, centered with a tight border.
- Removes the legacy menu-bar close button artifact (extra "X"); use the normal window close button or `File > Close`.
- Fixes a close-confirmation bug where clicking "Cancel" would still close the editor window.
- File dialogs: uses the native OS Open/Save dialog (`java.awt.FileDialog`) when possible (falls back to Swing `JFileChooser`).
- Canvas dark-mode fixes:
  - Transition arrows/labels stay visible on dark canvases.
  - Delete tool cursor is forced to a visible crosshair.
- Toolbar icons are recolored at runtime to match the current theme.
- Pumping Lemma readability fixes:
  - HTML panes stay readable on dark themes (no need to toggle the theme).
  - The "Explain" text area is taller so it doesn't clip as easily at higher UI scale.
- Help pages: ships minimal `DOCS/` stubs so Help doesn't error in the slim build.

## Command Palette

Open it with `Ctrl+K` (or `Cmd+K` on macOS) or via `View > Command Palette...`.

- Type to search actions from the current window's menu bar (shows the full path like `File > Open...`).
- Use Up/Down arrows to select, `Enter` to run, `Esc` to close.

## Changelog

### 2026-02-15 (v7.1.5.1)

- Fixed hex color paste/edit crashes in the theme customization dialog.
- Added node + arrow color overrides in the theme customization dialog.
- Export: tightened image borders and centered automata consistently for PNG/SVG output.
- Added overrides for final-state outer ring color and start triangle color.

### 2026-02-14 (v7.1.5.1)

- Undo/redo now guarded while transition-edit tables are open to prevent crashes.
- Transition edit tables cancel safely if the underlying states are no longer valid (prevents addTransition NPE).
- Fast Run shortcut (`Ctrl+Shift+R`) with a rerun prompt for another input.
- Theme: add a selection box color override.
- Export: add PNG/SVG export that respects the canvas background color.

### 2026-02-13 (v7.1.5)

- Canvas: added zoom controls (Ctrl+wheel, Ctrl+Plus, Ctrl+Minus).
- Canvas: delete selected items with Delete / Backspace.
- Canvas: undo/redo shortcuts (Ctrl+Z / Ctrl+Y / Ctrl+Shift+Z).
- Paste: clears selection after paste so new states are immediately editable.

### 2026-02-11

- Pumping Lemma: fixed dark-mode text + enlarged the "Explain" area.
- Help: added minimal `DOCS/` stubs to avoid missing-doc popups in slim builds.
- Build: slim jar by default; `-Fat` keeps the full reference contents.
- Canvas: added copy/paste on editor canvases (Ctrl+C/Ctrl+V).

## Screenshots

Dark canvas:

![Dark canvas arrow visibility](assets/dark-canvas-arrow.png)

Theme menu (dark):

![Theme menu (dark)](assets/view-menu-theme-dark.png)

Customize Theme dialog (dark):

![Customize Theme dialog (dark)](assets/customize-theme-dialog-dark.png)

Example dialog styling (dark):

![Dialog styling (dark)](assets/new-dialog-dark.png)

View menu:

![View menu (light)](assets/view-menu-light.png)

Customize Theme dialog:

![Customize Theme dialog (light)](assets/customize-theme-dialog-light.png)

Theme menu:

![Theme menu (light)](assets/view-menu-theme-light.png)

Video:

- `assets/demo vid 1.mp4`

## Build

Requirements:

- A JDK on PATH (`javac`, `jar`)
- Internet access on first run (downloads FlatLaf + ASM to `deps/`)

Place the original JFLAP jar in the repo root (for example: `JFLAP7.1.jar`) or pass an explicit path via `-InputJar`.

Slim build:

```powershell
.\build.ps1 -InputJar .\JFLAP7.1.jar -OutputJar JFLAP7.1.5.1-better-ui.jar
```

Fat build (keeps full reference jar contents):

```powershell
.\build.ps1 -InputJar .\JFLAP7.1.jar -OutputJar JFLAP7.1.5.1-better-ui-fat.jar -Fat
```

Notes:

- First run downloads FlatLaf + ASM + Batik + xml-apis into `deps/` (Batik/xml-apis are for SVG export).
- `build.ps1` delegates to `build-modern.ps1` and repackages the input jar plus launcher sources.

## License / Redistribution Notes

Per the **JFLAP 7.1** license, distributing modified copies requires (at minimum):

- Include the JFLAP license text.
- Do **not** charge a fee for any product that includes any part of JFLAP.
- Provide clear contact info for the modifications.
- If the maintainer asks, provide the modified source code without fee.

This build includes:

- JFLAP license text inside the jar as `LICENSE`
- `MODIFICATIONS.txt` (contact: `egasudjali2@gmail.com`)
- Source code for the modifications inside the jar under `launcher-src/` and `tools-src/`
- FlatLaf license inside the jar as `META-INF/LICENSE` (Apache License 2.0)
- Convenience copies of both licenses in `licenses/`

Disclaimer: This is not an official JFLAP release and is not endorsed by the original author/maintainer.
