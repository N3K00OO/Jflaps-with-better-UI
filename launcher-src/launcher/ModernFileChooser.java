package launcher;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.HeadlessException;
import java.awt.FileDialog;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.FilenameFilter;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ModernFileChooser extends JFileChooser {
  private static final Pattern EXT_PATTERN = Pattern.compile("\\*\\.([a-zA-Z0-9]+)");
  private static final int FAILED_OPTION = -2;

  ModernFileChooser(File currentDirectory) {
    super(currentDirectory);
  }

  @Override
  public int showOpenDialog(Component parent) throws HeadlessException {
    return showSystemDialog(parent, FileDialog.LOAD, null);
  }

  @Override
  public int showSaveDialog(Component parent) throws HeadlessException {
    return showSystemDialog(parent, FileDialog.SAVE, null);
  }

  @Override
  public int showDialog(Component parent, String approveButtonText) throws HeadlessException {
    // Custom approve buttons are not supported by AWT FileDialog. Fall back to Swing.
    return super.showDialog(parent, approveButtonText);
  }

  private int showSystemDialog(Component parent, int awtMode, String approveButtonText) throws HeadlessException {
    try {
      int result = tryAwtFileDialog(parent, awtMode, approveButtonText);
      if (result != FAILED_OPTION) {
        return result;
      }

      // Fall back to Swing JFileChooser.
      if (awtMode == FileDialog.SAVE) {
        return super.showSaveDialog(parent);
      }
      return super.showOpenDialog(parent);
    } catch (HeadlessException e) {
      throw e;
    } catch (Throwable ignored) {
      if (awtMode == FileDialog.SAVE) {
        return super.showSaveDialog(parent);
      }
      return super.showOpenDialog(parent);
    }
  }

  private int tryAwtFileDialog(Component parent, int awtMode, String approveButtonText) {
    // FileDialog only supports the standard Open/Save variants.
    if (approveButtonText != null) {
      return FAILED_OPTION;
    }

    final Window owner = resolveOwnerWindow(parent);
    final String title = safeDialogTitle();

    final FileDialog dialog = createFileDialog(owner, title, awtMode);
    if (dialog == null) {
      return FAILED_OPTION;
    }

    dialog.setMultipleMode(awtMode == FileDialog.LOAD && isMultiSelectionEnabled());

    File initialDir = null;
    try {
      initialDir = getCurrentDirectory();
    } catch (Throwable ignored) {
      initialDir = null;
    }

    File preselected = null;
    try {
      preselected = getSelectedFile();
    } catch (Throwable ignored) {
      preselected = null;
    }

    if (preselected != null) {
      try {
        if (preselected.isDirectory()) {
          dialog.setDirectory(preselected.getAbsolutePath());
        } else {
          File parentDir = preselected.getParentFile();
          if (parentDir != null) {
            dialog.setDirectory(parentDir.getAbsolutePath());
          }
          dialog.setFile(preselected.getName());
        }
      } catch (Throwable ignored) {
        // best-effort only
      }
    } else if (initialDir != null) {
      try {
        dialog.setDirectory(initialDir.getAbsolutePath());
      } catch (Throwable ignored) {
        // best-effort only
      }
    }

    String[] exts = extensionsForCurrentFilter();
    FilenameFilter filter = filenameFilterForExtensions(exts);
    if (filter != null) {
      try {
        dialog.setFilenameFilter(filter);
      } catch (Throwable ignored) {
        // best-effort only
      }
    }

    final int[] resultHolder = new int[] {JFileChooser.CANCEL_OPTION};

    dialog.setVisible(true);

    File[] files;
    try {
      files = dialog.getFiles();
    } catch (Throwable ignored) {
      files = null;
    }

    if (files == null || files.length == 0) {
      resultHolder[0] = JFileChooser.CANCEL_OPTION;
      return resultHolder[0];
    }

    try {
      setSelectedFiles(files);
    } catch (Throwable ignored) {
      // best-effort only
    }

    try {
      setSelectedFile(files[0]);
    } catch (Throwable ignored) {
      // best-effort only
    }

    try {
      String dir = dialog.getDirectory();
      if (dir != null && !dir.trim().isEmpty()) {
        setCurrentDirectory(new File(dir));
      }
    } catch (Throwable ignored) {
      // best-effort only
    }

    resultHolder[0] = JFileChooser.APPROVE_OPTION;

    return resultHolder[0];
  }

  private Window resolveOwnerWindow(Component parent) {
    try {
      if (parent instanceof Window) {
        return (Window) parent;
      }
    } catch (Throwable ignored) {
      // ignore
    }

    try {
      if (parent != null) {
        return SwingUtilities.windowForComponent(parent);
      }
    } catch (Throwable ignored) {
      // ignore
    }

    try {
      return KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static FileDialog createFileDialog(Window owner, String title, int mode) {
    try {
      if (owner instanceof Dialog) {
        return new FileDialog((Dialog) owner, title, mode);
      }
      if (owner instanceof Frame) {
        return new FileDialog((Frame) owner, title, mode);
      }
      return new FileDialog((Frame) null, title, mode);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private String safeDialogTitle() {
    try {
      String t = getDialogTitle();
      if (t != null && !t.trim().isEmpty()) {
        return t.trim();
      }
    } catch (Throwable ignored) {
      // ignore
    }

    return "Select File";
  }

  private String[] extensionsForCurrentFilter() {
    FileFilter ff = null;
    try {
      ff = getFileFilter();
    } catch (Throwable ignored) {
      ff = null;
    }

    if (ff == null) {
      return new String[0];
    }

    if (ff instanceof file.XMLCodec) {
      return new String[] {"jff"};
    }

    if (ff instanceof javax.swing.filechooser.FileNameExtensionFilter) {
      javax.swing.filechooser.FileNameExtensionFilter nef = (javax.swing.filechooser.FileNameExtensionFilter) ff;
      return normalizeExtensions(nef.getExtensions());
    }

    if ("gui.action.FileNameExtensionFilter".equals(ff.getClass().getName())) {
      return normalizeExtensions(tryReadStringArrayField(ff, "myAcceptedFormats"));
    }

    return parseExtensionsFromDescription(safeDescription(ff));
  }

  private static FilenameFilter filenameFilterForExtensions(final String[] exts) {
    if (exts == null || exts.length == 0) {
      return null;
    }

    return new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        if (name == null) {
          return false;
        }

        try {
          if (dir != null) {
            File f = new File(dir, name);
            if (f.isDirectory()) {
              return true;
            }
          }
        } catch (Throwable ignored) {
          // ignore
        }

        String lower = name.toLowerCase();
        for (int i = 0; i < exts.length; i++) {
          String ext = exts[i];
          if (ext == null || ext.isEmpty()) {
            continue;
          }
          if (lower.endsWith("." + ext.toLowerCase())) {
            return true;
          }
        }
        return false;
      }
    };
  }

  private static String safeDescription(FileFilter filter) {
    if (filter == null) {
      return "Files";
    }
    try {
      String d = filter.getDescription();
      if (d != null && !d.trim().isEmpty()) {
        return d.trim();
      }
    } catch (Throwable ignored) {
      // fall through
    }
    return "Files";
  }

  private static String[] tryReadStringArrayField(Object obj, String fieldName) {
    if (obj == null || fieldName == null) {
      return new String[0];
    }

    try {
      Field f = obj.getClass().getDeclaredField(fieldName);
      f.setAccessible(true);
      Object value = f.get(obj);
      if (value instanceof String[]) {
        return (String[]) value;
      }
    } catch (Throwable ignored) {
      // best-effort only
    }

    return new String[0];
  }

  private static String[] parseExtensionsFromDescription(String description) {
    if (description == null) {
      return new String[0];
    }

    Matcher m = EXT_PATTERN.matcher(description);
    List<String> exts = new ArrayList<>();
    while (m.find()) {
      String ext = m.group(1);
      if (ext != null && !ext.trim().isEmpty()) {
        exts.add(ext.trim());
      }
    }

    return normalizeExtensions(exts.toArray(new String[0]));
  }

  private static String[] normalizeExtensions(String[] extensions) {
    if (extensions == null || extensions.length == 0) {
      return new String[0];
    }

    List<String> out = new ArrayList<>();
    for (int i = 0; i < extensions.length; i++) {
      String ext = extensions[i];
      if (ext == null) {
        continue;
      }

      String e = ext.trim();
      if (e.isEmpty()) {
        continue;
      }

      if (e.startsWith("*.")) {
        e = e.substring(2);
      } else if (e.startsWith(".")) {
        e = e.substring(1);
      }

      if (e.isEmpty()) {
        continue;
      }

      out.add(e.toLowerCase());
    }

    return out.toArray(new String[0]);
  }
}
