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
import de.fraunhofer.iosb.ilt.frostclient.model.property.EntityProperty;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.Utils;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 *
 * @author hylke
 */
public class GuiGlueSimpleString implements PropertyGuiGlue<GuiGlueSimpleString> {

    private final ComplexValue<?> entity;
    private final EntityProperty<String> property;
    private TextField field;

    public GuiGlueSimpleString(ComplexValue entity, EntityProperty<String> property) {
        this.entity = entity;
        this.property = property;
    }

    public GuiGlueSimpleString init(GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        return init("", gridProperties, itemCount, editable);
    }

    public GuiGlueSimpleString init(String namePrefix, GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        field = helper.addFieldTo(gridProperties, itemCount.getAndIncrement(), namePrefix + property, new TextField(), false, editable);
        return this;
    }

    @Override
    public void entityToGui() {
        final Object value = entity.getProperty(property);
        if (value == null) {
            field.setText("");
        } else {
            field.setText(Objects.toString(value));
        }
    }

    @Override
    public void guiToEntity() {
        if (property.isReadOnly()) {
            return;
        }
        final String value = field.getText();
        if (property.isNullable() && value.length() == 0) {
            entity.setProperty(property, null);
        } else {
            entity.setProperty(property, value);
        }
    }

    @Override
    public boolean isGuiNullOrEmpty() {
        return Utils.isNullOrEmpty(field.getText());
    }

    @Override
    public GuiGlueSimpleString setEnabled(boolean enabled) {
        field.setEditable(enabled);
        return this;
    }

}
