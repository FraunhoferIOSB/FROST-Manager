package de.fraunhofer.iosb.ilt.sensorthingsmanager.controller;

import de.fraunhofer.iosb.ilt.sensorthingsmanager.controller.gui.GuiControllerDefault;
import de.fraunhofer.iosb.ilt.frostclient.SensorThingsService;
import de.fraunhofer.iosb.ilt.frostclient.model.Entity;
import de.fraunhofer.iosb.ilt.frostclient.model.EntityType;
import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

/**
 *
 * @author scf
 */
public class FactoryEntityPanel {

    private static final String ENTITY_PANE_FXML = "/fxml/PaneEntity.fxml";

    public static Node getPane(SensorThingsService service, EntityType type, Entity entity, boolean showNavProps) throws IOException {
        if (entity != null && entity.getEntityType() != type) {
            throw new IllegalArgumentException("Entity must have given type or be null.");
        }
        if (entity == null) {
            return new Label("No " + type.entityName + ".");
        }
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Node content = (Pane) loader.load();
        ControllerEntity controller = loader.<ControllerEntity>getController();
        controller.setEntity(service, entity, new GuiControllerDefault(type), showNavProps);
        return content;
    }

}
