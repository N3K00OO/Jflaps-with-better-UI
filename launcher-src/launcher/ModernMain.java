package launcher;

import java.util.ArrayList;
import java.util.List;

public final class ModernMain {
  public static void main(String[] args) {
    ParsedArgs parsed = ParsedArgs.parse(args);
    if (parsed.showHelp) {
      printHelp();
      return;
    }

    ExceptionReporter.install();

    enableSmootherText();
    applyUiScale(parsed.uiScale);
    ThemeManager.applyInitialTheme(parsed.theme);
    UiEnhancements.install();
    ModernFileDialogs.install();

    if (parsed.selfTest) {
      int exitCode = SelfTest.run();
      try {
        System.exit(exitCode);
      } catch (Throwable ignored) {
        // ignore
      }
      return;
    }

    delegateToJflap(parsed.remainingArgs);
  }

  private static void printHelp() {
    System.out.println("JFLAP modern launcher options:");
    System.out.println("  --theme=light|dark|intellij|darcula");
    System.out.println("  --uiScale=<number>        (e.g. 1.25)");
    System.out.println("  --light | --dark | --intellij | --darcula");
    System.out.println("  --selftest               (diagnose crashes / window creation)");
    System.out.println("  --help");
    System.out.println("");
    System.out.println("In-app:");
    System.out.println("  Ctrl+K opens Command Palette");
    System.out.println("  View > Theme switches theme (saved)");
    System.out.println("  View > Customize Theme... sets accent/background/canvas");
  }

  private static void enableSmootherText() {
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");
  }

  private static void applyUiScale(String uiScale) {
    if (isBlank(uiScale)) {
      return;
    }
    System.setProperty("flatlaf.uiScale", uiScale.trim());
  }

  private static void delegateToJflap(String[] args) {
    try {
      Class<?> mainClass = Class.forName("gui.Main");
      mainClass.getMethod("main", String[].class).invoke(null, (Object) args);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Failed to launch JFLAP (gui.Main).", e);
    }
  }

  private static final class ParsedArgs {
    final boolean showHelp;
    final boolean selfTest;
    final Theme theme;
    final String uiScale;
    final String[] remainingArgs;

    private ParsedArgs(boolean showHelp, boolean selfTest, Theme theme, String uiScale, String[] remainingArgs) {
      this.showHelp = showHelp;
      this.selfTest = selfTest;
      this.theme = theme;
      this.uiScale = uiScale;
      this.remainingArgs = remainingArgs;
    }

    static ParsedArgs parse(String[] args) {
      boolean showHelp = false;
      boolean selfTest = false;
      Theme theme = parseThemeFromEnvPropsOrPrefs();
      String uiScale = System.getProperty("flatlaf.uiScale");

      List<String> remaining = new ArrayList<>();
      for (String arg : args) {
        if (arg == null) {
          continue;
        }

        switch (arg) {
          case "-h":
          case "--help":
            showHelp = true;
            continue;
          case "--selftest":
            selfTest = true;
            continue;
          case "--light":
            theme = Theme.LIGHT;
            continue;
          case "--dark":
            theme = Theme.DARK;
            continue;
          case "--intellij":
            theme = Theme.INTELLIJ;
            continue;
          case "--darcula":
            theme = Theme.DARCULA;
            continue;
          default:
            break;
        }

        if (arg.startsWith("--theme=")) {
          theme = parseThemeValue(arg.substring("--theme=".length()));
          continue;
        }

        if (arg.startsWith("--uiScale=")) {
          uiScale = arg.substring("--uiScale=".length());
          continue;
        }

        remaining.add(arg);
      }

      return new ParsedArgs(showHelp, selfTest, theme, uiScale, remaining.toArray(new String[0]));
    }

    private static Theme parseThemeFromEnvPropsOrPrefs() {
      String theme = System.getProperty("jflap.theme");
      if (isBlank(theme)) {
        theme = System.getenv("JFLAP_THEME");
      }

      if (isBlank(theme)) {
        return ThemeManager.loadPreferredTheme(Theme.DARCULA);
      }

      return Theme.fromString(theme, Theme.DARCULA);
    }

    private static Theme parseThemeValue(String value) {
      return Theme.fromString(value, Theme.DARCULA);
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
