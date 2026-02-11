package launcher;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal smoke tests that can be run on a user's machine to diagnose crashes.
 *
 * <p>Run with: {@code java -jar JFLAP7.1-modern.jar --selftest}</p>
 */
public final class SelfTest {
  private SelfTest() {
  }

  public static int run() {
    System.out.println("[JFLAP Modern] Self-test starting...");

    final List<String> failures = new ArrayList<>();
    final List<Window> windowsToDispose = new ArrayList<>();

    try {
      runOnEdt(new Runnable() {
        @Override
        public void run() {
          try {
            testReplacementClasses(failures);
            if (!GraphicsEnvironment.isHeadless()) {
              testCreateFrames(windowsToDispose, failures);
            } else {
              System.out.println("[JFLAP Modern] Headless environment detected; skipping window creation tests.");
            }
          } catch (Throwable t) {
            failures.add("Unexpected exception during self-test: " + t);
            t.printStackTrace(System.err);
          } finally {
            for (int i = 0; i < windowsToDispose.size(); i++) {
              try {
                windowsToDispose.get(i).dispose();
              } catch (Throwable ignored) {
                // ignore
              }
            }
          }
        }
      });
    } catch (Throwable t) {
      failures.add("Failed to run self-test on EDT: " + t);
      t.printStackTrace(System.err);
    }

    if (!failures.isEmpty()) {
      System.err.println("[JFLAP Modern] Self-test FAILED:");
      for (int i = 0; i < failures.size(); i++) {
        System.err.println("  - " + failures.get(i));
      }
      return 1;
    }

    System.out.println("[JFLAP Modern] Self-test OK.");
    return 0;
  }

  private static void runOnEdt(Runnable r) throws Exception {
    if (SwingUtilities.isEventDispatchThread()) {
      r.run();
      return;
    }
    SwingUtilities.invokeAndWait(r);
  }

  private static void testReplacementClasses(List<String> failures) {
    // Ensure that our replacement popup class matches the expected constructor signature.
    try {
      Class<?> arrowTool = Class.forName("gui.editor.ArrowTool");
      Class<?> emptyMenu = Class.forName("gui.editor.ArrowTool$EmptyMenu");
      Constructor<?> ctor = emptyMenu.getDeclaredConstructor(arrowTool);
      if (ctor == null) {
        failures.add("ArrowTool$EmptyMenu missing expected constructor.");
      }
    } catch (Throwable t) {
      failures.add("ArrowTool$EmptyMenu replacement check failed: " + t);
    }
  }

  private static void testCreateFrames(List<Window> windowsToDispose, List<String> failures) {
    java.lang.reflect.Method createFrame = null;
    try {
      Class<?> frameFactory = Class.forName("gui.environment.FrameFactory");
      createFrame = frameFactory.getMethod("createFrame", Serializable.class);
    } catch (Throwable t) {
      failures.add("Failed to resolve gui.environment.FrameFactory.createFrame(Serializable): " + t);
      return;
    }

    // Mirrors the New... dialog buttons (at least the most common ones).
    String[] types = new String[] {
      "automata.fsa.FiniteStateAutomaton",
      "automata.mealy.MealyMachine",
      "automata.mealy.MooreMachine",
      "automata.pda.PushdownAutomaton",
      "automata.turing.TuringMachine"
    };

    for (int i = 0; i < types.length; i++) {
      String type = types[i];
      Serializable instance;
      try {
        instance = (Serializable) Class.forName(type).getDeclaredConstructor().newInstance();
      } catch (Throwable t) {
        failures.add("Failed to instantiate " + type + ": " + t);
        continue;
      }

      Object frameObj;
      try {
        frameObj = createFrame.invoke(null, instance);
      } catch (Throwable t) {
        failures.add("FrameFactory.createFrame failed for " + type + ": " + t);
        continue;
      }

      if (!(frameObj instanceof Window)) {
        failures.add("FrameFactory.createFrame returned non-window for " + type + ": " + safeClassName(frameObj));
        continue;
      }

      windowsToDispose.add((Window) frameObj);
    }
  }

  private static String safeClassName(Object o) {
    try {
      return (o == null) ? "null" : o.getClass().getName();
    } catch (Throwable ignored) {
      return "(unknown)";
    }
  }
}
