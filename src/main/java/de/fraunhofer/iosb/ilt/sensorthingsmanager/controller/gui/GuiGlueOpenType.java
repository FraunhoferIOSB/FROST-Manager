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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.ilt.frostclient.model.ComplexValue;
import de.fraunhofer.iosb.ilt.frostclient.model.property.EntityProperty;
import de.fraunhofer.iosb.ilt.frostclient.models.ext.MapValue;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.ObjectMapperFactory;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.Utils;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hylke
 */
public class GuiGlueOpenType implements PropertyGuiGlue<GuiGlueOpenType> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuiGlueOpenType.class);

    private ComplexValue<? extends ComplexValue> entity;
    private final EntityProperty<ComplexValue> property;
    private TextArea field;

    public GuiGlueOpenType(ComplexValue entity, EntityProperty<ComplexValue> property) {
        this.entity = entity;
        this.property = property;
    }

    public GuiGlueOpenType init(GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        return init("", gridProperties, itemCount, editable);
    }

    public GuiGlueOpenType init(String namePrefix, GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        field = Helper.addFieldTo(gridProperties, itemCount.getAndIncrement(), namePrefix + property.getName(), new TextArea(), true, editable);
        return this;
    }

    @Override
    public void entityToGui() {
        final ObjectMapper mapper = ObjectMapperFactory.get();
        try {
            final ComplexValue<? extends ComplexValue> value = entity.getProperty(property);
            if (value == null) {
                field.setText("");
            } else {
                String textValue = mapper.writeValueAsString(value);
                field.setText(textValue);
            }
        } catch (JsonProcessingException ex) {
            LOGGER.error("Properties can not be converted to JSON.", ex);
        }
    }

    @Override
    public void guiToEntity() {
        final ObjectMapper mapper = ObjectMapperFactory.get();
        try {
            final String textInput = field.getText();
            if (!Utils.isNullOrEmpty(textInput)) {
                ComplexValue properties = mapper.readValue(textInput, MapValue.class);
                entity.setProperty(property, properties);
            }
        } catch (IOException ex) {
            LOGGER.error("Not valid json.", ex);
        }
    }

    @Override
    public boolean isGuiNullOrEmpty() {
        return Utils.isNullOrEmpty(field.getText());
    }

    @Override
    public GuiGlueOpenType setEnabled(boolean enabled) {
        field.setEditable(enabled);
        return this;
    }

    @Override
    public ComplexValue<? extends ComplexValue> getEntity() {
        return entity;
    }

    @Override
    public void setEntity(ComplexValue<? extends ComplexValue> entity) {
        this.entity = entity;
    }

}
