package launcher;

import automata.Automaton;
import automata.State;
import automata.Transition;
import automata.fsa.FSATransition;
import automata.mealy.MealyTransition;
import automata.mealy.MooreMachine;
import automata.mealy.MooreTransition;
import automata.pda.PDATransition;
import automata.turing.TMState;
import automata.turing.TMTransition;
import automata.vdg.VDGTransition;
import gui.environment.AutomatonEnvironment;
import gui.environment.Environment;
import gui.environment.EnvironmentFrame;
import gui.viewer.AutomatonDrawer;
import gui.viewer.AutomatonPane;
import gui.viewer.SelectionDrawer;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class AutomatonCopyPaste {
  private static final String CLIP_PREFIX = "JFLAP_CLIP_v1\n";
  private static final String LEGACY_PREFIX = "JFLAP_UI_SELECTION_V1:";
  private static final String COPY_SUFFIX = "_copy";
  private static final String COPYPASTE_BINDING_KEY = "launcher.modern.canvasCopyPasteBindings";
  private static final int PASTE_OFFSET_STEP = 24;
  private static final int PASTE_OFFSET_WRAP = 240;
  private static final double ZOOM_STEP = 1.12;
  private static final double ZOOM_MIN = 0.20;
  private static final double ZOOM_MAX = 4.00;

  private static int pasteOffset = 0;

  private AutomatonCopyPaste() {
  }

  public static void copyForActiveWindow() {
    if (!tryCopyForActiveWindow()) {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  public static void pasteForActiveWindow() {
    if (!tryPasteForActiveWindow()) {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  public static void undoForActiveWindow() {
    if (!tryUndoForActiveWindow()) {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  public static void redoForActiveWindow() {
    if (!tryRedoForActiveWindow()) {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  public static void deleteSelectionForActiveWindow() {
    if (!tryDeleteSelectionForActiveWindow()) {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  public static void installCopyPasteBindings(final AutomatonPane pane) {
    if (pane == null) {
      return;
    }

    if (Boolean.TRUE.equals(pane.getClientProperty(COPYPASTE_BINDING_KEY))) {
      return;
    }
    pane.putClientProperty(COPYPASTE_BINDING_KEY, Boolean.TRUE);

    try {
      pane.setFocusable(true);
    } catch (Throwable ignored) {
      // ignore
    }

    try {
      pane.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          pane.requestFocusInWindow();
        }
      });
    } catch (Throwable ignored) {
      // ignore
    }

    int shortcutMask = menuShortcutMask();
    int shortcutMaskEx = menuShortcutMaskEx();
    KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutMask);
    KeyStroke copyEx = KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutMaskEx);
    KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutMask);
    KeyStroke pasteEx = KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutMaskEx);
    KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
    KeyStroke backspace = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
    KeyStroke zoomIn = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, shortcutMask);
    KeyStroke zoomInShift = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, shortcutMaskEx | InputEvent.SHIFT_DOWN_MASK);
    KeyStroke zoomInNumpad = KeyStroke.getKeyStroke(KeyEvent.VK_ADD, shortcutMask);
    KeyStroke zoomOut = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, shortcutMask);
    KeyStroke zoomOutNumpad = KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, shortcutMask);
    KeyStroke undo = KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutMask);
    KeyStroke undoEx = KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutMaskEx);
    KeyStroke redo = KeyStroke.getKeyStroke(KeyEvent.VK_Y, shortcutMask);
    KeyStroke redoEx = KeyStroke.getKeyStroke(KeyEvent.VK_Y, shortcutMaskEx);
    KeyStroke redoShift = KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutMaskEx | InputEvent.SHIFT_DOWN_MASK);

    String copyKey = "launcher.canvasCopy";
    String pasteKey = "launcher.canvasPaste";
    String deleteKey = "launcher.canvasDeleteSelection";
    String backspaceKey = "launcher.canvasBackspaceSelection";
    String zoomInKey = "launcher.canvasZoomIn";
    String zoomOutKey = "launcher.canvasZoomOut";
    String undoKey = "launcher.canvasUndo";
    String redoKey = "launcher.canvasRedo";

    InputMap inputMap = pane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    ActionMap actionMap = pane.getActionMap();

    if (actionMap.get(copyKey) == null) {
      actionMap.put(copyKey, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (!delegateCopyPasteForTextFocus(false)) {
            copyForActiveWindow();
          }
        }
      });
    }

    if (actionMap.get(pasteKey) == null) {
      actionMap.put(pasteKey, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (!delegateCopyPasteForTextFocus(true)) {
            pasteForActiveWindow();
          }
        }
      });
    }

    if (actionMap.get(deleteKey) == null) {
      actionMap.put(deleteKey, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (!delegateDeleteForTextFocus(true)) {
            deleteSelectionForActiveWindow();
          }
        }
      });
    }

    if (actionMap.get(backspaceKey) == null) {
      actionMap.put(backspaceKey, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (!delegateDeleteForTextFocus(false)) {
            deleteSelectionForActiveWindow();
          }
        }
      });
    }

    if (actionMap.get(zoomInKey) == null) {
      actionMap.put(zoomInKey, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (isTextFocusActive()) {
            return;
          }
          applyZoom(pane, ZOOM_STEP);
        }
      });
    }

    if (actionMap.get(zoomOutKey) == null) {
      actionMap.put(zoomOutKey, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (isTextFocusActive()) {
            return;
          }
          applyZoom(pane, 1.0 / ZOOM_STEP);
        }
      });
    }

    if (actionMap.get(undoKey) == null) {
      actionMap.put(undoKey, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (isTextFocusActive()) {
            return;
          }
          undoForActiveWindow();
        }
      });
    }

    if (actionMap.get(redoKey) == null) {
      actionMap.put(redoKey, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (isTextFocusActive()) {
            return;
          }
          redoForActiveWindow();
        }
      });
    }

    inputMap.put(copy, copyKey);
    inputMap.put(copyEx, copyKey);
    inputMap.put(paste, pasteKey);
    inputMap.put(pasteEx, pasteKey);
    inputMap.put(delete, deleteKey);
    inputMap.put(backspace, backspaceKey);
    inputMap.put(zoomIn, zoomInKey);
    inputMap.put(zoomInShift, zoomInKey);
    inputMap.put(zoomInNumpad, zoomInKey);
    inputMap.put(zoomOut, zoomOutKey);
    inputMap.put(zoomOutNumpad, zoomOutKey);
    inputMap.put(undo, undoKey);
    inputMap.put(undoEx, undoKey);
    inputMap.put(redo, redoKey);
    inputMap.put(redoEx, redoKey);
    inputMap.put(redoShift, redoKey);

    try {
      pane.addMouseWheelListener(new MouseAdapter() {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
          if (e == null || !isMenuShortcutDown(e) || isTextFocusActive()) {
            return;
          }
          double rotation = e.getPreciseWheelRotation();
          if (rotation == 0.0) {
            return;
          }
          double factor = Math.pow(ZOOM_STEP, -rotation);
          if (applyZoom(pane, factor)) {
            e.consume();
          }
        }
      });
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  public static boolean tryCopyForActiveWindow() {
    Window window = activeWindow();
    if (window == null) {
      return false;
    }

    AutomatonPane pane = findTargetPane(window, true);
    if (pane == null) {
      return false;
    }

    Automaton automaton = automatonForPane(pane);
    if (automaton == null) {
      return false;
    }

    List<State> stateList = selectedStatesFromAutomaton(automaton);

    Transition[] selectedTransitions;
    try {
      selectedTransitions = selectedTransitionsFromAutomaton(automaton);
    } catch (Throwable ignored) {
      selectedTransitions = null;
    }

    boolean hasSelectedTransitions = selectedTransitions != null && selectedTransitions.length > 0;
    if (stateList.isEmpty() && hasSelectedTransitions) {
      Set<State> stateSet = new HashSet<>();
      for (int i = 0; i < selectedTransitions.length; i++) {
        Transition t = selectedTransitions[i];
        if (t == null) {
          continue;
        }
        State from = t.getFromState();
        State to = t.getToState();
        if (from != null && stateSet.add(from)) {
          stateList.add(from);
        }
        if (to != null && stateSet.add(to)) {
          stateList.add(to);
        }
      }
    }

    if (stateList.isEmpty()) {
      return false;
    }

    SelectionSnapshot snapshot = SelectionSnapshot.fromSelection(
      automaton,
      stateList.toArray(new State[0]),
      hasSelectedTransitions ? selectedTransitions : null
    );
    if (snapshot == null || snapshot.states.isEmpty()) {
      return false;
    }

    return writeSnapshotToClipboard(snapshot);
  }

  public static boolean tryPasteForActiveWindow() {
    final Window window = activeWindow();
    if (window == null) {
      return false;
    }

    final SelectionSnapshot snapshot = readSnapshotFromClipboard();
    if (snapshot == null) {
      return false;
    }

    final AutomatonPane pane = findTargetPane(window, false);
    if (pane == null) {
      return false;
    }

    final Automaton automaton = automatonForPane(pane);
    if (automaton == null) {
      return false;
    }

    String currentClass = automaton.getClass().getName();
    if (snapshot.automatonClassName != null && !snapshot.automatonClassName.equals(currentClass)) {
      showPasteError(window,
        "Clipboard contains a " + simpleName(snapshot.automatonClassName) +
          " selection. Paste is only supported into " + simpleName(currentClass) + " editors.");
      return false;
    }

    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        pasteOffset = (pasteOffset + PASTE_OFFSET_STEP) % PASTE_OFFSET_WRAP;
        pasteInto(snapshot, pane, selectionDrawer(pane), automaton, pasteOffset);
      }
    };

    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    } else {
      SwingUtilities.invokeLater(runnable);
    }

    return true;
  }

  public static boolean tryUndoForActiveWindow() {
    final Window window = activeWindow();
    if (window == null) {
      return false;
    }

    final AutomatonPane pane = findTargetPane(window, false);
    if (pane == null) {
      return false;
    }

    final Automaton automaton = automatonForPane(pane);
    if (automaton == null) {
      return false;
    }

    try {
      EnvironmentFrame frame = automaton.getEnvironmentFrame();
      if (frame == null) {
        return false;
      }
      Environment env = frame.getEnvironment();
      if (env instanceof AutomatonEnvironment) {
        ((AutomatonEnvironment) env).restoreStatus();
      } else {
        return false;
      }
    } catch (Throwable ignored) {
      return false;
    }

    try {
      pane.repaint();
    } catch (Throwable ignored) {
      // ignore
    }

    return true;
  }

  public static boolean tryRedoForActiveWindow() {
    final Window window = activeWindow();
    if (window == null) {
      return false;
    }

    final AutomatonPane pane = findTargetPane(window, false);
    if (pane == null) {
      return false;
    }

    final Automaton automaton = automatonForPane(pane);
    if (automaton == null) {
      return false;
    }

    try {
      EnvironmentFrame frame = automaton.getEnvironmentFrame();
      if (frame == null) {
        return false;
      }
      Environment env = frame.getEnvironment();
      if (env instanceof AutomatonEnvironment) {
        ((AutomatonEnvironment) env).redo();
      } else {
        return false;
      }
    } catch (Throwable ignored) {
      return false;
    }

    try {
      pane.repaint();
    } catch (Throwable ignored) {
      // ignore
    }

    return true;
  }

  public static boolean tryDeleteSelectionForActiveWindow() {
    final Window window = activeWindow();
    if (window == null) {
      return false;
    }

    final AutomatonPane pane = findTargetPane(window, true);
    if (pane == null) {
      return false;
    }

    final Automaton automaton = automatonForPane(pane);
    if (automaton == null) {
      return false;
    }

    SelectionDrawer sd = selectionDrawer(pane);

    Set<State> selectedStates = new HashSet<>();
    if (sd != null) {
      try {
        State[] states = sd.getSelected();
        if (states != null) {
          for (int i = 0; i < states.length; i++) {
            if (states[i] != null) {
              selectedStates.add(states[i]);
            }
          }
        }
      } catch (Throwable ignored) {
        // best-effort only
      }
    }

    List<State> automatonStates = selectedStatesFromAutomaton(automaton);
    if (automatonStates != null && !automatonStates.isEmpty()) {
      selectedStates.addAll(automatonStates);
    }

    Set<Transition> selectedTransitions = new HashSet<>();
    if (sd != null) {
      try {
        Transition[] transitions = sd.getSelectedTransitions();
        if (transitions != null) {
          for (int i = 0; i < transitions.length; i++) {
            if (transitions[i] != null) {
              selectedTransitions.add(transitions[i]);
            }
          }
        }
      } catch (Throwable ignored) {
        // best-effort only
      }
    }

    Transition[] automatonTransitions = selectedTransitionsFromAutomaton(automaton);
    if (automatonTransitions != null && automatonTransitions.length > 0) {
      for (int i = 0; i < automatonTransitions.length; i++) {
        if (automatonTransitions[i] != null) {
          selectedTransitions.add(automatonTransitions[i]);
        }
      }
    }

    if (selectedStates.isEmpty() && selectedTransitions.isEmpty()) {
      return false;
    }

    saveUndoStatus(automaton);

    if (sd != null) {
      try {
        sd.clearSelected();
      } catch (Throwable ignored) {
        // ignore
      }
      try {
        sd.clearSelectedTransitions();
      } catch (Throwable ignored) {
        // ignore
      }
    }

    clearSelectedFlags(automaton);

    for (Transition t : selectedTransitions) {
      if (t == null) {
        continue;
      }
      try {
        automaton.removeTransition(t);
      } catch (Throwable ignored) {
        // best-effort only
      }
    }

    for (State s : selectedStates) {
      if (s == null) {
        continue;
      }
      try {
        automaton.removeState(s);
      } catch (Throwable ignored) {
        // best-effort only
      }
    }

    try {
      if (sd != null) {
        sd.invalidate();
      }
    } catch (Throwable ignored) {
      // ignore
    }

    try {
      pane.repaint();
    } catch (Throwable ignored) {
      // ignore
    }

    return true;
  }

  public static boolean hasPasteDataOnClipboard() {
    return readSnapshotFromClipboard() != null;
  }

  private static void pasteInto(SelectionSnapshot snapshot,
                                AutomatonPane pane,
                                SelectionDrawer selectionDrawer,
                                Automaton automaton,
                                int extraOffset) {
    if (snapshot == null || snapshot.states == null || snapshot.states.isEmpty()) {
      return;
    }

    Point anchor = bestPasteAnchorInAutomatonCoords(pane);
    int originX = anchor.x - (snapshot.boundsW / 2) + extraOffset;
    int originY = anchor.y - (snapshot.boundsH / 2) + extraOffset;

    saveUndoStatus(automaton);
    clearSelectedFlags(automaton);

    Map<Integer, State> idMap = new HashMap<>();
    Set<String> usedLabels = collectUsedLabels(automaton);

    for (StateSnapshot s : snapshot.states) {
      Point p = new Point(originX + s.relX, originY + s.relY);
      State created = automaton.createState(p);
      if (created == null) {
        continue;
      }

      String newLabel = uniqueCopyLabel(labelBaseForState(s), usedLabels);
      if (newLabel != null) {
        try {
          created.setLabel(newLabel);
        } catch (Throwable ignored) {
          // best-effort only
        }
      }

      if (s.tmInternalName != null && created instanceof TMState) {
        try {
          ((TMState) created).setInternalName(s.tmInternalName);
        } catch (Throwable ignored) {
          // best-effort only
        }
      }

      if (s.isFinal) {
        try {
          automaton.addFinalState(created);
        } catch (Throwable ignored) {
          // best-effort only
        }
      }

      if (s.mooreOutput != null && automaton instanceof MooreMachine) {
        try {
          ((MooreMachine) automaton).setOutput(created, s.mooreOutput);
        } catch (Throwable ignored) {
          // best-effort only
        }
      }

      idMap.put(s.id, created);
    }

    for (TransitionSnapshot t : snapshot.transitions) {
      State from = idMap.get(t.fromId);
      State to = idMap.get(t.toId);
      if (from == null || to == null) {
        continue;
      }

      Transition created = createTransition(automaton, from, to, t);
      if (created == null) {
        continue;
      }

      if (t.controlRelX != null && t.controlRelY != null) {
        try {
          created.setControl(new Point(originX + t.controlRelX.intValue(), originY + t.controlRelY.intValue()));
        } catch (Throwable ignored) {
          // best-effort only
        }
      }

      try {
        automaton.addTransition(created);
      } catch (Throwable ignored) {
        // best-effort only
      }
    }

    try {
      if (selectionDrawer != null) {
        selectionDrawer.clearSelected();
        selectionDrawer.clearSelectedTransitions();
      }
    } catch (Throwable ignored) {
      // best-effort only
    }

    try {
      selectionDrawer.invalidate();
    } catch (Throwable ignored) {
      // best-effort only
    }

    try {
      pane.repaint();
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static Transition createTransition(Automaton automaton, State from, State to, TransitionSnapshot t) {
    if (t == null || t.transitionClassName == null) {
      return null;
    }

    try {
      if (FSATransition.class.getName().equals(t.transitionClassName)) {
        return new FSATransition(from, to, nullToEmpty(t.label));
      }
      if (MooreTransition.class.getName().equals(t.transitionClassName)) {
        if (t.output != null) {
          return new MooreTransition(from, to, nullToEmpty(t.label), t.output);
        }
        return new MooreTransition(from, to, nullToEmpty(t.label));
      }
      if (MealyTransition.class.getName().equals(t.transitionClassName)) {
        return new MealyTransition(from, to, nullToEmpty(t.label), nullToEmpty(t.output));
      }
      if (PDATransition.class.getName().equals(t.transitionClassName)) {
        return new PDATransition(from, to, nullToEmpty(t.read), nullToEmpty(t.pop), nullToEmpty(t.push));
      }
      if (TMTransition.class.getName().equals(t.transitionClassName)) {
        String[] r = (t.reads == null) ? new String[0] : t.reads;
        String[] w = (t.writes == null) ? new String[0] : t.writes;
        String[] d = (t.directions == null) ? new String[0] : t.directions;
        TMTransition created = new TMTransition(from, to, r, w, d);
        if (t.blockTransition != null) {
          created.setBlockTransition(t.blockTransition.booleanValue());
        }
        return created;
      }
      if (VDGTransition.class.getName().equals(t.transitionClassName)) {
        return new VDGTransition(from, to);
      }
    } catch (Throwable ignored) {
      return null;
    }

    return null;
  }

  private static String nullToEmpty(String s) {
    return (s == null) ? "" : s;
  }

  private static Point bestPasteAnchorInAutomatonCoords(AutomatonPane pane) {
    try {
      Point mouse = pane.getMousePosition();
      if (mouse != null) {
        Point p = pane.transformFromViewToAutomaton(mouse);
        if (p != null) {
          return p;
        }
      }
    } catch (Throwable ignored) {
      // best-effort only
    }

    try {
      Rectangle vr = pane.getVisibleRect();
      Point center = new Point(vr.x + (vr.width / 2), vr.y + (vr.height / 2));
      Point p = pane.transformFromViewToAutomaton(center);
      if (p != null) {
        return p;
      }
    } catch (Throwable ignored) {
      // best-effort only
    }

    return new Point(60, 60);
  }

  private static void saveUndoStatus(Automaton automaton) {
    if (automaton == null) {
      return;
    }

    try {
      EnvironmentFrame frame = automaton.getEnvironmentFrame();
      if (frame == null) {
        return;
      }
      Environment env = frame.getEnvironment();
      if (env instanceof AutomatonEnvironment) {
        ((AutomatonEnvironment) env).saveStatus();
      }
    } catch (Throwable ignored) {
      // best-effort only
    }
  }

  private static Set<String> collectUsedLabels(Automaton automaton) {
    Set<String> used = new HashSet<>();
    if (automaton == null) {
      return used;
    }

    State[] states;
    try {
      states = automaton.getStates();
    } catch (Throwable ignored) {
      return used;
    }

    if (states == null) {
      return used;
    }

    for (int i = 0; i < states.length; i++) {
      State state = states[i];
      if (state == null) {
        continue;
      }

      String label = null;
      try {
        label = state.getLabel();
      } catch (Throwable ignored) {
        label = null;
      }
      addUsedLabel(used, safeTrim(label));

      String name = null;
      try {
        name = state.getName();
      } catch (Throwable ignored) {
        name = null;
      }
      addUsedLabel(used, safeTrim(name));
    }

    return used;
  }

  private static void addUsedLabel(Set<String> used, String label) {
    if (used == null || label == null || label.isEmpty()) {
      return;
    }
    used.add(label);
  }

  private static String labelBaseForState(StateSnapshot snapshot) {
    if (snapshot == null) {
      return null;
    }
    String label = safeTrim(snapshot.label);
    if (label != null) {
      return label;
    }
    return "q" + snapshot.id;
  }

  private static String uniqueCopyLabel(String baseLabel, Set<String> usedLabels) {
    if (baseLabel == null) {
      return null;
    }
    String base = baseLabel.trim();
    if (base.isEmpty()) {
      return null;
    }

    int index = 1;
    String candidate;
    do {
      candidate = base + COPY_SUFFIX + index;
      index++;
    } while (usedLabels != null && usedLabels.contains(candidate));

    if (usedLabels != null) {
      usedLabels.add(candidate);
    }
    return candidate;
  }

  private static String safeTrim(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static void showPasteError(final Window window, final String message) {
    if (message == null) {
      return;
    }

    Runnable action = new Runnable() {
      @Override
      public void run() {
        JOptionPane.showMessageDialog(window, message, "Paste", JOptionPane.INFORMATION_MESSAGE);
      }
    };

    if (SwingUtilities.isEventDispatchThread()) {
      action.run();
    } else {
      SwingUtilities.invokeLater(action);
    }
  }

  private static String simpleName(String className) {
    if (className == null) {
      return "unknown";
    }
    int idx = className.lastIndexOf('.');
    return (idx >= 0 && idx + 1 < className.length()) ? className.substring(idx + 1) : className;
  }

  private static int menuShortcutMask() {
    try {
      return Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    } catch (Throwable ignored) {
      return InputEvent.CTRL_MASK;
    }
  }

  private static int menuShortcutMaskEx() {
    int mask = menuShortcutMask();
    if ((mask & InputEvent.META_MASK) != 0) {
      return InputEvent.META_DOWN_MASK;
    }
    return InputEvent.CTRL_DOWN_MASK;
  }

  private static boolean writeSnapshotToClipboard(SelectionSnapshot snapshot) {
    Clipboard clipboard;
    try {
      clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    } catch (Throwable ignored) {
      return false;
    }

    String payload = encodeSnapshot(snapshot);
    if (payload == null) {
      return false;
    }

    try {
      clipboard.setContents(new StringSelection(payload), null);
      return true;
    } catch (Throwable ignored) {
      return false;
    }
  }

  private static SelectionSnapshot readSnapshotFromClipboard() {
    Clipboard clipboard;
    try {
      clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    } catch (Throwable ignored) {
      return null;
    }

    Transferable t;
    try {
      t = clipboard.getContents(null);
    } catch (Throwable ignored) {
      return null;
    }

    if (t == null || !t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
      return null;
    }

    String s;
    try {
      s = (String) t.getTransferData(DataFlavor.stringFlavor);
    } catch (Throwable ignored) {
      return null;
    }

    if (s == null) {
      return null;
    }

    if (s.startsWith(CLIP_PREFIX)) {
      return decodeSnapshot(s.substring(CLIP_PREFIX.length()));
    }

    if (s.startsWith(LEGACY_PREFIX)) {
      return decodeSnapshot(s.substring(LEGACY_PREFIX.length()));
    }

    return null;
  }

  private static String encodeSnapshot(SelectionSnapshot snapshot) {
    if (snapshot == null) {
      return null;
    }

    byte[] bytes;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(snapshot);
      oos.flush();
      oos.close();
      bytes = bos.toByteArray();
    } catch (Throwable ignored) {
      return null;
    }

    String base64;
    try {
      base64 = java.util.Base64.getEncoder().encodeToString(bytes);
    } catch (Throwable ignored) {
      return null;
    }

    return CLIP_PREFIX + base64;
  }

  private static SelectionSnapshot decodeSnapshot(String base64) {
    if (base64 == null || base64.trim().isEmpty()) {
      return null;
    }

    byte[] bytes;
    try {
      bytes = java.util.Base64.getDecoder().decode(base64.trim());
    } catch (Throwable ignored) {
      return null;
    }

    try {
      ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
      Object obj = ois.readObject();
      ois.close();
      if (obj instanceof SelectionSnapshot) {
        return (SelectionSnapshot) obj;
      }
      return null;
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static SelectionDrawer selectionDrawer(AutomatonPane pane) {
    if (pane == null) {
      return null;
    }

    AutomatonDrawer drawer;
    try {
      drawer = pane.getDrawer();
    } catch (Throwable ignored) {
      drawer = null;
    }

    if (drawer instanceof SelectionDrawer) {
      return (SelectionDrawer) drawer;
    }
    return null;
  }

  private static Window activeWindow() {
    try {
      Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
      if (window != null) {
        return window;
      }
      return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
    } catch (Throwable ignored) {
      return null;
    }
  }

  static boolean delegateCopyPasteForTextFocus(boolean paste) {
    Component focus = null;
    try {
      focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    } catch (Throwable ignored) {
      focus = null;
    }

    if (!(focus instanceof JTextComponent)) {
      return false;
    }

    try {
      if (paste) {
        ((JTextComponent) focus).paste();
      } else {
        ((JTextComponent) focus).copy();
      }
      return true;
    } catch (Throwable ignored) {
      // best-effort only
      return false;
    }
  }

  static boolean delegateDeleteForTextFocus(boolean forwardDelete) {
    Component focus = null;
    try {
      focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    } catch (Throwable ignored) {
      focus = null;
    }

    if (!(focus instanceof JTextComponent)) {
      return false;
    }

    JTextComponent text = (JTextComponent) focus;
    try {
      if (!text.isEditable() || !text.isEnabled()) {
        return true;
      }

      int start = text.getSelectionStart();
      int end = text.getSelectionEnd();
      if (start != end) {
        text.replaceSelection("");
        return true;
      }

      int pos = text.getCaretPosition();
      Document doc = text.getDocument();
      if (doc == null) {
        return true;
      }

      if (forwardDelete) {
        if (pos < doc.getLength()) {
          doc.remove(pos, 1);
        }
      } else {
        if (pos > 0) {
          doc.remove(pos - 1, 1);
        }
      }
      return true;
    } catch (Throwable ignored) {
      // best-effort only
      return true;
    }
  }

  private static boolean applyZoom(AutomatonPane pane, double factor) {
    if (pane == null || factor <= 0.0) {
      return false;
    }

    double current;
    try {
      current = pane.getScale();
    } catch (Throwable ignored) {
      return false;
    }

    double next = clamp(current * factor, ZOOM_MIN, ZOOM_MAX);
    if (Math.abs(next - current) < 0.000001) {
      return false;
    }

    try {
      if (pane.getAdapt()) {
        pane.setAdapt(false);
      }
    } catch (Throwable ignored) {
      // ignore
    }

    try {
      pane.setScale(next);
    } catch (Throwable ignored) {
      return false;
    }

    try {
      pane.requestTransform();
    } catch (Throwable ignored) {
      // ignore
    }

    return true;
  }

  private static double clamp(double value, double min, double max) {
    if (value < min) {
      return min;
    }
    if (value > max) {
      return max;
    }
    return value;
  }

  private static boolean isTextFocusActive() {
    Component focus = null;
    try {
      focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    } catch (Throwable ignored) {
      focus = null;
    }
    return focus instanceof JTextComponent;
  }

  private static boolean isMenuShortcutDown(InputEvent e) {
    if (e == null) {
      return false;
    }
    return (e.getModifiersEx() & menuShortcutMaskEx()) != 0;
  }

  private static AutomatonPane findTargetPane(Window window, boolean requireSelection) {
    if (window == null) {
      return null;
    }

    Component focus = null;
    try {
      focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    } catch (Throwable ignored) {
      focus = null;
    }

    AutomatonPane fromFocus = findPaneFromComponent(focus);
    if (fromFocus != null) {
      if (!requireSelection || hasSelection(fromFocus)) {
        return fromFocus;
      }
    }

    return findPaneInWindow(window, requireSelection);
  }

  private static boolean hasSelection(AutomatonPane pane) {
    SelectionDrawer sd = selectionDrawer(pane);
    if (sd != null) {
      try {
        State[] selected = sd.getSelected();
        if (selected != null && selected.length > 0) {
          return true;
        }
      } catch (Throwable ignored) {
        // ignore
      }

      try {
        Transition[] transitions = sd.getSelectedTransitions();
        if (transitions != null && transitions.length > 0) {
          return true;
        }
      } catch (Throwable ignored) {
        // ignore
      }
    }

    Automaton automaton = automatonForPane(pane);
    if (automaton == null) {
      return false;
    }
    if (!selectedStatesFromAutomaton(automaton).isEmpty()) {
      return true;
    }
    Transition[] transitions = selectedTransitionsFromAutomaton(automaton);
    return transitions != null && transitions.length > 0;
  }

  private static Automaton automatonForPane(AutomatonPane pane) {
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

  private static List<State> selectedStatesFromAutomaton(Automaton automaton) {
    List<State> selected = new ArrayList<>();
    if (automaton == null) {
      return selected;
    }

    State[] states;
    try {
      states = automaton.getStates();
    } catch (Throwable ignored) {
      return selected;
    }

    if (states == null) {
      return selected;
    }

    for (int i = 0; i < states.length; i++) {
      State s = states[i];
      if (s != null && s.isSelected()) {
        selected.add(s);
      }
    }

    return selected;
  }

  private static Transition[] selectedTransitionsFromAutomaton(Automaton automaton) {
    if (automaton == null) {
      return null;
    }

    Transition[] transitions;
    try {
      transitions = automaton.getTransitions();
    } catch (Throwable ignored) {
      return null;
    }

    if (transitions == null || transitions.length == 0) {
      return null;
    }

    List<Transition> selected = new ArrayList<>();
    for (int i = 0; i < transitions.length; i++) {
      Transition t = transitions[i];
      if (t != null && t.isSelected) {
        selected.add(t);
      }
    }

    if (selected.isEmpty()) {
      return null;
    }

    return selected.toArray(new Transition[0]);
  }

  private static void clearSelectedFlags(Automaton automaton) {
    if (automaton == null) {
      return;
    }

    State[] states;
    try {
      states = automaton.getStates();
    } catch (Throwable ignored) {
      states = null;
    }

    if (states != null) {
      for (int i = 0; i < states.length; i++) {
        State s = states[i];
        if (s != null && s.isSelected()) {
          try {
            s.setSelect(false);
          } catch (Throwable ignored) {
            // ignore
          }
        }
      }
    }

    Transition[] transitions;
    try {
      transitions = automaton.getTransitions();
    } catch (Throwable ignored) {
      transitions = null;
    }

    if (transitions != null) {
      for (int i = 0; i < transitions.length; i++) {
        Transition t = transitions[i];
        if (t != null && t.isSelected) {
          t.isSelected = false;
        }
      }
    }
  }

  private static AutomatonPane findPaneFromComponent(Component component) {
    Component c = component;
    while (c != null) {
      if (c instanceof AutomatonPane) {
        return (AutomatonPane) c;
      }

      try {
        c = c.getParent();
      } catch (Throwable ignored) {
        return null;
      }
    }
    return null;
  }

  private static AutomatonPane findPaneInWindow(Window window, boolean requireSelection) {
    Queue<Component> queue = new ArrayDeque<>();
    queue.add(window);

    AutomatonPane first = null;

    while (!queue.isEmpty()) {
      Component c = queue.remove();
      if (c instanceof AutomatonPane) {
        AutomatonPane pane = (AutomatonPane) c;
        if (first == null) {
          first = pane;
        }
        if (!requireSelection) {
          return pane;
        }
        if (hasSelection(pane)) {
          return pane;
        }
      }

      if (!(c instanceof java.awt.Container)) {
        continue;
      }

      Component[] children;
      try {
        children = ((java.awt.Container) c).getComponents();
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

    return (requireSelection) ? null : first;
  }

  private static final class SelectionSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    final String automatonClassName;
    final int boundsW;
    final int boundsH;
    final List<StateSnapshot> states;
    final List<TransitionSnapshot> transitions;

    private SelectionSnapshot(String automatonClassName,
                              int boundsW,
                              int boundsH,
                              List<StateSnapshot> states,
                              List<TransitionSnapshot> transitions) {
      this.automatonClassName = automatonClassName;
      this.boundsW = boundsW;
      this.boundsH = boundsH;
      this.states = states;
      this.transitions = transitions;
    }

    static SelectionSnapshot fromSelection(Automaton automaton,
                                           State[] selectedStates,
                                           Transition[] selectedTransitions) {
      if (automaton == null || selectedStates == null || selectedStates.length == 0) {
        return null;
      }

      Set<State> selectedSet = new HashSet<>();
      List<State> selectedList = new ArrayList<>();
      int minX = Integer.MAX_VALUE;
      int minY = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int maxY = Integer.MIN_VALUE;

      for (int i = 0; i < selectedStates.length; i++) {
        State s = selectedStates[i];
        if (s == null) {
          continue;
        }
        if (!selectedSet.add(s)) {
          continue;
        }
        selectedList.add(s);
        Point p = s.getPoint();
        if (p == null) {
          continue;
        }
        minX = Math.min(minX, p.x);
        minY = Math.min(minY, p.y);
        maxX = Math.max(maxX, p.x);
        maxY = Math.max(maxY, p.y);
      }

      if (selectedSet.isEmpty()) {
        return null;
      }

      if (minX == Integer.MAX_VALUE) {
        minX = 0;
        minY = 0;
        maxX = 0;
        maxY = 0;
      }

      int boundsW = Math.max(0, maxX - minX);
      int boundsH = Math.max(0, maxY - minY);

      boolean isMoore = automaton instanceof MooreMachine;
      MooreMachine moore = isMoore ? (MooreMachine) automaton : null;

      List<StateSnapshot> stateSnapshots = new ArrayList<>();
      Collections.sort(selectedList, new Comparator<State>() {
        @Override
        public int compare(State a, State b) {
          if (a == b) {
            return 0;
          }
          if (a == null) {
            return -1;
          }
          if (b == null) {
            return 1;
          }
          return a.getID() - b.getID();
        }
      });

      for (int i = 0; i < selectedList.size(); i++) {
        State s = selectedList.get(i);
        if (s == null) {
          continue;
        }

        Point p = s.getPoint();
        if (p == null) {
          continue;
        }

        String label = null;
        try {
          label = s.getLabel();
        } catch (Throwable ignored) {
          label = null;
        }

        boolean isFinal = false;
        try {
          isFinal = automaton.isFinalState(s);
        } catch (Throwable ignored) {
          isFinal = false;
        }

        String mooreOutput = null;
        if (moore != null) {
          try {
            mooreOutput = moore.getOutput(s);
          } catch (Throwable ignored) {
            mooreOutput = null;
          }
        }

        String tmInternalName = null;
        if (s instanceof TMState) {
          try {
            tmInternalName = ((TMState) s).getInternalName();
          } catch (Throwable ignored) {
            tmInternalName = null;
          }
        }

        stateSnapshots.add(new StateSnapshot(
          s.getID(),
          p.x - minX,
          p.y - minY,
          label,
          isFinal,
          mooreOutput,
          tmInternalName
        ));
      }

      List<TransitionSnapshot> transitionSnapshots = new ArrayList<>();
      Transition[] transitions = selectedTransitions;
      if (transitions == null || transitions.length == 0) {
        try {
          transitions = automaton.getTransitions();
        } catch (Throwable ignored) {
          transitions = null;
        }
      }

      if (transitions != null) {
        for (int i = 0; i < transitions.length; i++) {
          Transition t = transitions[i];
          if (t == null) {
            continue;
          }

          State from = t.getFromState();
          State to = t.getToState();
          if (from == null || to == null) {
            continue;
          }

          if (!selectedSet.contains(from) || !selectedSet.contains(to)) {
            continue;
          }

          transitionSnapshots.add(TransitionSnapshot.fromTransition(t, minX, minY));
        }
      }

      return new SelectionSnapshot(
        automaton.getClass().getName(),
        boundsW,
        boundsH,
        stateSnapshots,
        transitionSnapshots
      );
    }
  }

  private static final class StateSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    final int id;
    final int relX;
    final int relY;
    final String label;
    final boolean isFinal;
    final String mooreOutput;
    final String tmInternalName;

    private StateSnapshot(int id,
                          int relX,
                          int relY,
                          String label,
                          boolean isFinal,
                          String mooreOutput,
                          String tmInternalName) {
      this.id = id;
      this.relX = relX;
      this.relY = relY;
      this.label = label;
      this.isFinal = isFinal;
      this.mooreOutput = mooreOutput;
      this.tmInternalName = tmInternalName;
    }
  }

  private static final class TransitionSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    final String transitionClassName;
    final int fromId;
    final int toId;

    final Integer controlRelX;
    final Integer controlRelY;

    final String label;
    final String output;

    final String read;
    final String pop;
    final String push;

    final String[] reads;
    final String[] writes;
    final String[] directions;
    final Boolean blockTransition;

    private TransitionSnapshot(String transitionClassName,
                               int fromId,
                               int toId,
                               Integer controlRelX,
                               Integer controlRelY,
                               String label,
                               String output,
                               String read,
                               String pop,
                               String push,
                               String[] reads,
                               String[] writes,
                               String[] directions,
                               Boolean blockTransition) {
      this.transitionClassName = transitionClassName;
      this.fromId = fromId;
      this.toId = toId;
      this.controlRelX = controlRelX;
      this.controlRelY = controlRelY;
      this.label = label;
      this.output = output;
      this.read = read;
      this.pop = pop;
      this.push = push;
      this.reads = reads;
      this.writes = writes;
      this.directions = directions;
      this.blockTransition = blockTransition;
    }

    static TransitionSnapshot fromTransition(Transition transition, int minX, int minY) {
      if (transition == null) {
        return null;
      }

      String className = transition.getClass().getName();
      State from = transition.getFromState();
      State to = transition.getToState();

      Integer ctrlX = null;
      Integer ctrlY = null;
      try {
        Point control = transition.getControl();
        if (control != null) {
          ctrlX = Integer.valueOf(control.x - minX);
          ctrlY = Integer.valueOf(control.y - minY);
        }
      } catch (Throwable ignored) {
        ctrlX = null;
        ctrlY = null;
      }

      String label = null;
      String output = null;
      String read = null;
      String pop = null;
      String push = null;
      String[] reads = null;
      String[] writes = null;
      String[] directions = null;
      Boolean blockTransition = null;

      try {
        if (transition instanceof FSATransition) {
          label = ((FSATransition) transition).getLabel();
        } else if (transition instanceof MooreTransition) {
          MooreTransition mt = (MooreTransition) transition;
          label = mt.getLabel();
          output = mt.getOutput();
        } else if (transition instanceof MealyTransition) {
          MealyTransition mt = (MealyTransition) transition;
          label = mt.getLabel();
          output = mt.getOutput();
        } else if (transition instanceof PDATransition) {
          PDATransition pt = (PDATransition) transition;
          read = pt.getInputToRead();
          pop = pt.getStringToPop();
          push = pt.getStringToPush();
        } else if (transition instanceof TMTransition) {
          TMTransition tt = (TMTransition) transition;
          int tapes = Math.max(0, tt.getTapeLength());
          reads = new String[tapes];
          writes = new String[tapes];
          directions = new String[tapes];
          for (int i = 0; i < tapes; i++) {
            reads[i] = tt.getRead(i);
            writes[i] = tt.getWrite(i);
            directions[i] = tt.getDirection(i);
          }
          blockTransition = Boolean.valueOf(tt.isBlockTransition());
        } else if (transition instanceof VDGTransition) {
          // no extra fields
        }
      } catch (Throwable ignored) {
        // best-effort only
      }

      int fromId = (from == null) ? -1 : from.getID();
      int toId = (to == null) ? -1 : to.getID();

      return new TransitionSnapshot(
        className,
        fromId,
        toId,
        ctrlX,
        ctrlY,
        label,
        output,
        read,
        pop,
        push,
        reads,
        writes,
        directions,
        blockTransition
      );
    }
  }
}
