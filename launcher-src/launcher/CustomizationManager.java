package launcher;

import com.formdev.flatlaf.FlatLaf;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public final class CustomizationManager {
  private static final Preferences PREFS = Preferences.userNodeForPackage(CustomizationManager.class);

  private static final String KEY_ACCENT = "accentColor";
  private static final String KEY_BACKGROUND = "backgroundColor";
  private static final String KEY_CANVAS = "canvasColor";
  private static final String KEY_SELECTION_BOX = "selectionBoxColor";
  private static final String KEY_NODE = "nodeColor";
  private static final String KEY_ARROW = "arrowColor";
  private static final String KEY_FINAL_RING = "finalRingColor";
  private static final String KEY_START_TRIANGLE = "startTriangleColor";

  private CustomizationManager() {
  }

  public static Map<String, String> loadExtraDefaults() {
    Map<String, String> extra = new LinkedHashMap<>();

    // Subtle rounding to make Swing components feel more modern.
    extra.put("Button.arc", "10");
    extra.put("Component.arc", "10");
    extra.put("TextComponent.arc", "8");
    extra.put("ScrollPane.arc", "8");
    extra.put("ToolBar.hoverButtonGroupArc", "10");

    String accent = get(KEY_ACCENT);
    if (!isBlank(accent)) {
      extra.put("@accentColor", accent.trim());
    }

    String background = get(KEY_BACKGROUND);
    if (!isBlank(background)) {
      String bgHex = normalizeHexColor(background);
      extra.put("@background", bgHex);

      // If the user overrides only the background color (especially to very dark/very light),
      // built-in themes can end up with low-contrast text because @foreground remains unchanged.
      // Keep UI readable by deriving a matching foreground and a few core component colors.
      Color bg = parseHexColor(bgHex);
      if (bg != null) {
        boolean dark = isDark(bg);
        Color fg = dark ? new Color(0xEDEDED) : new Color(0x202020);
        Color disabledFg = dark ? new Color(0xA0A0A0) : new Color(0x808080);

        extra.put("@foreground", toHex(fg));

        // Use explicit component colors for robustness (some components don't exclusively rely on @foreground).
        extra.put("Label.foreground", toHex(fg));
        extra.put("Button.foreground", toHex(fg));
        extra.put("Menu.foreground", toHex(fg));
        extra.put("MenuItem.foreground", toHex(fg));
        extra.put("CheckBox.foreground", toHex(fg));
        extra.put("RadioButton.foreground", toHex(fg));
        extra.put("TextField.foreground", toHex(fg));
        extra.put("TextArea.foreground", toHex(fg));
        extra.put("TextPane.foreground", toHex(fg));
        extra.put("FormattedTextField.foreground", toHex(fg));

        extra.put("Label.disabledForeground", toHex(disabledFg));
        extra.put("Button.disabledText", toHex(disabledFg));
        extra.put("Menu.disabledForeground", toHex(disabledFg));
        extra.put("MenuItem.disabledForeground", toHex(disabledFg));

        // Ensure buttons remain visually distinct from the window background.
        Color buttonBg = dark ? blend(bg, Color.WHITE, 0.12f) : blend(bg, Color.BLACK, 0.06f);
        Color buttonHoverBg = dark ? blend(bg, Color.WHITE, 0.18f) : blend(bg, Color.BLACK, 0.10f);
        Color buttonPressedBg = dark ? blend(bg, Color.WHITE, 0.08f) : blend(bg, Color.BLACK, 0.14f);

        extra.put("Button.background", toHex(buttonBg));
        extra.put("Button.hoverBackground", toHex(buttonHoverBg));
        extra.put("Button.pressedBackground", toHex(buttonPressedBg));
      }
    }

    return extra;
  }

  public static void applyExtraDefaultsToFlatLaf() {
    try {
      FlatLaf.setGlobalExtraDefaults(loadExtraDefaults());
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  public static String loadAccentColor() {
    return get(KEY_ACCENT);
  }

  public static String loadBackgroundColor() {
    return get(KEY_BACKGROUND);
  }

  public static void saveAccentColor(String color) {
    setOrRemove(KEY_ACCENT, normalizeHexColor(color));
  }

  public static void saveBackgroundColor(String color) {
    setOrRemove(KEY_BACKGROUND, normalizeHexColor(color));
  }

  public static String loadCanvasColor() {
    return get(KEY_CANVAS);
  }

  public static void saveCanvasColor(String color) {
    setOrRemove(KEY_CANVAS, normalizeHexColor(color));
  }

  public static String loadSelectionBoxColor() {
    return get(KEY_SELECTION_BOX);
  }

  public static void saveSelectionBoxColor(String color) {
    setOrRemove(KEY_SELECTION_BOX, normalizeHexColor(color));
  }

  public static String loadNodeColor() {
    return get(KEY_NODE);
  }

  public static void saveNodeColor(String color) {
    setOrRemove(KEY_NODE, normalizeHexColor(color));
  }

  public static String loadArrowColor() {
    return get(KEY_ARROW);
  }

  public static void saveArrowColor(String color) {
    setOrRemove(KEY_ARROW, normalizeHexColor(color));
  }

  public static String loadFinalRingColor() {
    return get(KEY_FINAL_RING);
  }

  public static void saveFinalRingColor(String color) {
    setOrRemove(KEY_FINAL_RING, normalizeHexColor(color));
  }

  public static String loadStartTriangleColor() {
    return get(KEY_START_TRIANGLE);
  }

  public static void saveStartTriangleColor(String color) {
    setOrRemove(KEY_START_TRIANGLE, normalizeHexColor(color));
  }

  public static void clearAll() {
    try {
      PREFS.remove(KEY_ACCENT);
      PREFS.remove(KEY_BACKGROUND);
      PREFS.remove(KEY_CANVAS);
      PREFS.remove(KEY_SELECTION_BOX);
      PREFS.remove(KEY_NODE);
      PREFS.remove(KEY_ARROW);
      PREFS.remove(KEY_FINAL_RING);
      PREFS.remove(KEY_START_TRIANGLE);
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static String get(String key) {
    try {
      return PREFS.get(key, null);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static void setOrRemove(String key, String value) {
    try {
      if (isBlank(value)) {
        PREFS.remove(key);
      } else {
        PREFS.put(key, value.trim());
      }
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private static String normalizeHexColor(String value) {
    if (isBlank(value)) {
      return null;
    }

    String trimmed = value.trim();
    if (trimmed.startsWith("#")) {
      return trimmed;
    }
    return "#" + trimmed;
  }

  private static Color parseHexColor(String value) {
    if (isBlank(value)) {
      return null;
    }
    String v = value.trim();
    try {
      return Color.decode(v);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private static boolean isDark(Color c) {
    if (c == null) {
      return false;
    }
    double r = c.getRed() / 255.0;
    double g = c.getGreen() / 255.0;
    double b = c.getBlue() / 255.0;
    // Relative luminance (rough sRGB).
    double lum = 0.2126 * r + 0.7152 * g + 0.0722 * b;
    return lum < 0.5;
  }

  private static Color blend(Color base, Color over, float overAlpha) {
    if (base == null) {
      return over;
    }
    if (over == null) {
      return base;
    }
    float a = Math.max(0f, Math.min(1f, overAlpha));
    int r = clamp(Math.round(base.getRed() * (1f - a) + over.getRed() * a));
    int g = clamp(Math.round(base.getGreen() * (1f - a) + over.getGreen() * a));
    int b = clamp(Math.round(base.getBlue() * (1f - a) + over.getBlue() * a));
    return new Color(r, g, b);
  }

  private static int clamp(int v) {
    if (v < 0) {
      return 0;
    }
    if (v > 255) {
      return 255;
    }
    return v;
  }

  private static String toHex(Color c) {
    if (c == null) {
      return null;
    }
    return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
  }
}
