package de.fraunhofer.iosb.ilt.sensorthingsmanager.controller;

import de.fraunhofer.iosb.ilt.sensorthingsmanager.controller.EntityGuiController;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.controller.ControllerEntity;
import java.io.IOException;

import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.Entity;
import de.fraunhofer.iosb.ilt.sta.model.EntityType;
import de.fraunhofer.iosb.ilt.sta.model.FeatureOfInterest;
import de.fraunhofer.iosb.ilt.sta.model.HistoricalLocation;
import de.fraunhofer.iosb.ilt.sta.model.Location;
import de.fraunhofer.iosb.ilt.sta.model.MultiDatastream;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.ObservedProperty;
import de.fraunhofer.iosb.ilt.sta.model.Sensor;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

/**
 *
 * @author scf
 */
public class FactoryEntityPanel {

    private static final String ENTITY_PANE_FXML = "/fxml/PaneEntity.fxml";

    public static <T extends Entity<T>> Node getPane(SensorThingsService service, EntityType type, T entity, boolean showNavProps) throws IOException {
        if (entity != null && entity.getType() != type) {
            throw new IllegalArgumentException("Entity must have given type or be null.");
        }
        switch (type) {
            case DATASTREAM:
                return getDatastreamPane(service, (Datastream) entity, showNavProps);

            case FEATURE_OF_INTEREST:
                return getFeatureOfInterestPane(service, (FeatureOfInterest) entity, showNavProps);

            case HISTORICAL_LOCATION:
                return getHistoricalLocationPane(service, (HistoricalLocation) entity, showNavProps);

            case LOCATION:
                return getLocationPane(service, (Location) entity, showNavProps);

            case MULTIDATASTREAM:
                return getMultiDatastreamPane(service, (MultiDatastream) entity, showNavProps);

            case OBSERVATION:
                return getObservationPane(service, (Observation) entity, showNavProps);

            case OBSERVED_PROPERTY:
                return getObsPropPane(service, (ObservedProperty) entity, showNavProps);

            case SENSOR:
                return getSensorPane(service, (Sensor) entity, showNavProps);

            case THING:
                return getThingPane(service, (Thing) entity, showNavProps);

        }
        return null;
    }

    public static Node getDatastreamPane(SensorThingsService service, Datastream entity, boolean showNavProps) throws IOException {
        if (entity == null) {
            return new Label("No Datastream.");
        }
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Node content = (Pane) loader.load();
        ControllerEntity<Datastream> controller = loader.<ControllerEntity<Datastream>>getController();
        controller.setEntity(service, entity, new EntityGuiController.GuiControllerDatastream(), showNavProps);
        return content;
    }

    public static Node getFeatureOfInterestPane(SensorThingsService service, FeatureOfInterest entity, boolean showNavProps) throws IOException {
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<FeatureOfInterest> controller = loader.<ControllerEntity<FeatureOfInterest>>getController();
        controller.setEntity(service, entity, new EntityGuiController.GuiControllerFeatureOfInterest(), showNavProps);
        return content;
    }

    public static Node getHistoricalLocationPane(SensorThingsService service, HistoricalLocation entity, boolean showNavProps) throws IOException {
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<HistoricalLocation> controller = loader.<ControllerEntity<HistoricalLocation>>getController();
        controller.setEntity(service, entity, new EntityGuiController.GuiControllerHistoricalLocation(), showNavProps);
        return content;
    }

    public static Node getLocationPane(SensorThingsService service, Location entity, boolean showNavProps) throws IOException {
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<Location> controller = loader.<ControllerEntity<Location>>getController();
        controller.setEntity(service, entity, new EntityGuiController.GuiControllerLocation(), showNavProps);
        return content;
    }

    public static Node getMultiDatastreamPane(SensorThingsService service, MultiDatastream entity, boolean showNavProps) throws IOException {
        if (entity == null) {
            return new Label("No MultiDatastream.");
        }
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<MultiDatastream> controller = loader.<ControllerEntity<MultiDatastream>>getController();
        controller.setEntity(service, entity, new EntityGuiController.GuiControllerMultiDatastream(), showNavProps);
        return content;
    }

    public static Node getObservationPane(SensorThingsService service, Observation entity, boolean showNavProps) throws IOException {
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<Observation> controller = loader.<ControllerEntity<Observation>>getController();
        controller.setEntity(service, entity, new EntityGuiController.GuiControllerObservation(), showNavProps);
        return content;
    }

    public static Node getObsPropPane(SensorThingsService service, ObservedProperty entity, boolean showNavProps) throws IOException {
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<ObservedProperty> controller = loader.<ControllerEntity<ObservedProperty>>getController();
        controller.setEntity(service, entity, new EntityGuiController.GuiControllerObsProp(), showNavProps);
        return content;
    }

    public static Node getSensorPane(SensorThingsService service, Sensor entity, boolean showNavProps) throws IOException {
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<Sensor> controller = loader.<ControllerEntity<Sensor>>getController();
        controller.setEntity(service, entity, new EntityGuiController.GuiControllerSensor(), showNavProps);
        return content;
    }

    public static Node getThingPane(SensorThingsService service, Thing entity, boolean showNavProps) throws IOException {
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<Thing> controller = loader.<ControllerEntity<Thing>>getController();
        controller.setEntity(service, entity, new EntityGuiController.GuiControllerThing(), showNavProps);
        return content;
    }

}
