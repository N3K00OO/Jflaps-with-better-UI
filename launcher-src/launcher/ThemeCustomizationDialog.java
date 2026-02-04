package launcher;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public final class ThemeCustomizationDialog {
  private ThemeCustomizationDialog() {
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
    final JDialog dialog = createDialog(ownerWindow);
    dialog.setTitle("Customize Theme");

    final JCheckBox accentEnabled = new JCheckBox("Override accent color");
    final RgbColorPicker accentPicker = new RgbColorPicker();

    final JCheckBox backgroundEnabled = new JCheckBox("Override background color");
    final RgbColorPicker backgroundPicker = new RgbColorPicker();

    final JCheckBox canvasEnabled = new JCheckBox("Override canvas background");
    final RgbColorPicker canvasPicker = new RgbColorPicker();

    loadInitial(accentEnabled, accentPicker, backgroundEnabled, backgroundPicker, canvasEnabled, canvasPicker);

    accentEnabled.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        accentPicker.setEnabled(accentEnabled.isSelected());
      }
    });

    backgroundEnabled.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        backgroundPicker.setEnabled(backgroundEnabled.isSelected());
      }
    });

    canvasEnabled.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        canvasPicker.setEnabled(canvasEnabled.isSelected());
      }
    });

    accentPicker.setEnabled(accentEnabled.isSelected());
    backgroundPicker.setEnabled(backgroundEnabled.isSelected());
    canvasPicker.setEnabled(canvasEnabled.isSelected());

    JPanel fields = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 1.0;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.insets = new Insets(6, 10, 0, 10);
    fields.add(accentEnabled, c);

    c.gridy++;
    c.insets = new Insets(2, 10, 10, 10);
    fields.add(accentPicker, c);

    c.gridy++;
    c.insets = new Insets(0, 10, 0, 10);
    fields.add(backgroundEnabled, c);

    c.gridy++;
    c.insets = new Insets(2, 10, 10, 10);
    fields.add(backgroundPicker, c);

    c.gridy++;
    c.insets = new Insets(0, 10, 0, 10);
    fields.add(canvasEnabled, c);

    c.gridy++;
    c.insets = new Insets(2, 10, 10, 10);
    fields.add(canvasPicker, c);

    c.gridy++;
    c.weighty = 1.0;
    c.fill = GridBagConstraints.BOTH;
    fields.add(new JLabel(""), c);

    JButton apply = new JButton("Apply");
    apply.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        apply(accentEnabled, accentPicker, backgroundEnabled, backgroundPicker, canvasEnabled, canvasPicker);
        dialog.dispose();
      }
    });

    JButton cancel = new JButton("Cancel");
    cancel.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        dialog.dispose();
      }
    });

    JButton reset = new JButton("Reset");
    reset.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        CustomizationManager.clearAll();
        ThemeManager.refreshCurrentTheme();
        dialog.dispose();
      }
    });

    JPanel buttons = new JPanel();
    buttons.add(reset);
    buttons.add(cancel);
    buttons.add(apply);

    dialog.getContentPane().setLayout(new BorderLayout());
    dialog.getContentPane().add(fields, BorderLayout.CENTER);
    dialog.getContentPane().add(buttons, BorderLayout.SOUTH);

    dialog.getRootPane().setDefaultButton(apply);
    dialog.getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
      .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "launcher.close");
    dialog.getRootPane().getActionMap().put("launcher.close", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dialog.dispose();
      }
    });

    dialog.pack();
    dialog.setMinimumSize(new Dimension(680, 280));
    dialog.setLocationRelativeTo(ownerWindow);
    dialog.setVisible(true);
  }

  private static void loadInitial(JCheckBox accentEnabled, RgbColorPicker accentPicker,
                                  JCheckBox backgroundEnabled, RgbColorPicker backgroundPicker,
                                  JCheckBox canvasEnabled, RgbColorPicker canvasPicker) {
    Color initialAccent = parseHexColor(CustomizationManager.loadAccentColor());
    if (initialAccent != null) {
      accentEnabled.setSelected(true);
      accentPicker.setColor(initialAccent);
    } else {
      accentEnabled.setSelected(false);
    }

    Color initialBackground = parseHexColor(CustomizationManager.loadBackgroundColor());
    if (initialBackground != null) {
      backgroundEnabled.setSelected(true);
      backgroundPicker.setColor(initialBackground);
    } else {
      backgroundEnabled.setSelected(false);
    }

    Color initialCanvas = parseHexColor(CustomizationManager.loadCanvasColor());
    if (initialCanvas != null) {
      canvasEnabled.setSelected(true);
      canvasPicker.setColor(initialCanvas);
    } else {
      canvasEnabled.setSelected(false);
    }
  }

  private static void apply(JCheckBox accentEnabled, RgbColorPicker accentPicker,
                            JCheckBox backgroundEnabled, RgbColorPicker backgroundPicker,
                            JCheckBox canvasEnabled, RgbColorPicker canvasPicker) {
    String accent = null;
    if (accentEnabled.isSelected()) {
      accent = RgbColorPicker.toHex(accentPicker.getColor());
    }

    String background = null;
    if (backgroundEnabled.isSelected()) {
      background = RgbColorPicker.toHex(backgroundPicker.getColor());
    }

    String canvas = null;
    if (canvasEnabled.isSelected()) {
      canvas = RgbColorPicker.toHex(canvasPicker.getColor());
    }

    CustomizationManager.saveAccentColor(accent);
    CustomizationManager.saveBackgroundColor(background);
    CustomizationManager.saveCanvasColor(canvas);
    ThemeManager.refreshCurrentTheme();
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
    dialog.setAlwaysOnTop(true);
    dialog.setResizable(true);
    return dialog;
  }
}
