package com.eteks.sweethome3d.plugin.wallsocket;

import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.plugin.Plugin;
import com.eteks.sweethome3d.plugin.PluginAction;
import com.eteks.sweethome3d.tools.ResourceURLContent;
import com.eteks.sweethome3d.viewcontroller.HomeView;

/**
 * Configurator for modular wall sockets.
 */
public class WallSocketPlugin extends Plugin {
  static final String RESOURCE_BASE =
      "com.eteks.sweethome3d.plugin.wallsocket.ApplicationPlugin";

  private SocketConfiguratorPanel configPanel;

  @Override
  public PluginAction[] getActions() {
    return new PluginAction[] {new PluginAction(RESOURCE_BASE, "WALL_SOCKET",
        getPluginClassLoader(), true) {
      @Override
      public void execute() {
        generateSocket();
      }
    }};
  }

  private void generateSocket() {
    ResourceBundle resource = ResourceBundle.getBundle(
        RESOURCE_BASE, Locale.getDefault(), getPluginClassLoader());
    HomeView homeView = getHomeController().getView();

    if (this.configPanel == null) {
      this.configPanel = new SocketConfiguratorPanel(resource, getPluginClassLoader());
    }
    this.configPanel.refresh();

    if (JOptionPane.showConfirmDialog((JComponent) homeView, this.configPanel,
        resource.getString("dialog.title"), JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
      addSocket(this.configPanel.getConfig(), resource);
    }
  }

  private void addSocket(SocketModel.Config config, ResourceBundle resource) {
    try {
      InsertCatalog catalog = new InsertCatalog(getPluginClassLoader());
      SocketModel.Result result = SocketModel.compose(config, catalog);
      Content model = SocketModel.saveToTemp(result.obj, result.mtl);
      Content icon = new ResourceURLContent(WallSocketPlugin.class, "socket.png");

      HomePieceOfFurniture piece = new HomePieceOfFurniture(new CatalogPieceOfFurniture(
          null, result.name, null, icon, model,
          result.width, result.depth, result.height, 110f, true, null,
          System.getProperty("user.name"), true, null, null));

      UndoableEditSupport undoSupport = getUndoableEditSupport();
      undoSupport.beginUpdate();
      getHomeController().getFurnitureController().addFurniture(
          Arrays.asList(new HomePieceOfFurniture[] {piece}));
      undoSupport.postEdit(new AbstractUndoableEdit() {
        @Override
        public String getPresentationName() {
          return resource.getString("undoInsertSocket");
        }
      });
      undoSupport.endUpdate();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
