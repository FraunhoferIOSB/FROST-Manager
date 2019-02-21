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
package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.FeatureOfInterest;
import de.fraunhofer.iosb.ilt.sta.model.Location;
import de.fraunhofer.iosb.ilt.sta.model.MultiDatastream;
import de.fraunhofer.iosb.ilt.sta.model.ObservedProperty;
import de.fraunhofer.iosb.ilt.sta.model.Sensor;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
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

    @FXML
    private void actionClean(ActionEvent event) {
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
        } catch (ServiceFailureException ex) {
            LOGGER.error("Failed to clean.", ex);
        }
    }

    private void cleanThings() throws ServiceFailureException {
        EntityList<Thing> list = service.things()
                .query()
                .select("name", "id")
                .expand("Datastreams($select=id;$top=1),MultiDatastreams($select=id;$top=1)")
                .list();
        List<Thing> toDelete = new ArrayList<>();
        Iterator<Thing> it;
        for (it = list.fullIterator(); it.hasNext();) {
            Thing next = it.next();
            if (next.getDatastreams().isEmpty() && next.getMultiDatastreams().isEmpty()) {
                toDelete.add(next);
            }
        }
        LOGGER.info("Deleting {} Things", toDelete.size());
        for (Thing item : toDelete) {
            LOGGER.debug("Deleting {}", item);
            service.delete(item.withOnlyId());
        }
    }

    private void cleanSensors() throws ServiceFailureException {
        EntityList<Sensor> list = service.sensors()
                .query()
                .select("name", "id")
                .expand("Datastreams($select=id;$top=1),MultiDatastreams($select=id;$top=1)")
                .list();
        List<Sensor> toDelete = new ArrayList<>();
        Iterator<Sensor> it;
        for (it = list.fullIterator(); it.hasNext();) {
            Sensor next = it.next();
            if (next.getDatastreams().isEmpty() && next.getMultiDatastreams().isEmpty()) {
                toDelete.add(next);
            }
        }
        LOGGER.info("Deleting {} Sensors", toDelete.size());
        for (Sensor item : toDelete) {
            LOGGER.debug("Deleting {}", item);
            service.delete(item.withOnlyId());
        }
    }

    private void cleanObservedProperties() throws ServiceFailureException {
        EntityList<ObservedProperty> list = service.observedProperties()
                .query()
                .select("name", "id")
                .expand("Datastreams($select=id;$top=1),MultiDatastreams($select=id;$top=1)")
                .list();
        List<ObservedProperty> toDelete = new ArrayList<>();
        Iterator<ObservedProperty> it;
        for (it = list.fullIterator(); it.hasNext();) {
            ObservedProperty next = it.next();
            if (next.getDatastreams().isEmpty() && next.getMultiDatastreams().isEmpty()) {
                toDelete.add(next);
            }
        }
        LOGGER.info("Deleting {} ObservedProperties", toDelete.size());
        for (ObservedProperty item : toDelete) {
            LOGGER.debug("Deleting {}", item);
            service.delete(item.withOnlyId());
        }
    }

    private void cleanDatastreams() throws ServiceFailureException {
        EntityList<Datastream> list = service.datastreams()
                .query()
                .select("name", "id")
                .expand("Observations($select=id;$top=1)")
                .list();
        List<Datastream> toDelete = new ArrayList<>();
        Iterator<Datastream> it;
        for (it = list.fullIterator(); it.hasNext();) {
            Datastream next = it.next();
            if (next.getObservations().isEmpty()) {
                toDelete.add(next);
            }
        }
        LOGGER.info("Deleting {} Datastreams", toDelete.size());
        for (Datastream item : toDelete) {
            LOGGER.debug("Deleting {}", item);
            service.delete(item.withOnlyId());
        }
    }

    private void cleanMultiDatastreams() throws ServiceFailureException {
        EntityList<MultiDatastream> list = service.multiDatastreams()
                .query()
                .select("name", "id")
                .expand("Observations($select=id;$top=1)")
                .list();
        List<MultiDatastream> toDelete = new ArrayList<>();
        Iterator<MultiDatastream> it;
        for (it = list.fullIterator(); it.hasNext();) {
            MultiDatastream next = it.next();
            if (next.getObservations().isEmpty()) {
                toDelete.add(next);
            }
        }
        LOGGER.info("Deleting {} MultiDatastream", toDelete.size());
        for (MultiDatastream item : toDelete) {
            LOGGER.debug("Deleting {}", item);
            service.delete(item.withOnlyId());
        }
    }

    private void cleanFeatures() throws ServiceFailureException {
        EntityList<FeatureOfInterest> list = service.featuresOfInterest()
                .query()
                .select("name", "id")
                .expand("Observations($select=id;$top=1)")
                .list();
        List<FeatureOfInterest> toDelete = new ArrayList<>();
        Iterator<FeatureOfInterest> it;
        for (it = list.fullIterator(); it.hasNext();) {
            FeatureOfInterest next = it.next();
            if (next.getObservations().isEmpty()) {
                toDelete.add(next);
            }
        }
        LOGGER.info("Deleting {} FeaturesOfInterest", toDelete.size());
        for (FeatureOfInterest item : toDelete) {
            LOGGER.debug("Deleting {}", item);
            service.delete(item.withOnlyId());
        }
    }

    private void cleanLocations() throws ServiceFailureException {
        EntityList<Location> list = service.locations()
                .query()
                .select("name", "id")
                .expand("Things($select=id;$top=1)")
                .list();
        List<Location> toDelete = new ArrayList<>();
        Iterator<Location> it;
        for (it = list.fullIterator(); it.hasNext();) {
            Location next = it.next();
            if (next.getThings().isEmpty()) {
                toDelete.add(next);
            }
        }
        LOGGER.info("Deleting {} Locations", toDelete.size());
        for (Location item : toDelete) {
            LOGGER.debug("Deleting {}", item);
            service.delete(item.withOnlyId());
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
