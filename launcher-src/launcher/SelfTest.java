package launcher;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import javax.swing.JEditorPane;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal smoke tests that can be run on a user's machine to diagnose crashes.
 *
 * <p>Run with: {@code java -jar JFLAP7.1-modern.jar --selftest}</p>
 */
public final class SelfTest {
  private SelfTest() {
  }

  public static int run() {
    System.out.println("[JFLAP Modern] Self-test starting...");

    final List<String> failures = new ArrayList<>();

    try {
      runOnEdt(new Runnable() {
        @Override
        public void run() {
          try {
            testReplacementClasses(failures);
            if (!GraphicsEnvironment.isHeadless()) {
              testPumpingLemmaChooserHtmlContrast(failures);
              testBasicUiDefaultsContrast(failures);
            } else {
              System.out.println("[JFLAP Modern] Headless environment detected; skipping UI window tests.");
            }
          } catch (Throwable t) {
            failures.add("Unexpected exception during self-test: " + t);
            t.printStackTrace(System.err);
          }
        }
      });
    } catch (Throwable t) {
      failures.add("Failed to run self-test on EDT: " + t);
      t.printStackTrace(System.err);
    }

    if (!failures.isEmpty()) {
      System.err.println("[JFLAP Modern] Self-test FAILED:");
      for (int i = 0; i < failures.size(); i++) {
        System.err.println("  - " + failures.get(i));
      }
      return 1;
    }

    System.out.println("[JFLAP Modern] Self-test OK.");
    return 0;
  }

  private static void runOnEdt(Runnable r) throws Exception {
    if (SwingUtilities.isEventDispatchThread()) {
      r.run();
      return;
    }
    SwingUtilities.invokeAndWait(r);
  }

  private static void testReplacementClasses(List<String> failures) {
    // Ensure that our replacement popup class matches the expected constructor signature.
    try {
      Class<?> arrowTool = Class.forName("gui.editor.ArrowTool");
      Class<?> emptyMenu = Class.forName("gui.editor.ArrowTool$EmptyMenu");
      Constructor<?> ctor = emptyMenu.getDeclaredConstructor(arrowTool);
      if (ctor == null) {
        failures.add("ArrowTool$EmptyMenu missing expected constructor.");
      }
    } catch (Throwable t) {
      failures.add("ArrowTool$EmptyMenu replacement check failed: " + t);
    }
  }

  private static void testPumpingLemmaChooserHtmlContrast(List<String> failures) {
    Theme originalTheme = ThemeManager.getCurrentTheme();
    String originalBackground = CustomizationManager.loadBackgroundColor();

    try {
      // 1) Standard dark theme
      CustomizationManager.saveBackgroundColor(null);
      ThemeManager.applyInitialTheme(Theme.DARK);
      checkPumpingLemmaChooserReadable("Theme=DARK", failures);

      // 2) Background override "dark mode" even on light theme
      CustomizationManager.saveBackgroundColor("#000000");
      ThemeManager.applyInitialTheme(Theme.LIGHT);
      checkPumpingLemmaChooserReadable("Theme=LIGHT + background=#000000", failures);
    } catch (Throwable t) {
      failures.add("Pumping lemma chooser UI contrast test failed: " + t);
      t.printStackTrace(System.err);
    } finally {
      try {
        CustomizationManager.saveBackgroundColor(originalBackground);
      } catch (Throwable ignored) {
        // ignore
      }
      try {
        ThemeManager.applyInitialTheme(originalTheme);
      } catch (Throwable ignored) {
        // ignore
      }
    }
  }

  private static void testBasicUiDefaultsContrast(List<String> failures) {
    Theme originalTheme = ThemeManager.getCurrentTheme();
    String originalBackground = CustomizationManager.loadBackgroundColor();

    try {
      CustomizationManager.saveBackgroundColor(null);
      ThemeManager.applyInitialTheme(Theme.DARK);
      checkUiDefaultsReadable("Theme=DARK", failures);

      CustomizationManager.saveBackgroundColor("#000000");
      ThemeManager.applyInitialTheme(Theme.LIGHT);
      checkUiDefaultsReadable("Theme=LIGHT + background=#000000", failures);
    } catch (Throwable t) {
      failures.add("Basic UI defaults contrast test failed: " + t);
      t.printStackTrace(System.err);
    } finally {
      try {
        CustomizationManager.saveBackgroundColor(originalBackground);
      } catch (Throwable ignored) {
        // ignore
      }
      try {
        ThemeManager.applyInitialTheme(originalTheme);
      } catch (Throwable ignored) {
        // ignore
      }
    }
  }

  private static void checkUiDefaultsReadable(String scenario, List<String> failures) {
    Color panelBg = javax.swing.UIManager.getColor("Panel.background");
    if (panelBg == null) {
      panelBg = Color.BLACK;
    }

    Color buttonBg = javax.swing.UIManager.getColor("Button.background");
    if (buttonBg == null) {
      buttonBg = panelBg;
    }

    Color buttonFg = javax.swing.UIManager.getColor("Button.foreground");
    if (buttonFg == null) {
      buttonFg = javax.swing.UIManager.getColor("Label.foreground");
    }
    if (buttonFg == null) {
      buttonFg = Color.WHITE;
    }

    double textContrast = contrastRatio(buttonFg, buttonBg);
    if (textContrast < 3.0) {
      failures.add(scenario + ": Low-contrast Button.foreground vs Button.background (" + format(textContrast) + ") fg="
        + hex(buttonFg) + " bg=" + hex(buttonBg));
    }

    double buttonVsPanel = contrastRatio(buttonBg, panelBg);
    if (buttonVsPanel < 1.10) {
      failures.add(scenario + ": Button.background too close to Panel.background (" + format(buttonVsPanel) + ") buttonBg="
        + hex(buttonBg) + " panelBg=" + hex(panelBg));
    }

    System.out.println("[JFLAP Modern] UI defaults OK (" + scenario + "): textContrast=" + format(textContrast)
      + " buttonVsPanel=" + format(buttonVsPanel));
  }

  private static void checkPumpingLemmaChooserReadable(String scenario, List<String> failures) throws Exception {
    try {
      Container pane = createRegPumpingLemmaChooserPane();
      if (pane == null) {
        failures.add(scenario + ": Failed to create PumpingLemmaChooserPane.");
        return;
      }

      // Apply our runtime readability fix directly (no window required).
      UiEnhancements.applyReadabilityFixes(pane);

      List<JEditorPane> panes = findHtmlDisabledEditorPanes(pane);
      if (panes.isEmpty()) {
        failures.add(scenario + ": No disabled HTML editor panes found.");
        return;
      }

      int bad = 0;
      for (int i = 0; i < panes.size(); i++) {
        JEditorPane ep = panes.get(i);
        Color bg = ep.getBackground();
        if (bg == null) {
          bg = (ep.getParent() == null) ? null : ep.getParent().getBackground();
        }
        if (bg == null) {
          bg = javax.swing.UIManager.getColor("Panel.background");
        }
        if (bg == null) {
          bg = Color.BLACK;
        }
        Color fg = ep.getForeground();
        if (fg == null) {
          fg = ep.getDisabledTextColor();
        }

        double contrast = contrastRatio(fg, bg);
        if (contrast < 3.0) {
          bad++;
          failures.add(scenario + ": Low-contrast pumping lemma text (" + format(contrast) + "): fg=" + hex(fg) + " bg=" + hex(bg));
        }
      }

      if (bad == 0) {
        System.out.println("[JFLAP Modern] Pumping Lemma chooser contrast OK (" + scenario + "), panes=" + panes.size());
      }
    } finally {
      // nothing to dispose
    }
  }

  private static Container createRegPumpingLemmaChooserPane() throws Exception {
    Class<?> regChooserClass = Class.forName("gui.pumping.RegPumpingLemmaChooser");
    Object chooser = regChooserClass.getDeclaredConstructor().newInstance();

    Class<?> chooserBase = Class.forName("gui.pumping.PumpingLemmaChooser");
    Class<?> envBase = Class.forName("gui.environment.Environment");
    Class<?> envClass = Class.forName("gui.environment.PumpingLemmaEnvironment");

    Object env = envClass.getDeclaredConstructor(chooserBase).newInstance(chooser);

    Class<?> paneClass = Class.forName("gui.pumping.PumpingLemmaChooserPane");
    Object pane = paneClass.getDeclaredConstructor(chooserBase, envBase).newInstance(chooser, env);
    return (pane instanceof Container) ? (Container) pane : null;
  }

  private static List<JEditorPane> findHtmlDisabledEditorPanes(Container root) {
    List<JEditorPane> panes = new ArrayList<>();
    if (root == null) {
      return panes;
    }

    java.util.ArrayDeque<Component> q = new java.util.ArrayDeque<>();
    q.add(root);

    while (!q.isEmpty()) {
      Component c = q.remove();
      if (c instanceof JEditorPane) {
        JEditorPane ep = (JEditorPane) c;
        String ct = ep.getContentType();
        if (ct != null && ct.toLowerCase().contains("text/html") && !ep.isEnabled()) {
          panes.add(ep);
        }
      }

      if (c instanceof Container) {
        Component[] children;
        try {
          children = ((Container) c).getComponents();
        } catch (Throwable ignored) {
          children = null;
        }
        if (children != null) {
          for (int i = 0; i < children.length; i++) {
            if (children[i] != null) {
              q.add(children[i]);
            }
          }
        }
      }
    }

    return panes;
  }

  // WCAG-ish contrast ratio.
  private static double contrastRatio(Color a, Color b) {
    if (a == null || b == null) {
      return 1.0;
    }
    double la = relativeLuminance(a);
    double lb = relativeLuminance(b);
    double light = Math.max(la, lb);
    double dark = Math.min(la, lb);
    return (light + 0.05) / (dark + 0.05);
  }

  private static double relativeLuminance(Color c) {
    double r = srgbToLinear(c.getRed() / 255.0);
    double g = srgbToLinear(c.getGreen() / 255.0);
    double b = srgbToLinear(c.getBlue() / 255.0);
    return 0.2126 * r + 0.7152 * g + 0.0722 * b;
  }

  private static double srgbToLinear(double v) {
    if (v <= 0.04045) {
      return v / 12.92;
    }
    return Math.pow((v + 0.055) / 1.055, 2.4);
  }

  private static String safeClassName(Object o) {
    try {
      return (o == null) ? "null" : o.getClass().getName();
    } catch (Throwable ignored) {
      return "(unknown)";
    }
  }

  private static String safeText(String t) {
    return (t == null) ? "" : t.trim();
  }

  private static String hex(Color c) {
    if (c == null) {
      return "null";
    }
    return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
  }

  private static String format(double v) {
    return String.format(java.util.Locale.ROOT, "%.2f", v);
  }
}
