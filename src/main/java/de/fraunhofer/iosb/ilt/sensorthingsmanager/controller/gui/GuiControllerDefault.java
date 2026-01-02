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
package de.fraunhofer.iosb.ilt.sensorthingsmanager.controller.gui;

import static de.fraunhofer.iosb.ilt.frostclient.utils.StringHelper.formatKeyValuesForUrl;
import static de.fraunhofer.iosb.ilt.sensorthingsmanager.controller.gui.Helper.createCollectionPaneFor;
import static de.fraunhofer.iosb.ilt.sensorthingsmanager.controller.gui.Helper.createEditableEntityPane;

import de.fraunhofer.iosb.ilt.frostclient.SensorThingsService;
import de.fraunhofer.iosb.ilt.frostclient.model.Entity;
import de.fraunhofer.iosb.ilt.frostclient.model.EntityType;
import de.fraunhofer.iosb.ilt.frostclient.model.property.EntityPropertyMain;
import de.fraunhofer.iosb.ilt.frostclient.model.property.NavigationProperty;
import de.fraunhofer.iosb.ilt.frostclient.model.property.NavigationPropertyEntitySet;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.controller.EntityGuiController;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default GUI controller for entities.
 */
public class GuiControllerDefault implements EntityGuiController {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GuiControllerDefault.class);
    private Label labelId;
    private final EntityType entityType;
    private Entity entity;
    private final List<PropertyGuiGlue> controls = new ArrayList<>();
    private final AtomicInteger itemCount = new AtomicInteger();

    public GuiControllerDefault(EntityType entityType) {
        this.entityType = entityType;
    }

    @Override
    public void loadFields() {
        if (entity == null) {
            return;
        }
        if (entity.primaryKeyFullySet()) {
            labelId.setText(formatKeyValuesForUrl(entity));
        }
        for (PropertyGuiGlue control : controls) {
            control.entityToGui();
        }
    }

    @Override
    public void saveFields() {
        for (PropertyGuiGlue control : controls) {
            control.guiToEntity();
        }
    }

    @Override
    public EntityType getType() {
        return entity.getEntityType();
    }

    @Override
    public void init(SensorThingsService service, Entity entity, GridPane gridProperties, Accordion accordionLinks, Label labelId, boolean editable) {
        this.labelId = labelId;
        this.entity = entity;
        if (entityType != entity.getEntityType()) {
            LOGGER.error("Entity types do not match. {} != {}", entityType, entity.getEntityType());
        }
        Set<EntityPropertyMain> entityProperties = entityType.getEntityProperties();
        for (EntityPropertyMain ep : entityProperties) {
            final boolean editableProp = editable && !ep.isReadOnly();
            addGuiElement(ep.getJsonName(), ep, editableProp, gridProperties);
        }
        if (accordionLinks != null) {
            try {
                for (NavigationProperty<Entity> npe : entityType.getNavigationEntities()) {
                    String order = "";
                    if (npe.getEntityType().hasProperty("name")) {
                        order = "name asc";
                    }
                    final Entity linkedEntity = entity.getProperty(npe);
                    TitledPane tp = createEditableEntityPane(entity, npe, linkedEntity, service.query(npe.getEntityType()), order, child -> entity.setProperty(npe, child));
                    accordionLinks.getPanes().add(tp);
                }
                if (entity.hasService()) {
                    for (NavigationPropertyEntitySet nps : entityType.getNavigationSets()) {
                        Pane pane = createCollectionPaneFor(entity.query(nps), "");
                        accordionLinks.getPanes().add(new TitledPane(nps.getName(), pane));
                    }
                }
            } catch (IOException ex) {
                LOGGER.error("Failed to create panel.", ex);
            } catch (NullPointerException e) {
                // Happens when entity is new.
                LOGGER.trace("Failed to create panel.", e);
            }
        }
    }

    private void addGuiElement(String name, EntityPropertyMain property, boolean editable, GridPane gridProperties) {
        PropertyGuiGlue item = PropertyGuiGlue.createGuiElement(entity, property, editable, gridProperties, itemCount);
        controls.add(item);
    }

}
