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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.ilt.frostclient.model.ComplexValue;
import de.fraunhofer.iosb.ilt.frostclient.model.property.EntityProperty;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.ObjectMapperFactory;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.Utils;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hylke
 */
public class GuiGlueArray implements PropertyGuiGlue<GuiGlueArray> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuiGlueArray.class);
    private final ComplexValue<? extends ComplexValue> entity;
    private final EntityProperty<List> property;
    private TextArea field;

    public GuiGlueArray(ComplexValue entity, EntityProperty<List> property) {
        this.entity = entity;
        this.property = property;
    }

    public GuiGlueArray init(GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        return init("", gridProperties, itemCount, editable);
    }

    public GuiGlueArray init(String namePrefix, GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        field = helper.addFieldTo(gridProperties, itemCount.getAndIncrement(), namePrefix + property.getName(), new TextArea(), true, editable);
        return this;
    }

    @Override
    public void entityToGui() {
        final ObjectMapper mapper = ObjectMapperFactory.get();
        try {
            List value = entity.getProperty(property);
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
                List value = mapper.readValue(textInput, List.class);
                entity.setProperty(property, value);
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
    public GuiGlueArray setEnabled(boolean enabled) {
        field.setEditable(enabled);
        return this;
    }

}
