package launcher;

import com.formdev.flatlaf.FlatLaf;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public final class CustomizationManager {
  private static final Preferences PREFS = Preferences.userNodeForPackage(CustomizationManager.class);

  private static final String KEY_ACCENT = "accentColor";
  private static final String KEY_BACKGROUND = "backgroundColor";
  private static final String KEY_CANVAS = "canvasColor";

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
      extra.put("@background", background.trim());
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

  public static void clearAll() {
    try {
      PREFS.remove(KEY_ACCENT);
      PREFS.remove(KEY_BACKGROUND);
      PREFS.remove(KEY_CANVAS);
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
}
