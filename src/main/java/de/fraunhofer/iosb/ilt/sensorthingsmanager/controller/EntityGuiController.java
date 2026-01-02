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
