package launcher;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.BasicStroke;
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
    Color customArrow = parseHexColor(CustomizationManager.loadArrowColor());
    if (customArrow != null) {
      return customArrow;
    }
    Color fill = canvasFillColor();
    if (isDark(fill)) {
      return new Color(230, 230, 230);
    }
    return new Color(20, 20, 20);
  }

  public static Color selectionBoxColor() {
    Color custom = parseHexColor(CustomizationManager.loadSelectionBoxColor());
    if (custom != null) {
      return custom;
    }

    Color fill = canvasFillColor();
    if (isDark(fill)) {
      return new Color(0, 200, 255);
    }
    return new Color(0, 90, 180);
  }

  public static Color finalRingColor() {
    Color custom = parseHexColor(CustomizationManager.loadFinalRingColor());
    if (custom != null) {
      return custom;
    }
    return canvasLineColor();
  }

  public static Color startTriangleColor() {
    Color custom = parseHexColor(CustomizationManager.loadStartTriangleColor());
    if (custom != null) {
      return custom;
    }
    return canvasLineColor();
  }

  public static Color startTriangleFillColor() {
    // Fill the triangle with the same color as its outline for better visibility.
    return startTriangleColor();
  }

  public static Color nodeColor() {
    Color custom = parseHexColor(CustomizationManager.loadNodeColor());
    if (custom != null) {
      return custom;
    }
    Color fallback = readStaticColor("gui.viewer.StateDrawer", "STATE_COLOR", new Color(255, 255, 150));
    return fallback;
  }

  public static Color selectedNodeColor() {
    Color base = nodeColor();
    Color fallback = readStaticColor("gui.viewer.StateDrawer", "HIGHLIGHT_COLOR", new Color(100, 200, 200));
    if (base == null) {
      return fallback;
    }
    if (isDark(base)) {
      return blend(base, Color.WHITE, 0.45f);
    }
    return blend(base, Color.BLACK, 0.25f);
  }

  public static void applyToJflap() {
    Color arrow = canvasLineColor();
    setStaticColor("gui.viewer.CurvedArrow", "ARROW_COLOR", arrow);
    setStaticColor("gui.editor.TransitionTool", "COLOR", withAlpha(arrow, 160));

    Color node = nodeColor();
    setStaticColor("gui.viewer.StateDrawer", "STATE_COLOR", node);
    setStaticColor("gui.viewer.StateDrawer", "HIGHLIGHT_COLOR", selectedNodeColor());
    setStaticColor("gui.viewer.SelectionDrawer", "SELECTED_COLOR", selectedNodeColor());
  }

  public static void drawFinalStateMarkerOval(java.awt.Graphics g, int x, int y, int w, int h) {
    if (g == null) {
      return;
    }
    try {
      g.setColor(finalRingColor());
    } catch (Throwable ignored) {
      // ignore
    }
    if (g instanceof Graphics2D) {
      Graphics2D g2 = (Graphics2D) g;
      Stroke oldStroke = g2.getStroke();
      try {
        // Slightly bolder than default for better final-state emphasis.
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawOval(x, y, w, h);
      } finally {
        g2.setStroke(oldStroke);
      }
      return;
    }
    g.drawOval(x, y, w, h);
  }

  public static void fillStartMarkerTriangle(java.awt.Graphics g, int[] xPoints, int[] yPoints, int nPoints) {
    if (g == null) {
      return;
    }
    try {
      g.setColor(startTriangleFillColor());
    } catch (Throwable ignored) {
      // ignore
    }
    g.fillPolygon(xPoints, yPoints, nPoints);
  }

  public static void drawStartMarkerTriangle(java.awt.Graphics g, int[] xPoints, int[] yPoints, int nPoints) {
    if (g == null) {
      return;
    }
    try {
      g.setColor(startTriangleColor());
    } catch (Throwable ignored) {
      // ignore
    }
    g.drawPolygon(xPoints, yPoints, nPoints);
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

  private static Color readStaticColor(String className, String fieldName, Color fallback) {
    try {
      Class<?> clazz = Class.forName(className);
      Field field = clazz.getDeclaredField(fieldName);
      field.setAccessible(true);
      Object value = field.get(null);
      if (value instanceof Color) {
        return (Color) value;
      }
    } catch (Throwable ignored) {
      // best-effort only
    }
    return fallback;
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
