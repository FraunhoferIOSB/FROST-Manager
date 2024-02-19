/*
 * Copyright (C) 2024 Fraunhofer IOSB
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.sensorthingsmanager.controller.gui;

import de.fraunhofer.iosb.ilt.frostclient.model.Entity;
import de.fraunhofer.iosb.ilt.frostclient.model.EntityType;
import de.fraunhofer.iosb.ilt.frostclient.model.property.NavigationProperty;
import de.fraunhofer.iosb.ilt.frostclient.query.Query;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.controller.ControllerCollection;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.controller.EntityGuiController;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.controller.FactoryEntityPanel;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hylke
 */
public class helper {

    private helper() {
        // Utility class, not for instantiation.
    }

    public static interface ChildSetter {

        public void setChild(Entity child);

        default public void setChildren(List<Entity> children) {
            // Does nothing by default.
        }
    }

    public static TitledPane createEditableEntityPane(
            final Entity parentEntity,
            final NavigationProperty<Entity> npe,
            final Entity childEntity,
            final Query childQuery,
            String orderby,
            final ChildSetter setter) throws IOException {

        EntityType type = childQuery.getEntityType();
        String paneTitle;
        if (childEntity == null) {
            paneTitle = npe.getName() + ": No " + npe.getEntityType().getShortName() + " selected";
        } else {
            paneTitle = npe.getName() + ": " + childEntity.toString();
        }
        Node pane = FactoryEntityPanel.getPane(childQuery.getService(), type, childEntity, false);
        TitledPane tp = new TitledPane(paneTitle, pane);
        Button edit = new Button("🔧");
        tp.setGraphic(edit);
        edit.setOnAction((ActionEvent event) -> {
            Optional<List<Entity>> result = entitySearchDialog(childQuery, false, orderby);
            if (result.isPresent() && !result.get().isEmpty()) {
                Entity newChild = result.get().get(0);
                setter.setChild(newChild);
                try {
                    tp.setContent(FactoryEntityPanel.getPane(childQuery.getService(), type, childEntity, false));
                } catch (IOException ex) {
                    LoggerFactory.getLogger(EntityGuiController.class).error("Failed to load Collection Pane.", ex);
                }
                tp.setText(newChild.getEntityType().getEntityName() + ": " + newChild.toString());
            }
        });

        return tp;
    }

    public static void addLabelTo(GridPane gp, int row, String title) {
        gp.getRowConstraints().add(new RowConstraints(Region.USE_PREF_SIZE, Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE, Priority.NEVER, VPos.BASELINE, false));
        gp.add(new Label(title), 0, row, 2, 1);
    }

    public static void addSeparatorTo(GridPane gp, int row) {
        gp.getRowConstraints().add(new RowConstraints(10, Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE, Priority.NEVER, VPos.CENTER, false));
        final Separator separator = new Separator();
        separator.setPrefWidth(200);
        gp.add(separator, 0, row, 2, 1);
    }
    public static final String SUPPRESS = "_SUPPRESS_";

    public static <T extends Node> T addFieldTo(GridPane gp, int row, String title, T node, boolean fillHeight, boolean editable) {
        gp.getRowConstraints().add(new RowConstraints(Region.USE_PREF_SIZE, Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE, Priority.NEVER, VPos.BASELINE, fillHeight));
        final int idx = title == null ? -1 : title.indexOf(SUPPRESS);
        if (idx >= 0) {
            title = title.substring(0, idx);
        }
        if (Utils.isNullOrEmpty(title)) {
            gp.add(node, 0, row, 2, 1);
        } else {
            gp.add(new Label(title), 0, row);
            gp.add(node, 1, row);
        }
        if (node instanceof TextArea) {
            ((TextArea) node).setPrefRowCount(4);
        }
        if (node instanceof TextInputControl) {
            ((TextInputControl) node).setEditable(editable);
        } else {
            node.setDisable(!editable);
        }
        return node;
    }

    public static Pane createCollectionPaneFor(Query query, String orderBy) {
        return createCollectionPaneFor(query, orderBy, false, null);
    }

    public static Pane createCollectionPaneFor(Query query, String orderBy, boolean canLinkNew, ChildSetter childSetter) {
        try {
            FXMLLoader loader = new FXMLLoader(EntityGuiController.class.getResource("/fxml/Collection.fxml"));
            AnchorPane content = (AnchorPane) loader.load();
            ControllerCollection controller = loader.<ControllerCollection>getController();
            controller.setQuery(query, true, true, canLinkNew, true, orderBy);
            if (childSetter != null) {
                controller.setChildSetter(childSetter);
            }
            return content;
        } catch (IOException ex) {
            LoggerFactory.getLogger(EntityGuiController.class).error("Failed to load Collection Pane.", ex);
        }
        return null;
    }

    public static Optional<List<Entity>> entitySearchDialog(Query query, boolean multiSelect, String orderBy) {
        try {
            FXMLLoader loader = new FXMLLoader(EntityGuiController.class.getResource("/fxml/Collection.fxml"));
            AnchorPane content = (AnchorPane) loader.load();
            final ControllerCollection controller = loader.<ControllerCollection>getController();
            controller.setQuery(query, false, false, false, multiSelect, orderBy);

            Dialog<List<Entity>> dialog = new Dialog<>();
            dialog.setHeight(800);
            if (multiSelect) {
                dialog.setTitle("Choose one or more " + query.getEntityType().getEntityName());
            } else {
                dialog.setTitle("Choose a " + query.getEntityType().getEntityName());
            }
            dialog.setResizable(true);
            dialog.getDialogPane().setContent(content);
            ButtonType buttonTypeOk = new ButtonType("Set", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);
            ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().add(buttonTypeCancel);
            dialog.setResultConverter((ButtonType button) -> {
                if (button == buttonTypeOk) {
                    if (multiSelect) {
                        return controller.getSelectedEntities();
                    }
                    List<Entity> list = new ArrayList<>();
                    list.add(controller.getSelectedEntity());
                    return list;
                }
                return null;
            });
            return dialog.showAndWait();

        } catch (IOException ex) {
            LoggerFactory.getLogger(EntityGuiController.class).error("Failed to load Tab.", ex);
            return Optional.empty();
        }
    }

}
