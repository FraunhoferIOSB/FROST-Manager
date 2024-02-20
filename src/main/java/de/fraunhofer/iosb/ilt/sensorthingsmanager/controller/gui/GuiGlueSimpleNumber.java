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
import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.control.TextField;
import javafx.scene.layout.Border;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

/**
 *
 * @author hylke
 */
public class GuiGlueSimpleNumber implements PropertyGuiGlue<GuiGlueSimpleNumber> {

    private final ComplexValue<?> entity;
    private final EntityProperty<Number> property;
    private TextField field;
    private Border origBorder;

    public GuiGlueSimpleNumber(ComplexValue entity, EntityProperty<Number> property) {
        this.entity = entity;
        this.property = property;
    }

    public GuiGlueSimpleNumber init(GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        return init("", gridProperties, itemCount, editable);
    }

    public GuiGlueSimpleNumber init(String namePrefix, GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        field = helper.addFieldTo(gridProperties, itemCount.getAndIncrement(), namePrefix + property, new TextField(), false, editable);
        origBorder = field.getBorder();
        return this;
    }

    @Override
    public void entityToGui() {
        final Number value = entity.getProperty(property);
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
            try {
                entity.setProperty(property, new BigDecimal(value));
                field.setBorder(origBorder);
            } catch (NumberFormatException ex) {
                field.setBorder(Border.stroke(Color.RED));
            }
        }
    }

    @Override
    public boolean isGuiNullOrEmpty() {
        return Utils.isNullOrEmpty(field.getText());
    }

    @Override
    public GuiGlueSimpleNumber setEnabled(boolean enabled) {
        field.setEditable(enabled);
        return this;
    }

}
