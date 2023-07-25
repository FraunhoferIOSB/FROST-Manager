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

import de.fraunhofer.iosb.ilt.frostclient.model.Entity;
import de.fraunhofer.iosb.ilt.frostclient.model.ext.TimeValue;
import de.fraunhofer.iosb.ilt.frostclient.model.ext.UnitOfMeasurement;
import de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsSensingV11;
import java.nio.charset.Charset;
import java.util.regex.Pattern;
import net.time4j.Moment;

/**
 *
 * @author scf
 */
public class Utils {

    public static final UnitOfMeasurement NULL_UNIT = new UnitOfMeasurement(null, null, null);
    public static final String KEY_AGGREGATE_SOURCE = "aggregateSource";
    public static final String KEY_AGGREGATE_SOURCE_D = KEY_AGGREGATE_SOURCE + ".Datastream@iot.id";
    public static final String KEY_AGGREGATE_SOURCE_MD = KEY_AGGREGATE_SOURCE + ".MultiDatastream@iot.id";
    public static final String KEY_AGGREGATE_FOR = "aggregateFor";
    public static final String KEY_AGGREGATE_UNIT = "aggregateUnit";
    public static final String KEY_AGGREGATE_AMOUNT = "aggregateAmount";
    public static final Charset UTF8 = Charset.forName("UTF-8");
    public static final String LB = Pattern.quote("[");
    public static final String RB = Pattern.quote("]");
    public static final Pattern POSTFIX_PATTERN = Pattern.compile("(.+)" + LB + "([0-9]+ [a-zA-Z]+)" + RB);

    public static enum AggregationLevels {
        HOURLY("[1 hour]", "Aggregated hourly"),
        DAILY("[1 day]", "Aggregated daily");

        public final String postfix;
        public final String description;

        private AggregationLevels(String postfix, String description) {
            this.postfix = postfix;
            this.description = description;
        }

    }

    private Utils() {
        // Not to be instantiated.
    }

    public static Moment getPhenTimeStart(Entity obs) {
        TimeValue phenTime = obs.getProperty(SensorThingsSensingV11.EP_PHENOMENONTIME);
        return getPhenTimeStart(phenTime);
    }

    public static Moment getPhenTimeStart(TimeValue phenTime) {
        if (phenTime.isInterval()) {
            return phenTime.getInterval().getStart();
        }
        return phenTime.getInstant().getDateTime();
    }

    public static Moment getPhenTimeEnd(Entity obs) {
        TimeValue phenTime = obs.getProperty(SensorThingsSensingV11.EP_PHENOMENONTIME);
        return getPhenTimeEnd(phenTime);
    }

    public static Moment getPhenTimeEnd(TimeValue phenTime) {
        if (phenTime.isInterval()) {
            return phenTime.getInterval().getEnd();
        }
        return phenTime.getInstant().getDateTime();
    }
}
