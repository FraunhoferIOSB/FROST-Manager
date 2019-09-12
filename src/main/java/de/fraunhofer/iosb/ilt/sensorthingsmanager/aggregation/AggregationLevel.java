/*
 * Copyright (C) 2019 Fraunhofer IOSB
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
package de.fraunhofer.iosb.ilt.sensorthingsmanager.aggregation;

import com.google.common.collect.ComparisonChain;
import de.fraunhofer.iosb.ilt.configurable.AbstractConfigurable;
import de.fraunhofer.iosb.ilt.configurable.annotations.ConfigurableField;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorEnum;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorInt;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalUnit;
import java.util.Objects;

/**
 *
 * @author scf
 */
public class AggregationLevel extends AbstractConfigurable<Void, Void> implements Comparable<AggregationLevel> {

    @ConfigurableField(editor = EditorEnum.class,
            label = "Unit",
            description = "The unit of the given amount.")
    @EditorEnum.EdOptsEnum(
            sourceType = ChronoUnit.class,
            dflt = "HOURS")
    public TemporalUnit unit;

    @ConfigurableField(editor = EditorInt.class,
            label = "Amount",
            description = "The amount of the given unit.")
    @EditorInt.EdOptsInt(min = 1, max = Integer.MAX_VALUE, step = 1, dflt = 1)
    public int amount;

    public Duration duration;

    public AggregationLevel() {
    }

    public void setUnit(TemporalUnit unit) {
        if (this.unit != null) {
            throw new IllegalStateException("The unit of an aggregation level must not be changed. Already is: " + this.unit);
        }
        this.unit = unit;
        calculateDuration();
    }

    public void setAmount(int amount) {
        if (this.amount != 0) {
            throw new IllegalStateException("The amount of an aggregation level must not be changed. Already is: " + this.amount);
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Aggregation Levels must have a positive, non-zero amount. Given: " + amount);
        }
        this.amount = amount;
        calculateDuration();
    }

    private void calculateDuration() {
        if (unit != null && amount != 0) {
            this.duration = unit.getDuration().multipliedBy(amount);
        }
    }

    public static AggregationLevel of(String postfix) {
        postfix = postfix.trim();
        if (postfix.startsWith("[")) {
            postfix = postfix.substring(1);
        }
        if (postfix.endsWith("]")) {
            postfix = postfix.substring(0, postfix.length() - 1);
        }
        String[] split = postfix.split(" ");
        if (split.length != 2) {
            return null;
        }
        int amount = Integer.parseInt(split[0]);
        ChronoUnit unit = getUnit(split[1]);
        if (unit == null) {
            return null;
        }
        return new AggregationLevel(unit, amount);
    }

    public static boolean isPostfix(String postfix) {
        return of(postfix) != null;
    }

    public static boolean nameHasPostfix(String dsName) {
        return Utils.POSTFIX_PATTERN.matcher(dsName).matches();
    }

    private static ChronoUnit getUnit(String unitString) {
        unitString = unitString.toUpperCase();
        try {
            ChronoUnit unit = ChronoUnit.valueOf(unitString);
            return unit;
        } catch (IllegalArgumentException exc) {
        }
        try {
            ChronoUnit unit = ChronoUnit.valueOf(unitString + "S");
            return unit;
        } catch (IllegalArgumentException exc) {
        }
        return null;
    }

    public AggregationLevel(TemporalUnit unit, int amount) {
        this.unit = unit;
        this.amount = amount;
        this.duration = unit.getDuration().multipliedBy(amount);
    }

    public ZonedDateTime toIntervalStart(ZonedDateTime time) {
        ZonedDateTime start;
        switch (ChronoUnit.valueOf(unit.toString().toUpperCase())) {
            case SECONDS:
                start = time.truncatedTo(ChronoUnit.MINUTES);
                break;
            case MINUTES:
                start = time.truncatedTo(ChronoUnit.HOURS);
                break;
            case HOURS:
                start = time.truncatedTo(ChronoUnit.DAYS);
                break;
            case DAYS:
                start = time.with(TemporalAdjusters.firstDayOfYear()).truncatedTo(ChronoUnit.DAYS);
                break;
            default:
                start = time.with(TemporalAdjusters.firstDayOfYear()).truncatedTo(ChronoUnit.DAYS);
        }
        long haveMillis = Duration.between(start, time).toMillis();
        long maxMillis = duration.toMillis();
        long periods = haveMillis / maxMillis;
        start = start.plus(duration.multipliedBy(periods));
        return start;
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            return true;
        }
        if (!(obj instanceof AggregationLevel)) {
            return false;
        }
        AggregationLevel otherLevel = (AggregationLevel) obj;
        return duration.equals(otherLevel.duration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration);
    }

    @Override
    public int compareTo(AggregationLevel o) {
        return ComparisonChain.start().compare(duration, o.duration).result();
    }

    public String toPostFix() {
        return "[" + amount + " " + unit + "]";
    }

    @Override
    public String toString() {
        return amount + " " + unit;
    }

}
