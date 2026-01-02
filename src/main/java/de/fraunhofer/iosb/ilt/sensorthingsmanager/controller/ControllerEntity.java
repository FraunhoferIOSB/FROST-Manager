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
import de.fraunhofer.iosb.ilt.frostclient.exception.ServiceFailureException;
import de.fraunhofer.iosb.ilt.frostclient.model.Entity;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.Utils;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for showing one entity.
 */
public class ControllerEntity implements Initializable {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerEntity.class);
    @FXML
    private Label labelId;
    @FXML
    private Label labelType;
    @FXML
    private Accordion accordionLinks;
    @FXML
    private GridPane gridProperties;
    @FXML
    private SplitPane splitPaneMain;
    @FXML
    private Button buttonSave;
    private Entity entity;
    private EntityGuiController controller;
    private SensorThingsService service;

    @FXML
    private void actionSave(ActionEvent event) {
        controller.saveFields();
        try {
            if (!entity.primaryKeyFullySet()) {
                service.create(entity);
                controller.loadFields();
            } else {
                entity.getService().update(entity);
                controller.loadFields();
            }
        } catch (ServiceFailureException ex) {
            LOGGER.error("Failed to update or create entity.", ex);
            Utils.showAlert(
                    Alert.AlertType.ERROR,
                    "Failed to update or create",
                    "Failed to update or create the entity.",
                    ex);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    /**
     * @return the entity
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * @param service The service.
     * @param entity the entity to set
     * @param controller
     * @param showNavigationProperties Should navigation properties of the
     * selected Entity be shown, or just entityProperties.
     * @return this Controller.
     */
    public ControllerEntity setEntity(SensorThingsService service, Entity entity, EntityGuiController controller, boolean showNavigationProperties) {
        this.service = service;
        this.entity = entity;
        this.controller = controller;
        labelType.setText(entity.getEntityType().getEntityName());
        if (showNavigationProperties) {
            controller.init(service, entity, gridProperties, accordionLinks, labelId, true);
        } else {
            splitPaneMain.getItems().remove(accordionLinks);
            controller.init(service, entity, gridProperties, null, labelId, false);
            buttonSave.setVisible(false);
        }
        controller.loadFields();
        return this;
    }

    public EntityGuiController getController() {
        return controller;
    }

}
