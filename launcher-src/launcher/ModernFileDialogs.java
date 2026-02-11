package launcher;

import javax.swing.JFileChooser;
import java.io.File;
import java.lang.reflect.Field;

final class ModernFileDialogs {
  private ModernFileDialogs() {
  }

  static void install() {
    try {
      Class<?> universe = Class.forName("gui.environment.Universe");
      Field chooserField = universe.getField("CHOOSER");

      Object existing = chooserField.get(null);
      if (existing instanceof ModernFileChooser) {
        return;
      }

      File currentDir = null;
      if (existing instanceof JFileChooser) {
        try {
          currentDir = ((JFileChooser) existing).getCurrentDirectory();
        } catch (Throwable ignored) {
          currentDir = null;
        }
      }

      chooserField.set(null, new ModernFileChooser(currentDir));
    } catch (Throwable ignored) {
      // best-effort only
    }
  }
}

