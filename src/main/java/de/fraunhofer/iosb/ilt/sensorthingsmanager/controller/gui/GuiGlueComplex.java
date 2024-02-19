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

import de.fraunhofer.iosb.ilt.frostclient.model.ComplexValue;
import de.fraunhofer.iosb.ilt.frostclient.model.Property;
import de.fraunhofer.iosb.ilt.frostclient.model.PropertyType;
import de.fraunhofer.iosb.ilt.frostclient.model.property.EntityProperty;
import de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypeComplex;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.layout.GridPane;

/**
 *
 * @author hylke
 */
public class GuiGlueComplex implements PropertyGuiGlue<GuiGlueComplex> {

    private final ComplexValue<? extends ComplexValue> entity;
    private final EntityProperty<ComplexValue> property;
    private final Map<String, PropertyGuiGlue> subProperties = new HashMap<>();
    private ComplexValue value;

    public GuiGlueComplex(ComplexValue entity, EntityProperty<ComplexValue> property) {
        this.entity = entity;
        this.property = property;
    }

    public GuiGlueComplex init(GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        return init("", gridProperties, itemCount, editable);
    }

    public GuiGlueComplex init(String namePrefix, GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        helper.addLabelTo(gridProperties, itemCount.getAndIncrement(), property.getName());
        PropertyType pt = property.getType();
        if (pt instanceof TypeComplex ptc) {
            value = entity.getProperty(property);
            if (value == null) {
                value = ptc.instantiate();
            }
            for (Property subProperty : ptc.getProperties()) {
                if (subProperty instanceof EntityProperty ep) {
                    PropertyGuiGlue subItem = PropertyGuiGlue.createGuiElement("-", value, ep, editable, gridProperties, itemCount);
                    subProperties.put(subProperty.getJsonName(), subItem);
                }
            }
        }
        helper.addSeparatorTo(gridProperties, itemCount.getAndIncrement());
        return this;
    }

    @Override
    public void entityToGui() {
        for (PropertyGuiGlue subProp : subProperties.values()) {
            subProp.entityToGui();
        }
    }

    @Override
    public void guiToEntity() {
        if (property.isReadOnly()) {
            return;
        }
        if (property.isNullable() && isGuiNullOrEmpty()) {
            entity.setProperty(property, null);
        } else {
            entity.setProperty(property, value);
            for (PropertyGuiGlue subProp : subProperties.values()) {
                subProp.guiToEntity();
            }
        }
    }

    @Override
    public boolean isGuiNullOrEmpty() {
        for (PropertyGuiGlue subProp : subProperties.values()) {
            if (!subProp.isGuiNullOrEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public GuiGlueComplex setEnabled(boolean enabled) {
        for (PropertyGuiGlue subProp : subProperties.values()) {
            subProp.setEnabled(enabled);
        }
        return this;
    }

}
