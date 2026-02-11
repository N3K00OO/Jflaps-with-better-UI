package launcher;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Map;

public final class AssetThemer {
  private static final String ORIG_ICON_KEY = "launcher.modern.origIcon";
  private static final String DELETE_HOOK_KEY = "launcher.modern.deleteCursorHooked";
  private static volatile Cursor deleteToolCursor;

  private AssetThemer() {
  }

  public static void applyToWindow(Window window) {
    if (window == null) {
      return;
    }

    if (SwingUtilities.isEventDispatchThread()) {
      applyToComponentTree(window);
    } else {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          applyToComponentTree(window);
        }
      });
    }
  }

  private static void applyToComponentTree(Component component) {
    if (component instanceof JToolBar) {
      applyToToolBar((JToolBar) component);
    }

    if (!(component instanceof Container)) {
      return;
    }

    Component[] children = ((Container) component).getComponents();
    for (int i = 0; i < children.length; i++) {
      Component child = children[i];
      if (child != null) {
        applyToComponentTree(child);
      }
    }
  }

  private static void applyToToolBar(JToolBar toolBar) {
    Component[] components = toolBar.getComponents();
    for (int i = 0; i < components.length; i++) {
      Component c = components[i];
      if (c instanceof AbstractButton) {
        styleToolBarButton((AbstractButton) c);
      }
    }

    if ("gui.editor.ToolBar".equals(toolBar.getClass().getName())) {
      hookDeleteCursor(toolBar);
    }
  }

  private static void styleToolBarButton(AbstractButton button) {
    if (button == null) {
      return;
    }

    try {
      button.putClientProperty("JButton.buttonType", "toolBarButton");
    } catch (Throwable ignored) {
      // best-effort only
    }

    Icon original = (Icon) button.getClientProperty(ORIG_ICON_KEY);
    if (original == null) {
      original = button.getIcon();
      button.putClientProperty(ORIG_ICON_KEY, original);
    }

    Icon themed = createThemedIcon(original);
    if (themed != null) {
      button.setIcon(themed);
    }
  }

  private static void hookDeleteCursor(JToolBar toolBar) {
    try {
      Field buttonsToToolsField = toolBar.getClass().getDeclaredField("buttonsToTools");
      buttonsToToolsField.setAccessible(true);
      Object mapObj = buttonsToToolsField.get(toolBar);
      if (!(mapObj instanceof Map)) {
        return;
      }

      Field viewField = toolBar.getClass().getDeclaredField("view");
      viewField.setAccessible(true);
      final Component view = (Component) viewField.get(toolBar);
      if (view == null) {
        return;
      }

      @SuppressWarnings("unchecked")
      Map<Object, Object> map = (Map<Object, Object>) mapObj;

      for (Map.Entry<Object, Object> entry : map.entrySet()) {
        Object key = entry.getKey();
        Object tool = entry.getValue();
        if (!(key instanceof AbstractButton)) {
          continue;
        }
        if (tool == null || !"gui.editor.DeleteTool".equals(tool.getClass().getName())) {
          continue;
        }

        final AbstractButton button = (AbstractButton) key;
        if (Boolean.TRUE.equals(button.getClientProperty(DELETE_HOOK_KEY))) {
          continue;
        }
        button.putClientProperty(DELETE_HOOK_KEY, Boolean.TRUE);

        installDeleteCursorEnforcer(button, view);

        button.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (!button.isSelected()) {
              return;
            }

            // ToolBar sets its own (often hard-to-see) delete cursor inside its actionPerformed.
            // Override it after ToolBar runs, and keep overriding on mouse move.
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                applyDeleteCursorIfSelected(button, view);
              }
            });
          }
        });
      }
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static void installDeleteCursorEnforcer(final AbstractButton deleteButton, final Component view) {
    if (deleteButton == null || view == null) {
      return;
    }

    try {
      // Re-apply the cursor on mouse activity because some components reset cursors on enter/move.
      view.addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
          applyDeleteCursorIfSelected(deleteButton, view);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
          applyDeleteCursorIfSelected(deleteButton, view);
        }
      });

      view.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
          applyDeleteCursorIfSelected(deleteButton, view);
        }
      });
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static void applyDeleteCursorIfSelected(AbstractButton deleteButton, Component view) {
    if (deleteButton == null || view == null) {
      return;
    }
    try {
      if (!deleteButton.isSelected()) {
        return;
      }
    } catch (Throwable ignored) {
      return;
    }

    Cursor c = highContrastCrosshairCursor();
    if (c == null) {
      c = Cursor.getDefaultCursor();
    }

    try {
      view.setCursor(c);
    } catch (Throwable ignored) {
      // ignore
    }
  }

  private static Cursor highContrastCrosshairCursor() {
    if (deleteToolCursor != null) {
      return deleteToolCursor;
    }

    try {
      java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
      java.awt.Dimension best = toolkit.getBestCursorSize(32, 32);
      int w = (best == null || best.width <= 0) ? 32 : best.width;
      int h = (best == null || best.height <= 0) ? 32 : best.height;

      BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = image.createGraphics();
      try {
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_STROKE_CONTROL, java.awt.RenderingHints.VALUE_STROKE_PURE);

        int cx = w / 2;
        int cy = h / 2;

        // Draw a thick black outline, then a white crosshair, then a red center dot.
        g.setStroke(new java.awt.BasicStroke(3f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0, 0, 0, 230));
        g.drawLine(cx - 11, cy, cx + 11, cy);
        g.drawLine(cx, cy - 11, cx, cy + 11);
        g.drawOval(cx - 5, cy - 5, 10, 10);

        g.setStroke(new java.awt.BasicStroke(1.6f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 255, 255, 255));
        g.drawLine(cx - 10, cy, cx + 10, cy);
        g.drawLine(cx, cy - 10, cx, cy + 10);
        g.drawOval(cx - 4, cy - 4, 8, 8);

        g.setStroke(new java.awt.BasicStroke(1f));
        g.setColor(new Color(255, 60, 60, 255));
        g.fillOval(cx - 1, cy - 1, 3, 3);
      } finally {
        g.dispose();
      }

      deleteToolCursor = toolkit.createCustomCursor(image, new java.awt.Point(w / 2, h / 2), "jflap-delete");
      return deleteToolCursor;
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static Icon createThemedIcon(Icon baseIcon) {
    if (baseIcon == null) {
      return null;
    }

    BufferedImage image = renderIcon(baseIcon);
    if (image == null) {
      return null;
    }

    if (!isMostlyBlackMonochrome(image)) {
      return null;
    }

    Color tint = UIManager.getColor("Label.foreground");
    if (tint == null) {
      tint = Color.white;
    }

    int tintRgb = tint.getRGB() & 0x00ffffff;

    BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        int argb = image.getRGB(x, y);
        int a = (argb >>> 24) & 0xff;
        if (a == 0) {
          out.setRGB(x, y, 0);
          continue;
        }
        out.setRGB(x, y, (a << 24) | tintRgb);
      }
    }

    return new ImageIcon(out);
  }

  private static BufferedImage renderIcon(Icon icon) {
    int w = icon.getIconWidth();
    int h = icon.getIconHeight();
    if (w <= 0 || h <= 0) {
      return null;
    }

    BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    try {
      icon.paintIcon(null, g, 0, 0);
    } finally {
      g.dispose();
    }
    return image;
  }

  private static boolean isMostlyBlackMonochrome(BufferedImage image) {
    int colored = 0;
    int nonBlackish = 0;

    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        int argb = image.getRGB(x, y);
        int a = (argb >>> 24) & 0xff;
        if (a == 0) {
          continue;
        }
        colored++;

        int r = (argb >>> 16) & 0xff;
        int g = (argb >>> 8) & 0xff;
        int b = argb & 0xff;

        if (r > 40 || g > 40 || b > 40) {
          nonBlackish++;
        }
      }
    }

    if (colored == 0) {
      return false;
    }

    return nonBlackish == 0;
  }
}
