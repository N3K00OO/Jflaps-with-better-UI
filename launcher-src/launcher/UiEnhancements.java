package launcher;

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
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
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
  private static final String MENUBAR_WATCHER_KEY = "launcher.modern.menuBarWatcher";
  private static final String MENUBAR_CLOSE_WATCHER_KEY = "launcher.modern.menuBarCloseWatcher";
  private static final String ENVFRAME_CLOSE_FIX_KEY = "launcher.modern.envFrameCloseFix";

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
          if (!(event instanceof WindowEvent)) {
            return;
          }

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
        }
      }, AWTEvent.WINDOW_EVENT_MASK);
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
      installEnvironmentFrameCloseFix(frame);
      injectViewMenu(frame);
      injectHelpMenu(frame);
      installMenuBarWatcher(frame);
      removeLegacyCloseButton(frame);
      modernizeBevelBorders(frame);
      AssetThemer.applyToWindow(frame);
      return;
    }

    if (window instanceof javax.swing.JDialog) {
      javax.swing.JDialog dialog = (javax.swing.JDialog) window;
      installCommandPaletteBinding(dialog.getRootPane());
      modernizeBevelBorders(dialog);
      AssetThemer.applyToWindow(dialog);
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
