/*
 * Copyright (C) 2024 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.sensorthingsmanager.controller;

import de.fraunhofer.iosb.ilt.frostclient.SensorThingsService;
import de.fraunhofer.iosb.ilt.frostclient.model.Entity;
import de.fraunhofer.iosb.ilt.frostclient.model.EntityType;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.controller.gui.GuiControllerDefault;
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
        ControllerEntity controller = loader.<ControllerEntity> getController();
        controller.setEntity(service, entity, new GuiControllerDefault(type), showNavProps);
        return content;
    }

}
