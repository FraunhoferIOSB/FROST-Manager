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

import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.Server;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.aggregation.ControllerAggManager;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.Entity;
import de.fraunhofer.iosb.ilt.sta.model.FeatureOfInterest;
import de.fraunhofer.iosb.ilt.sta.model.HistoricalLocation;
import de.fraunhofer.iosb.ilt.sta.model.Location;
import de.fraunhofer.iosb.ilt.sta.model.MultiDatastream;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.ObservedProperty;
import de.fraunhofer.iosb.ilt.sta.model.Sensor;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import de.fraunhofer.iosb.ilt.sta.query.Query;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scf
 */
public class ControllerServer implements Initializable {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerServer.class);
    private Server entry;
    @FXML
    private Label serverTitle;
    @FXML
    private TabPane collectionTabs;
    private SensorThingsService service;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void setServerEntry(Server entry) {
        this.entry = entry;
        serverTitle.setText(entry.getName() + " @ " + entry.getUrl());
        try {
            service = new SensorThingsService(new URL(entry.getUrl()));
            if (entry.getAuthMethod() != null) {
                entry.getAuthMethod().setAuth(service);
            }

            addTabFor("Things", "name asc", service.things().query(), () -> {
                Thing entity = new Thing();
                entity.setName("New Thing");
                return entity;
            });
            addTabFor("Sensors", "name asc", service.sensors().query(), () -> {
                Sensor entity = new Sensor();
                entity.setName("New Sensor");
                return entity;
            });
            addTabFor("Datastreams", "name asc", service.datastreams().query(), () -> {
                Datastream entity = new Datastream();
                entity.setName("New Datastream");
                return entity;
            });
            addTabFor("MultiDtstrms", "name asc", service.multiDatastreams().query(), () -> {
                MultiDatastream entity = new MultiDatastream();
                entity.setName("New MultiDatastream");
                return entity;
            });
            addTabFor("ObsrvdProps", "name asc", service.observedProperties().query(), () -> {
                ObservedProperty entity = new ObservedProperty();
                entity.setName("New ObservedProperty");
                return entity;
            });
            addTabFor("Locations", "name asc", service.locations().query(), () -> {
                Location entity = new Location();
                entity.setName("New Location");
                return entity;
            });
            addTabFor("HistLctns", "time desc", service.historicalLocations().query(), () -> {
                HistoricalLocation entity = new HistoricalLocation();
                return entity;
            });
            addTabFor("Observations", "phenomenonTime asc", service.observations().query(), () -> {
                Observation entity = new Observation();
                return entity;
            });
            addTabFor("FoIs", "name asc", service.featuresOfInterest().query(), () -> {
                FeatureOfInterest entity = new FeatureOfInterest();
                entity.setName("New FeatureOfInterest");
                return entity;
            });
            addCleanerTab();
            addAggregationTab();
        } catch (MalformedURLException ex) {
            LOGGER.error("Failed to create service url.", ex);
        }
    }

    private <T extends Entity<T>> void addTabFor(String title, String orderBy, Query<T> query, ControllerCollection.EntityFactory<T> factory) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Collection.fxml"));
            AnchorPane content = (AnchorPane) loader.load();
            ControllerCollection controller = loader.<ControllerCollection>getController();
            controller.setQuery(query, factory, orderBy);

            Tab tab = new Tab(title);
            tab.setContent(content);
            collectionTabs.getTabs().add(tab);
        } catch (IOException ex) {
            LOGGER.error("Failed to load Tab.", ex);
        }
    }

    private void addCleanerTab() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Cleaner.fxml"));
            BorderPane content = (BorderPane) loader.load();
            ControllerCleaner controller = loader.<ControllerCleaner>getController();
            controller.setService(service);

            Tab tab = new Tab("Cleaner");
            tab.setContent(content);
            collectionTabs.getTabs().add(tab);
        } catch (IOException ex) {
            LOGGER.error("Failed to load Tab.", ex);
        }
    }

    private void addAggregationTab() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AggregationManager.fxml"));
            BorderPane content = (BorderPane) loader.load();
            ControllerAggManager controller = loader.<ControllerAggManager>getController();
            controller.setService(service);

            Tab tab = new Tab("Aggregations");
            tab.setContent(content);
            collectionTabs.getTabs().add(tab);
        } catch (IOException ex) {
            LOGGER.error("Failed to load Tab.", ex);
        }
    }

}
