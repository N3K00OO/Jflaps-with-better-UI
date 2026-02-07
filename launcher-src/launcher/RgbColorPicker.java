package launcher;

import javax.swing.BorderFactory;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public final class RgbColorPicker extends JPanel {
  private final JSpinner red = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
  private final JSpinner green = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
  private final JSpinner blue = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));

  private final JPanel preview = new JPanel();
  private final JTextField hex = new JTextField();

  private boolean updating = false;

  public RgbColorPicker() {
    super(new GridBagLayout());

    preview.setPreferredSize(new Dimension(28, 28));
    preview.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 40)));

    hex.setEditable(true);
    hex.setColumns(8);
    try {
      hex.putClientProperty("JTextField.placeholderText", "#RRGGBB");
    } catch (Throwable ignored) {
      // best-effort only
    }

    configureSpinner(red);
    configureSpinner(green);
    configureSpinner(blue);

    ChangeListener listener = new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        updatePreview();
      }
    };

    red.addChangeListener(listener);
    green.addChangeListener(listener);
    blue.addChangeListener(listener);

    hex.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        updateFromHexIfValid();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        updateFromHexIfValid();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        updateFromHexIfValid();
      }
    });

    int row = 0;

    add(new JLabel("R"), gbc(0, row));
    add(red, gbc(1, row));
    add(new JLabel("G"), gbc(2, row));
    add(green, gbc(3, row));
    add(new JLabel("B"), gbc(4, row));
    add(blue, gbc(5, row));

    GridBagConstraints previewConstraints = gbc(6, row);
    previewConstraints.insets = new Insets(2, 10, 2, 0);
    add(preview, previewConstraints);

    GridBagConstraints hexConstraints = gbc(7, row);
    hexConstraints.insets = new Insets(2, 10, 2, 0);
    add(hex, hexConstraints);

    updatePreview();
  }

  public void setColor(Color color) {
    if (color == null) {
      color = Color.BLACK;
    }

    red.setValue(color.getRed());
    green.setValue(color.getGreen());
    blue.setValue(color.getBlue());
    updatePreview();
  }

  public Color getColor() {
    return new Color(intValue(red), intValue(green), intValue(blue));
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    red.setEnabled(enabled);
    green.setEnabled(enabled);
    blue.setEnabled(enabled);
    preview.setEnabled(enabled);
    hex.setEnabled(enabled);

    setSpinnerEditorEnabled(red, enabled);
    setSpinnerEditorEnabled(green, enabled);
    setSpinnerEditorEnabled(blue, enabled);
  }

  private static int intValue(JSpinner spinner) {
    Object value = spinner.getValue();
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return 0;
  }

  private void updatePreview() {
    Color color = getColor();
    preview.setBackground(color);
    withUpdating(new Runnable() {
      @Override
      public void run() {
        hex.setText(toHex(color));
      }
    });
  }

  public static String toHex(Color color) {
    if (color == null) {
      return null;
    }
    return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
  }

  private static GridBagConstraints gbc(int x, int y) {
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = x;
    c.gridy = y;
    c.insets = new Insets(2, 6, 2, 0);
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.WEST;
    return c;
  }

  private void updateFromHexIfValid() {
    if (updating || !hex.isEnabled()) {
      return;
    }

    String text = hex.getText();
    if (text == null) {
      return;
    }

    String t = text.trim();
    if (t.isEmpty()) {
      return;
    }

    if (t.startsWith("#")) {
      t = t.substring(1);
    }

    if (t.length() != 6) {
      return;
    }

    int rgb;
    try {
      rgb = Integer.parseInt(t, 16);
    } catch (NumberFormatException ignored) {
      return;
    }

    final int r = (rgb >> 16) & 0xff;
    final int g = (rgb >> 8) & 0xff;
    final int b = rgb & 0xff;

    withUpdating(new Runnable() {
      @Override
      public void run() {
        red.setValue(r);
        green.setValue(g);
        blue.setValue(b);
      }
    });

    updatePreview();
  }

  private void withUpdating(Runnable r) {
    boolean prev = updating;
    updating = true;
    try {
      r.run();
    } finally {
      updating = prev;
    }
  }

  private static void configureSpinner(JSpinner spinner) {
    if (spinner == null) {
      return;
    }

    try {
      spinner.setEditor(new JSpinner.NumberEditor(spinner, "0"));
    } catch (Throwable ignored) {
      // best-effort only
    }

    try {
      JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
      if (editor != null) {
        JTextField field = editor.getTextField();
        field.setColumns(3);
        field.setEditable(true);
        if (field instanceof JFormattedTextField) {
          ((JFormattedTextField) field).setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        }
      }
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static void setSpinnerEditorEnabled(JSpinner spinner, boolean enabled) {
    if (spinner == null) {
      return;
    }

    try {
      JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
      if (editor != null) {
        editor.setEnabled(enabled);
        editor.getTextField().setEnabled(enabled);
      }
    } catch (Throwable ignored) {
      // best-effort only
    }
  }
}
