/*
 * Copyright (C) 2018 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
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
package de.fraunhofer.iosb.ilt.sensorthingsmanager.utils;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import net.time4j.Moment;

/**
 *
 * @author scf
 */
public class DateTimePicker extends GridPane {

    public static final ZoneId ZONE_UTC = ZoneId.of("Z");

    final ZonedDateTime initialValue;
    private DatePicker date;
    private Spinner<Integer> hour;
    private Spinner<Integer> minute;

    public DateTimePicker() {
        this(ZonedDateTime.now());
    }

    public DateTimePicker(Moment initialValue) {
        this(initialValue.toTemporalAccessor());
    }

    public DateTimePicker(Instant initialValue) {
        if (initialValue == null) {
            initialValue = Instant.now();
        }
        this.initialValue = initialValue.atZone(ZONE_UTC);
        buildGui();
    }

    public DateTimePicker(ZonedDateTime initialValue) {
        if (initialValue == null) {
            initialValue = ZonedDateTime.now();
        }
        this.initialValue = initialValue;
        buildGui();
    }

    private void buildGui() {
        date = new DatePicker(initialValue.toLocalDate());

        int startHourVal = initialValue.get(ChronoField.HOUR_OF_DAY);
        hour = new Spinner<>(0, 23, startHourVal);
        hour.setEditable(true);
        hour.setPrefWidth(80);

        int startMinuteVal = initialValue.get(ChronoField.MINUTE_OF_HOUR);
        minute = new Spinner<>(0, 59, startMinuteVal);
        minute.setEditable(true);
        minute.setPrefWidth(80);

        this.add(date, 1, 0);
        this.add(hour, 2, 0);
        this.add(new Text(":"), 3, 0);
        this.add(minute, 4, 0);
    }

    public ZonedDateTime getValue() {
        return ZonedDateTime.of(date.getValue(), LocalTime.of(hour.getValue(), minute.getValue()), ZONE_UTC);
    }

    public ZonedDateTime getValue(ZoneId zoneId) {
        return ZonedDateTime.of(date.getValue(), LocalTime.of(hour.getValue(), minute.getValue()), zoneId);
    }

}
