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

import de.fraunhofer.iosb.ilt.sensorthingsmanager.aggregation.Utils.AggregationLevels;
import static de.fraunhofer.iosb.ilt.sensorthingsmanager.aggregation.Utils.KEY_AGGREGATE_FOR;
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.Utils;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.Id;
import de.fraunhofer.iosb.ilt.sta.model.Location;
import de.fraunhofer.iosb.ilt.sta.model.MultiDatastream;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.ObservedProperty;
import de.fraunhofer.iosb.ilt.sta.model.Sensor;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;
import de.fraunhofer.iosb.ilt.sta.model.ext.UnitOfMeasurement;
import de.fraunhofer.iosb.ilt.sta.query.Query;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geojson.GeoJsonObject;
import org.geojson.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scf
 */
public class SensorThingsUtils {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SensorThingsUtils.class);

    public SensorThingsUtils() {
    }

    public final List<Thing> things = new ArrayList<>();
    public final List<Location> locations = new ArrayList<>();
    public final List<Sensor> sensors = new ArrayList<>();
    public final List<ObservedProperty> oProps = new ArrayList<>();
    public final Map<ObservedProperty, List<ObservedProperty>> aggregateProperties = new HashMap<>();
    public final List<Datastream> datastreams = new ArrayList<>();
    public final List<MultiDatastream> multiDatastreams = new ArrayList<>();
    public final List<Observation> observations = new ArrayList<>();

    public final Map<String, Integer> obsPropIds = new HashMap<>();

    public static String quoteForUrl(Object in) {
        if (in instanceof Number) {
            return in.toString();
        }
        return "'" + Utils.escapeForStringConstant(in.toString()) + "'";
    }

    public Thing findOrCreateThing(SensorThingsService service, String filter, String name, String description, double lon, double lat, Map<String, Object> properties) throws ServiceFailureException {
        EntityList<Thing> thingList;
        if (Utils.isNullOrEmpty(filter)) {
            filter = "name eq " + quoteForUrl(name);
        }
        thingList = service.things()
                .query()
                .filter(filter)
                .expand("Locations")
                .list();
        if (thingList.size() > 1) {
            throw new IllegalStateException("More than one thing found with filter " + filter);
        }
        Thing thing;
        if (thingList.size() == 1) {
            thing = thingList.iterator().next();
        } else {
            LOGGER.info("Creating Thing {}.", name);
            thing = new Thing(name, description);
            thing.setProperties(properties);
            service.create(thing);

            if (lat != 0 && lon != 0) {
                Location location = new Location(
                        name,
                        "Location of Thing " + name + ".",
                        "application/vnd.geo+json",
                        new Point(lon, lat));
                location.getThings().add(thing);
                service.create(location);
                locations.add(location);
                thing.getLocations().add(location);
            }
        }
        things.add(thing);
        return thing;
    }

    public Sensor findOrCreateSensor(SensorThingsService service, String name, String description, Map<String, Object> properties) throws ServiceFailureException {
        EntityList<Sensor> sensorList = service.sensors().query().filter("name eq '" + name + "'").list();
        if (sensorList.size() > 1) {
            throw new IllegalStateException("More than one sensor with name " + name);
        }
        Sensor sensor;
        if (sensorList.size() == 1) {
            sensor = sensorList.iterator().next();
        } else {
            LOGGER.info("Creating Sensor {}.", name);
            sensor = new Sensor(name, description, "text", "Properties not known");
            sensor.setProperties(properties);
            service.create(sensor);
        }
        sensors.add(sensor);
        return sensor;
    }

    public ObservedProperty findOrCreateOp(SensorThingsService service, String name, String def, String description, Map<String, Object> properties) throws ServiceFailureException {
        return findOrCreateOp(service, name, def, description, properties, "", false);
    }

    public ObservedProperty findOrCreateOp(SensorThingsService service, String name, String def, String description, Map<String, Object> properties, String filter, boolean aggregates) throws ServiceFailureException {
        Query<ObservedProperty> query = service.observedProperties().query();
        if (Utils.isNullOrEmpty(filter)) {
            query.filter("name eq '" + Utils.escapeForStringConstant(name) + "'");
        } else {
            query.filter(filter);
        }
        EntityList<ObservedProperty> opList = query.list();
        if (opList.size() > 1) {
            throw new IllegalStateException("More than one observedProperty with name " + name);
        }
        ObservedProperty op;
        if (opList.size() == 1) {
            op = opList.iterator().next();
        } else {
            LOGGER.info("Creating ObservedProperty {}.", name);
            op = new ObservedProperty();
            op.setName(name);
            op.setDefinition(def);
            op.setDescription(description);
            op.setProperties(properties);
            service.create(op);
        }
        oProps.add(op);
        if (aggregates) {
            findOrCreateAggregateOps(service, op);
        }
        return op;
    }

    public void findOrCreateAggregateOps(SensorThingsService service, ObservedProperty op) throws ServiceFailureException {
        List<ObservedProperty> agList = aggregateProperties.get(op);
        if (agList != null && agList.size() == 4) {
            return;
        }
        agList = new ArrayList<>();
        aggregateProperties.put(op, agList);

        String opName = op.getName();
        String opDef = op.getDefinition();
        String opDesc = op.getDescription();
        String def = op.getDefinition();
        {
            String agOpName = opName + " Min";
            String agOpDesc = opDesc + " Minimum";
            ObservedProperty agOp = findOrCreateOp(service, agOpName, def, agOpDesc, null, "", false);
            agList.add(agOp);
        }
        {
            String agOpName = opName + " Max";
            String agOpDesc = opDesc + " Maximum";
            ObservedProperty agOp = findOrCreateOp(service, agOpName, def, agOpDesc, null, "", false);
            agList.add(agOp);
        }
        {
            String agOpName = opName + " Dev";
            String agOpDesc = opDesc + " Standard deviation";
            ObservedProperty agOp = findOrCreateOp(service, agOpName, def, agOpDesc, null, "", false);
            agList.add(agOp);
        }
    }

    public Datastream findOrCreateDatastream(SensorThingsService service, String dsName, String dsDescription, Map<String, Object> properties, Thing t, ObservedProperty op, UnitOfMeasurement uom, Sensor s, AggregationLevels... aggregates) throws ServiceFailureException, IOException {
        Datastream ds = findOrCreateDatastream(
                service,
                dsName,
                dsDescription,
                properties,
                uom, t, op, s);

        if (aggregates.length > 0) {
            List<ObservedProperty> ops = new ArrayList<>();
            ops.add(op);
            ops.addAll(aggregateProperties.get(op));
            List<UnitOfMeasurement> uoms = new ArrayList<>();
            for (ObservedProperty tempOp : ops) {
                uoms.add(uom);
            }
            Map<String, Object> aggProps = new HashMap<>();
            aggProps.put(KEY_AGGREGATE_FOR, "/Datastreams(" + ds.getId().getUrl() + ")");
            for (AggregationLevels level : aggregates) {
                String mdsName = dsName + " " + level.postfix;
                String mdsDesc = dsDescription + " " + level.description;
                MultiDatastream mds = findOrCreateMultiDatastream(service, mdsName, mdsDesc, uoms, t, ops, s, aggProps);
                aggProps.put(KEY_AGGREGATE_FOR, "/MultiDatastreams(" + mds.getId().getUrl() + ")");
            }
        }
        return ds;
    }

    public Datastream findOrCreateDatastream(SensorThingsService service, String name, String desc, Map<String, Object> properties, UnitOfMeasurement uom, Thing t, ObservedProperty op, Sensor s) throws ServiceFailureException {
        EntityList<Datastream> datastreamList = service.datastreams().query().filter("name eq '" + Utils.escapeForStringConstant(name) + "'").list();
        if (datastreamList.size() > 1) {
            throw new IllegalStateException("More than one datastream with name " + name);
        }
        Datastream ds;
        if (datastreamList.size() == 1) {
            ds = datastreamList.iterator().next();
        } else {
            LOGGER.info("Creating Datastream {}.", name);
            ds = new Datastream(
                    name,
                    desc,
                    "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement",
                    uom);
            ds.setProperties(properties);
            ds.setThing(t);
            ds.setSensor(s);
            ds.setObservedProperty(op);
            service.create(ds);
        }
        datastreams.add(ds);
        return ds;
    }

    public MultiDatastream findOrCreateMultiDatastream(SensorThingsService service, String name, String desc, List<UnitOfMeasurement> uoms, Thing t, List<ObservedProperty> ops, Sensor s, Map<String, Object> props, AggregationLevels... aggregates) throws ServiceFailureException {
        MultiDatastream mds = findOrCreateMultiDatastream(service, name, desc, uoms, t, ops, s, props);
        Id lastId = mds.getId();
        if (aggregates.length > 0) {
            for (AggregationLevels level : aggregates) {
                Map<String, Object> aggProps = new HashMap<>();
                aggProps.put(KEY_AGGREGATE_FOR, "/MultiDatastreams(" + lastId.getUrl() + ")");
                String mdsName = mds.getName() + " " + level.postfix;
                String mdsDesc = mds.getDescription() + " " + level.description;
                lastId = findOrCreateMultiDatastream(service, mdsName, mdsDesc, uoms, t, ops, s, aggProps).getId();
            }
        }
        return mds;
    }

    public MultiDatastream findOrCreateMultiDatastream(SensorThingsService service, String name, String desc, List<UnitOfMeasurement> uoms, Thing t, List<ObservedProperty> ops, Sensor s, Map<String, Object> props) throws ServiceFailureException {
        EntityList<MultiDatastream> mdsList = service.multiDatastreams().query().filter("name eq '" + Utils.escapeForStringConstant(name) + "'").list();
        if (mdsList.size() > 1) {
            throw new IllegalStateException("More than one multidatastream with name " + name);
        }
        MultiDatastream mds;
        if (mdsList.size() == 1) {
            mds = mdsList.iterator().next();
        } else {
            LOGGER.info("Creating multiDatastream {}.", name);
            List<String> dataTypes = new ArrayList<>();
            for (ObservedProperty op : ops) {
                dataTypes.add("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement");
            }
            mds = new MultiDatastream(
                    name,
                    desc,
                    dataTypes,
                    uoms);
            mds.setProperties(props);
            mds.setThing(t);
            mds.setSensor(s);
            mds.getObservedProperties().addAll(ops);
            service.create(mds);
        }
        multiDatastreams.add(mds);
        return mds;
    }

    public Location findOrCreateLocation(SensorThingsService service, String name, String description, GeoJsonObject geoJson) throws ServiceFailureException {
        EntityList<Location> lList = service.locations().query().filter("name eq '" + Utils.escapeForStringConstant(name) + "'").list();
        if (lList.size() > 1) {
            throw new IllegalStateException("More than one Location with name " + name);
        }
        Location location;
        if (lList.size() == 1) {
            location = lList.iterator().next();
        } else {
            LOGGER.info("Creating Location {}.", name);
            location = new Location(
                    name,
                    description,
                    "application/geo+json",
                    geoJson);
            service.create(location);
        }
        locations.add(location);
        return location;
    }
}
