package launcher;

import gui.viewer.AutomatonPane;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.text.JTextComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Queue;

public final class UiEnhancements {
  private static final String MENU_INJECTED_KEY = "launcher.modern.viewMenuInjected";
  private static final String HELP_INJECTED_KEY = "launcher.modern.helpMenuInjected";
  private static final String ROOTPANE_BINDING_KEY = "launcher.modern.commandPaletteBinding";
  private static final String FAST_RUN_BINDING_KEY = "launcher.modern.fastRunBinding";
  private static final String FILE_EXPORT_INJECTED_KEY = "launcher.modern.fileExportInjected";
  private static final String MENUBAR_WATCHER_KEY = "launcher.modern.menuBarWatcher";
  private static final String MENUBAR_CLOSE_WATCHER_KEY = "launcher.modern.menuBarCloseWatcher";
  private static final String ENVFRAME_CLOSE_FIX_KEY = "launcher.modern.envFrameCloseFix";
  private static final String READABILITY_TIMER_KEY = "launcher.modern.readabilityTimer";
  private static final String HTML_READABILITY_KEY = "launcher.modern.htmlReadabilityHooked";
  private static final String FAST_RUN_MENU_TEXT = "Fast Run... (CapsLock+R)";

  private static volatile boolean installed = false;

  private UiEnhancements() {
  }

  public static synchronized void install() {
    if (installed) {
      return;
    }
    installed = true;

    try {
      Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
        @Override
        public void eventDispatched(AWTEvent event) {
          if (event instanceof WindowEvent) {
            int id = event.getID();
            if (id != WindowEvent.WINDOW_OPENED && id != WindowEvent.WINDOW_ACTIVATED) {
              return;
            }

            final Window window = ((WindowEvent) event).getWindow();
            if (window == null) {
              return;
            }

            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                enhanceWindow(window);
              }
            });
            return;
          }

          if (event instanceof ActionEvent) {
            Object src = ((ActionEvent) event).getSource();
            if (!(src instanceof Component)) {
              return;
            }

            Component sourceComponent = (Component) src;
            final Window window = (sourceComponent instanceof Window)
              ? (Window) sourceComponent
              : SwingUtilities.getWindowAncestor(sourceComponent);
            if (window == null) {
              return;
            }

            final JComponent marker = (window instanceof RootPaneContainer)
              ? ((RootPaneContainer) window).getRootPane()
              : null;

            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                if (marker != null) {
                  scheduleReadabilityFix(marker, window);
                } else {
                  applyReadabilityFixes(window);
                }
                installAutomatonCopyPasteBindings(window);
              }
            });
            return;
          }

          if (event instanceof ContainerEvent) {
            if (event.getID() != ContainerEvent.COMPONENT_ADDED) {
              return;
            }

            ContainerEvent ce = (ContainerEvent) event;
            Component parent = ce.getContainer();
            if (parent == null) {
              return;
            }

            final Window window;
            if (parent instanceof Window) {
              window = (Window) parent;
            } else {
              window = SwingUtilities.getWindowAncestor(parent);
            }
            if (window == null) {
              return;
            }

            final JComponent marker;
            if (window instanceof RootPaneContainer) {
              marker = ((RootPaneContainer) window).getRootPane();
            } else {
              marker = null;
            }

            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                // Some JFLAP panels set black HTML colors during/after they are added.
                // Re-apply readability fixes shortly after any dynamic UI insertion.
                if (marker != null) {
                  scheduleReadabilityFix(marker, window);
                } else {
                  applyReadabilityFixes(window);
                }
                installAutomatonCopyPasteBindings(window);
              }
            });
          }
        }
      }, AWTEvent.WINDOW_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK | AWTEvent.ACTION_EVENT_MASK);
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  public static void refreshAllWindows() {
    try {
      Window[] windows = Window.getWindows();
      if (windows == null) {
        return;
      }

      for (int i = 0; i < windows.length; i++) {
        Window window = windows[i];
        if (window == null || !window.isDisplayable()) {
          continue;
        }
        enhanceWindow(window);
      }
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static void enhanceWindow(Window window) {
    if (window instanceof JFrame) {
      JFrame frame = (JFrame) window;
      installCommandPaletteBinding(frame.getRootPane());
      installFastRunBinding(frame.getRootPane());
      installEnvironmentFrameCloseFix(frame);
      injectViewMenu(frame);
      injectHelpMenu(frame);
      injectFileExportMenu(frame);
      annotateFastRunMenuItem(frame);
      installMenuBarWatcher(frame);
      installAutomatonCopyPasteBindings(frame);
      removeLegacyCloseButton(frame);
      modernizeBevelBorders(frame);
      AssetThemer.applyToWindow(frame);
      applyReadabilityFixes(frame);
      scheduleReadabilityFix(frame.getRootPane(), frame);
      return;
    }

    if (window instanceof javax.swing.JDialog) {
      javax.swing.JDialog dialog = (javax.swing.JDialog) window;
      installCommandPaletteBinding(dialog.getRootPane());
      installFastRunBinding(dialog.getRootPane());
      modernizeBevelBorders(dialog);
      installAutomatonCopyPasteBindings(dialog);
      AssetThemer.applyToWindow(dialog);
      applyReadabilityFixes(dialog);
      scheduleReadabilityFix(dialog.getRootPane(), dialog);
    }
  }

  private static void installMenuBarWatcher(final JFrame frame) {
    if (frame == null) {
      return;
    }

    JRootPane rootPane = frame.getRootPane();
    if (rootPane == null) {
      return;
    }

    if (Boolean.TRUE.equals(rootPane.getClientProperty(MENUBAR_WATCHER_KEY))) {
      return;
    }
    rootPane.putClientProperty(MENUBAR_WATCHER_KEY, Boolean.TRUE);

    try {
      frame.addPropertyChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          if (evt == null) {
            return;
          }
          String name = evt.getPropertyName();
          if (name == null) {
            return;
          }

          // JFrame uses "JMenuBar" as the property name, but accept a few variants defensively.
          if ("JMenuBar".equals(name) || "menuBar".equalsIgnoreCase(name)) {
            removeLegacyCloseButton(frame);
          }
        }
      });
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static void installEnvironmentFrameCloseFix(final JFrame frame) {
    if (frame == null) {
      return;
    }

    // Only patch JFLAP environment windows.
    if (!"gui.environment.EnvironmentFrame".equals(frame.getClass().getName())) {
      return;
    }

    JRootPane rootPane = frame.getRootPane();
    if (rootPane == null) {
      return;
    }

    if (Boolean.TRUE.equals(rootPane.getClientProperty(ENVFRAME_CLOSE_FIX_KEY))) {
      return;
    }
    rootPane.putClientProperty(ENVFRAME_CLOSE_FIX_KEY, Boolean.TRUE);

    try {
      frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    } catch (Throwable ignored) {
      // ignore
    }

    // Remove JFLAP's listener that calls EnvironmentFrame.close(), which closes even if "Save As" is cancelled.
    try {
      WindowListener[] listeners = frame.getWindowListeners();
      for (int i = 0; listeners != null && i < listeners.length; i++) {
        WindowListener wl = listeners[i];
        if (wl == null) {
          continue;
        }
        if ("gui.environment.EnvironmentFrame$Listener".equals(wl.getClass().getName())) {
          try {
            frame.removeWindowListener(wl);
          } catch (Throwable ignored) {
            // ignore
          }
        }
      }
    } catch (Throwable ignored) {
      // ignore
    }

    // Install our fixed close handler.
    try {
      frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          fixedCloseEnvironmentFrame(frame);
        }
      });
    } catch (Throwable ignored) {
      // ignore
    }
  }

  private static void fixedCloseEnvironmentFrame(JFrame frame) {
    if (frame == null) {
      return;
    }

    try {
      Object env = invokeNoArg(frame, "getEnvironment");
      boolean dirty = toBoolean(invokeNoArg(env, "isDirty"));

      if (dirty) {
        File file = (File) invokeNoArg(env, "getFile");
        String name = (file == null) ? "untitled" : file.getName();

        int choice = JOptionPane.showConfirmDialog(
          frame,
          "Save " + name + " before closing?"
        );

        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
          return;
        }

        if (choice == JOptionPane.YES_OPTION) {
          boolean saved = toBoolean(invoke(frame, "save", new Class<?>[] {boolean.class}, new Object[] {Boolean.FALSE}));
          if (!saved) {
            // User cancelled Save/Save As dialog or save failed.
            return;
          }
        }
      }

      // Dispose + unregister.
      try {
        frame.dispose();
      } catch (Throwable ignored) {
        // ignore
      }

      try {
        Class<?> universe = Class.forName("gui.environment.Universe");
        Method unregister = universe.getMethod("unregisterFrame", Class.forName("gui.environment.EnvironmentFrame"));
        unregister.invoke(null, frame);
      } catch (Throwable ignored) {
        // best-effort only
      }
    } catch (Throwable ignored) {
      // If anything goes wrong, fall back to JFLAP's original close() behavior.
      try {
        invokeNoArg(frame, "close");
      } catch (Throwable ignored2) {
        // ignore
      }
    }
  }

  private static Object invokeNoArg(Object target, String method) throws Exception {
    return invoke(target, method, new Class<?>[0], new Object[0]);
  }

  private static Object invoke(Object target, String method, Class<?>[] argTypes, Object[] args) throws Exception {
    if (target == null) {
      throw new NullPointerException("target");
    }
    Method m = target.getClass().getMethod(method, argTypes);
    return m.invoke(target, args);
  }

  private static boolean toBoolean(Object v) {
    if (v instanceof Boolean) {
      return (Boolean) v;
    }
    return false;
  }

  private static void removeLegacyCloseButton(JFrame frame) {
    if (frame == null) {
      return;
    }

    JMenuBar menuBar = frame.getJMenuBar();
    if (menuBar == null) {
      return;
    }

    installCloseButtonWatcher(menuBar);

    boolean removed = removeByClassName(menuBar, "gui.action.CloseButton");
    if (!removed) {
      return;
    }

    try {
      menuBar.revalidate();
      menuBar.repaint();
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static void installCloseButtonWatcher(final JMenuBar menuBar) {
    if (menuBar == null) {
      return;
    }
    if (Boolean.TRUE.equals(menuBar.getClientProperty(MENUBAR_CLOSE_WATCHER_KEY))) {
      return;
    }
    menuBar.putClientProperty(MENUBAR_CLOSE_WATCHER_KEY, Boolean.TRUE);

    try {
      menuBar.addContainerListener(new ContainerAdapter() {
        @Override
        public void componentAdded(ContainerEvent e) {
          Component child = (e == null) ? null : e.getChild();
          if (child == null) {
            return;
          }
          if (!"gui.action.CloseButton".equals(child.getClass().getName())) {
            return;
          }

          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              removeByClassName(menuBar, "gui.action.CloseButton");
              try {
                menuBar.revalidate();
                menuBar.repaint();
              } catch (Throwable ignored) {
                // best-effort only
              }
            }
          });
        }
      });
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static void modernizeBevelBorders(Component root) {
    if (!(root instanceof Container)) {
      return;
    }

    Queue<Component> queue = new ArrayDeque<>();
    queue.add(root);

    while (!queue.isEmpty()) {
      Component c = queue.remove();
      if (c instanceof javax.swing.JComponent) {
        javax.swing.JComponent jc = (javax.swing.JComponent) c;
        try {
          if (jc.getBorder() instanceof BevelBorder) {
            jc.setBorder(BorderFactory.createEmptyBorder());
          }
        } catch (Throwable ignored) {
          // ignore
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
              queue.add(children[i]);
            }
          }
        }
      }
    }
  }

  /**
   * JFLAP uses disabled {@code JEditorPane} controls to show HTML titles (e.g. pumping lemma chooser).
   * Some of these panes explicitly set {@code setDisabledTextColor(Color.BLACK)}, which becomes unreadable on dark themes.
   * This pass forces disabled HTML panes to use theme-appropriate text colors.
   */
  public static void applyReadabilityFixes(Component root) {
    if (!(root instanceof Container)) {
      return;
    }

    final Color defaultFg = bestEffortTextForeground();

    Queue<Component> queue = new ArrayDeque<>();
    queue.add(root);

    while (!queue.isEmpty()) {
      Component c = queue.remove();

      if (c instanceof JComponent) {
        tryEnlargePumpingLemmaExplain((JComponent) c);
      }

      if (c instanceof javax.swing.JEditorPane) {
        javax.swing.JEditorPane pane = (javax.swing.JEditorPane) c;
        try {
          String contentType = pane.getContentType();
          if (contentType != null && contentType.toLowerCase().contains("text/html")) {
            Color bg = pane.getBackground();
            if (bg == null) {
              bg = UIManager.getColor("Panel.background");
            }

            // Make HTML respect component foreground/font so we can force readable text.
            try {
              pane.putClientProperty(javax.swing.JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            } catch (Throwable ignored) {
              // ignore
            }

            installHtmlReadabilityHooks(pane);

            Color disabled = null;
            try {
              disabled = pane.getDisabledTextColor();
            } catch (Throwable ignored) {
              disabled = null;
            }

            // Only override if it's currently low-contrast (or explicitly black on a dark background).
            if (isLowContrast(disabled, bg) || (isVeryDark(disabled) && isDark(bg))) {
              pane.setDisabledTextColor(defaultFg);
            }

            // Some LAFs/HTML renderers use foreground even when disabled.
            if (isLowContrast(pane.getForeground(), bg)) {
              pane.setForeground(defaultFg);
            }

            // Some HTML kits default text to black regardless of disabledTextColor/foreground.
            // Force the document stylesheet to use the current theme foreground.
            try {
              if (pane.getDocument() instanceof HTMLDocument) {
                HTMLDocument doc = (HTMLDocument) pane.getDocument();
                StyleSheet ss = doc.getStyleSheet();
                if (ss != null) {
                  String css = toCssColor(defaultFg);
                  ss.addRule("body { color: " + css + " !important; }");
                  ss.addRule("* { color: " + css + " !important; }");
                }
              }
            } catch (Throwable ignored) {
              // best-effort only
            }

            try {
              pane.revalidate();
              pane.repaint();
            } catch (Throwable ignored) {
              // ignore
            }

            // If someone set the pane disabled just to prevent editing, keep it readable.
            // (Don't change enabled state here; it can affect focus/keyboard behavior.)
          }
        } catch (Throwable ignored) {
          // best-effort only
        }
      } else if (c instanceof JTextComponent) {
        // Keep disabled text readable in dark themes (e.g. read-only fields).
        JTextComponent tc = (JTextComponent) c;
        try {
          Color bg = tc.getBackground();
          if (bg == null) {
            bg = UIManager.getColor("TextField.background");
          }
          if (!tc.isEnabled()) {
            Color disabled = tc.getDisabledTextColor();
            if (isLowContrast(disabled, bg)) {
              tc.setDisabledTextColor(defaultFg);
            }
          }
        } catch (Throwable ignored) {
          // ignore
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
              queue.add(children[i]);
            }
          }
        }
      }
    }
  }

  private static void installHtmlReadabilityHooks(final javax.swing.JEditorPane pane) {
    if (pane == null) {
      return;
    }
    if (Boolean.TRUE.equals(pane.getClientProperty(HTML_READABILITY_KEY))) {
      return;
    }
    pane.putClientProperty(HTML_READABILITY_KEY, Boolean.TRUE);

    try {
      pane.addPropertyChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          String name = (evt == null) ? null : evt.getPropertyName();
          if (name == null) {
            return;
          }
          if ("document".equals(name) || "text".equals(name) || "enabled".equals(name)) {
            // Apply after the update so we see the final document/text.
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                applyReadabilityFixes(pane);
              }
            });
          }
        }
      });
    } catch (Throwable ignored) {
      // best-effort only
    }

    try {
      if (pane.getDocument() != null) {
        pane.getDocument().addDocumentListener(new DocumentListener() {
          @Override
          public void insertUpdate(DocumentEvent e) {
            reapplySoon();
          }

          @Override
          public void removeUpdate(DocumentEvent e) {
            reapplySoon();
          }

          @Override
          public void changedUpdate(DocumentEvent e) {
            reapplySoon();
          }

          private void reapplySoon() {
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                applyReadabilityFixes(pane);
              }
            });
          }
        });
      }
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static void scheduleReadabilityFix(JComponent marker, final Component target) {
    if (marker == null || target == null) {
      return;
    }

    Object existing = marker.getClientProperty(READABILITY_TIMER_KEY);
    if (existing instanceof javax.swing.Timer) {
      try {
        ((javax.swing.Timer) existing).restart();
      } catch (Throwable ignored) {
        // ignore
      }
      return;
    }

    try {
      // Some JFLAP panes (including pumping lemma chooser panes) set their colors during/after show.
      // Re-apply our readability fix shortly after the window becomes visible.
      javax.swing.Timer t = new javax.swing.Timer(250, new java.awt.event.ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          try {
            applyReadabilityFixes(target);
          } catch (Throwable ignored) {
            // ignore
          }
        }
      });
      t.setRepeats(false);
      marker.putClientProperty(READABILITY_TIMER_KEY, t);
      t.start();
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static Color bestEffortTextForeground() {
    Color fg = UIManager.getColor("Label.foreground");
    if (fg != null) {
      return fg;
    }
    fg = UIManager.getColor("TextField.foreground");
    if (fg != null) {
      return fg;
    }
    // Reasonable default for dark-ish themes.
    return new Color(0xEDEDED);
  }

  private static boolean isLowContrast(Color fg, Color bg) {
    if (fg == null || bg == null) {
      return false;
    }
    return contrastRatio(fg, bg) < 3.0;
  }

  private static boolean isDark(Color c) {
    if (c == null) {
      return false;
    }
    double r = c.getRed() / 255.0;
    double g = c.getGreen() / 255.0;
    double b = c.getBlue() / 255.0;
    double lum = 0.2126 * r + 0.7152 * g + 0.0722 * b;
    return lum < 0.5;
  }

  private static boolean isVeryDark(Color c) {
    if (c == null) {
      return false;
    }
    return c.getRed() < 40 && c.getGreen() < 40 && c.getBlue() < 40;
  }

  private static double contrastRatio(Color a, Color b) {
    double la = relativeLuminance(a);
    double lb = relativeLuminance(b);
    double light = Math.max(la, lb);
    double dark = Math.min(la, lb);
    return (light + 0.05) / (dark + 0.05);
  }

  private static double relativeLuminance(Color c) {
    if (c == null) {
      return 0.0;
    }
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

  private static String toCssColor(Color c) {
    if (c == null) {
      return "#ffffff";
    }
    return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
  }

  private static void tryEnlargePumpingLemmaExplain(JComponent c) {
    if (c == null) {
      return;
    }

    Border border;
    try {
      border = c.getBorder();
    } catch (Throwable ignored) {
      border = null;
    }

    if (!(border instanceof TitledBorder)) {
      return;
    }

    String title;
    try {
      title = ((TitledBorder) border).getTitle();
    } catch (Throwable ignored) {
      title = null;
    }

    if (title == null || !title.trim().startsWith("Objective")) {
      return;
    }

    // This is the PumpingLemmaInputPane "Objective:" panel (contains Clear All, Explain, and a small HTML JTextPane).
    JScrollPane scroll = findHtmlTextScrollPane(c);
    if (scroll == null) {
      return;
    }

    Component view = null;
    try {
      if (scroll.getViewport() != null) {
        view = scroll.getViewport().getView();
      }
    } catch (Throwable ignored) {
      view = null;
    }

    if (!(view instanceof JTextComponent)) {
      return;
    }

    int lineHeight;
    try {
      lineHeight = view.getFontMetrics(view.getFont()).getHeight();
    } catch (Throwable ignored) {
      lineHeight = 16;
    }

    Dimension scrollPref = safeDim(scroll.getPreferredSize(), 220, 28);
    Dimension panelPref = safeDim(c.getPreferredSize(), 640, 58);
    Dimension panelMax = safeDim(c.getMaximumSize(), panelPref.width, panelPref.height);

    // Give the explanation text area enough height to be readable at common DPI scales.
    int desiredScrollHeight = Math.max(scrollPref.height, lineHeight * 5 + 12);

    // Keep the panel's original overhead (border + FlowLayout gaps + button height), but allow more room for the text pane.
    int overhead = Math.max(0, panelPref.height - scrollPref.height);
    int desiredPanelHeight = Math.max(panelPref.height, desiredScrollHeight + overhead);

    // Apply sizes (idempotent).
    try {
      scroll.setPreferredSize(new Dimension(scrollPref.width, desiredScrollHeight));
    } catch (Throwable ignored) {
      // ignore
    }

    try {
      Dimension scrollMax = safeDim(scroll.getMaximumSize(), scrollPref.width, desiredScrollHeight);
      scroll.setMaximumSize(new Dimension(scrollMax.width, Math.max(scrollMax.height, desiredScrollHeight)));
    } catch (Throwable ignored) {
      // ignore
    }

    try {
      c.setPreferredSize(new Dimension(panelPref.width, desiredPanelHeight));
      c.setMaximumSize(new Dimension(panelMax.width, Math.max(panelMax.height, desiredPanelHeight)));
      // Don't force a minimum; let layouts shrink if necessary.
    } catch (Throwable ignored) {
      // ignore
    }

    if (desiredScrollHeight != scrollPref.height || desiredPanelHeight != panelPref.height) {
      try {
        c.revalidate();
        c.repaint();
      } catch (Throwable ignored) {
        // ignore
      }
    }
  }

  private static JScrollPane findHtmlTextScrollPane(Container root) {
    if (root == null) {
      return null;
    }

    Queue<Component> queue = new ArrayDeque<>();
    queue.add(root);

    while (!queue.isEmpty()) {
      Component c = queue.remove();
      if (c instanceof JScrollPane) {
        JScrollPane sp = (JScrollPane) c;
        try {
          Component view = (sp.getViewport() == null) ? null : sp.getViewport().getView();
          if (view instanceof javax.swing.JEditorPane) {
            javax.swing.JEditorPane ep = (javax.swing.JEditorPane) view;
            String ct = ep.getContentType();
            if (ct != null && ct.toLowerCase().contains("text/html")) {
              return sp;
            }
          }
        } catch (Throwable ignored) {
          // ignore
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
              queue.add(children[i]);
            }
          }
        }
      }
    }

    return null;
  }

  private static Dimension safeDim(Dimension d, int fallbackW, int fallbackH) {
    int w = (d == null || d.width <= 0) ? fallbackW : d.width;
    int h = (d == null || d.height <= 0) ? fallbackH : d.height;
    return new Dimension(w, h);
  }

  private static boolean removeByClassName(java.awt.Container root, String className) {
    if (root == null || className == null || className.trim().isEmpty()) {
      return false;
    }

    boolean removedAny = false;

    Queue<java.awt.Container> queue = new ArrayDeque<>();
    queue.add(root);

    while (!queue.isEmpty()) {
      java.awt.Container container = queue.remove();
      Component[] children;
      try {
        children = container.getComponents();
      } catch (Throwable ignored) {
        children = null;
      }

      if (children == null || children.length == 0) {
        continue;
      }

      for (int i = 0; i < children.length; i++) {
        Component child = children[i];
        if (child == null) {
          continue;
        }

        if (className.equals(child.getClass().getName())) {
          try {
            container.remove(child);
            removedAny = true;
          } catch (Throwable ignored) {
            // ignore
          }
          continue;
        }

        if (child instanceof java.awt.Container) {
          queue.add((java.awt.Container) child);
        }
      }
    }

    return removedAny;
  }

  private static void installAutomatonCopyPasteBindings(Component root) {
    if (root == null) {
      return;
    }

    Queue<Component> queue = new ArrayDeque<>();
    queue.add(root);

    while (!queue.isEmpty()) {
      Component c = queue.remove();
      if (c instanceof AutomatonPane) {
        AutomatonCopyPaste.installCopyPasteBindings((AutomatonPane) c);
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
              queue.add(children[i]);
            }
          }
        }
      }
    }
  }

  private static void installCommandPaletteBinding(JRootPane rootPane) {
    if (rootPane == null) {
      return;
    }
    if (Boolean.TRUE.equals(rootPane.getClientProperty(ROOTPANE_BINDING_KEY))) {
      return;
    }
    rootPane.putClientProperty(ROOTPANE_BINDING_KEY, Boolean.TRUE);

    int shortcutMask = menuShortcutMask();
    KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_K, shortcutMask);
    int shortcutMaskEx = menuShortcutMaskEx();
    KeyStroke ksEx = KeyStroke.getKeyStroke(KeyEvent.VK_K, shortcutMaskEx);

    String actionKey = "launcher.commandPalette";
    InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap actionMap = rootPane.getActionMap();

    if (actionMap.get(actionKey) == null) {
      actionMap.put(actionKey, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          CommandPalette.showForActiveWindow();
        }
      });
    }

    inputMap.put(ks, actionKey);
    inputMap.put(ksEx, actionKey);
  }

  private static void installFastRunBinding(JRootPane rootPane) {
    if (rootPane == null) {
      return;
    }
    if (Boolean.TRUE.equals(rootPane.getClientProperty(FAST_RUN_BINDING_KEY))) {
      return;
    }
    rootPane.putClientProperty(FAST_RUN_BINDING_KEY, Boolean.TRUE);

    KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_R, 0);
    KeyStroke ksShift = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.SHIFT_DOWN_MASK);

    String actionKey = "launcher.fastRunWithRerun";
    InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap actionMap = rootPane.getActionMap();

    if (actionMap.get(actionKey) == null) {
      actionMap.put(actionKey, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (!isCapsLockEnabled()) {
            return;
          }
          FastRun.triggerFastRunWithRerun();
        }
      });
    }

    inputMap.put(ks, actionKey);
    inputMap.put(ksShift, actionKey);
  }

  private static boolean isCapsLockEnabled() {
    try {
      return Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
    } catch (Throwable ignored) {
      return false;
    }
  }

  private static void injectViewMenu(JFrame frame) {
    JMenuBar menuBar = frame.getJMenuBar();
    if (menuBar == null) {
      return;
    }
    if (Boolean.TRUE.equals(menuBar.getClientProperty(MENU_INJECTED_KEY))) {
      return;
    }
    menuBar.putClientProperty(MENU_INJECTED_KEY, Boolean.TRUE);

    JMenu viewMenu = findOrCreateMenu(menuBar, "View", "Help");

    JMenuItem paletteItem = new JMenuItem("Command Palette...");
    paletteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, menuShortcutMask()));
    paletteItem.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        CommandPalette.showForActiveWindow();
      }
    });

    viewMenu.add(paletteItem);

    JMenuItem customizeItem = new JMenuItem("Customize Theme...");
    customizeItem.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        ThemeCustomizationDialog.showForActiveWindow();
      }
    });
    viewMenu.add(customizeItem);

    viewMenu.add(new JSeparator());

    JMenu themeMenu = new JMenu("Theme");
    ButtonGroup group = new ButtonGroup();

    addThemeItem(themeMenu, group, "IntelliJ", Theme.INTELLIJ);
    addThemeItem(themeMenu, group, "Darcula", Theme.DARCULA);
    addThemeItem(themeMenu, group, "Light", Theme.LIGHT);
    addThemeItem(themeMenu, group, "Dark", Theme.DARK);

    viewMenu.add(themeMenu);

    menuBar.revalidate();
    menuBar.repaint();
  }

  private static void injectHelpMenu(JFrame frame) {
    JMenuBar menuBar = frame.getJMenuBar();
    if (menuBar == null) {
      return;
    }
    if (Boolean.TRUE.equals(menuBar.getClientProperty(HELP_INJECTED_KEY))) {
      return;
    }
    menuBar.putClientProperty(HELP_INJECTED_KEY, Boolean.TRUE);

    JMenu helpMenu = findMenu(menuBar, "Help");
    if (helpMenu == null) {
      return;
    }

    helpMenu.add(new JSeparator());
    JMenuItem aboutItem = new JMenuItem("About Modern UI...");
    aboutItem.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        AboutDialog.showForActiveWindow();
      }
    });
    helpMenu.add(aboutItem);
  }

  private static void injectFileExportMenu(JFrame frame) {
    JMenuBar menuBar = frame.getJMenuBar();
    if (menuBar == null) {
      return;
    }

    JMenu fileMenu = findMenu(menuBar, "File");
    if (fileMenu == null) {
      return;
    }

    if (Boolean.TRUE.equals(fileMenu.getClientProperty(FILE_EXPORT_INJECTED_KEY))) {
      return;
    }
    fileMenu.putClientProperty(FILE_EXPORT_INJECTED_KEY, Boolean.TRUE);

    JMenuItem exportPng = new JMenuItem("Export PNG...");
    exportPng.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ExportManager.exportPngForActiveWindow();
      }
    });

    JMenuItem exportSvg = new JMenuItem("Export SVG...");
    exportSvg.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ExportManager.exportSvgForActiveWindow();
      }
    });

    fileMenu.add(new JSeparator());
    fileMenu.add(exportPng);
    fileMenu.add(exportSvg);
  }

  private static void annotateFastRunMenuItem(JFrame frame) {
    if (frame == null) {
      return;
    }

    JMenuBar menuBar = frame.getJMenuBar();
    if (menuBar == null) {
      return;
    }

    int topCount = menuBar.getMenuCount();
    for (int i = 0; i < topCount; i++) {
      JMenu menu = menuBar.getMenu(i);
      if (menu == null) {
        continue;
      }
      annotateFastRunMenuItem(menu);
    }
  }

  private static void annotateFastRunMenuItem(JMenu menu) {
    if (menu == null) {
      return;
    }

    int itemCount = menu.getItemCount();
    for (int i = 0; i < itemCount; i++) {
      JMenuItem item = menu.getItem(i);
      if (item == null) {
        continue;
      }

      String text = item.getText();
      if (text != null) {
        String trimmed = text.trim();
        if (trimmed.startsWith("Fast Run") && !trimmed.contains("CapsLock+R")) {
          item.setText(FAST_RUN_MENU_TEXT);
        }
      }

      if (item instanceof JMenu) {
        annotateFastRunMenuItem((JMenu) item);
      }
    }
  }

  private static void addThemeItem(JMenu themeMenu, ButtonGroup group, String label, final Theme theme) {
    final JRadioButtonMenuItem item = new JRadioButtonMenuItem(label);
    item.setSelected(ThemeManager.getCurrentTheme() == theme);
    item.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ThemeManager.switchTheme(theme);
      }
    });
    group.add(item);
    themeMenu.add(item);
  }

  private static JMenu findOrCreateMenu(JMenuBar menuBar, String menuName, String beforeMenuName) {
    JMenu existing = findMenu(menuBar, menuName);
    if (existing != null) {
      return existing;
    }

    JMenu menu = new JMenu(menuName);
    int insertIndex = findMenuIndex(menuBar, beforeMenuName);
    if (insertIndex >= 0) {
      menuBar.add(menu, insertIndex);
    } else {
      menuBar.add(menu);
    }
    return menu;
  }

  private static JMenu findMenu(JMenuBar menuBar, String name) {
    if (menuBar == null || name == null) {
      return null;
    }

    int count = menuBar.getMenuCount();
    for (int i = 0; i < count; i++) {
      JMenu menu = menuBar.getMenu(i);
      if (menu == null) {
        continue;
      }
      String text = menu.getText();
      if (text != null && name.equalsIgnoreCase(text.trim())) {
        return menu;
      }
    }
    return null;
  }

  private static int findMenuIndex(JMenuBar menuBar, String name) {
    if (menuBar == null || name == null) {
      return -1;
    }

    int count = menuBar.getMenuCount();
    for (int i = 0; i < count; i++) {
      JMenu menu = menuBar.getMenu(i);
      if (menu == null) {
        continue;
      }
      String text = menu.getText();
      if (text != null && name.equalsIgnoreCase(text.trim())) {
        return i;
      }
    }
    return -1;
  }

  private static int menuShortcutMask() {
    try {
      return Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    } catch (Throwable ignored) {
      return InputEvent.CTRL_MASK;
    }
  }

  private static int menuShortcutMaskEx() {
    int mask = menuShortcutMask();
    if ((mask & InputEvent.META_MASK) != 0) {
      return InputEvent.META_DOWN_MASK;
    }
    return InputEvent.CTRL_DOWN_MASK;
  }
}
