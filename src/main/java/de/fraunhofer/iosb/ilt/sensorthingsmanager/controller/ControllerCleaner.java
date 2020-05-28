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
package de.fraunhofer.iosb.ilt.sensorthingsmanager.controller;

import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.ChangingStatusLogger;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.Utils;
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.FeatureOfInterest;
import de.fraunhofer.iosb.ilt.sta.model.Location;
import de.fraunhofer.iosb.ilt.sta.model.MultiDatastream;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.ObservedProperty;
import de.fraunhofer.iosb.ilt.sta.model.Sensor;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import de.fraunhofer.iosb.ilt.sta.model.TimeObject;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scf
 */
public class ControllerCleaner implements Initializable {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerCleaner.class);

    private static class LogStatusCleaner extends ChangingStatusLogger.ChangingStatusDefault {

        public LogStatusCleaner() {
            super("Cleaning... {} T, {} L, {} S, {} Op, {} Ds, {} MDs, {} F, {} O. To Delete: {} ", 9);
            setAllTo(Long.valueOf(0));
        }

        public void setThings(Long value) {
            setObjectAt(0, value);
        }

        public void setLocations(Long value) {
            setObjectAt(1, value);
        }

        public void setSensors(Long value) {
            setObjectAt(2, value);
        }

        public void setObsProps(Long value) {
            setObjectAt(3, value);
        }

        public void setDatastreams(Long value) {
            setObjectAt(4, value);
        }

        public void setMultiDatastreams(Long value) {
            setObjectAt(5, value);
        }

        public void setFeatures(Long value) {
            setObjectAt(6, value);
        }

        public void setObservations(Long value) {
            setObjectAt(7, value);
        }

        public void setToDelete(Long value) {
            setObjectAt(8, value);
        }
    }

    @FXML
    private CheckBox cleanThings;
    @FXML
    private CheckBox cleanDatastreams;
    @FXML
    private CheckBox cleanMultiDatastreams;
    @FXML
    private CheckBox cleanSensors;
    @FXML
    private CheckBox cleanObservedProperties;
    @FXML
    private CheckBox cleanFeatures;
    @FXML
    private CheckBox cleanLocations;
    @FXML
    private CheckBox cleanDuplicateObsPhentime;
    @FXML
    private CheckBox deleteThings;
    @FXML
    private CheckBox deleteSensors;
    @FXML
    private CheckBox deleteObservedProperties;
    @FXML
    private CheckBox deleteFeatures;
    @FXML
    private CheckBox deleteLocations;
    @FXML
    private CheckBox deleteDatastreams;
    @FXML
    private CheckBox deleteMultiDatastreams;
    @FXML
    private CheckBox deleteObservations;

    private SensorThingsService service;
    private final LogStatusCleaner logStatusCleaner = new LogStatusCleaner();
    private final ChangingStatusLogger statusLogger = new ChangingStatusLogger(LOGGER)
            .addLogStatus(logStatusCleaner)
            .setLogIntervalMs(2000);

    @FXML
    private void actionClean(ActionEvent event) {
        statusLogger.start();
        try {
            if (cleanThings.isSelected()) {
                cleanThings();
            }
            if (cleanSensors.isSelected()) {
                cleanSensors();
            }
            if (cleanObservedProperties.isSelected()) {
                cleanObservedProperties();
            }
            if (cleanFeatures.isSelected()) {
                cleanFeatures();
            }
            if (cleanDatastreams.isSelected()) {
                cleanDatastreams();
            }
            if (cleanMultiDatastreams.isSelected()) {
                cleanMultiDatastreams();
            }
            if (cleanLocations.isSelected()) {
                cleanLocations();
            }
            if (cleanDuplicateObsPhentime.isSelected()) {
                cleanObservationsDuplicatePhentime();
            }
        } catch (ServiceFailureException ex) {
            LOGGER.error("Failed to clean.", ex);
        }
        statusLogger.stop();
    }

    private void cleanThings() throws ServiceFailureException {
        EntityList<Thing> list = service.things()
                .query()
                .select("name", "id")
                .expand("Datastreams($select=id;$top=1),MultiDatastreams($select=id;$top=1)")
                .orderBy("id asc")
                .list();
        List<Thing> toDelete = new ArrayList<>();
        Iterator<Thing> it;
        long count = 0;
        long toDel = 0;
        for (it = list.fullIterator(); it.hasNext();) {
            Thing next = it.next();
            logStatusCleaner.setThings(++count);
            if (next.getDatastreams().isEmpty() && next.getMultiDatastreams().isEmpty()) {
                toDelete.add(next);
                logStatusCleaner.setToDelete(++toDel);
            }
        }
        LOGGER.info("Deleting {} Things", toDelete.size());
        for (Thing item : toDelete) {
            logStatusCleaner.setToDelete(--toDel);
            service.delete(item.withOnlyId());
        }
    }

    private void cleanSensors() throws ServiceFailureException {
        EntityList<Sensor> list = service.sensors()
                .query()
                .select("name", "id")
                .expand("Datastreams($select=id;$top=1),MultiDatastreams($select=id;$top=1)")
                .orderBy("id asc")
                .list();
        List<Sensor> toDelete = new ArrayList<>();
        Iterator<Sensor> it;
        long count = 0;
        long toDel = 0;
        for (it = list.fullIterator(); it.hasNext();) {
            Sensor next = it.next();
            count++;
            logStatusCleaner.setSensors(count);
            if (next.getDatastreams().isEmpty() && next.getMultiDatastreams().isEmpty()) {
                toDelete.add(next);
                logStatusCleaner.setToDelete(++toDel);
            }
        }
        LOGGER.info("Deleting {} Sensors", toDelete.size());
        for (Sensor item : toDelete) {
            logStatusCleaner.setToDelete(--toDel);
            service.delete(item.withOnlyId());
        }
    }

    private void cleanObservedProperties() throws ServiceFailureException {
        EntityList<ObservedProperty> list = service.observedProperties()
                .query()
                .select("name", "id")
                .expand("Datastreams($select=id;$top=1),MultiDatastreams($select=id;$top=1)")
                .orderBy("id asc")
                .list();
        List<ObservedProperty> toDelete = new ArrayList<>();
        Iterator<ObservedProperty> it;
        long count = 0;
        long toDel = 0;
        for (it = list.fullIterator(); it.hasNext();) {
            ObservedProperty next = it.next();
            count++;
            logStatusCleaner.setObsProps(count);
            if (next.getDatastreams().isEmpty() && next.getMultiDatastreams().isEmpty()) {
                toDelete.add(next);
                logStatusCleaner.setToDelete(++toDel);
            }
        }
        LOGGER.info("Deleting {} ObservedProperties", toDelete.size());
        for (ObservedProperty item : toDelete) {
            logStatusCleaner.setToDelete(--toDel);
            service.delete(item.withOnlyId());
        }
    }

    private void cleanDatastreams() throws ServiceFailureException {
        LOGGER.info("Cleaning Datastreams");
        EntityList<Datastream> list = service.datastreams()
                .query()
                .select("name", "id")
                .expand("Observations($select=id;$top=1)")
                .orderBy("id asc")
                .list();
        List<Datastream> toDelete = new ArrayList<>();
        Iterator<Datastream> it;
        long count = 0;
        long toDel = 0;
        for (it = list.fullIterator(); it.hasNext();) {
            Datastream next = it.next();
            count++;
            logStatusCleaner.setDatastreams(count);
            if (next.getObservations().isEmpty()) {
                toDelete.add(next);
                logStatusCleaner.setToDelete(++toDel);
            }
        }
        LOGGER.info("Deleting {} Datastreams", toDelete.size());
        for (Datastream item : toDelete) {
            logStatusCleaner.setToDelete(--toDel);
            service.delete(item.withOnlyId());
        }
    }

    private void cleanMultiDatastreams() throws ServiceFailureException {
        LOGGER.info("Cleaning MultiDatastreams");
        EntityList<MultiDatastream> list = service.multiDatastreams()
                .query()
                .select("name", "id")
                .expand("Observations($select=id;$top=1)")
                .orderBy("id asc")
                .list();
        List<MultiDatastream> toDelete = new ArrayList<>();
        Iterator<MultiDatastream> it;
        long count = 0;
        long toDel = 0;
        for (it = list.fullIterator(); it.hasNext();) {
            MultiDatastream next = it.next();
            count++;
            logStatusCleaner.setMultiDatastreams(count);
            if (next.getObservations().isEmpty()) {
                toDelete.add(next);
                logStatusCleaner.setToDelete(++toDel);
            }
        }
        LOGGER.info("Deleting {} MultiDatastream", toDelete.size());
        for (MultiDatastream item : toDelete) {
            logStatusCleaner.setToDelete(--toDel);
            service.delete(item.withOnlyId());
        }
    }

    private void cleanFeatures() throws ServiceFailureException {
        LOGGER.info("Cleaning Features");
        EntityList<FeatureOfInterest> list = service.featuresOfInterest()
                .query()
                .select("name", "id")
                .expand("Observations($select=id;$top=1)")
                .orderBy("id asc")
                .list();
        long count = 0;
        long toDel = 0;
        List<FeatureOfInterest> toDelete = new ArrayList<>();
        Iterator<FeatureOfInterest> it;
        for (it = list.fullIterator(); it.hasNext();) {
            FeatureOfInterest next = it.next();
            count++;
            logStatusCleaner.setFeatures(count);
            if (next.getObservations().isEmpty()) {
                toDelete.add(next);
                logStatusCleaner.setToDelete(++toDel);
            }
        }
        LOGGER.info("Deleting {} FeaturesOfInterest", toDelete.size());
        for (FeatureOfInterest item : toDelete) {
            logStatusCleaner.setToDelete(--toDel);
            service.delete(item.withOnlyId());
        }
    }

    private void cleanLocations() throws ServiceFailureException {
        EntityList<Location> list = service.locations()
                .query()
                .select("name", "id")
                .expand("Things($select=id;$top=1),HistoricalLocations($select=id;$top=1)")
                .orderBy("id asc")
                .list();
        List<Location> toDelete = new ArrayList<>();
        Iterator<Location> it;
        long count = 0;
        long toDel = 0;
        for (it = list.fullIterator(); it.hasNext();) {
            Location next = it.next();
            count++;
            logStatusCleaner.setLocations(count);
            if (next.getThings().isEmpty() && next.getHistoricalLocations().isEmpty()) {
                toDelete.add(next);
                logStatusCleaner.setToDelete(++toDel);
            }
        }
        LOGGER.info("Deleting {} Locations", toDelete.size());
        for (Location item : toDelete) {
            logStatusCleaner.setToDelete(--toDel);
            service.delete(item.withOnlyId());
        }
    }

    private void cleanObservationsDuplicatePhentime() throws ServiceFailureException {
        EntityList<Datastream> list = service.datastreams()
                .query()
                .select("name", "id")
                .orderBy("id asc")
                .expand("Observations($select=id;$top=1)")
                .filter("id ge 3699")
                .list();
        Iterator<Datastream> it;
        long count = 0;
        for (it = list.fullIterator(); it.hasNext();) {
            Datastream next = it.next();
            count++;
            logStatusCleaner.setDatastreams(count);
            cleanObsForDatastream(next);
        }
    }

    private void cleanObsForDatastream(Datastream ds) throws ServiceFailureException {
        List<Observation> toDelete = new ArrayList<>();
        EntityList<Observation> list = ds.observations().query()
                .orderBy("phenomenonTime asc,id asc")
                .select("id,phenomenonTime")
                .top(100000)
                .list();
        Iterator<Observation> it = list.fullIterator();
        long count = 0;
        long toDel = 0;
        TimeObject last = null;
        while (it.hasNext()) {
            Observation next = it.next();
            count++;
            logStatusCleaner.setObservations(count);
            TimeObject cur = next.getPhenomenonTime();
            if (last == null) {
                last = cur;
            } else {
                if (last.equals(cur)) {
                    toDelete.add(next);
                    logStatusCleaner.setToDelete(++toDel);
                }
                last = cur;
            }
        }
        if (toDelete.isEmpty()) {
            return;
        }
        LOGGER.info("Deleting {} obs for Datastream {}", toDelete.size(), ds);
        for (Observation obs : toDelete) {
            logStatusCleaner.setToDelete(--toDel);
            service.delete(obs);
        }
    }

    @FXML
    private void actionDelete(ActionEvent event) {
        try {
            if (deleteThings.isSelected()) {
                Utils.deleteAll(service.things());
            }
            if (deleteSensors.isSelected()) {
                Utils.deleteAll(service.sensors());
            }
            if (deleteObservedProperties.isSelected()) {
                Utils.deleteAll(service.observedProperties());
            }
            if (deleteFeatures.isSelected()) {
                Utils.deleteAll(service.featuresOfInterest());
            }
            if (deleteLocations.isSelected()) {
                Utils.deleteAll(service.locations());
            }
            if (deleteDatastreams.isSelected()) {
                Utils.deleteAll(service.datastreams());
            }
            if (deleteMultiDatastreams.isSelected()) {
                Utils.deleteAll(service.multiDatastreams());
            }
            if (deleteObservations.isSelected()) {
                Utils.deleteAll(service.observations());
            }

        } catch (ServiceFailureException ex) {
            LOGGER.error("Exception deleting.", ex);
        }
    }

    public void setService(SensorThingsService service) {
        this.service = service;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

}
