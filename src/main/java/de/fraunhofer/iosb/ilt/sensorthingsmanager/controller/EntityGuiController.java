package de.fraunhofer.iosb.ilt.sensorthingsmanager.controller;

import de.fraunhofer.iosb.ilt.frostclient.SensorThingsService;
import de.fraunhofer.iosb.ilt.frostclient.model.Entity;
import de.fraunhofer.iosb.ilt.frostclient.model.EntityType;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

/**
 *
 */
public interface EntityGuiController {

    /**
     * Load the fields from the entity into the gui.
     */
    public void loadFields();

    /**
     * Save the fields from the gui into the entity.
     */
    public void saveFields();

    public EntityType getType();

    /**
     *
     * @param service The service the entity belongs to.
     * @param entity the entity.
     * @param gridProperties the grid for entity properties.
     * @param accordionLinks The accordion for navigation properties.
     * @param labelId The label that shows the entity id.
     * @param editable is the entity editable.
     */
    public void init(SensorThingsService service, Entity entity, GridPane gridProperties, Accordion accordionLinks, Label labelId, boolean editable);

}
