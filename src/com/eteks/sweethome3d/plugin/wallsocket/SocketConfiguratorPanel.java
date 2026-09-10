package com.eteks.sweethome3d.plugin.wallsocket;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.swing.ModelPreviewComponent;

/**
 * Configuration panel for a modular wall socket: finishes, gang count, an insert
 * (full or two half-width) per gang, and a live 3D preview.
 */
public class SocketConfiguratorPanel extends JPanel {
  private final ResourceBundle resource;
  private final InsertCatalog catalog;
  private final JComboBox<Standard> standardCombo = new JComboBox<Standard>(Standard.values());
  private final JSpinner gangsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
  private final JComboBox<Finish> frameFinishCombo = new JComboBox<Finish>(Finish.values());
  private final JComboBox<Finish> insertFinishCombo = new JComboBox<Finish>(Finish.values());
  private final JPanel gangsPanel = new JPanel(new GridBagLayout());
  private final List<JComboBox<Choice>> leftCombos = new ArrayList<JComboBox<Choice>>();
  private final List<JComboBox<Choice>> rightCombos = new ArrayList<JComboBox<Choice>>();
  private final ModelPreviewComponent preview;

  /** A selectable insert choice: empty, or a full/half insert of a given type. */
  private static final class Choice {
    final InsertType type;   // null = empty
    final boolean half;

    Choice(InsertType type, boolean half) {
      this.type = type;
      this.half = half;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Choice)) {
        return false;
      }
      Choice c = (Choice) o;
      return c.type == type && c.half == half;
    }

    @Override
    public int hashCode() {
      return 31 * (type == null ? 0 : type.hashCode()) + (half ? 1 : 0);
    }
  }

  private static final Choice EMPTY = new Choice(null, false);

  public SocketConfiguratorPanel(ResourceBundle resource, ClassLoader loader) {
    super(new BorderLayout(10, 0));
    this.resource = resource;
    this.catalog = new InsertCatalog(loader);
    this.preview = new ModelPreviewComponent(true, true, true, true);
    this.preview.setFocusable(false);
    this.preview.setPreferredSize(new Dimension(260, 300));

    frameFinishCombo.setRenderer(new FinishRenderer());
    insertFinishCombo.setRenderer(new FinishRenderer());
    standardCombo.setRenderer(new StandardRenderer());

    JPanel controls = new JPanel(new GridBagLayout());
    int row = 0;
    controls.add(new JLabel(resource.getString("standardLabel.text")),
        gbc(0, row, 1, GridBagConstraints.LINE_START, 0, 0));
    controls.add(standardCombo, gbc(1, row, 1, GridBagConstraints.LINE_START, 5, 0));
    row++;
    controls.add(new JLabel(resource.getString("frameFinishLabel.text")),
        gbc(0, row, 1, GridBagConstraints.LINE_START, 0, 0));
    controls.add(frameFinishCombo, gbc(1, row, 1, GridBagConstraints.LINE_START, 5, 0));
    row++;
    controls.add(new JLabel(resource.getString("insertFinishLabel.text")),
        gbc(0, row, 1, GridBagConstraints.LINE_START, 0, 0));
    controls.add(insertFinishCombo, gbc(1, row, 1, GridBagConstraints.LINE_START, 5, 0));
    row++;
    controls.add(new JLabel(resource.getString("gangsLabel.text")),
        gbc(0, row, 1, GridBagConstraints.LINE_START, 0, 0));
    controls.add(gangsSpinner, gbc(1, row, 1, GridBagConstraints.LINE_START, 5, 0));
    row++;
    controls.add(gangsPanel, gbc(0, row, 2, GridBagConstraints.LINE_START, 0, 0));

    JPanel left = new JPanel(new BorderLayout());
    left.setBorder(BorderFactory.createTitledBorder(resource.getString("configGroup")));
    left.add(controls, BorderLayout.NORTH);
    add(left, BorderLayout.WEST);
    add(preview, BorderLayout.CENTER);

    gangsSpinner.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent ev) {
        rebuildGangs();
        updatePreview();
      }
    });
    frameFinishCombo.addActionListener(ev -> updatePreview());
    insertFinishCombo.addActionListener(ev -> updatePreview());
    standardCombo.addActionListener(ev -> updatePreview());

    rebuildGangs();
    updatePreview();
  }

  public SocketModel.Config getConfig() {
    SocketModel.Config config = new SocketModel.Config();
    config.standard = (Standard) standardCombo.getSelectedItem();
    config.frameFinish = (Finish) frameFinishCombo.getSelectedItem();
    config.insertFinish = (Finish) insertFinishCombo.getSelectedItem();
    for (int i = 0; i < leftCombos.size(); i++) {
      List<SocketModel.Insert> inserts = new ArrayList<SocketModel.Insert>();
      Choice left = (Choice) leftCombos.get(i).getSelectedItem();
      Choice right = (Choice) rightCombos.get(i).getSelectedItem();
      if (left != null && left.type != null) {
        inserts.add(new SocketModel.Insert(left.type, left.half));
        if (left.half && right != null && right.type != null) {
          inserts.add(new SocketModel.Insert(right.type, true));
        }
      } else if (right != null && right.type != null) {
        inserts.add(new SocketModel.Insert(right.type, true));
      }
      config.gangs.add(inserts);
    }
    return config;
  }

  /** Refreshes the preview (called when the dialog is shown again). */
  public void refresh() {
    updatePreview();
  }

  private void rebuildGangs() {
    int n = (Integer) gangsSpinner.getValue();
    Choice[][] old = new Choice[leftCombos.size()][2];
    for (int i = 0; i < leftCombos.size(); i++) {
      old[i][0] = (Choice) leftCombos.get(i).getSelectedItem();
      old[i][1] = (Choice) rightCombos.get(i).getSelectedItem();
    }

    leftCombos.clear();
    rightCombos.clear();
    gangsPanel.removeAll();
    for (int i = 0; i < n; i++) {
      JComboBox<Choice> left = newCombo(leftOptions());
      JComboBox<Choice> right = newCombo(rightOptions());
      if (i < old.length) {
        left.setSelectedItem(old[i][0]);
        right.setSelectedItem(old[i][1]);
      }
      leftCombos.add(left);
      rightCombos.add(right);
      gangsPanel.add(new JLabel(resource.getString("gangLabel.text") + " " + (i + 1) + ":"),
          gbc(0, i, 1, GridBagConstraints.LINE_START, 0, 5));
      gangsPanel.add(left, gbc(1, i, 1, GridBagConstraints.LINE_START, 5, 5));
      gangsPanel.add(right, gbc(2, i, 1, GridBagConstraints.LINE_START, 5, 5));
    }
    gangsPanel.revalidate();
    gangsPanel.repaint();
    syncRight();
  }

  private Choice[] leftOptions() {
    List<Choice> list = new ArrayList<Choice>();
    list.add(EMPTY);
    for (InsertType t : InsertType.values()) {
      list.add(new Choice(t, false));
      list.add(new Choice(t, true));
    }
    return list.toArray(new Choice[0]);
  }

  private Choice[] rightOptions() {
    List<Choice> list = new ArrayList<Choice>();
    list.add(EMPTY);
    for (InsertType t : InsertType.values()) {
      list.add(new Choice(t, true));
    }
    return list.toArray(new Choice[0]);
  }

  private JComboBox<Choice> newCombo(Choice[] options) {
    JComboBox<Choice> combo = new JComboBox<Choice>(options);
    combo.setRenderer(new ChoiceRenderer());
    combo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent ev) {
        syncRight();
        updatePreview();
      }
    });
    return combo;
  }

  private void syncRight() {
    for (int i = 0; i < leftCombos.size(); i++) {
      Choice left = (Choice) leftCombos.get(i).getSelectedItem();
      JComboBox<Choice> right = rightCombos.get(i);
      boolean full = left != null && left.type != null && !left.half;
      right.setEnabled(!full);
      if (full) {
        right.setSelectedItem(EMPTY);
      }
    }
  }

  private void updatePreview() {
    try {
      SocketModel.Result result = SocketModel.compose(getConfig(), catalog);
      Content model = SocketModel.saveToTemp(result.obj, result.mtl);
      Content old = preview.getModel();
      preview.setModel(model);
      if (old != null && old != model) {
        SocketModel.deleteContent(old);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private GridBagConstraints gbc(int x, int y, int w, int anchor, int insetLeft, int insetBottom) {
    return new GridBagConstraints(x, y, w, 1, 0, 0, anchor,
        GridBagConstraints.NONE, new Insets(0, insetLeft, insetBottom, 5), 0, 0);
  }

  private final class ChoiceRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (value != null) {
        Choice c = (Choice) value;
        if (c.type == null) {
          setText(resource.getString("empty"));
        } else {
          String text = resource.getString("insert." + c.type.name().toLowerCase(Locale.ENGLISH));
          if (c.half) {
            text += " " + resource.getString("halfSuffix");
          }
          setText(text);
        }
      }
      return this;
    }
  }

  private final class FinishRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (value != null) {
        setText(resource.getString("finish." + ((Finish) value).name().toLowerCase(Locale.ENGLISH)));
      }
      return this;
    }
  }

  private final class StandardRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (value != null) {
        setText(resource.getString("standard." + ((Standard) value).name().toLowerCase(Locale.ENGLISH)));
      }
      return this;
    }
  }
}
