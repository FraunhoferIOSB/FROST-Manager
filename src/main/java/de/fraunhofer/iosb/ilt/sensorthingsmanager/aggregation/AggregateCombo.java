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
package de.fraunhofer.iosb.ilt.sensorthingsmanager.aggregation;

import static de.fraunhofer.iosb.ilt.frostclient.models.CommonProperties.EP_PROPERTIES;
import static de.fraunhofer.iosb.ilt.frostclient.utils.StringHelper.formatKeyValuesForUrl;

import de.fraunhofer.iosb.ilt.frostclient.SensorThingsService;
import de.fraunhofer.iosb.ilt.frostclient.dao.Dao;
import de.fraunhofer.iosb.ilt.frostclient.exception.ServiceFailureException;
import de.fraunhofer.iosb.ilt.frostclient.model.Entity;
import de.fraunhofer.iosb.ilt.frostclient.model.EntitySet;
import de.fraunhofer.iosb.ilt.frostclient.model.EntityType;
import de.fraunhofer.iosb.ilt.frostclient.model.PkValue;
import de.fraunhofer.iosb.ilt.frostclient.model.property.NavigationPropertyEntitySet;
import de.fraunhofer.iosb.ilt.frostclient.models.ext.MapValue;
import de.fraunhofer.iosb.ilt.frostclient.models.ext.TimeInterval;
import de.fraunhofer.iosb.ilt.frostclient.models.ext.TimeValue;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.time4j.Moment;
import net.time4j.ZonalDateTime;
import net.time4j.tz.TZID;
import net.time4j.tz.Timezone;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scf
 */
public class AggregateCombo implements Comparable<AggregateCombo> {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregateCombo.class);

    private final SensorThingsService service;
    public final Entity targetThing;
    public final Entity targetMds;
    public Entity sourceDs;
    public Entity sourceMds;
    public boolean sourceIsAggregate;
    /**
     * Indicates that observations in the source contain a list of values.
     */
    public boolean sourceIsCollection = false;
    public AggregationLevel level;
    public String baseName;
    private TZID zoneId;
    private TimeInterval currentInterval;

    public AggregateCombo(SensorThingsService service, Entity targetThing, Entity target) {
        this.service = service;
        this.targetThing = targetThing;
        this.targetMds = target;
    }

    public boolean hasSource() {
        return sourceDs != null || sourceMds != null;
    }

    public EntityType getSourceType() {
        if (sourceDs != null) {
            return sourceDs.getEntityType();
        }
        if (sourceMds != null) {
            return sourceMds.getEntityType();
        }
        return null;
    }

    public PkValue getSourceId() {
        if (sourceDs != null) {
            return sourceDs.getPrimaryKeyValues();
        }
        if (sourceMds != null) {
            return sourceMds.getPrimaryKeyValues();
        }
        return null;
    }

    public Dao getObsDaoForSource() {
        final EntityType sourceType = getSourceType();
        if (sourceType == null) {
            return null;
        }
        NavigationPropertyEntitySet npes = sourceType.getNavigationPropertySet("Observations");
        if (sourceDs != null) {
            return sourceDs.dao(npes);
        }
        if (sourceMds != null) {
            return sourceMds.dao(npes);
        }
        return null;
    }

    public Entity getLastForTarget() {
        try {
            NavigationPropertyEntitySet npObs = targetMds.getEntityType().getNavigationPropertySet("Observations");
            return targetMds.query(npObs).select("id", "phenomenonTime").orderBy("phenomenonTime desc").first();
        } catch (ServiceFailureException ex) {
            LOGGER.error("Error fetching last observation.", ex);
            return null;
        }
    }

    public Entity getFirstForSource() {
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

    public Entity getLastForSource() {
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

    public List<Entity> getObservationsForSource(Instant start, Instant end) {
        List<Entity> result = new ArrayList<>();
        if (hasSource()) {
            try {
                StringBuilder filter = new StringBuilder();
                filter.append("overlaps(phenomenonTime,").append(start.toString()).append("/").append(end.toString()).append(")");
                EntitySet entityList = getObsDaoForSource().query().filter(filter.toString()).orderBy("phenomenonTime asc").top(1000).list();
                for (Entity entity : entityList) {
                    result.add(entity);
                }
            } catch (ServiceFailureException ex) {
                LOGGER.error("Failed to fetch observations.", ex);
            }
        }
        return result;
    }

    public void resolveZoneId(TZID dflt) {
        if (zoneId == null) {
            MapValue properties = targetThing.getProperty(EP_PROPERTIES);
            Object zoneName = properties.get("timeZone");
            if (zoneName == null || zoneName.toString().isEmpty()) {
                zoneId = dflt;
            } else {
                try {
                    zoneId = Timezone.normalize(zoneName.toString());
                } catch (DateTimeException ex) {
                    LOGGER.warn("Invalid zone: " + zoneName, ex);
                    zoneId = dflt;
                }
            }
        }
    }

    public TZID getZoneId() {
        return zoneId;
    }

    public String getSourceObsMqttPath() {
        if (sourceDs != null) {
            return "v1.0/Datastreams(" + formatKeyValuesForUrl(sourceDs) + ")/Observations?$select=id,phenomenonTime";
        }
        if (sourceMds != null) {
            return "v1.0/MultiDatastreams(" + formatKeyValuesForUrl(sourceMds) + ")/Observations?$select=id,phenomenonTime";
        }
        return "";
    }

    public List<TimeInterval> calculateIntervalsForTime(TimeValue phenTime) {
        List<TimeInterval> retval = new ArrayList<>();
        Moment phenTimeStart = Utils.getPhenTimeStart(phenTime);
        Moment phenTimeEnd = Utils.getPhenTimeEnd(phenTime);
        ZonalDateTime atZone = phenTimeStart.inZonalView(getZoneId());
        Moment intStart = ZonalDateTime.from(level.toIntervalStart(atZone.toTemporalAccessor())).toMoment();
        Moment intEnd = Moment.from(intStart.toTemporalAccessor().plus(level.amount, level.unit));
        retval.add(TimeInterval.create(intStart, intEnd));
        while (intEnd.isBefore(phenTimeEnd)) {
            intStart = intEnd;
            intEnd = Moment.from(intStart.toTemporalAccessor().plus(level.amount, level.unit));
            retval.add(TimeInterval.create(intStart, intEnd));
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
    public TimeInterval replaceIfNotCurrent(TimeInterval other) {
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
            TimeInterval old = currentInterval;
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
    public TimeInterval unsetCurrent(TimeInterval other) {
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
            TimeInterval old = currentInterval;
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
        return Objects.equals(targetMds.getPrimaryKeyValues(), otherCombo.targetMds.getPrimaryKeyValues());
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetMds, level, baseName);
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
            return baseName + " " + level + ". (d " + sourceDs.getPrimaryKeyValues() + " -> md " + targetMds.getPrimaryKeyValues() + ")";
        }
        if (sourceMds != null) {
            return baseName + " " + level + ". (md " + sourceMds.getPrimaryKeyValues() + " -> md " + targetMds.getPrimaryKeyValues() + ")";
        }
        return baseName + " " + level + ". (? -> md " + targetMds.getPrimaryKeyValues() + ")";
    }

    public String getBaseName() {
        return baseName;
    }

}
