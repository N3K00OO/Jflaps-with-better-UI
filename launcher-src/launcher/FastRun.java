package launcher;

import automata.Automaton;
import gui.action.NoInteractionSimulateAction;
import gui.editor.EditBlockPane;
import gui.environment.Environment;
import gui.environment.EnvironmentFrame;
import gui.viewer.AutomatonDrawer;
import gui.viewer.AutomatonPane;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayDeque;
import java.util.Queue;

public final class FastRun {
  private static final String TITLE = "Fast Run";

  private FastRun() {
  }

  public static void triggerFastRunWithRerun() {
    if (SwingUtilities.isEventDispatchThread()) {
      fastRunWithRerun();
    } else {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          fastRunWithRerun();
        }
      });
    }
  }

  private static void fastRunWithRerun() {
    if (isTextFocusActive()) {
      return;
    }

    EnvironmentFrame frame = findEnvironmentFrame();
    if (frame == null) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }

    Environment env = safeEnvironment(frame);
    if (env == null) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }

    while (true) {
      Automaton automaton = findActiveAutomaton(env, frame);
      if (automaton == null) {
        Toolkit.getDefaultToolkit().beep();
        return;
      }

      FastRunAction action = new FastRunAction(automaton, env);
      try {
        action.actionPerformed(new ActionEvent(frame, ActionEvent.ACTION_PERFORMED, "launcher.fastRun"));
      } catch (Throwable ignored) {
        Toolkit.getDefaultToolkit().beep();
        return;
      }

      if (!action.shouldPromptRerun()) {
        return;
      }

      int choice = JOptionPane.showConfirmDialog(
        frame,
        "Run another input?",
        TITLE,
        JOptionPane.YES_NO_OPTION
      );
      if (choice != JOptionPane.YES_OPTION) {
        return;
      }
    }
  }

  private static Environment safeEnvironment(EnvironmentFrame frame) {
    if (frame == null) {
      return null;
    }
    try {
      return frame.getEnvironment();
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static Automaton findActiveAutomaton(Environment env, Window window) {
    Automaton automaton = null;

    if (env != null) {
      try {
        Component active = env.getActive();
        if (active instanceof EditBlockPane) {
          automaton = ((EditBlockPane) active).getAutomaton();
        }
      } catch (Throwable ignored) {
        automaton = null;
      }
    }

    if (automaton != null) {
      return automaton;
    }

    AutomatonPane pane = findAutomatonPane(window);
    if (pane != null) {
      automaton = automatonFromPane(pane);
    }

    return automaton;
  }

  private static Automaton automatonFromPane(AutomatonPane pane) {
    if (pane == null) {
      return null;
    }
    try {
      AutomatonDrawer drawer = pane.getDrawer();
      if (drawer != null) {
        return drawer.getAutomaton();
      }
    } catch (Throwable ignored) {
      // ignore
    }
    return null;
  }

  private static AutomatonPane findAutomatonPane(Window window) {
    if (window == null) {
      return null;
    }

    Queue<Component> queue = new ArrayDeque<>();
    queue.add(window);

    while (!queue.isEmpty()) {
      Component c = queue.remove();
      if (c instanceof AutomatonPane) {
        return (AutomatonPane) c;
      }

      if (!(c instanceof Container)) {
        continue;
      }

      Component[] children;
      try {
        children = ((Container) c).getComponents();
      } catch (Throwable ignored) {
        children = null;
      }

      if (children == null) {
        continue;
      }

      for (int i = 0; i < children.length; i++) {
        if (children[i] != null) {
          queue.add(children[i]);
        }
      }
    }

    return null;
  }

  private static EnvironmentFrame findEnvironmentFrame() {
    Window window = null;
    try {
      window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    } catch (Throwable ignored) {
      window = null;
    }

    if (window == null) {
      try {
        window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
      } catch (Throwable ignored) {
        window = null;
      }
    }

    EnvironmentFrame frame = findEnvironmentFrame(window);
    if (frame != null) {
      return frame;
    }

    try {
      Window[] windows = Window.getWindows();
      if (windows != null) {
        for (int i = 0; i < windows.length; i++) {
          Window w = windows[i];
          if (w instanceof EnvironmentFrame && w.isShowing()) {
            return (EnvironmentFrame) w;
          }
        }
      }
    } catch (Throwable ignored) {
      // ignore
    }

    return null;
  }

  private static EnvironmentFrame findEnvironmentFrame(Window window) {
    Window current = window;
    while (current != null) {
      if (current instanceof EnvironmentFrame) {
        return (EnvironmentFrame) current;
      }
      try {
        current = current.getOwner();
      } catch (Throwable ignored) {
        return null;
      }
    }
    return null;
  }

  private static boolean isTextFocusActive() {
    try {
      Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
      return focus instanceof JTextComponent;
    } catch (Throwable ignored) {
      return false;
    }
  }

  private static final class FastRunAction extends NoInteractionSimulateAction {
    private boolean inputAttempted = false;
    private boolean inputCanceled = false;

    private FastRunAction(Automaton automaton, Environment env) {
      super(automaton, env);
    }

    @Override
    protected Object initialInput(Component parent, String label) {
      inputAttempted = true;
      Object input = super.initialInput(parent, label);
      if (input == null) {
        inputCanceled = true;
      }
      return input;
    }

    private boolean shouldPromptRerun() {
      return inputAttempted && !inputCanceled;
    }
  }
}