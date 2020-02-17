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

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.dao.BaseDao;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.EntityType;
import de.fraunhofer.iosb.ilt.sta.model.Id;
import de.fraunhofer.iosb.ilt.sta.model.MultiDatastream;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import de.fraunhofer.iosb.ilt.sta.model.TimeObject;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

/**
 *
 * @author scf
 */
public class AggregateCombo implements Comparable<AggregateCombo> {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregateCombo.class);

    public final Thing targetThing;
    public final MultiDatastream target;
    public Datastream sourceDs;
    public MultiDatastream sourceMds;
    public boolean sourceIsAggregate;
    /**
     * Indicates that observations in the source contain a list of values.
     */
    public boolean sourceIsCollection = false;
    public AggregationLevel level;
    public String baseName;
    private ZoneId zoneId;
    private Interval currentInterval;

    public AggregateCombo(Thing targetThing, MultiDatastream target) {
        this.targetThing = targetThing;
        this.target = target;
    }

    public boolean hasSource() {
        return sourceDs != null || sourceMds != null;
    }

    public EntityType getSourceType() {
        if (sourceDs != null) {
            return EntityType.DATASTREAM;
        }
        if (sourceMds != null) {
            return EntityType.MULTIDATASTREAM;
        }
        return null;
    }

    public Id getSourceId() {
        if (sourceDs != null) {
            return sourceDs.getId();
        }
        if (sourceMds != null) {
            return sourceMds.getId();
        }
        return null;
    }

    public BaseDao<Observation> getObsDaoForSource() {
        if (sourceDs != null) {
            return sourceDs.observations();
        }
        if (sourceMds != null) {
            return sourceMds.observations();
        }
        return null;
    }

    public Observation getLastForTarget() {
        try {
            return target.observations().query().select("id", "phenomenonTime").orderBy("phenomenonTime desc").first();
        } catch (ServiceFailureException ex) {
            LOGGER.error("Error fetching last observation.", ex);
            return null;
        }
    }

    public Observation getFirstForSource() {
        try {
            if (hasSource()) {
                return getObsDaoForSource().query().select("id", "phenomenonTime").orderBy("phenomenonTime asc").first();
            }
            return null;
        } catch (ServiceFailureException ex) {
            LOGGER.error("Error fetching first observation.", ex);
            return null;
        }
    }

    public Observation getLastForSource() {
        try {
            if (hasSource()) {
                return getObsDaoForSource().query().select("id", "phenomenonTime").orderBy("phenomenonTime desc").first();
            }
            return null;
        } catch (ServiceFailureException ex) {
            LOGGER.error("Error fetching last observation.", ex);
            return null;
        }
    }

    public List<Observation> getObservationsForSource(Instant start, Instant end) {
        List<Observation> result = new ArrayList<>();
        if (hasSource()) {
            try {
                StringBuilder filter = new StringBuilder();
                filter.append("overlaps(phenomenonTime,").append(start.toString()).append("/").append(end.toString()).append(")");
                EntityList<Observation> entityList = getObsDaoForSource().query().filter(filter.toString()).orderBy("phenomenonTime asc").top(1000).list();
                for (Iterator<Observation> it = entityList.fullIterator(); it.hasNext();) {
                    Observation entity = it.next();
                    result.add(entity);
                }
            } catch (ServiceFailureException ex) {
                LOGGER.error("Failed to fetch observations.", ex);
            }
        }
        return result;
    }

    public void resolveZoneId(ZoneId dflt) {
        if (zoneId == null) {
            Map<String, Object> properties = targetThing.getProperties();
            Object zoneName = properties.get("timeZone");
            if (zoneName == null || zoneName.toString().isEmpty()) {
                zoneId = dflt;
            } else {
                try {
                    zoneId = ZoneId.of(zoneName.toString());
                } catch (DateTimeException ex) {
                    LOGGER.warn("Invalid zone: " + zoneName, ex);
                    zoneId = dflt;
                }
            }
        }
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public String getSourceObsMqttPath() {
        if (sourceDs != null) {
            return "v1.0/Datastreams(" + sourceDs.getId() + ")/Observations?$select=id,phenomenonTime";
        }
        if (sourceMds != null) {
            return "v1.0/MultiDatastreams(" + sourceMds.getId() + ")/Observations?$select=id,phenomenonTime";
        }
        return "";
    }

    public List<Interval> calculateIntervalsForTime(TimeObject phenTime) {
        List<Interval> retval = new ArrayList<>();
        Instant phenTimeStart = Utils.getPhenTimeStart(phenTime);
        Instant phenTimeEnd = Utils.getPhenTimeEnd(phenTime);
        ZonedDateTime atZone = phenTimeStart.atZone(getZoneId());
        ZonedDateTime intStart = level.toIntervalStart(atZone);
        ZonedDateTime intEnd = intStart.plus(level.amount, level.unit);
        retval.add(Interval.of(intStart.toInstant(), intEnd.toInstant()));
        while (intEnd.toInstant().isBefore(phenTimeEnd)) {
            intStart = intEnd;
            intEnd = intStart.plus(level.amount, level.unit);
            retval.add(Interval.of(intStart.toInstant(), intEnd.toInstant()));
        }
        return retval;
    }

    /**
     * Checks if the given interval is the same as the current interval. If they
     * are not the same, the current interval is updated.
     *
     * @param other The interval to check against the current interval and to
     * replace the current interval with if they are not the same.
     * @return null if the given interval is the same as the current interval,
     * otherwise the current interval.
     */
    public Interval replaceIfNotCurrent(Interval other) {
        if (currentInterval == null) {
            // There is no interval yet. This happens the first time at startup.
            currentInterval = other;
            return null;
        }
        if (currentInterval.equals(other)) {
            // The given interval is the same. Do nothing.
            return null;
        } else {
            // The interval changed. Recalculate the old interval.
            Interval old = currentInterval;
            currentInterval = other;
            return old;
        }
    }

    /**
     * Unsets the current interval. If the given interval is the same as the
     * current interval, null is returned. If the given interval is not the same
     * as the current interval, the current interval is returned.
     *
     * @param other The interval to check against the current interval.
     * @return null if the given interval is the same as the current interval.
     */
    public Interval unsetCurrent(Interval other) {
        if (currentInterval == null) {
            // There is no interval.
            return null;
        }
        if (currentInterval.equals(other)) {
            // The given interval is the same. Do nothing.
            currentInterval = null;
            return null;
        } else {
            // The interval is different. Recalculate the old interval.
            Interval old = currentInterval;
            currentInterval = null;
            return old;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            return true;
        }
        if (!(obj instanceof AggregateCombo)) {
            return false;
        }
        AggregateCombo otherCombo = (AggregateCombo) obj;
        if (!level.equals(otherCombo.level)) {
            return false;
        }
        if (!baseName.equals(otherCombo.baseName)) {
            return false;
        }
        return target.getId().equals(otherCombo.target.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, level, baseName);
    }

    @Override
    public int compareTo(AggregateCombo o) {
        return new CompareToBuilder()
                .append(level, o.level)
                .append(baseName, o.baseName)
                .toComparison();
    }

    @Override
    public String toString() {
        if (sourceDs != null) {
            return baseName + " " + level + ". (d " + sourceDs.getId() + " -> md " + target.getId() + ")";
        }
        if (sourceMds != null) {
            return baseName + " " + level + ". (md " + sourceMds.getId() + " -> md " + target.getId() + ")";
        }
        return baseName + " " + level + ". (? -> md " + target.getId() + ")";
    }

    public String getBaseName() {
        return baseName;
    }

}
