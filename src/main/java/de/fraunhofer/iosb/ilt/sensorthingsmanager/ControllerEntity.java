package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import java.net.URL;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Entity;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    private Button buttonSave;
    private T entity;
    private EntityGuiController<T> controller;

    @FXML
    private void actionSave(ActionEvent event) {
        controller.saveFields();
        try {
            entity.getService().update(entity);
            controller.loadFields();
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
     * @param entity the entity to set
     * @param controller
     * @return this Controller.
     */
    public ControllerEntity setEntity(T entity, EntityGuiController<T> controller) {
        this.entity = entity;
        this.controller = controller;
        labelType.setText(entity.getType().getName());
        controller.init(entity, gridProperties, accordionLinks, labelId);
        controller.loadFields();
        return this;
    }
}
