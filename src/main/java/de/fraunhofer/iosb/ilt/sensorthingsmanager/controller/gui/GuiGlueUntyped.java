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
import static de.fraunhofer.iosb.ilt.sensorthingsmanager.controller.gui.Helper.SUPPRESS;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;

/**
 *
 * @author hylke
 */
public class GuiGlueUntyped implements PropertyGuiGlue<GuiGlueUntyped> {

    private enum Type {
        NUMBER("Number"),
        STRING("String"),
        OBJECT("Object"),
        ARRAY("Array"),
        BOOLEAN("Boolean");
        private final String label;

        private Type(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private ComplexValue<?> entity;
    private final EntityProperty property;
    private ComboBox<Type> comboBoxType;
    private PropertyGuiGlue subBox;
    GridPane gridPane;
    private boolean editable;

    public GuiGlueUntyped(ComplexValue entity, EntityProperty property) {
        this.entity = entity;
        this.property = property;
    }

    public GuiGlueUntyped init(GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        return init("", gridProperties, itemCount, editable);
    }

    public GuiGlueUntyped init(String namePrefix, GridPane parentPane, AtomicInteger itemCount, boolean editable) {
        this.editable = editable;
        gridPane = new GridPane();
        comboBoxType = new ComboBox<>();
        comboBoxType.getItems().setAll(Type.values());
        comboBoxType.setValue(Type.NUMBER);
        comboBoxType.setDisable(!editable);
        comboBoxType.setOnAction(evt -> boxChanged());
        Helper.addFieldTo(parentPane, itemCount.getAndIncrement(), namePrefix + property, comboBoxType, false, editable);
        Helper.addFieldTo(parentPane, itemCount.getAndIncrement(), " ", gridPane, false, editable);
        subBox = new GuiGlueSimpleNumber(entity, property)
                .init(SUPPRESS, gridPane, itemCount, editable);
        return this;
    }

    private void boxChanged() {
        Type type = comboBoxType.getValue();
        gridPane.getChildren().clear();
        switch (type) {
            case NUMBER:
                subBox = new GuiGlueSimpleNumber(entity, property)
                        .init(SUPPRESS, gridPane, new AtomicInteger(), editable);
                break;

            case STRING:
                subBox = new GuiGlueSimpleString(entity, property)
                        .init(SUPPRESS, gridPane, new AtomicInteger(), editable);
                break;

            case OBJECT:
                subBox = new GuiGlueOpenType(entity, property)
                        .init(SUPPRESS, gridPane, new AtomicInteger(), editable);
                break;

            case ARRAY:
                subBox = new GuiGlueArray(entity, property)
                        .init(SUPPRESS, gridPane, new AtomicInteger(), editable);
                break;

            case BOOLEAN:
                break;

            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    @Override
    public void entityToGui() {
        Type type = comboBoxType.getValue();
        Object value = entity.getProperty(property);
        if (value instanceof Number) {
            if (type != Type.NUMBER) {
                comboBoxType.setValue(Type.NUMBER);
                boxChanged();
            }
        } else if (value instanceof List) {
            if (type != Type.ARRAY) {
                comboBoxType.setValue(Type.ARRAY);
                boxChanged();
            }
        } else if (value instanceof String) {
            if (type != Type.STRING) {
                comboBoxType.setValue(Type.STRING);
                boxChanged();
            }
        } else {
            if (type != Type.OBJECT) {
                comboBoxType.setValue(Type.OBJECT);
                boxChanged();
            }
        }
        subBox.entityToGui();
    }

    @Override
    public void guiToEntity() {
        if (!property.isReadOnly()) {
            subBox.guiToEntity();
        }
    }

    @Override
    public boolean isGuiNullOrEmpty() {
        return subBox.isGuiNullOrEmpty();
    }

    @Override
    public GuiGlueUntyped setEnabled(boolean enabled) {
        comboBoxType.setEditable(enabled);
        subBox.setEnabled(enabled);
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
