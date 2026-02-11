package gui.editor;

import automata.Note;
import automata.StateRenamer;
import automata.graph.AutomatonGraph;
import automata.graph.layout.GEMLayoutAlgorithm;
import gui.environment.AutomatonEnvironment;
import gui.environment.Environment;
import gui.environment.EnvironmentFrame;
import gui.viewer.AutomatonDrawer;
import gui.viewer.AutomatonPane;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;

/**
 * Replacement for JFLAP's {@code ArrowTool.EmptyMenu}.
 *
 * <p>Note: this is intentionally named with a {@code $} so that it replaces the original
 * class file ({@code gui/editor/ArrowTool$EmptyMenu.class}) when packaging the jar.</p>
 */
class ArrowTool$EmptyMenu extends JPopupMenu implements ActionListener {
  private final JCheckBoxMenuItem stateLabels;
  private final JMenuItem layoutGraph;
  private final JMenuItem renameStates;
  private final JMenuItem addNote;
  private final JCheckBoxMenuItem adaptView;

  private Point myPoint;
  private transient ArrowTool cachedTool;

  public ArrowTool$EmptyMenu() {
    super();

    ArrowTool tool = tool();

    stateLabels = new JCheckBoxMenuItem("Display State Labels");
    stateLabels.addActionListener(this);
    add(stateLabels);

    layoutGraph = new JMenuItem("Layout Graph");
    if (!(tool instanceof ArrowDisplayOnlyTool)) {
      layoutGraph.addActionListener(this);
      add(layoutGraph);
    }

    renameStates = new JMenuItem("Rename States");
    if (!(tool instanceof ArrowDisplayOnlyTool)) {
      renameStates.addActionListener(this);
      add(renameStates);
    }

    addNote = new JMenuItem("Add Note");
    if (!(tool instanceof ArrowDisplayOnlyTool)) {
      addNote.addActionListener(this);
      add(addNote);
    }

    adaptView = new JCheckBoxMenuItem("Auto-Zoom");
    if (!(tool instanceof ArrowDisplayOnlyTool)) {
      adaptView.addActionListener(this);
      add(adaptView);
    }
  }

  public void show(Component invoker, Point point) {
    stateLabels.setSelected(drawsStateLabels());
    adaptView.setSelected(isAutoZoomEnabled());
    myPoint = point;

    super.show(invoker, point.x, point.y);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    ArrowTool tool = tool();
    if (tool == null) {
      return;
    }

    JMenuItem source = (JMenuItem) e.getSource();

    if (source == stateLabels) {
      AutomatonPane view = tool.getView();
      if (view != null) {
        AutomatonDrawer drawer = view.getDrawer();
        if (drawer != null) {
          drawer.shouldDrawStateLabels(source.isSelected());
        }
      }
    } else if (source == layoutGraph) {
      AutomatonGraph graph = new AutomatonGraph(tool.getAutomaton());
      GEMLayoutAlgorithm layout = new GEMLayoutAlgorithm();
      layout.layout(graph, null);
      graph.moveAutomatonStates();
      AutomatonPane view = tool.getView();
      if (view != null) {
        view.fitToBounds(30);
      }
    } else if (source == renameStates) {
      saveStatusIfPossible();
      StateRenamer.rename(tool.getAutomaton());
    } else if (source == adaptView) {
      AutomatonPane view = tool.getView();
      if (view != null) {
        view.setAdapt(source.isSelected());
      }
    } else if (source == addNote) {
      saveStatusIfPossible();
      Note note = new Note(myPoint, "insert_text");
      AutomatonPane view = tool.getView();
      if (view != null) {
        note.initializeForView(view);
        AutomatonDrawer drawer = view.getDrawer();
        if (drawer != null && drawer.getAutomaton() != null) {
          drawer.getAutomaton().addNote(note);
        }
      }
    }

    AutomatonPane view = tool.getView();
    if (view != null) {
      view.repaint();
    }
  }

  private boolean drawsStateLabels() {
    try {
      ArrowTool tool = tool();
      if (tool == null) {
        return false;
      }
      AutomatonDrawer drawer = tool.getDrawer();
      return drawer != null && drawer.doesDrawStateLabels();
    } catch (Throwable ignored) {
      return false;
    }
  }

  private boolean isAutoZoomEnabled() {
    try {
      ArrowTool tool = tool();
      if (tool == null) {
        return false;
      }
      AutomatonPane view = tool.getView();
      return view != null && view.getAdapt();
    } catch (Throwable ignored) {
      return false;
    }
  }

  private void saveStatusIfPossible() {
    try {
      ArrowTool tool = tool();
      if (tool == null) {
        return;
      }
      AutomatonDrawer drawer = tool.getDrawer();
      if (drawer == null) {
        return;
      }

      if (drawer.getAutomaton() == null) {
        return;
      }

      EnvironmentFrame frame = drawer.getAutomaton().getEnvironmentFrame();
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

  private ArrowTool tool() {
    if (cachedTool != null) {
      return cachedTool;
    }

    try {
      Field f = getClass().getDeclaredField("this$0");
      f.setAccessible(true);
      Object value = f.get(this);
      if (value instanceof ArrowTool) {
        cachedTool = (ArrowTool) value;
        return cachedTool;
      }
    } catch (Throwable ignored) {
      // ignore
    }

    try {
      Field f = getClass().getDeclaredField("this$0$");
      f.setAccessible(true);
      Object value = f.get(this);
      if (value instanceof ArrowTool) {
        cachedTool = (ArrowTool) value;
        return cachedTool;
      }
    } catch (Throwable ignored) {
      // ignore
    }

    return null;
  }


}
