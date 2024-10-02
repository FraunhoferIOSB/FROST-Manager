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

import de.fraunhofer.iosb.ilt.frostclient.SensorThingsService;
import de.fraunhofer.iosb.ilt.frostclient.exception.ServiceFailureException;
import de.fraunhofer.iosb.ilt.frostclient.model.Entity;
import de.fraunhofer.iosb.ilt.frostclient.model.EntitySet;
import de.fraunhofer.iosb.ilt.frostclient.model.PkValue;
import static de.fraunhofer.iosb.ilt.frostclient.models.CommonProperties.EP_NAME;
import static de.fraunhofer.iosb.ilt.frostclient.models.CommonProperties.EP_PROPERTIES;
import de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsV11MultiDatastream;
import static de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsV11MultiDatastream.EP_MULTIOBSERVATIONDATATYPES;
import static de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsV11MultiDatastream.EP_UNITOFMEASUREMENTS;
import de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsV11Sensing;
import de.fraunhofer.iosb.ilt.frostclient.models.ext.MapValue;
import de.fraunhofer.iosb.ilt.frostclient.query.Query;
import static de.fraunhofer.iosb.ilt.frostclient.utils.StringHelper.formatKeyValuesForUrl;
import static de.fraunhofer.iosb.ilt.sensorthingsmanager.aggregation.Utils.KEY_AGGREGATE_SOURCE_D;
import static de.fraunhofer.iosb.ilt.sensorthingsmanager.aggregation.Utils.KEY_AGGREGATE_SOURCE_MD;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import net.time4j.tz.TZID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scf
 */
public class AggregationData {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregationData.class);

    public static interface ProgressListener {

        public void setProgress(double progress);
    }
    private final SensorThingsService service;
    private final List<AggregationBase> aggregationBases = new ArrayList<>();
    private final Map<String, AggregationBase> aggregationBasesByName = new HashMap<>();
    private Map<String, List<AggregateCombo>> combosBySource;
    private TZID zoneId;
    private final boolean fixReferences;
    private final boolean addEmptyBases;
    private final boolean sourceEqualsTarget = true;
    private double progressBase = 0;
    private double progressTarget = 1;
    private final List<ProgressListener> progressListeners = new CopyOnWriteArrayList<>();
    private final SensorThingsV11Sensing sMdl;
    private final SensorThingsV11MultiDatastream mMdl;

    public AggregationData(SensorThingsService service, boolean fixReferences, boolean addEmptyBases) {
        this.service = service;
        this.fixReferences = fixReferences;
        this.addEmptyBases = addEmptyBases;
        sMdl = service.getModel(SensorThingsV11Sensing.class);
        mMdl = service.getModel(SensorThingsV11MultiDatastream.class);
    }

    public AggregationData(SensorThingsService service, boolean fixReferences) {
        this(service, fixReferences, true);
    }

    private AggregationBase getAggregationBase(String baseName) {
        AggregationBase aggBase = aggregationBasesByName.computeIfAbsent(
                baseName,
                name -> {
                    AggregationBase base = new AggregationBase(name);
                    aggregationBases.add(base);
                    return base;
                }
        );
        return aggBase;
    }

    private void findAllBases() {
        try {
            Query query = service.query(sMdl.etDatastream)
                    .select("id", "name", "description", "properties", "unitOfMeasurement")
                    .top(1000)
                    .orderBy("id asc");
            if (hasListeners()) {
                query.count();
            }
            EntitySet dsList = query.list();
            long count = dsList.getCount();
            double pPart = (progressTarget - progressBase) / count;
            int nr = 0;
            for (Entity datastream : dsList) {
                String name = datastream.getProperty(EP_NAME);
                String base = baseNameFromName(name);
                AggregationBase aggregationBase = getAggregationBase(base);
                aggregationBase.setBaseDatastream(datastream);
                nr++;
                setProgress(progressBase + nr * pPart);
            }
            LOGGER.info("Loaded {} Datastreams", nr);
        } catch (ServiceFailureException exc) {
            LOGGER.error("Service error loading Datastreams: ", exc);
        }
    }

    private void findTargetMultiDatastreams() {
        try {
            Query query = service.query(sMdl.etThing)
                    .top(1000)
                    .select("id,name,properties")
                    .orderBy("id asc")
                    .expand("MultiDatastreams($top=1000;$orderby=id asc;$select=id,name,properties)");
            if (hasListeners()) {
                query.count();
            }
            EntitySet thingList = query.list();
            long count = thingList.getCount();
            double pPart = (progressTarget - progressBase) / count;
            int nr = 0;
            for (Entity thing : thingList) {
                EntitySet dsList = thing.getProperty(mMdl.npThingMultidatastreams);
                for (Entity mds : dsList) {
                    String name = mds.getProperty(EP_NAME);
                    Matcher matcher = Utils.POSTFIX_PATTERN.matcher(name);
                    if (!matcher.matches()) {
                        LOGGER.debug("MultiDatastream {} is not an aggregate.");
                        continue;
                    }
                    AggregateCombo combo = new AggregateCombo(service, thing, mds);
                    combo.baseName = matcher.group(1).trim();
                    String postfix = matcher.group(2);
                    combo.level = AggregationLevel.of(postfix);
                    if (combo.level == null) {
                        LOGGER.debug("Not a postfix: {}.", postfix);
                        continue;
                    }
                    combo.resolveZoneId(zoneId);
                    LOGGER.debug("Found: {} from {}, timeZone {}", combo.level, combo.targetMds.getProperty(EP_NAME), combo.getZoneId());
                    AggregationBase aggBase = getAggregationBase(combo.baseName);
                    aggBase.addCombo(combo);
                }
                nr++;
                setProgress(progressBase + nr * pPart);
            }
        } catch (ServiceFailureException exc) {
            LOGGER.error("Service error: ", exc);
        }
    }

    private void findSourceDatastreams(AggregateCombo target) {
        try {
            AggregationBase base = aggregationBasesByName.get(target.baseName);
            if (base != null && base.getBaseDatastream() != null) {
                target.sourceDs = base.getBaseDatastream();
                target.sourceIsAggregate = false;
                checkReferenceFromDs(target.sourceDs, target.targetMds, target.level);
                return;
            }
            {
                MapValue properties = target.targetMds.getProperty(EP_PROPERTIES);
                PkValue sourceDsId = PkValue.of(properties.get(KEY_AGGREGATE_SOURCE_D));
                PkValue sourceMdsId = PkValue.of(properties.get(KEY_AGGREGATE_SOURCE_MD));
                if (sourceDsId != null) {
                    Entity sourceDs = null;
                    try {
                        sourceDs = service.dao(sMdl.etDatastream).find(sourceDsId);
                    } catch (ServiceFailureException ex) {
                        LOGGER.info("Invalid reference to Datastreams {} from {}", sourceDsId, target.targetMds);
                    }
                    if (sourceDs != null) {
                        target.sourceDs = sourceDs;
                        target.baseName = target.sourceDs.getProperty(EP_NAME);
                        String expectedName = target.baseName + " " + target.level.toPostFix();
                        if (!expectedName.equals(target.targetMds.getProperty(EP_NAME))) {
                            LOGGER.info("Updating name of MultiDatastreams({}) to {}", formatKeyValuesForUrl(target.targetMds), expectedName);
                            target.targetMds.setProperty(EP_NAME, expectedName);
                            try {
                                service.update(target.targetMds);
                            } catch (ServiceFailureException ex) {
                                LOGGER.error("Failed to update {}", target.targetMds, ex);
                            }
                            target.sourceIsAggregate = false;
                        }
                        return;
                    }
                }
            }
            String nameQuoted = "'" + target.baseName.replaceAll("'", "''") + "'";
            {
                List<Entity> list = null;
                try {
                    list = service.query(mMdl.etMultiDatastream)
                            .filter("name eq " + nameQuoted)
                            .top(1000)
                            .orderBy("id asc")
                            .list()
                            .toList();
                } catch (ServiceFailureException ex) {
                    LOGGER.error("Failed to fetch MultiDatastreams", ex);
                }
                if (list != null) {
                    if (list.size() > 1) {
                        LOGGER.warn("Multiple ({}) sources found for '{}'.", list.size(), target.baseName);
                    }
                    if (!list.isEmpty()) {
                        target.sourceMds = list.get(0);
                        target.sourceIsAggregate = false;
                        checkReferenceFromDs(target.sourceMds, target.targetMds, target.level);
                        return;
                    }
                }
            }
            {
                List<Entity> list = null;
                try {
                    list = service.query(sMdl.etDatastream)
                            .filter("name eq " + nameQuoted)
                            .top(1000)
                            .orderBy("id asc")
                            .list()
                            .toList();
                    if (list.isEmpty()) {
                        list = service.query(sMdl.etDatastream)
                                .filter("startswith(name," + nameQuoted + ")")
                                .top(1000)
                                .orderBy("id asc")
                                .list()
                                .toList();
                    }
                } catch (ServiceFailureException ex) {
                    LOGGER.error("Failed to fetch Datastreams", ex);
                }
                if (list != null) {
                    if (list.size() > 1) {
                        LOGGER.warn("Multiple ({}) sources found for '{}'.", list.size(), target.baseName);
                    }
                    for (Entity sourceDs : list) {
                        String postfix = sourceDs.getProperty(EP_NAME).substring(target.baseName.length());
                        if (!AggregationLevel.isPostfix(postfix)) {
                            continue;
                        }
                        target.sourceDs = sourceDs;
                        target.sourceIsAggregate = false;
                        target.sourceIsCollection = true;
                        checkReferenceFromDs(target.sourceDs, target.targetMds, target.level);
                        return;
                    }
                }
            }
            LOGGER.warn("No source found for '{}', {}.", target.baseName, target.targetMds);
        } catch (RuntimeException ex) {
            LOGGER.error("Failed to find source for {} MultiDatastreams({}).", target.baseName, formatKeyValuesForUrl(target.targetMds));
            LOGGER.debug("Exception:", ex);
        }
    }

    private void findSourceDatastreams(AggregationBase base) {
        Set<AggregateCombo> comboSet = base.getCombos();
        AggregateCombo[] targets = comboSet.toArray(AggregateCombo[]::new);
        int i = 0;
        for (AggregateCombo target : targets) {
            int idx = i;
            boolean found = false;
            while (!found && idx > 0) {
                // check the other combos
                idx--;
                AggregateCombo test = targets[idx];
                long smaller = test.level.duration.getSeconds();
                long larger = target.level.duration.getSeconds();
                if (larger % smaller == 0) {
                    LOGGER.debug("{}: {} ~ {} ({})", target.baseName, target.level, test.level, (larger / smaller));
                    target.sourceMds = test.targetMds;
                    target.sourceIsAggregate = true;
                    found = true;
                    checkReferenceFromDs(target.sourceMds, target.targetMds, target.level);
                }
            }
            if (!found) {
                // No other combo is valid.
                findSourceDatastreams(target);
                if (target.sourceDs != null) {
                    base.setBaseDatastream(target.sourceDs);
                    base.setBaseMultiDatastream(null);
                } else if (base.getBaseDatastream() == null) {
                    base.setBaseMultiDatastream(target.sourceMds);
                }
            }
            i++;
            LOGGER.debug("Found source for: {}.", target);
        }
    }

    private void findSourceDatastreams(List<AggregationBase> bases) {
        long count = bases.size();
        double pPart = (progressTarget - progressBase) / count;
        int nr = 0;
        for (AggregationBase base : bases) {
            findSourceDatastreams(base);
            nr++;
            setProgress(progressBase + nr * pPart);
        }
    }

    private String baseNameFromName(String name) {
        Matcher matcher = Utils.POSTFIX_PATTERN.matcher(name);
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return name;
    }

    private void gatherData() {
        setProgress(0, 0.1);
        if (addEmptyBases) {
            findAllBases();
            LOGGER.info("Found {} base names", aggregationBases.size());
        }
        moveProgress(0.3);

        // Find target multidatastreams
        findTargetMultiDatastreams();
        LOGGER.info("Found {} comboSets", aggregationBasesByName.size());
        moveProgress(0.9);

        // Find source datastreams matching the targets
        findSourceDatastreams(aggregationBases);
        moveProgress(1);

        combosBySource = new HashMap<>();
        long count = aggregationBasesByName.size();
        double pPart = (progressTarget - progressBase) / count;
        int nr = 0;
        for (AggregationBase base : aggregationBasesByName.values()) {
            for (AggregateCombo combo : base.getCombos()) {
                String path = combo.getSourceObsMqttPath();
                if (path.isEmpty()) {
                    continue;
                }
                List<AggregateCombo> bySource = combosBySource.get(path);
                if (bySource == null) {
                    bySource = new ArrayList<>();
                    combosBySource.put(path, bySource);
                }
                bySource.add(combo);
            }
            nr++;
            setProgress(progressBase + nr * pPart);
        }
        setProgress(1);
        LOGGER.info("Found {} unique source datastreams", combosBySource.size());
    }

    public List<AggregationBase> getAggregationBases() {
        if (aggregationBases.isEmpty()) {
            gatherData();
        }
        return aggregationBases;
    }

    public Map<String, AggregationBase> getCombosByBase() {
        if (aggregationBasesByName.isEmpty()) {
            gatherData();
        }
        return aggregationBasesByName;
    }

    public Map<String, List<AggregateCombo>> getComboBySource() {
        return getComboBySource(false);
    }

    public Map<String, List<AggregateCombo>> getComboBySource(boolean recalculate) {
        if (combosBySource == null || recalculate) {
            gatherData();
        }
        return combosBySource;
    }

    private void checkReferenceFromDs(Entity sourceDs, Entity aggregateMds, AggregationLevel level) {
        String expectedAggFor;
        String aggKey = null;
        Object aggId = null;
        if (sourceEqualsTarget) {
            final PkValue primaryKeyValues = sourceDs.getPrimaryKeyValues();
            expectedAggFor = "/Datastreams(" + formatKeyValuesForUrl(sourceDs) + ")";
            aggKey = KEY_AGGREGATE_SOURCE_D;
            if (primaryKeyValues.size() == 1) {
                aggId = primaryKeyValues.get(0);
            } else {
                aggId = formatKeyValuesForUrl(sourceDs);
            }
        } else {
            expectedAggFor = sourceDs.getSelfLink();
        }
        checkReference(aggregateMds, expectedAggFor, level, aggKey, aggId);
    }

    private void checkReferenceFromMds(Entity sourceMds, Entity aggregateMds, AggregationLevel level) {
        String expectedAggFor;
        String aggKey = null;
        Object aggId = null;
        if (sourceEqualsTarget) {
            expectedAggFor = "/MultiDatastreams(" + formatKeyValuesForUrl(sourceMds) + ")";
            aggKey = KEY_AGGREGATE_SOURCE_MD;
            aggId = formatKeyValuesForUrl(sourceMds);
        } else {
            expectedAggFor = sourceMds.getSelfLink();
        }
        checkReference(aggregateMds, expectedAggFor, level, aggKey, aggId);
    }

    private boolean checkProperty(MapValue properties, String property, Object value) {
        Object checkValue = value;
        boolean changed = false;
        Object oldValue = properties.get(property);
        if (!(value instanceof Number) && !(value instanceof Boolean) && !(value instanceof String) && value != null) {
            checkValue = value.toString();
        }
        if (value instanceof Number && oldValue instanceof Number) {
            checkValue = value.toString();
            oldValue = oldValue.toString();
        }
        if (!checkValue.equals(oldValue)) {
            LOGGER.info("Fixing property {}. Is {}, should be {}.", property, oldValue, value);
            properties.put(property, value);
            changed = true;
        }
        return changed;
    }

    private void checkReference(Entity aggregateMds, String expectedAggFor, AggregationLevel level, String aggSourceKey, Object aggSourceId) {
        MapValue properties = aggregateMds.getProperty(EP_PROPERTIES);
        if (properties == null) {
            properties = new MapValue();
            aggregateMds.setProperty(EP_PROPERTIES, properties);
        }
        boolean changed = false;
        changed = changed | checkProperty(properties, Utils.KEY_AGGREGATE_AMOUNT, level.amount);
        changed = changed | checkProperty(properties, Utils.KEY_AGGREGATE_UNIT, level.unit.toString());
        if (aggSourceKey != null) {
            changed = changed | checkProperty(properties, aggSourceKey, aggSourceId);
        }

        String aggFor = Objects.toString(properties.get(Utils.KEY_AGGREGATE_FOR));
        if (!expectedAggFor.equals(aggFor)) {
            if (fixReferences) {
                LOGGER.info("Setting source reference for {} to {}.", aggregateMds.getProperty(EP_NAME), expectedAggFor);
                properties.put(Utils.KEY_AGGREGATE_FOR, expectedAggFor);
                changed = true;
            } else {
                LOGGER.info("Source reference for {} not correct. Should be {}.", aggregateMds.getProperty(EP_NAME), expectedAggFor);
            }
        }
        if (changed && fixReferences) {
            try {
                Entity copyMds = aggregateMds.withOnlyPk();
                copyMds.setProperty(EP_PROPERTIES, aggregateMds.getProperty(EP_PROPERTIES));
                copyMds.setProperty(EP_MULTIOBSERVATIONDATATYPES, null);
                copyMds.setProperty(EP_UNITOFMEASUREMENTS, null);
                service.update(copyMds);
            } catch (ServiceFailureException ex) {
                LOGGER.error("Failed to update reference.", ex);
            }
        }
    }

    public void setZoneId(TZID zoneId) {
        this.zoneId = zoneId;
    }

    public void moveProgress(double target) {
        progressBase = progressTarget;
        progressTarget = target;
        setProgress(progressBase);
    }

    public void setProgress(double base, double target) {
        progressBase = base;
        progressTarget = target;
        setProgress(base);
    }

    public void setProgress(double p) {
        for (ProgressListener l : progressListeners) {
            l.setProgress(p);
        }
    }

    public boolean hasListeners() {
        return !progressListeners.isEmpty();
    }

    public void addProgressListener(ProgressListener l) {
        progressListeners.add(l);
    }

    public void removeProgressListener(ProgressListener l) {
        progressListeners.remove(l);
    }

}
