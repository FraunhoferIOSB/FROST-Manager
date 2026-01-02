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
import de.fraunhofer.iosb.ilt.frostclient.model.Property;
import de.fraunhofer.iosb.ilt.frostclient.model.PropertyType;
import de.fraunhofer.iosb.ilt.frostclient.model.csdl.annotation.Annotation;
import de.fraunhofer.iosb.ilt.frostclient.model.property.EntityProperty;
import de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypeCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

/**
 *
 * @author hylke
 */
public class GuiGlueCollection implements PropertyGuiGlue<GuiGlueCollection> {

    private ComplexValue<? extends ComplexValue> entity;
    private final EntityProperty<List> property;
    private final PropertyType subPropertyType;
    private final PropertyPlaceholder subProperty;

    private final List<PropertyGuiGlue> subValues = new ArrayList<>();
    private List value;

    private GridPane subGrid;
    private Button button;
    private boolean editable;

    public GuiGlueCollection(ComplexValue entity, EntityProperty<List> property, PropertyType subPropertyType) {
        this.entity = entity;
        this.property = property;
        this.subPropertyType = subPropertyType;
        this.subProperty = new PropertyPlaceholder(subPropertyType);
    }

    public GuiGlueCollection init(GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        return init("", gridProperties, itemCount, editable);
    }

    public GuiGlueCollection init(String namePrefix, GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        this.editable = editable;
        Helper.addLabelTo(gridProperties, itemCount.getAndIncrement(), property.getName());
        PropertyType pt = property.getType();
        if (pt instanceof TypeCollection ptc) {
            value = entity.getProperty(property);
            if (value == null) {
                value = new ArrayList();
            }
            subGrid = Helper.addFieldTo(gridProperties, itemCount.getAndIncrement(), namePrefix + property, new GridPane(), false, editable);
            Button button = Helper.addFieldTo(subGrid, itemCount.getAndIncrement(), "", new Button("+item"), false, editable);
            AtomicInteger subItemCount = new AtomicInteger();
            for (Object subValue : value) {
                ComplexValuePlaceholder valueHolder = new ComplexValuePlaceholder().setProperty("item", subValue);
                PropertyGuiGlue subItem = PropertyGuiGlue.createGuiElement("", valueHolder, subProperty, editable, subGrid, subItemCount);
                subValues.add(subItem);
            }
        }
        Helper.addSeparatorTo(gridProperties, itemCount.getAndIncrement());
        return this;
    }

    @Override
    public void entityToGui() {
        value = entity.getProperty(property);
        subValues.clear();
        AtomicInteger subItemCount = new AtomicInteger();

        for (Object subValue : value) {
            ComplexValuePlaceholder valueHolder = new ComplexValuePlaceholder().setProperty("item", subValue);
            PropertyGuiGlue subItem = PropertyGuiGlue.createGuiElement("", valueHolder, subProperty, editable, subGrid, subItemCount);
            subValues.add(subItem);
        }
        for (PropertyGuiGlue subProp : subValues) {
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
            value.clear();
            for (PropertyGuiGlue subProp : subValues) {
                subProp.guiToEntity();
                Object item = subProp.getEntity().getProperty("item");
                value.add(item);
            }
        }
    }

    @Override
    public boolean isGuiNullOrEmpty() {
        if (subValues.isEmpty()) {
            return true;
        }
        for (PropertyGuiGlue subProp : subValues) {
            if (!subProp.isGuiNullOrEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public GuiGlueCollection setEnabled(boolean enabled) {
        for (PropertyGuiGlue subProp : subValues) {
            subProp.setEnabled(enabled);
        }
        return this;
    }

    @Override
    public ComplexValue<? extends ComplexValue> getEntity() {
        return entity;
    }

    @Override
    public void setEntity(ComplexValue entity) {
        this.entity = entity;
    }

    private static class ComplexValuePlaceholder implements ComplexValue<ComplexValuePlaceholder> {

        private Object value;

        @Override
        public <P> P getProperty(Property<P> property) {
            return (P) value;
        }

        @Override
        public <P> ComplexValuePlaceholder setProperty(Property<P> property, P value) {
            this.value = value;
            return this;
        }

        @Override
        public Object getProperty(String name) {
            return value;
        }

        @Override
        public ComplexValuePlaceholder setProperty(String name, Object value) {
            this.value = value;
            return this;
        }

    }

    private static class PropertyPlaceholder implements EntityProperty<Object> {

        private final PropertyType type;

        public PropertyPlaceholder(PropertyType type) {
            this.type = type;
        }

        @Override
        public String getName() {
            return " ";
        }

        @Override
        public String getJsonName() {
            return " ";
        }

        @Override
        public PropertyType getType() {
            return type;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public boolean isNullable() {
            return false;
        }

        @Override
        public boolean isKeyPart() {
            return false;
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public List<Annotation> getAnnotations() {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

    }
}
