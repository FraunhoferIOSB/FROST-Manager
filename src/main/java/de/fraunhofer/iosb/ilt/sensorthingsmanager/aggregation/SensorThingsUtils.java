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
import de.fraunhofer.iosb.ilt.sta.Utils;
import de.fraunhofer.iosb.ilt.sta.model.MultiDatastream;
import de.fraunhofer.iosb.ilt.sta.model.ObservedProperty;
import de.fraunhofer.iosb.ilt.sta.model.Sensor;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;
import de.fraunhofer.iosb.ilt.sta.model.ext.UnitOfMeasurement;
import de.fraunhofer.iosb.ilt.sta.query.Query;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public final Map<ObservedProperty, List<ObservedProperty>> aggregateProperties = new HashMap<>();

    public SensorThingsUtils() {
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
        if (aggregates) {
            findOrCreateAggregateOps(service, op);
        }
        return op;
    }

    public void findOrCreateAggregateOps(SensorThingsService service, ObservedProperty op) throws ServiceFailureException {
        List<ObservedProperty> agList = aggregateProperties.get(op);
        if (agList != null && agList.size() == 3) {
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
        return mds;
    }

}
