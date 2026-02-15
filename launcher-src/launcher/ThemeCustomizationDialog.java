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

    final JCheckBox selectionEnabled = new JCheckBox("Override selection box color");
    final RgbColorPicker selectionPicker = new RgbColorPicker();

    final JCheckBox nodeEnabled = new JCheckBox("Override node color");
    final RgbColorPicker nodePicker = new RgbColorPicker();

    final JCheckBox arrowEnabled = new JCheckBox("Override arrow color");
    final RgbColorPicker arrowPicker = new RgbColorPicker();

    final JCheckBox finalRingEnabled = new JCheckBox("Override final-state ring color");
    final RgbColorPicker finalRingPicker = new RgbColorPicker();

    final JCheckBox startTriangleEnabled = new JCheckBox("Override start triangle color");
    final RgbColorPicker startTrianglePicker = new RgbColorPicker();

    loadInitial(accentEnabled, accentPicker, backgroundEnabled, backgroundPicker,
      canvasEnabled, canvasPicker, selectionEnabled, selectionPicker,
      nodeEnabled, nodePicker, arrowEnabled, arrowPicker,
      finalRingEnabled, finalRingPicker, startTriangleEnabled, startTrianglePicker);

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

    selectionEnabled.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        selectionPicker.setEnabled(selectionEnabled.isSelected());
      }
    });

    nodeEnabled.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        nodePicker.setEnabled(nodeEnabled.isSelected());
      }
    });

    arrowEnabled.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        arrowPicker.setEnabled(arrowEnabled.isSelected());
      }
    });

    finalRingEnabled.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        finalRingPicker.setEnabled(finalRingEnabled.isSelected());
      }
    });

    startTriangleEnabled.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        startTrianglePicker.setEnabled(startTriangleEnabled.isSelected());
      }
    });

    accentPicker.setEnabled(accentEnabled.isSelected());
    backgroundPicker.setEnabled(backgroundEnabled.isSelected());
    canvasPicker.setEnabled(canvasEnabled.isSelected());
    selectionPicker.setEnabled(selectionEnabled.isSelected());
    nodePicker.setEnabled(nodeEnabled.isSelected());
    arrowPicker.setEnabled(arrowEnabled.isSelected());
    finalRingPicker.setEnabled(finalRingEnabled.isSelected());
    startTrianglePicker.setEnabled(startTriangleEnabled.isSelected());

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
    c.insets = new Insets(0, 10, 0, 10);
    fields.add(selectionEnabled, c);

    c.gridy++;
    c.insets = new Insets(2, 10, 10, 10);
    fields.add(selectionPicker, c);

    c.gridy++;
    c.insets = new Insets(0, 10, 0, 10);
    fields.add(nodeEnabled, c);

    c.gridy++;
    c.insets = new Insets(2, 10, 10, 10);
    fields.add(nodePicker, c);

    c.gridy++;
    c.insets = new Insets(0, 10, 0, 10);
    fields.add(arrowEnabled, c);

    c.gridy++;
    c.insets = new Insets(2, 10, 10, 10);
    fields.add(arrowPicker, c);

    c.gridy++;
    c.insets = new Insets(0, 10, 0, 10);
    fields.add(finalRingEnabled, c);

    c.gridy++;
    c.insets = new Insets(2, 10, 10, 10);
    fields.add(finalRingPicker, c);

    c.gridy++;
    c.insets = new Insets(0, 10, 0, 10);
    fields.add(startTriangleEnabled, c);

    c.gridy++;
    c.insets = new Insets(2, 10, 10, 10);
    fields.add(startTrianglePicker, c);

    c.gridy++;
    c.weighty = 1.0;
    c.fill = GridBagConstraints.BOTH;
    fields.add(new JLabel(""), c);

    JButton apply = new JButton("Apply");
    apply.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        apply(accentEnabled, accentPicker, backgroundEnabled, backgroundPicker,
          canvasEnabled, canvasPicker, selectionEnabled, selectionPicker,
          nodeEnabled, nodePicker, arrowEnabled, arrowPicker,
          finalRingEnabled, finalRingPicker, startTriangleEnabled, startTrianglePicker);
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
    dialog.setMinimumSize(new Dimension(680, 520));
    dialog.setLocationRelativeTo(ownerWindow);
    dialog.setVisible(true);
  }

  private static void loadInitial(JCheckBox accentEnabled, RgbColorPicker accentPicker,
                                  JCheckBox backgroundEnabled, RgbColorPicker backgroundPicker,
                                  JCheckBox canvasEnabled, RgbColorPicker canvasPicker,
                                  JCheckBox selectionEnabled, RgbColorPicker selectionPicker,
                                  JCheckBox nodeEnabled, RgbColorPicker nodePicker,
                                  JCheckBox arrowEnabled, RgbColorPicker arrowPicker,
                                  JCheckBox finalRingEnabled, RgbColorPicker finalRingPicker,
                                  JCheckBox startTriangleEnabled, RgbColorPicker startTrianglePicker) {
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

    Color initialSelection = parseHexColor(CustomizationManager.loadSelectionBoxColor());
    if (initialSelection != null) {
      selectionEnabled.setSelected(true);
      selectionPicker.setColor(initialSelection);
    } else {
      selectionEnabled.setSelected(false);
    }

    Color initialNode = parseHexColor(CustomizationManager.loadNodeColor());
    if (initialNode != null) {
      nodeEnabled.setSelected(true);
      nodePicker.setColor(initialNode);
    } else {
      nodeEnabled.setSelected(false);
    }

    Color initialArrow = parseHexColor(CustomizationManager.loadArrowColor());
    if (initialArrow != null) {
      arrowEnabled.setSelected(true);
      arrowPicker.setColor(initialArrow);
    } else {
      arrowEnabled.setSelected(false);
    }

    Color initialFinalRing = parseHexColor(CustomizationManager.loadFinalRingColor());
    if (initialFinalRing != null) {
      finalRingEnabled.setSelected(true);
      finalRingPicker.setColor(initialFinalRing);
    } else {
      finalRingEnabled.setSelected(false);
    }

    Color initialStartTriangle = parseHexColor(CustomizationManager.loadStartTriangleColor());
    if (initialStartTriangle != null) {
      startTriangleEnabled.setSelected(true);
      startTrianglePicker.setColor(initialStartTriangle);
    } else {
      startTriangleEnabled.setSelected(false);
    }
  }

  private static void apply(JCheckBox accentEnabled, RgbColorPicker accentPicker,
                            JCheckBox backgroundEnabled, RgbColorPicker backgroundPicker,
                            JCheckBox canvasEnabled, RgbColorPicker canvasPicker,
                            JCheckBox selectionEnabled, RgbColorPicker selectionPicker,
                            JCheckBox nodeEnabled, RgbColorPicker nodePicker,
                            JCheckBox arrowEnabled, RgbColorPicker arrowPicker,
                            JCheckBox finalRingEnabled, RgbColorPicker finalRingPicker,
                            JCheckBox startTriangleEnabled, RgbColorPicker startTrianglePicker) {
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

    String selection = null;
    if (selectionEnabled.isSelected()) {
      selection = RgbColorPicker.toHex(selectionPicker.getColor());
    }

    String node = null;
    if (nodeEnabled.isSelected()) {
      node = RgbColorPicker.toHex(nodePicker.getColor());
    }

    String arrow = null;
    if (arrowEnabled.isSelected()) {
      arrow = RgbColorPicker.toHex(arrowPicker.getColor());
    }

    String finalRing = null;
    if (finalRingEnabled.isSelected()) {
      finalRing = RgbColorPicker.toHex(finalRingPicker.getColor());
    }

    String startTriangle = null;
    if (startTriangleEnabled.isSelected()) {
      startTriangle = RgbColorPicker.toHex(startTrianglePicker.getColor());
    }

    CustomizationManager.saveAccentColor(accent);
    CustomizationManager.saveBackgroundColor(background);
    CustomizationManager.saveCanvasColor(canvas);
    CustomizationManager.saveSelectionBoxColor(selection);
    CustomizationManager.saveNodeColor(node);
    CustomizationManager.saveArrowColor(arrow);
    CustomizationManager.saveFinalRingColor(finalRing);
    CustomizationManager.saveStartTriangleColor(startTriangle);
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
