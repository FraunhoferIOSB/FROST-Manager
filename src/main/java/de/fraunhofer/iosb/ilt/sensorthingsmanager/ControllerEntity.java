package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import java.net.URL;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Entity;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.GridPane;

/**
 *
 * @author scf
 */
public class ControllerEntity<T extends Entity<T>> implements Initializable {

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
    private T entity;
    private EntityGuiController<T> controller;
    private SensorThingsService service;

    @FXML
    private void actionSave(ActionEvent event) {
        controller.saveFields();
        try {
            if (entity.getId() == null) {
                service.create(entity);
                controller.loadFields();
            } else {
                entity.getService().update(entity);
                controller.loadFields();
            }
        } catch (ServiceFailureException ex) {
            LOGGER.error("Failed to update entity.");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    /**
     * @return the entity
     */
    public T getEntity() {
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
    public ControllerEntity setEntity(SensorThingsService service, T entity, EntityGuiController<T> controller, boolean showNavigationProperties) {
        this.service = service;
        this.entity = entity;
        this.controller = controller;
        labelType.setText(controller.getType().getName());
        if (entity != null && showNavigationProperties) {
            controller.init(service, entity, gridProperties, accordionLinks, labelId, true);
        } else {
            splitPaneMain.getItems().remove(accordionLinks);
            controller.init(service, entity, gridProperties, null, labelId, false);
            buttonSave.setVisible(false);
        }
        controller.loadFields();
        return this;
    }

    public EntityGuiController<T> getController() {
        return controller;
    }

}
