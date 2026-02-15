package launcher;

import gui.editor.EditBlockPane;
import gui.environment.Environment;
import gui.environment.EnvironmentFrame;
import gui.viewer.AutomatonDrawer;
import gui.viewer.AutomatonPane;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;

public final class ExportManager {
  private static final int PADDING = 2;
  private static final int INVALID_SELECTION_SIZE = -1;
  private static final String DEFAULT_PNG_NAME = "automaton.png";
  private static final String DEFAULT_SVG_NAME = "automaton.svg";

  private ExportManager() {
  }

  public static void exportPngForActiveWindow() {
    runOnEdt(new Runnable() {
      @Override
      public void run() {
        exportPng(activeWindow());
      }
    });
  }

  public static void exportSvgForActiveWindow() {
    runOnEdt(new Runnable() {
      @Override
      public void run() {
        exportSvg(activeWindow());
      }
    });
  }

  private static void exportPng(Window owner) {
    AutomatonPane pane = findAutomatonPane(owner);
    if (pane == null) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }

    AutomatonDrawer drawer = drawerForPane(pane);
    if (drawer == null) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }

    File target = chooseFile(owner, "png", "PNG Image", DEFAULT_PNG_NAME, "Export PNG");
    if (target == null) {
      return;
    }

    AffineTransform restore = snapshotDrawerTransform(drawer);
    Rectangle selectionBoundsRestore = snapshotSelectionBounds(drawer);

    try {
      setDrawerTransform(drawer, new AffineTransform());
      clearSelectionBounds(drawer);

      ExportTarget exportTarget = buildExportTarget(pane, drawer);
      if (exportTarget == null) {
        Toolkit.getDefaultToolkit().beep();
        return;
      }

      BufferedImage image = new BufferedImage(exportTarget.width, exportTarget.height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = image.createGraphics();
      applyQualityHints(g);
      g.setColor(exportTarget.background);
      g.fillRect(0, 0, exportTarget.width, exportTarget.height);
      g.translate(exportTarget.translateX, exportTarget.translateY);
      exportTarget.drawer.drawAutomaton(g);
      g.dispose();

      ImageIO.write(image, "png", target);
    } catch (Exception ex) {
      showError(owner, "Failed to export PNG", ex);
    } finally {
      restoreSelectionBounds(drawer, selectionBoundsRestore);
      setDrawerTransform(drawer, restore);
    }
  }

  private static void exportSvg(Window owner) {
    AutomatonPane pane = findAutomatonPane(owner);
    if (pane == null) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }

    AutomatonDrawer drawer = drawerForPane(pane);
    if (drawer == null) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }

    if (!isSvgRuntimeAvailable(owner)) {
      return;
    }

    File target = chooseFile(owner, "svg", "SVG Image", DEFAULT_SVG_NAME, "Export SVG");
    if (target == null) {
      return;
    }

    AffineTransform restore = snapshotDrawerTransform(drawer);
    Rectangle selectionBoundsRestore = snapshotSelectionBounds(drawer);

    try {
      setDrawerTransform(drawer, new AffineTransform());
      clearSelectionBounds(drawer);

      ExportTarget exportTarget = buildExportTarget(pane, drawer);
      if (exportTarget == null) {
        Toolkit.getDefaultToolkit().beep();
        return;
      }

      DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
      String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
      Document doc = impl.createDocument(svgNS, "svg", null);

      SVGGraphics2D svg = new SVGGraphics2D(doc);
      svg.setSVGCanvasSize(new Dimension(exportTarget.width, exportTarget.height));
      applyQualityHints(svg);
      svg.setColor(exportTarget.background);
      svg.fillRect(0, 0, exportTarget.width, exportTarget.height);
      svg.translate(exportTarget.translateX, exportTarget.translateY);
      exportTarget.drawer.drawAutomaton(svg);

      try (Writer out = new OutputStreamWriter(new FileOutputStream(target), StandardCharsets.UTF_8)) {
        svg.stream(out, true);
      }
    } catch (Throwable ex) {
      showError(owner, "Failed to export SVG", ex);
    } finally {
      restoreSelectionBounds(drawer, selectionBoundsRestore);
      setDrawerTransform(drawer, restore);
    }
  }

  private static boolean isSvgRuntimeAvailable(Component parent) {
    try {
      Class.forName("org.w3c.dom.svg.SVGDocument", false, ExportManager.class.getClassLoader());
      return true;
    } catch (Throwable ex) {
      showError(parent, "SVG export is unavailable because SVG DOM classes are missing in this build", ex);
      return false;
    }
  }

  private static ExportTarget buildExportTarget(AutomatonPane pane, AutomatonDrawer drawer) {
    if (drawer == null) {
      return null;
    }

    Rectangle bounds;
    try {
      bounds = drawer.getBounds();
    } catch (Throwable ignored) {
      bounds = null;
    }

    if (bounds == null) {
      bounds = new Rectangle(0, 0, Math.max(1, pane.getWidth()), Math.max(1, pane.getHeight()));
    }

    Rectangle contentBounds = computeContentBounds(drawer, bounds);
    if (contentBounds == null) {
      contentBounds = bounds;
    }

    int width = Math.max(1, contentBounds.width + PADDING * 2);
    int height = Math.max(1, contentBounds.height + PADDING * 2);
    int tx = PADDING - contentBounds.x;
    int ty = PADDING - contentBounds.y;

    Color bg = CanvasColors.canvasFillColor();
    if (bg == null) {
      bg = Color.WHITE;
    }

    return new ExportTarget(drawer, width, height, tx, ty, bg);
  }

  private static AutomatonDrawer drawerForPane(AutomatonPane pane) {
    if (pane == null) {
      return null;
    }
    try {
      return pane.getDrawer();
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static AffineTransform snapshotDrawerTransform(AutomatonDrawer drawer) {
    if (drawer == null) {
      return new AffineTransform();
    }
    try {
      Field f = findField(drawer.getClass(), "curTransform");
      if (f != null) {
        f.setAccessible(true);
        Object value = f.get(drawer);
        if (value instanceof AffineTransform) {
          return new AffineTransform((AffineTransform) value);
        }
      }
    } catch (Throwable ignored) {
      // fall through
    }
    return new AffineTransform();
  }

  private static void setDrawerTransform(AutomatonDrawer drawer, AffineTransform transform) {
    if (drawer == null) {
      return;
    }
    try {
      drawer.setTransform(transform == null ? new AffineTransform() : transform);
      drawer.invalidateBounds();
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static Field findField(Class<?> type, String name) {
    Class<?> current = type;
    while (current != null) {
      try {
        return current.getDeclaredField(name);
      } catch (NoSuchFieldException ignored) {
        current = current.getSuperclass();
      }
    }
    return null;
  }

  private static Rectangle computeContentBounds(AutomatonDrawer drawer, Rectangle fallbackBounds) {
    if (drawer == null || fallbackBounds == null) {
      return fallbackBounds;
    }

    final int[] padLevels = new int[] {64, 128, 256, 512};
    Rectangle best = null;

    for (int i = 0; i < padLevels.length; i++) {
      ScanResult scanned = scanContentBounds(drawer, fallbackBounds, padLevels[i]);
      if (scanned == null || scanned.bounds == null) {
        continue;
      }

      best = scanned.bounds;
      if (!scanned.touchesEdge) {
        break;
      }
    }

    return (best == null) ? fallbackBounds : best;
  }

  private static ScanResult scanContentBounds(AutomatonDrawer drawer, Rectangle logicalBounds, int scanPad) {
    if (drawer == null || logicalBounds == null || scanPad < 0) {
      return null;
    }

    int scanWidth = Math.max(1, logicalBounds.width + scanPad * 2);
    int scanHeight = Math.max(1, logicalBounds.height + scanPad * 2);
    int scanTx = scanPad - logicalBounds.x;
    int scanTy = scanPad - logicalBounds.y;

    BufferedImage scan = new BufferedImage(scanWidth, scanHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = scan.createGraphics();
    try {
      applyQualityHints(g);
      g.translate(scanTx, scanTy);
      drawer.drawAutomaton(g);
    } catch (Throwable ignored) {
      return null;
    } finally {
      g.dispose();
    }

    int minX = scanWidth;
    int minY = scanHeight;
    int maxX = -1;
    int maxY = -1;

    for (int y = 0; y < scanHeight; y++) {
      for (int x = 0; x < scanWidth; x++) {
        int argb = scan.getRGB(x, y);
        int alpha = (argb >>> 24) & 0xff;
        if (alpha == 0) {
          continue;
        }

        if (x < minX) {
          minX = x;
        }
        if (y < minY) {
          minY = y;
        }
        if (x > maxX) {
          maxX = x;
        }
        if (y > maxY) {
          maxY = y;
        }
      }
    }

    if (maxX < minX || maxY < minY) {
      return new ScanResult(new Rectangle(logicalBounds), false);
    }

    int contentX = minX - scanTx;
    int contentY = minY - scanTy;
    int contentW = Math.max(1, (maxX - minX) + 1);
    int contentH = Math.max(1, (maxY - minY) + 1);

    boolean touchesEdge = minX <= 0 || minY <= 0 || maxX >= (scanWidth - 1) || maxY >= (scanHeight - 1);
    return new ScanResult(new Rectangle(contentX, contentY, contentW, contentH), touchesEdge);
  }

  private static Rectangle snapshotSelectionBounds(AutomatonDrawer drawer) {
    if (drawer == null) {
      return null;
    }
    try {
      Rectangle r = drawer.getSelectionBounds();
      return (r == null) ? null : new Rectangle(r);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static void clearSelectionBounds(AutomatonDrawer drawer) {
    if (drawer == null) {
      return;
    }
    try {
      drawer.setSelectionBounds(new Rectangle(0, 0, INVALID_SELECTION_SIZE, INVALID_SELECTION_SIZE));
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static void restoreSelectionBounds(AutomatonDrawer drawer, Rectangle original) {
    if (drawer == null) {
      return;
    }
    try {
      if (original == null) {
        drawer.setSelectionBounds(new Rectangle(0, 0, INVALID_SELECTION_SIZE, INVALID_SELECTION_SIZE));
      } else {
        drawer.setSelectionBounds(new Rectangle(original));
      }
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static File chooseFile(Window owner,
                                 String extension,
                                 String description,
                                 String defaultName,
                                 String title) {
    JFileChooser chooser = createChooser(owner);
    chooser.setDialogTitle(title);
    chooser.setFileFilter(new FileNameExtensionFilter(description, extension));
    chooser.setSelectedFile(new File(defaultName));

    int result;
    try {
      result = chooser.showSaveDialog(owner);
    } catch (Throwable ignored) {
      result = JFileChooser.CANCEL_OPTION;
    }
    if (result != JFileChooser.APPROVE_OPTION) {
      return null;
    }

    File selected = chooser.getSelectedFile();
    if (selected == null) {
      return null;
    }

    String name = selected.getName();
    if (!name.toLowerCase().endsWith("." + extension)) {
      selected = new File(selected.getParentFile(), name + "." + extension);
    }
    return selected;
  }

  private static JFileChooser createChooser(Window owner) {
    File currentDir = null;
    try {
      Class<?> universe = Class.forName("gui.environment.Universe");
      java.lang.reflect.Field chooserField = universe.getField("CHOOSER");
      Object existing = chooserField.get(null);
      if (existing instanceof JFileChooser) {
        try {
          currentDir = ((JFileChooser) existing).getCurrentDirectory();
        } catch (Throwable ignored) {
          currentDir = null;
        }
      }
    } catch (Throwable ignored) {
      currentDir = null;
    }

    return new ModernFileChooser(currentDir);
  }

  private static AutomatonPane findAutomatonPane(Window window) {
    EnvironmentFrame frame = findEnvironmentFrame(window);
    if (frame != null) {
      try {
        Environment env = frame.getEnvironment();
        if (env != null) {
          Component active = env.getActive();
          if (active instanceof EditBlockPane) {
            AutomatonPane pane = findAutomatonPane(active);
            if (pane != null) {
              return pane;
            }
          }
        }
      } catch (Throwable ignored) {
        // ignore
      }
    }

    return findAutomatonPane((Component) window);
  }

  private static AutomatonPane findAutomatonPane(Component root) {
    if (root == null) {
      return null;
    }

    Queue<Component> queue = new ArrayDeque<>();
    queue.add(root);

    while (!queue.isEmpty()) {
      Component c = queue.remove();
      if (c instanceof AutomatonPane) {
        return (AutomatonPane) c;
      }

      if (!(c instanceof Container)) {
        continue;
      }

      Component[] children;
      try {
        children = ((Container) c).getComponents();
      } catch (Throwable ignored) {
        children = null;
      }

      if (children == null) {
        continue;
      }

      for (int i = 0; i < children.length; i++) {
        if (children[i] != null) {
          queue.add(children[i]);
        }
      }
    }

    return null;
  }

  private static EnvironmentFrame findEnvironmentFrame(Window window) {
    Window current = window;
    while (current != null) {
      if (current instanceof EnvironmentFrame) {
        return (EnvironmentFrame) current;
      }
      try {
        current = current.getOwner();
      } catch (Throwable ignored) {
        return null;
      }
    }
    return null;
  }

  private static Window activeWindow() {
    try {
      Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
      if (window != null) {
        return window;
      }
      return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static void applyQualityHints(Graphics2D g) {
    if (g == null) {
      return;
    }
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    } catch (Throwable ignored) {
      // ignore
    }
  }

  private static void showError(Component parent, String message, Throwable ex) {
    String details;
    try {
      details = (ex == null || ex.getMessage() == null || ex.getMessage().trim().isEmpty())
        ? ((ex == null) ? "" : ex.getClass().getSimpleName())
        : ex.getMessage();
    } catch (Throwable ignored) {
      details = "";
    }

    try {
      if (details == null || details.trim().isEmpty()) {
        JOptionPane.showMessageDialog(parent, message, "Export Failed", JOptionPane.ERROR_MESSAGE);
      } else {
        JOptionPane.showMessageDialog(parent, message + ": " + details, "Export Failed", JOptionPane.ERROR_MESSAGE);
      }
    } catch (Throwable ignored) {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  private static void runOnEdt(Runnable r) {
    if (SwingUtilities.isEventDispatchThread()) {
      r.run();
    } else {
      SwingUtilities.invokeLater(r);
    }
  }

  private static final class ExportTarget {
    final AutomatonDrawer drawer;
    final int width;
    final int height;
    final int translateX;
    final int translateY;
    final Color background;

    private ExportTarget(AutomatonDrawer drawer,
                         int width,
                         int height,
                         int translateX,
                         int translateY,
                         Color background) {
      this.drawer = drawer;
      this.width = width;
      this.height = height;
      this.translateX = translateX;
      this.translateY = translateY;
      this.background = background;
    }
  }

  private static final class ScanResult {
    final Rectangle bounds;
    final boolean touchesEdge;

    private ScanResult(Rectangle bounds, boolean touchesEdge) {
      this.bounds = bounds;
      this.touchesEdge = touchesEdge;
    }
  }
}
