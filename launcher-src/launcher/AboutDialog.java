package launcher;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class AboutDialog {
  private AboutDialog() {
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

  public static void show(Window ownerWindow) {
    JDialog dialog = createDialog(ownerWindow);
    dialog.setTitle("About - Modern UI");

    JTextArea textArea = new JTextArea(readResourceText("MODIFICATIONS.txt"));
    textArea.setEditable(false);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setCaretPosition(0);

    JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setPreferredSize(new Dimension(720, 420));

    JButton close = new JButton("Close");
    close.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        dialog.dispose();
      }
    });

    JPanel buttons = new JPanel();
    buttons.add(close);

    dialog.getContentPane().setLayout(new BorderLayout());
    dialog.getContentPane().add(scrollPane, BorderLayout.CENTER);
    dialog.getContentPane().add(buttons, BorderLayout.SOUTH);

    dialog.getRootPane().setDefaultButton(close);
    dialog.getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
      .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "launcher.close");
    dialog.getRootPane().getActionMap().put("launcher.close", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dialog.dispose();
      }
    });

    dialog.pack();
    dialog.setLocationRelativeTo(ownerWindow);
    dialog.setVisible(true);
  }

  private static String readResourceText(String resourceName) {
    if (resourceName == null) {
      return fallbackText();
    }

    InputStream in = AboutDialog.class.getClassLoader().getResourceAsStream(resourceName);
    if (in == null) {
      return fallbackText();
    }

    StringBuilder sb = new StringBuilder();
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
      try {
        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line).append('\n');
        }
      } finally {
        reader.close();
      }
    } catch (Exception ignored) {
      return fallbackText();
    }

    return sb.toString();
  }

  private static String fallbackText() {
    return "JFLAP Modern UI build.\n\n" +
      "This jar contains the original JFLAP LICENSE and a MODIFICATIONS.txt file.\n";
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

    dialog.setModal(false);
    dialog.setAlwaysOnTop(false);
    dialog.setResizable(true);
    return dialog;
  }
}

