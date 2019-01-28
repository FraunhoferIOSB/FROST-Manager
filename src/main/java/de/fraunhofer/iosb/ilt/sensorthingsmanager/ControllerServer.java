package de.fraunhofer.iosb.ilt.sensorthingsmanager;

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

}
