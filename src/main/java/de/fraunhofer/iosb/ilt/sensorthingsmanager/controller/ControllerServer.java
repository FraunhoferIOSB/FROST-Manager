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

import de.fraunhofer.iosb.ilt.frostclient.SensorThingsService;
import de.fraunhofer.iosb.ilt.frostclient.model.EntityType;
import de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsMultiDatastreamV11;
import de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsSensingV11;
import de.fraunhofer.iosb.ilt.frostclient.query.Query;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.aggregation.ControllerAggManager;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.Server;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
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
        collectionTabs.setTabMinWidth(20);
        collectionTabs.setTabMaxWidth(20);
        collectionTabs.setTabMinHeight(150);
        collectionTabs.setTabMaxHeight(150);
    }

    public void setServerEntry(Server entry) {
        this.entry = entry;
        serverTitle.setText(entry.getName() + " @ " + entry.getUrl());

        try {
            service = new SensorThingsService(new URL(entry.getUrl()), entry.getDataModels());
            if (entry.getAuthMethod() != null) {
                entry.getAuthMethod().setAuth(service);
            }

            for (EntityType et : service.getModelRegistry().getEntityTypes()) {
                String orderBy = "";
                if (et.getProperty(SensorThingsSensingV11.NAME_NAME) != null) {
                    orderBy = "name asc";
                }
                addTabFor(et.plural, orderBy, service.query(et));
            }

            if (service.hasModel(SensorThingsMultiDatastreamV11.class)) {
                addAggregationTab();
            }
        } catch (MalformedURLException ex) {
            LOGGER.error("Failed to create service url.", ex);
        }
    }

    private void addTabFor(String title, String orderBy, Query query) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Collection.fxml"));
            AnchorPane content = (AnchorPane) loader.load();
            ControllerCollection controller = loader.<ControllerCollection>getController();
            controller.setQuery(query, orderBy);

            Tab tab = new Tab();
            tab.setGraphic(new StackPane(new Group(new Label(title))));
            tab.setContent(content);
            collectionTabs.getTabs().add(tab);
        } catch (IOException ex) {
            LOGGER.error("Failed to load Tab.", ex);
        }
    }

//    private void addCleanerTab() {
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Cleaner.fxml"));
//            BorderPane content = (BorderPane) loader.load();
//            ControllerCleaner controller = loader.<ControllerCleaner>getController();
//            controller.setService(service);
//
//            Tab tab = new Tab("Cleaner");
//            tab.setContent(content);
//            collectionTabs.getTabs().add(tab);
//        } catch (IOException ex) {
//            LOGGER.error("Failed to load Tab.", ex);
//        }
//    }
    private void addAggregationTab() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AggregationManager.fxml"));
            BorderPane content = (BorderPane) loader.load();
            ControllerAggManager controller = loader.<ControllerAggManager>getController();
            controller.setService(service);

            Tab tab = new Tab();
            tab.setGraphic(new StackPane(new Group(new Label("Aggregations"))));
            tab.setContent(content);
            collectionTabs.getTabs().add(tab);
        } catch (IOException ex) {
            LOGGER.error("Failed to load Tab.", ex);
        }
    }

}
