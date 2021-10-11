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

import static de.fraunhofer.iosb.ilt.sensorthingsmanager.aggregation.Utils.KEY_AGGREGATE_SOURCE_D;
import static de.fraunhofer.iosb.ilt.sensorthingsmanager.aggregation.Utils.KEY_AGGREGATE_SOURCE_MD;
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.MultiDatastream;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;
import de.fraunhofer.iosb.ilt.sta.query.Query;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
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
    private ZoneId zoneId;
    private final boolean fixReferences;
    private final boolean addEmptyBases;
    private final boolean sourceEqualsTarget = true;
    private double progressBase = 0;
    private double progressTarget = 1;
    private final List<ProgressListener> progressListeners = new CopyOnWriteArrayList<>();

    public AggregationData(SensorThingsService service, boolean fixReferences, boolean addEmptyBases) {
        this.service = service;
        this.fixReferences = fixReferences;
        this.addEmptyBases = addEmptyBases;
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
            Query<Datastream> query = service.datastreams()
                    .query()
                    .select("id", "name", "description", "properties", "unitOfMeasurement")
                    .top(1000)
                    .orderBy("id asc");
            if (hasListeners()) {
                query.count();
            }
            EntityList<Datastream> dsList = query.list();
            long count = dsList.getCount();
            double pPart = (progressTarget - progressBase) / count;
            int nr = 0;
            Iterator<Datastream> datastreams = dsList.fullIterator();
            while (datastreams.hasNext()) {
                Datastream datastream = datastreams.next();
                String name = datastream.getName();
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
            Query<Thing> query = service.things()
                    .query()
                    .top(1000)
                    .select("id,name,properties")
                    .orderBy("id asc")
                    .expand("MultiDatastreams($top=1000;$orderby=id asc;$select=id,name,properties)");
            if (hasListeners()) {
                query.count();
            }
            EntityList<Thing> thingList = query.list();
            long count = thingList.getCount();
            double pPart = (progressTarget - progressBase) / count;
            int nr = 0;
            Iterator<Thing> thingIt = thingList.fullIterator();
            while (thingIt.hasNext()) {
                Thing thing = thingIt.next();
                EntityList<MultiDatastream> dsList = thing.getMultiDatastreams();
                for (Iterator<MultiDatastream> it = dsList.fullIterator(); it.hasNext();) {
                    MultiDatastream mds = it.next();
                    String name = mds.getName();
                    Matcher matcher = Utils.POSTFIX_PATTERN.matcher(name);
                    if (!matcher.matches()) {
                        LOGGER.debug("MultiDatastream {} is not an aggregate.");
                        continue;
                    }
                    AggregateCombo combo = new AggregateCombo(thing, mds);
                    combo.baseName = matcher.group(1).trim();
                    String postfix = matcher.group(2);
                    combo.level = AggregationLevel.of(postfix);
                    if (combo.level == null) {
                        LOGGER.debug("Not a postfix: {}.", postfix);
                        continue;
                    }
                    combo.resolveZoneId(zoneId);
                    LOGGER.debug("Found: {} from {}, timeZone {}", combo.level, combo.target.getName(), combo.getZoneId());
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
                checkReference(target.sourceDs, target.target, target.level);
                return;
            }

            String nameQuoted = "'" + target.baseName.replaceAll("'", "''") + "'";
            {
                List<MultiDatastream> list = service.multiDatastreams()
                        .query()
                        .filter("name eq " + nameQuoted)
                        .top(1000)
                        .orderBy("id asc")
                        .list()
                        .toList();
                if (list.size() > 1) {
                    LOGGER.warn("Multiple ({}) sources found for '{}'.", list.size(), target.baseName);
                }
                if (list.size() > 0) {
                    target.sourceMds = list.get(0);
                    target.sourceIsAggregate = false;
                    checkReference(target.sourceMds, target.target, target.level);
                    return;
                }
            }
            {
                List<Datastream> list = service.datastreams()
                        .query()
                        .filter("name eq " + nameQuoted)
                        .top(1000)
                        .orderBy("id asc")
                        .list()
                        .toList();
                if (list.isEmpty()) {
                    list = service.datastreams()
                            .query()
                            .filter("startswith(name," + nameQuoted + ")")
                            .top(1000)
                            .orderBy("id asc")
                            .list()
                            .toList();
                }
                if (list.size() > 1) {
                    LOGGER.warn("Multiple ({}) sources found for '{}'.", list.size(), target.baseName);
                }
                for (Datastream source : list) {
                    String postfix = source.getName().substring(target.baseName.length());
                    if (!AggregationLevel.isPostfix(postfix)) {
                        continue;
                    }
                    target.sourceDs = source;
                    target.sourceIsAggregate = false;
                    target.sourceIsCollection = true;
                    checkReference(target.sourceDs, target.target, target.level);
                    return;
                }
            }
            LOGGER.warn("No source found for '{}'.", target.baseName);
        } catch (ServiceFailureException ex) {
            LOGGER.error("Failed to find source for {}." + target.baseName);
            LOGGER.debug("Exception:", ex);
        }
    }

    private void findSourceDatastreams(AggregationBase base) {
        Set<AggregateCombo> comboSet = base.getCombos();
        AggregateCombo[] targets = comboSet.toArray(new AggregateCombo[comboSet.size()]);
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
                    target.sourceMds = test.target;
                    target.sourceIsAggregate = true;
                    found = true;
                    checkReference(target.sourceMds, target.target, target.level);
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

    private void checkReference(Datastream source, MultiDatastream aggregate, AggregationLevel level) {
        String expectedAggFor;
        String aggKey = null;
        Object aggId = null;
        if (sourceEqualsTarget) {
            expectedAggFor = "/Datastreams(" + source.getId().getUrl() + ")";
            aggKey = KEY_AGGREGATE_SOURCE_D;
            aggId = source.getId().getValue();
        } else {
            expectedAggFor = source.getSelfLink().toString();
        }
        checkReference(aggregate, expectedAggFor, level, aggKey, aggId);
    }

    private void checkReference(MultiDatastream source, MultiDatastream aggregate, AggregationLevel level) {
        String expectedAggFor;
        String aggKey = null;
        Object aggId = null;
        if (sourceEqualsTarget) {
            expectedAggFor = "/MultiDatastreams(" + source.getId().getUrl() + ")";
            aggKey = KEY_AGGREGATE_SOURCE_MD;
            aggId = source.getId().getValue();
        } else {
            expectedAggFor = source.getSelfLink().toString();
        }
        checkReference(aggregate, expectedAggFor, level, aggKey, aggId);
    }

    private boolean checkProperty(Map<String, Object> properties, String property, Object value) {
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

    private void checkReference(MultiDatastream aggregate, String expectedAggFor, AggregationLevel level, String aggSourceKey, Object aggSourceId) {
        Map<String, Object> properties = aggregate.getProperties();
        if (properties == null) {
            properties = new HashMap<>();
            aggregate.setProperties(properties);
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
                LOGGER.info("Setting source reference for {} to {}.", aggregate.getName(), expectedAggFor);
                properties.put(Utils.KEY_AGGREGATE_FOR, expectedAggFor);
                changed = true;
            } else {
                LOGGER.info("Source reference for {} not correct. Should be {}.", aggregate.getName(), expectedAggFor);
            }
        }
        if (changed && fixReferences) {
            try {
                MultiDatastream copy = aggregate.withOnlyId();
                copy.setProperties(aggregate.getProperties());
                copy.setMultiObservationDataTypes(null);
                copy.setUnitOfMeasurements(null);
                service.update(copy);
            } catch (ServiceFailureException ex) {
                LOGGER.error("Failed to update reference.", ex);
            }
        }
    }

    public void setZoneId(ZoneId zoneId) {
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
