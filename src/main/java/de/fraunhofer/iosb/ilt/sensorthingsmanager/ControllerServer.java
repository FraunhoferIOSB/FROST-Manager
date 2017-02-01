package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iosb.ilt.sta.model.Entity;
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

            addTabFor("Things", service.things().query());
            addTabFor("Sensors", service.sensors().query());
            addTabFor("Datastreams", service.datastreams().query());
            addTabFor("MultiDtstrms", service.multiDatastreams().query());
            addTabFor("ObsrvdProps", service.observedProperties().query());
            addTabFor("Locations", service.locations().query());
            addTabFor("HistLctns", service.historicalLocations().query());
            addTabFor("Observations", service.observations().query());

        } catch (URISyntaxException | MalformedURLException ex) {
            LOGGER.error("Failed to create service url.", ex);
        }
    }

    private void addTabFor(String title, Query<? extends Entity<?>> query) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Collection.fxml"));
            AnchorPane content = (AnchorPane) loader.load();
            ControllerCollection controller = loader.<ControllerCollection>getController();
            controller.setQuery(query, true);

            Tab tab = new Tab(title);
            tab.setContent(content);
            collectionTabs.getTabs().add(tab);
        } catch (IOException ex) {
            LOGGER.error("Failed to load Tab.", ex);
        }
    }

}
