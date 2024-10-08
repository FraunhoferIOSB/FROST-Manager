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
import de.fraunhofer.iosb.ilt.frostclient.models.ext.TimeInstant;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.Utils;
import java.text.ParseException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.Border;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import net.time4j.Moment;
import net.time4j.format.expert.ChronoFormatter;
import net.time4j.format.expert.PatternType;
import net.time4j.tz.Timezone;

/**
 *
 * @author hylke
 */
public class GuiGlueSimpleDateTime implements PropertyGuiGlue<GuiGlueSimpleDateTime> {

    private ComplexValue<?> entity;
    private final EntityProperty<TimeInstant> property;
    private TextField field;
    private Border origBorder;
    private final ChronoFormatter<Moment> FORMATTER = ChronoFormatter.setUp(Moment.class, Locale.getDefault()).addPattern("uuuu-MM-dd HH:mm:ss.SSSXXX", PatternType.CLDR_24).build().withTimezone(Timezone.ofSystem().getID());

    public GuiGlueSimpleDateTime(ComplexValue entity, EntityProperty<TimeInstant> property) {
        this.entity = entity;
        this.property = property;
    }

    public GuiGlueSimpleDateTime init(GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        return init("", gridProperties, itemCount, editable);
    }

    public GuiGlueSimpleDateTime init(String namePrefix, GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
        field = Helper.addFieldTo(gridProperties, itemCount.getAndIncrement(), namePrefix + property, new TextField(), false, editable);
        origBorder = field.getBorder();
        return this;
    }

    @Override
    public void entityToGui() {
        Object value = entity.getProperty(property);
        if (value == null) {
            field.setText("");
        } else {
            Moment moment = null;
            if (value instanceof TimeInstant ti) {
                moment = ti.getDateTime();
            } else if (value instanceof Moment m) {
                moment = m;
            }
            if (moment != null) {
                field.setText(FORMATTER.print(moment));
            }
        }
    }

    @Override
    public void guiToEntity() {
        if (property.isReadOnly()) {
            return;
        }
        String textData = field.getText();
        if (property.isNullable() && Utils.isNullOrEmpty(textData)) {
            entity.setProperty(property, null);
        } else {
            try {
                Moment result = FORMATTER.parse(textData);
                entity.setProperty(property, TimeInstant.create(result));
                field.setText(FORMATTER.print(result));
                field.setBorder(origBorder);
            } catch (ParseException | IndexOutOfBoundsException ex) {
                field.setBorder(Border.stroke(Color.RED));
                Utils.showAlert(Alert.AlertType.ERROR, "Invalid DateTime", "Failed to parse DateTime. Expected yyyy-MM-dd HH:mm:ss.SSSXXX: " + textData, ex);
            }
        }
    }

    @Override
    public boolean isGuiNullOrEmpty() {
        return Utils.isNullOrEmpty(field.getText());
    }

    @Override
    public GuiGlueSimpleDateTime setEnabled(boolean enabled) {
        field.setEditable(enabled);
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

}
