package launcher;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.prefs.Preferences;

public final class ThemeManager {
  private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
  private static final String KEY_THEME = "theme";

  private static volatile Theme currentTheme = Theme.DARCULA;

  private ThemeManager() {
  }

  public static Theme getCurrentTheme() {
    return currentTheme;
  }

  public static Theme loadPreferredTheme(Theme fallback) {
    String stored = null;
    try {
      stored = PREFS.get(KEY_THEME, null);
    } catch (Throwable ignored) {
      stored = null;
    }

    if (stored == null || stored.trim().isEmpty()) {
      return fallback;
    }

    return Theme.fromString(stored, fallback);
  }

  public static void applyInitialTheme(Theme theme) {
    Theme resolved = (theme == null) ? Theme.DARCULA : theme;
    currentTheme = resolved;

    enableWindowDecorations();
    CustomizationManager.applyExtraDefaultsToFlatLaf();
    CanvasColors.applyToJflap();
    applyLookAndFeel(resolved);
  }

  public static void switchTheme(Theme theme) {
    applyTheme(theme, true);
  }

  public static void refreshCurrentTheme() {
    applyTheme(currentTheme, false);
  }

  private static void applyTheme(Theme theme, boolean persistPreference) {
    final Theme resolved = (theme == null) ? Theme.DARCULA : theme;
    currentTheme = resolved;

    if (persistPreference) {
      try {
        PREFS.put(KEY_THEME, resolved.name().toLowerCase());
      } catch (Throwable ignored) {
        // best-effort only
      }
    }

    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        enableWindowDecorations();
        CustomizationManager.applyExtraDefaultsToFlatLaf();
        CanvasColors.applyToJflap();
        applyLookAndFeel(resolved);

        try {
          FlatLaf.updateUI();
          FlatLaf.revalidateAndRepaintAllFramesAndDialogs();
        } catch (Throwable ignored) {
          // best-effort only
        }

        UiEnhancements.refreshAllWindows();
      }
    };

    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    } else {
      SwingUtilities.invokeLater(runnable);
    }
  }

  private static void applyLookAndFeel(Theme theme) {
    boolean installed = false;
    try {
      installed = FlatLaf.setup(createLookAndFeel(theme));
    } catch (Throwable ignored) {
      installed = false;
    }

    if (installed) {
      return;
    }

    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static LookAndFeel createLookAndFeel(Theme theme) {
    switch (theme) {
      case DARCULA:
        return new FlatDarculaLaf();
      case DARK:
        return new FlatDarkLaf();
      case LIGHT:
        return new FlatLightLaf();
      case INTELLIJ:
      default:
        return new FlatIntelliJLaf();
    }
  }

  private static void enableWindowDecorations() {
    try {
      JFrame.setDefaultLookAndFeelDecorated(true);
      JDialog.setDefaultLookAndFeelDecorated(true);
    } catch (Throwable ignored) {
      // best-effort only
    }

    try {
      if (FlatLaf.supportsNativeWindowDecorations()) {
        FlatLaf.setUseNativeWindowDecorations(true);
      }
    } catch (Throwable ignored) {
      // best-effort only
    }
  }
}
