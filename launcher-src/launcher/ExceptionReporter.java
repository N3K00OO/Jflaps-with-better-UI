package launcher;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Captures uncaught exceptions (including EDT exceptions) and writes them to a log file.
 *
 * <p>This helps diagnose user-reported "it just gave up" crashes where Swing otherwise only
 * prints to stderr or fails silently depending on how the app is launched.</p>
 */
public final class ExceptionReporter {
  private static volatile boolean installed = false;
  private static volatile boolean showingDialog = false;

  private ExceptionReporter() {
  }

  public static synchronized void install() {
    if (installed) {
      return;
    }
    installed = true;

    try {
      Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
          report(e, "Uncaught exception on thread: " + safeThreadName(t));
        }
      });
    } catch (Throwable ignored) {
      // best-effort only
    }

    try {
      Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueue() {
        @Override
        protected void dispatchEvent(AWTEvent event) {
          try {
            super.dispatchEvent(event);
          } catch (Throwable t) {
            report(t, "Exception while dispatching AWT event: " + safeEventName(event));
          }
        }
      });
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static void report(Throwable t, String context) {
    if (t == null) {
      return;
    }

    try {
      System.err.println("[JFLAP Modern] " + context);
      t.printStackTrace(System.err);
    } catch (Throwable ignored) {
      // ignore
    }

    final File logFile = appendToLog(context, t);

    // Avoid spamming dialogs if multiple exceptions occur.
    if (showingDialog) {
      return;
    }
    showingDialog = true;

    try {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          try {
            String path = (logFile != null) ? logFile.getAbsolutePath() : "(unable to write log)";
            JOptionPane.showMessageDialog(
              null,
              "JFLAP encountered an error and may not behave correctly.\n\n"
                + "Details were written to:\n"
                + path,
              "JFLAP Modern - Error",
              JOptionPane.ERROR_MESSAGE
            );
          } catch (Throwable ignored) {
            // ignore
          } finally {
            showingDialog = false;
          }
        }
      });
    } catch (Throwable ignored) {
      showingDialog = false;
    }
  }

  private static File appendToLog(String context, Throwable t) {
    PrintWriter out = null;
    try {
      File dir = defaultLogDir();
      if (dir != null && !dir.exists()) {
        dir.mkdirs();
      }

      File file = new File((dir != null) ? dir : new File("."), "jflap-modern-crash.log");
      out = new PrintWriter(new FileWriter(file, true));
      out.println("=== " + timestamp() + " ===");
      if (context != null) {
        out.println(context);
      }
      if (t != null) {
        t.printStackTrace(out);
      }
      out.println();
      out.flush();
      return file;
    } catch (Throwable ignored) {
      return null;
    } finally {
      try {
        if (out != null) {
          out.close();
        }
      } catch (Throwable ignored) {
        // ignore
      }
    }
  }

  private static File defaultLogDir() {
    try {
      String home = System.getProperty("user.home");
      if (home == null || home.trim().isEmpty()) {
        return null;
      }
      return new File(home, ".jflap-modern");
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static String timestamp() {
    try {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    } catch (Throwable ignored) {
      return String.valueOf(System.currentTimeMillis());
    }
  }

  private static String safeThreadName(Thread t) {
    try {
      if (t != null) {
        return t.getName();
      }
    } catch (Throwable ignored) {
      // ignore
    }
    return "(unknown)";
  }

  private static String safeEventName(AWTEvent e) {
    try {
      if (e != null) {
        return e.getClass().getName();
      }
    } catch (Throwable ignored) {
      // ignore
    }
    return "(unknown)";
  }
}

