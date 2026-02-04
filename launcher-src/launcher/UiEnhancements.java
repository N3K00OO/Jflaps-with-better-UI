package launcher;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

public final class UiEnhancements {
  private static final String MENU_INJECTED_KEY = "launcher.modern.viewMenuInjected";
  private static final String HELP_INJECTED_KEY = "launcher.modern.helpMenuInjected";
  private static final String ROOTPANE_BINDING_KEY = "launcher.modern.commandPaletteBinding";

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
      injectViewMenu(frame);
      injectHelpMenu(frame);
      AssetThemer.applyToWindow(frame);
      return;
    }

    if (window instanceof javax.swing.JDialog) {
      javax.swing.JDialog dialog = (javax.swing.JDialog) window;
      installCommandPaletteBinding(dialog.getRootPane());
      AssetThemer.applyToWindow(dialog);
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
      return InputEvent.CTRL_DOWN_MASK;
    }
  }
}
