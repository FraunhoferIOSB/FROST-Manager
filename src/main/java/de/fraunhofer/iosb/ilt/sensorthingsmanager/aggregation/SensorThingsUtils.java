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
import de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsMultiDatastreamV11;
import de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsSensingV11;
import static de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsSensingV11.EP_DEFINITION;
import static de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsSensingV11.EP_DESCRIPTION;
import static de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsSensingV11.EP_NAME;
import static de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsSensingV11.EP_PROPERTIES;
import de.fraunhofer.iosb.ilt.frostclient.models.ext.MapValue;
import de.fraunhofer.iosb.ilt.frostclient.models.ext.UnitOfMeasurement;
import de.fraunhofer.iosb.ilt.frostclient.query.Query;
import de.fraunhofer.iosb.ilt.frostclient.utils.StringHelper;
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

    public final Map<Entity, List<Entity>> aggregateProperties = new HashMap<>();

    public SensorThingsUtils() {
    }

    public Entity findOrCreateOp(SensorThingsService service, String name, String def, String description, MapValue properties, String filter, boolean aggregates) throws ServiceFailureException {
        SensorThingsSensingV11 sMdl = service.getModel(SensorThingsSensingV11.class);
        SensorThingsMultiDatastreamV11 mMdl = service.getModel(SensorThingsMultiDatastreamV11.class);
        Query query = service.query(sMdl.etObservedProperty);
        if (StringHelper.isNullOrEmpty(filter)) {
            query.filter("name eq " + StringHelper.quoteForUrl(name) + "");
        } else {
            query.filter(filter);
        }
        EntitySet opList = query.list();
        if (opList.size() > 1) {
            throw new IllegalStateException("More than one observedProperty with name " + name);
        }
        Entity op;
        if (opList.size() == 1) {
            op = opList.iterator().next();
        } else {
            LOGGER.info("Creating ObservedProperty {}.", name);
            op = sMdl.newObservedProperty(name, def, description)
                    .setProperty(EP_PROPERTIES, properties);
            service.create(op);
        }
        if (aggregates) {
            findOrCreateAggregateOps(service, op);
        }
        return op;
    }

    public void findOrCreateAggregateOps(SensorThingsService service, Entity op) throws ServiceFailureException {
        List<Entity> agList = aggregateProperties.get(op);
        if (agList != null && agList.size() == 3) {
            return;
        }
        agList = new ArrayList<>();
        aggregateProperties.put(op, agList);

        String opName = op.getProperty(EP_NAME);
        String opDef = op.getProperty(EP_DEFINITION);
        String opDesc = op.getProperty(EP_DESCRIPTION);
        String def = op.getProperty(EP_DEFINITION);
        {
            String agOpName = opName + " Min";
            String agOpDesc = opDesc + " Minimum";
            Entity agOp = findOrCreateOp(service, agOpName, def, agOpDesc, null, "", false);
            agList.add(agOp);
        }
        {
            String agOpName = opName + " Max";
            String agOpDesc = opDesc + " Maximum";
            Entity agOp = findOrCreateOp(service, agOpName, def, agOpDesc, null, "", false);
            agList.add(agOp);
        }
        {
            String agOpName = opName + " Dev";
            String agOpDesc = opDesc + " Standard deviation";
            Entity agOp = findOrCreateOp(service, agOpName, def, agOpDesc, null, "", false);
            agList.add(agOp);
        }
    }

    public Entity findOrCreateMultiDatastream(SensorThingsService service, String name, String desc, List<UnitOfMeasurement> uoms, Entity thing, List<Entity> ops, Entity sensor, MapValue props) throws ServiceFailureException {
        SensorThingsMultiDatastreamV11 mMdl = service.getModel(SensorThingsMultiDatastreamV11.class);

        EntitySet mdsList = service.query(mMdl.etMultiDatastream)
                .filter("name eq " + StringHelper.quoteForUrl(name) + "")
                .list();
        if (mdsList.size() > 1) {
            throw new IllegalStateException("More than one multidatastream with name " + name);
        }
        Entity mds;
        if (mdsList.size() == 1) {
            mds = mdsList.iterator().next();
        } else {
            LOGGER.info("Creating multiDatastream {}.", name);
            mds = mMdl.newMultiDatastream(name, desc, uoms)
                    .setProperty(EP_PROPERTIES, props)
                    .setProperty(mMdl.npMultidatastreamThing, thing)
                    .setProperty(mMdl.npMultidatastreamSensor, sensor)
                    .addNavigationEntity(mMdl.npMultidatastreamObservedproperties, ops);
            service.create(mds);
        }
        return mds;
    }

}
