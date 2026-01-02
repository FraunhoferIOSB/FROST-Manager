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

import de.fraunhofer.iosb.ilt.frostclient.model.ComplexValue;
import de.fraunhofer.iosb.ilt.frostclient.model.property.EntityProperty;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;

/**
 *
 * @author hylke
 */
public class GuiGlueSimpleBoolean implements PropertyGuiGlue<GuiGlueSimpleBoolean> {

    private enum Values {
        NULL,
        TRUE,
        FALSE,
    }

    private ComplexValue<?> entity;
    private final EntityProperty<Boolean> property;
    private ToggleButton fieldToggle;
    private ComboBox<Values> fieldCombo;

    public GuiGlueSimpleBoolean(ComplexValue entity, EntityProperty<Boolean> property) {
        this.entity = entity;
        this.property = property;
    }

    public GuiGlueSimpleBoolean init(GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        return init("", gridProperties, itemCount, editable);
    }

    public GuiGlueSimpleBoolean init(String namePrefix, GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        if (property.isNullable()) {
            fieldToggle = Helper.addFieldTo(gridProperties, itemCount.getAndIncrement(), namePrefix + property, new ToggleButton(), false, editable);
        } else {
            fieldCombo = new ComboBox<>();
            fieldCombo.getItems().setAll(Values.values());
            fieldCombo.setValue(Values.NULL);
            fieldCombo.setDisable(!editable);
            Helper.addFieldTo(gridProperties, itemCount.getAndIncrement(), namePrefix + property, fieldCombo, false, editable);
        }
        return this;
    }

    @Override
    public void entityToGui() {
        final Boolean value = entity.getProperty(property);
        if (fieldToggle != null) {
            fieldToggle.setSelected(value != null && value);
        }
        if (fieldCombo != null) {
            if (value == null) {
                fieldCombo.setValue(Values.NULL);
            } else if (value) {
                fieldCombo.setValue(Values.TRUE);
            } else {
                fieldCombo.setValue(Values.FALSE);
            }
        }
    }

    @Override
    public void guiToEntity() {
        if (property.isReadOnly()) {
            return;
        }
        if (fieldToggle != null) {
            entity.setProperty(property, fieldToggle.isSelected());
        }
        if (fieldCombo != null) {
            switch (fieldCombo.getValue()) {
                case TRUE:
                    entity.setProperty(property, true);
                    break;

                case FALSE:
                    entity.setProperty(property, false);
                    break;
                case NULL:
                default:
                    entity.setProperty(property, null);
            }
        }
    }

    @Override
    public boolean isGuiNullOrEmpty() {
        return fieldCombo != null && fieldCombo.getValue() == Values.NULL;
    }

    @Override
    public GuiGlueSimpleBoolean setEnabled(boolean enabled) {
        if (fieldToggle != null) {
            fieldToggle.setDisable(!enabled);
        }
        if (fieldCombo != null) {
            fieldCombo.setDisable(!enabled);
        }
        return this;
    }

    @Override
    public ComplexValue<? extends ComplexValue> getEntity() {
        return entity;
    }

    @Override
    public void setEntity(ComplexValue<?> entity) {
        this.entity = entity;
    }

}
