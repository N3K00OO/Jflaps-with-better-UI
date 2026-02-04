package launcher;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public final class CommandPalette {
  private static final int MAX_RESULTS = 200;

  private CommandPalette() {
  }

  public static void showForActiveWindow() {
    Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    if (window == null) {
      window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
    }
    if (window == null) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }

    show(window);
  }

  public static void show(final Window ownerWindow) {
    if (ownerWindow == null) {
      return;
    }

    JMenuBar menuBar = getMenuBar(ownerWindow);
    if (menuBar == null) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }

    final List<Command> allCommands = collectCommands(menuBar);
    if (allCommands.isEmpty()) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }

    final JDialog dialog = createDialog(ownerWindow);
    final JTextField query = new JTextField();
    final DefaultListModel<Command> model = new DefaultListModel<>();
    final JList<Command> list = new JList<>(model);

    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setVisibleRowCount(12);

    JPanel content = new JPanel(new BorderLayout(10, 10));
    content.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

    content.add(query, BorderLayout.NORTH);
    content.add(new JScrollPane(list), BorderLayout.CENTER);
    dialog.setContentPane(content);

    Runnable update = new Runnable() {
      @Override
      public void run() {
        String q = query.getText();
        model.clear();

        int added = 0;
        for (Command command : allCommands) {
          if (matches(command, q)) {
            model.addElement(command);
            added++;
            if (added >= MAX_RESULTS) {
              break;
            }
          }
        }

        if (model.getSize() > 0) {
          list.setSelectedIndex(0);
        }
      }
    };

    query.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
      @Override
      public void insertUpdate(javax.swing.event.DocumentEvent e) {
        update.run();
      }

      @Override
      public void removeUpdate(javax.swing.event.DocumentEvent e) {
        update.run();
      }

      @Override
      public void changedUpdate(javax.swing.event.DocumentEvent e) {
        update.run();
      }
    });

    AbstractAction activateAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Command selected = list.getSelectedValue();
        if (selected == null) {
          Toolkit.getDefaultToolkit().beep();
          return;
        }

        dialog.dispose();
        selected.invoke();
      }
    };

    list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "launcher.activate");
    list.getActionMap().put("launcher.activate", activateAction);

    query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "launcher.activate");
    query.getActionMap().put("launcher.activate", activateAction);

    content.getInputMap(javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
      .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "launcher.close");
    content.getActionMap().put("launcher.close", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dialog.dispose();
      }
    });

    update.run();
    dialog.pack();
    dialog.setMinimumSize(new Dimension(500, 320));
    dialog.setLocationRelativeTo(ownerWindow);
    dialog.setVisible(true);
    query.requestFocusInWindow();
  }

  private static boolean matches(Command command, String query) {
    if (query == null) {
      return true;
    }
    String q = query.trim().toLowerCase();
    if (q.isEmpty()) {
      return true;
    }
    return command.searchText.contains(q);
  }

  private static JMenuBar getMenuBar(Window ownerWindow) {
    if (ownerWindow instanceof JFrame) {
      return ((JFrame) ownerWindow).getJMenuBar();
    }
    if (ownerWindow instanceof JDialog) {
      return ((JDialog) ownerWindow).getJMenuBar();
    }
    return null;
  }

  private static JDialog createDialog(Window ownerWindow) {
    JDialog dialog;
    if (ownerWindow instanceof Frame) {
      dialog = new JDialog((Frame) ownerWindow);
    } else if (ownerWindow instanceof Dialog) {
      dialog = new JDialog((Dialog) ownerWindow);
    } else {
      dialog = new JDialog((Frame) null);
    }

    dialog.setTitle("Command Palette");
    dialog.setModal(false);
    dialog.setAlwaysOnTop(true);
    dialog.setResizable(true);
    return dialog;
  }

  private static List<Command> collectCommands(JMenuBar menuBar) {
    List<Command> commands = new ArrayList<>();

    int count = menuBar.getMenuCount();
    for (int i = 0; i < count; i++) {
      JMenu menu = menuBar.getMenu(i);
      if (menu == null) {
        continue;
      }

      String menuText = cleanText(menu.getText());
      if (menuText == null) {
        continue;
      }

      List<String> path = new ArrayList<>();
      path.add(menuText);
      collectFromMenu(menu, path, commands);
    }

    return commands;
  }

  private static void collectFromMenu(JMenu menu, List<String> path, List<Command> out) {
    Component[] items = menu.getMenuComponents();
    for (Component component : items) {
      if (component instanceof JMenu) {
        JMenu submenu = (JMenu) component;
        String text = cleanText(submenu.getText());
        if (text == null) {
          continue;
        }

        path.add(text);
        collectFromMenu(submenu, path, out);
        path.remove(path.size() - 1);
        continue;
      }

      if (!(component instanceof JMenuItem)) {
        continue;
      }

      JMenuItem item = (JMenuItem) component;
      if (item instanceof JMenu) {
        continue;
      }

      String text = cleanText(item.getText());
      if (text == null) {
        continue;
      }

      out.add(new Command(joinPath(path) + " > " + text, item));
    }
  }

  private static String cleanText(String text) {
    if (text == null) {
      return null;
    }
    String t = text.trim();
    if (t.isEmpty()) {
      return null;
    }
    return t;
  }

  private static String joinPath(List<String> path) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < path.size(); i++) {
      if (i > 0) {
        sb.append(" > ");
      }
      sb.append(path.get(i));
    }
    return sb.toString();
  }

  private static final class Command {
    private final String label;
    private final String searchText;
    private final JMenuItem item;

    Command(String label, JMenuItem item) {
      this.label = label;
      this.searchText = label.toLowerCase();
      this.item = item;
    }

    void invoke() {
      if (item == null || !item.isEnabled()) {
        Toolkit.getDefaultToolkit().beep();
        return;
      }

      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          item.doClick();
        }
      });
    }

    @Override
    public String toString() {
      return label;
    }
  }
}
