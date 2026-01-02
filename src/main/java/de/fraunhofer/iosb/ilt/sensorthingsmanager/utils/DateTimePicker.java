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
package de.fraunhofer.iosb.ilt.sensorthingsmanager.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import net.time4j.ClockUnit;
import net.time4j.Moment;
import net.time4j.PlainTimestamp;
import net.time4j.TemporalType;
import net.time4j.ZonalDateTime;
import net.time4j.tz.TZID;

/**
 *
 * @author scf
 */
public class DateTimePicker extends GridPane {

    public static final ZoneId ZONE_UTC = ZoneId.of("Z");

    final Moment initialValue;
    private DatePicker date;
    private Spinner<Integer> hour;
    private Spinner<Integer> minute;
    private Spinner<Integer> second;
    private Spinner<Integer> ms;

    public DateTimePicker() {
        this((Moment) null);
    }

    public DateTimePicker(Instant initialValue) {
        if (initialValue == null) {
            this.initialValue = null;
        } else {
            this.initialValue = Moment.from(initialValue);
        }
        buildGui();
    }

    public DateTimePicker(Moment initialValue) {
        this.initialValue = initialValue;
        buildGui();
    }

    public DateTimePicker(ZonedDateTime initialValue) {
        if (initialValue == null) {
            this.initialValue = null;
        } else {
            this.initialValue = TemporalType.ZONED_DATE_TIME.translate(initialValue).toMoment();
        }
        buildGui();
    }

    private void buildGui() {
        int startHourVal = 0;
        int startMinuteVal = 0;
        int startSecondVal = 0;
        int startMsVal = 0;
        if (initialValue == null) {
            date = new DatePicker();
        } else {
            final ZonalDateTime inLocalView = initialValue.inLocalView();
            date = new DatePicker(LocalDate.from(inLocalView.toTemporalAccessor()));
            startHourVal = inLocalView.get(ChronoField.HOUR_OF_DAY);
            startMinuteVal = inLocalView.get(ChronoField.MINUTE_OF_HOUR);
            startSecondVal = inLocalView.get(ChronoField.SECOND_OF_MINUTE);
            startMsVal = inLocalView.get(ChronoField.MILLI_OF_SECOND);
        }
        hour = new Spinner<>(0, 23, startHourVal);
        hour.setEditable(true);
        hour.setPrefWidth(80);

        minute = new Spinner<>(0, 59, startMinuteVal);
        minute.setEditable(true);
        minute.setPrefWidth(80);

        second = new Spinner<>(0, 59, startSecondVal);
        second.setEditable(true);
        second.setPrefWidth(80);

        ms = new Spinner<>(0, 999, startMsVal);
        ms.setEditable(true);
        ms.setPrefWidth(80);

        date.setConverter(new StringConverter<LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

            @Override
            public String toString(LocalDate localDate) {
                if (localDate == null) {
                    return "";
                }
                return formatter.format(localDate);
            }

            @Override
            public LocalDate fromString(String dateString) {
                if (Utils.isNullOrEmpty(dateString)) {
                    return null;
                }
                return LocalDate.parse(dateString, formatter);
            }
        });

        int i = 0;
        this.add(date, ++i, 0);
        this.add(hour, ++i, 0);
        this.add(new Text(":"), ++i, 0);
        this.add(minute, ++i, 0);
        this.add(new Text(":"), ++i, 0);
        this.add(second, ++i, 0);
        this.add(new Text("."), ++i, 0);
        this.add(ms, ++i, 0);
    }

    public Moment getValue() {
        if (date.getValue() == null) {
            return null;
        }
        LocalDate localDate = date.getValue();
        return PlainTimestamp.of(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth(), hour.getValue(), minute.getValue(), second.getValue())
                .plus(ms.getValue(), ClockUnit.MILLIS)
                .inStdTimezone();
    }

    public Moment getValue(TZID tzid) {
        if (date.getValue() == null) {
            return null;
        }
        LocalDate localDate = date.getValue();
        return PlainTimestamp.of(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth(), hour.getValue(), minute.getValue(), second.getValue())
                .plus(ms.getValue(), ClockUnit.MILLIS)
                .inTimezone(tzid);
    }

    public void setValue(Moment value) {
        if (value == null) {
            date.setValue(null);
            hour.getValueFactory().setValue(0);
            minute.getValueFactory().setValue(0);
            second.getValueFactory().setValue(0);
            ms.getValueFactory().setValue(0);
        } else {
            final ZonalDateTime inLocalView = value.inLocalView();
            date.setValue(LocalDate.from(inLocalView.toTemporalAccessor()));
            hour.getValueFactory().setValue(inLocalView.get(ChronoField.HOUR_OF_DAY));
            minute.getValueFactory().setValue(inLocalView.get(ChronoField.MINUTE_OF_HOUR));
            second.getValueFactory().setValue(inLocalView.get(ChronoField.SECOND_OF_MINUTE));
            ms.getValueFactory().setValue(inLocalView.get(ChronoField.MILLI_OF_SECOND));
        }
    }

}
