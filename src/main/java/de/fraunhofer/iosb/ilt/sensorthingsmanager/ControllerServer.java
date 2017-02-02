package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.Entity;
import de.fraunhofer.iosb.ilt.sta.model.HistoricalLocation;
import de.fraunhofer.iosb.ilt.sta.model.Location;
import de.fraunhofer.iosb.ilt.sta.model.MultiDatastream;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.ObservedProperty;
import de.fraunhofer.iosb.ilt.sta.model.Sensor;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import de.fraunhofer.iosb.ilt.sta.query.Query;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;

/**
 *
 * @author scf
 */
public class ControllerServer implements Initializable {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerServer.class);
    private ServerListEntry entry;
    @FXML
    private Label serverTitle;
    @FXML
    private TabPane collectionTabs;
    private SensorThingsService service;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void setServerEntry(ServerListEntry entry) {
        this.entry = entry;
        serverTitle.setText(entry.getName() + " @ " + entry.getUrl());
        try {
            service = new SensorThingsService(new URL(entry.getUrl()));

            addTabFor("Things", service.things().query(), () -> {
                Thing entity = new Thing();
                entity.setName("New Thing");
                return entity;
            });
            addTabFor("Sensors", service.sensors().query(), () -> {
                Sensor entity = new Sensor();
                entity.setName("New Sensor");
                return entity;
            });
            addTabFor("Datastreams", service.datastreams().query(), () -> {
                Datastream entity = new Datastream();
                entity.setName("New Datastream");
                return entity;
            });
            addTabFor("MultiDtstrms", service.multiDatastreams().query(), () -> {
                MultiDatastream entity = new MultiDatastream();
                entity.setName("New MultiDatastream");
                return entity;
            });
            addTabFor("ObsrvdProps", service.observedProperties().query(), () -> {
                ObservedProperty entity = new ObservedProperty();
                entity.setName("New ObservedProperty");
                return entity;
            });
            addTabFor("Locations", service.locations().query(), () -> {
                Location entity = new Location();
                entity.setName("New Location");
                return entity;
            });
            addTabFor("HistLctns", service.historicalLocations().query(), () -> {
                HistoricalLocation entity = new HistoricalLocation();
                return entity;
            });
            addTabFor("Observations", service.observations().query(), () -> {
                Observation entity = new Observation();
                return entity;
            });

        } catch (URISyntaxException | MalformedURLException ex) {
            LOGGER.error("Failed to create service url.", ex);
        }
    }

    private <T extends Entity<T>> void addTabFor(String title, Query<T> query, ControllerCollection.EntityFactory<T> factory) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Collection.fxml"));
            AnchorPane content = (AnchorPane) loader.load();
            ControllerCollection controller = loader.<ControllerCollection>getController();
            controller.setQuery(query, factory);

            Tab tab = new Tab(title);
            tab.setContent(content);
            collectionTabs.getTabs().add(tab);
        } catch (IOException ex) {
            LOGGER.error("Failed to load Tab.", ex);
        }
    }

}
