package launcher;

import java.awt.Color;
import java.lang.reflect.Field;

public final class CanvasColors {
  private CanvasColors() {
  }

  public static Color canvasFillColor() {
    Color canvas = parseHexColor(CustomizationManager.loadCanvasColor());
    if (canvas != null) {
      return canvas;
    }

    return Color.white;
  }

  public static Color canvasLineColor() {
    Color fill = canvasFillColor();
    if (isDark(fill)) {
      return new Color(230, 230, 230);
    }
    return new Color(20, 20, 20);
  }

  public static void applyToJflap() {
    setStaticColor("gui.viewer.CurvedArrow", "ARROW_COLOR", canvasLineColor());
    setStaticColor("gui.editor.TransitionTool", "COLOR", withAlpha(canvasLineColor(), 160));
  }

  private static void setStaticColor(String className, String fieldName, Color color) {
    try {
      Class<?> clazz = Class.forName(className);
      Field field = clazz.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(null, color);
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static Color withAlpha(Color color, int alpha) {
    if (color == null) {
      return null;
    }
    int a = clamp(alpha);
    return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
  }

  private static int clamp(int value) {
    if (value < 0) {
      return 0;
    }
    if (value > 255) {
      return 255;
    }
    return value;
  }

  private static boolean isDark(Color color) {
    if (color == null) {
      return false;
    }
    double luminance = (0.2126 * color.getRed()) + (0.7152 * color.getGreen()) + (0.0722 * color.getBlue());
    return luminance < 128.0;
  }

  private static Color parseHexColor(String value) {
    if (value == null) {
      return null;
    }
    String v = value.trim();
    if (v.isEmpty()) {
      return null;
    }
    try {
      return Color.decode(v);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }
}
