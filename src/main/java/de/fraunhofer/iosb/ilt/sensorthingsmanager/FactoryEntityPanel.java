package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import java.io.IOException;

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
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

/**
 *
 * @author scf
 */
public class FactoryEntityPanel {

    private static String ENTITY_PANE_FXML = "/fxml/PaneEntity.fxml";

    public static Node getPane(Entity<?> entity) throws IOException {
        switch (entity.getType()) {

            case DATASTREAM:
                return getDatastreamPane((Datastream) entity);

            case FEATURE_OF_INTEREST:
                return getFeatureOfInterestPane((FeatureOfInterest) entity);

            case HISTORICAL_LOCATION:
                return getHistoricalLocationPane((HistoricalLocation) entity);

            case LOCATION:
                return getLocationPane((Location) entity);

            case MULTIDATASTREAM:
                return getMultiDatastreamPane((MultiDatastream) entity);

            case OBSERVATION:
                return getObservationPane((Observation) entity);

            case OBSERVED_PROPERTY:
                return getObsPropPane((ObservedProperty) entity);

            case SENSOR:
                return getSensorPane((Sensor) entity);

            case THING:
                return getThingPane((Thing) entity);
        }
        return null;
    }

    public static Node getDatastreamPane(Datastream entity) throws IOException {
        if (entity == null) {
            return new Label("No Datastream.");
        }
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<Datastream> controller = loader.<ControllerEntity<Datastream>>getController();
        controller.setEntity(entity, new EntityGuiController.GuiControllerDatastream());
        return content;
    }

    public static Pane getFeatureOfInterestPane(FeatureOfInterest entity) throws IOException {
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<FeatureOfInterest> controller = loader.<ControllerEntity<FeatureOfInterest>>getController();
        controller.setEntity(entity, new EntityGuiController.GuiControllerFeatureOfInterest());
        return content;
    }

    public static Pane getHistoricalLocationPane(HistoricalLocation entity) throws IOException {
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<HistoricalLocation> controller = loader.<ControllerEntity<HistoricalLocation>>getController();
        controller.setEntity(entity, new EntityGuiController.GuiControllerHistoricalLocation());
        return content;
    }

    public static Pane getLocationPane(Location entity) throws IOException {
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<Location> controller = loader.<ControllerEntity<Location>>getController();
        controller.setEntity(entity, new EntityGuiController.GuiControllerLocation());
        return content;
    }

    public static Node getMultiDatastreamPane(MultiDatastream entity) throws IOException {
        if (entity == null) {
            return new Label("No MultiDatastream.");
        }
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<MultiDatastream> controller = loader.<ControllerEntity<MultiDatastream>>getController();
        controller.setEntity(entity, new EntityGuiController.GuiControllerMultiDatastream());
        return content;
    }

    public static Pane getObservationPane(Observation entity) throws IOException {
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<Observation> controller = loader.<ControllerEntity<Observation>>getController();
        controller.setEntity(entity, new EntityGuiController.GuiControllerObservation());
        return content;
    }

    public static Pane getObsPropPane(ObservedProperty entity) throws IOException {
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<ObservedProperty> controller = loader.<ControllerEntity<ObservedProperty>>getController();
        controller.setEntity(entity, new EntityGuiController.GuiControllerObsProp());
        return content;
    }

    public static Pane getSensorPane(Sensor entity) throws IOException {
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<Sensor> controller = loader.<ControllerEntity<Sensor>>getController();
        controller.setEntity(entity, new EntityGuiController.GuiControllerSensor());
        return content;
    }

    public static Pane getThingPane(Thing entity) throws IOException {
        FXMLLoader loader = new FXMLLoader(FactoryEntityPanel.class.getResource(ENTITY_PANE_FXML));
        Pane content = (Pane) loader.load();
        ControllerEntity<Thing> controller = loader.<ControllerEntity<Thing>>getController();
        controller.setEntity(entity, new EntityGuiController.GuiControllerThing());
        return content;
    }

}
