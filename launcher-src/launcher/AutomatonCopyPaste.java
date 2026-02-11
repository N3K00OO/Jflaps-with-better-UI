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
import gui.viewer.AutomatonDrawer;
import gui.viewer.AutomatonPane;
import gui.viewer.SelectionDrawer;

import javax.swing.SwingUtilities;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class AutomatonCopyPaste {
  private static final String CLIP_PREFIX = "JFLAP_UI_SELECTION_V1:";
  private static final int PASTE_OFFSET_STEP = 24;
  private static final int PASTE_OFFSET_WRAP = 240;

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

  public static boolean tryCopyForActiveWindow() {
    Window window = activeWindow();
    if (window == null) {
      return false;
    }

    AutomatonPane pane = findTargetPane(window, true);
    if (pane == null) {
      return false;
    }

    SelectionDrawer selectionDrawer = selectionDrawer(pane);
    if (selectionDrawer == null) {
      return false;
    }

    Automaton automaton = selectionDrawer.getAutomaton();
    if (automaton == null) {
      return false;
    }

    State[] selected = selectionDrawer.getSelected();
    if (selected == null || selected.length == 0) {
      // Some tools may only select transitions; fall back to using endpoints.
      Transition[] selectedTransitions;
      try {
        selectedTransitions = selectionDrawer.getSelectedTransitions();
      } catch (Throwable ignored) {
        selectedTransitions = null;
      }

      if (selectedTransitions == null || selectedTransitions.length == 0) {
        return false;
      }

      Set<State> stateSet = new HashSet<>();
      for (int i = 0; i < selectedTransitions.length; i++) {
        Transition t = selectedTransitions[i];
        if (t == null) {
          continue;
        }
        State from = t.getFromState();
        State to = t.getToState();
        if (from != null) {
          stateSet.add(from);
        }
        if (to != null) {
          stateSet.add(to);
        }
      }

      if (stateSet.isEmpty()) {
        return false;
      }

      selected = stateSet.toArray(new State[0]);
    }

    SelectionSnapshot snapshot = SelectionSnapshot.fromSelection(automaton, selected);
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

    final SelectionDrawer selectionDrawer = selectionDrawer(pane);
    if (selectionDrawer == null) {
      return false;
    }

    final Automaton automaton = selectionDrawer.getAutomaton();
    if (automaton == null) {
      return false;
    }

    String currentClass = automaton.getClass().getName();
    if (snapshot.automatonClassName != null && !snapshot.automatonClassName.equals(currentClass)) {
      return false;
    }

    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        pasteOffset = (pasteOffset + PASTE_OFFSET_STEP) % PASTE_OFFSET_WRAP;
        pasteInto(snapshot, pane, selectionDrawer, automaton, pasteOffset);
      }
    };

    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    } else {
      SwingUtilities.invokeLater(runnable);
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

    Map<Integer, State> idMap = new HashMap<>();
    List<Transition> newTransitions = new ArrayList<>();
    List<State> newStates = new ArrayList<>();

    for (StateSnapshot s : snapshot.states) {
      Point p = new Point(originX + s.relX, originY + s.relY);
      State created = automaton.createState(p);
      if (created == null) {
        continue;
      }

      if (s.label != null) {
        try {
          created.setLabel(s.label);
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
      newStates.add(created);
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
        newTransitions.add(created);
      } catch (Throwable ignored) {
        // best-effort only
      }
    }

    try {
      selectionDrawer.clearSelected();
      selectionDrawer.clearSelectedTransitions();
      for (State s : newStates) {
        selectionDrawer.addSelected(s);
      }
      for (Transition t : newTransitions) {
        selectionDrawer.addSelected(t);
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

    if (s == null || !s.startsWith(CLIP_PREFIX)) {
      return null;
    }

    return decodeSnapshot(s.substring(CLIP_PREFIX.length()));
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
    if (sd == null) {
      return false;
    }

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
      return transitions != null && transitions.length > 0;
    } catch (Throwable ignored) {
      return false;
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

    static SelectionSnapshot fromSelection(Automaton automaton, State[] selectedStates) {
      if (automaton == null || selectedStates == null || selectedStates.length == 0) {
        return null;
      }

      Set<State> selectedSet = new HashSet<>();
      int minX = Integer.MAX_VALUE;
      int minY = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int maxY = Integer.MIN_VALUE;

      for (int i = 0; i < selectedStates.length; i++) {
        State s = selectedStates[i];
        if (s == null) {
          continue;
        }
        selectedSet.add(s);
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
      for (int i = 0; i < selectedStates.length; i++) {
        State s = selectedStates[i];
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
      Transition[] transitions;
      try {
        transitions = automaton.getTransitions();
      } catch (Throwable ignored) {
        transitions = null;
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
