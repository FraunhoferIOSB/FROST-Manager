package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.geojson.GeoJsonObject;
import org.geojson.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
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
import de.fraunhofer.iosb.ilt.sta.model.TimeObject;
import de.fraunhofer.iosb.ilt.sta.model.ext.UnitOfMeasurement;
import de.fraunhofer.iosb.ilt.sta.query.Query;
import javafx.fxml.FXMLLoader;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;

/**
 *
 * @author scf
 * @param <T> The exact entity type this controller controls.
 */
public interface EntityGuiController<T extends Entity<T>> {

    public static TypeReference TYPE_MAP_STRING_OBJECT = new TypeReference<Map<String, Object>>() {
        // Empty on purpose.
    };
    public static TypeReference TYPE_LIST_UOM = new TypeReference<List<UnitOfMeasurement>>() {
        // Empty on purpose.
    };

    /**
     * Load the fields from the entity into the gui.
     */
    public void loadFields();

    /**
     * Save the fields from the gui into the entity.
     */
    public void saveFields();

    public void init(T entity, GridPane gridProperties, Accordion accordionLinks, Label labelId);

    public static class GuiControllerDatastream implements EntityGuiController<Datastream> {

        /**
         * The logger for this class.
         */
        private static final Logger LOGGER = LoggerFactory.getLogger(GuiControllerThing.class);
        private Label labelId;
        private Datastream entity;
        private TextField textName;
        private TextArea textDescription;
        private TextField textObservationType;
        private TextField textUomName;
        private TextField textUomSymbol;
        private TextField textUomDefinition;
        private TextField textObservedArea;
        private TextField textPhenomenonTime;
        private TextField textResultTime;

        @Override
        public void loadFields() {
            if (entity.getId() != null) {
                labelId.setText(entity.getId().toString());
            }
            textName.setText(entity.getName());
            textDescription.setText(entity.getDescription());
            textObservationType.setText(entity.getObservationType());
            UnitOfMeasurement uom = entity.getUnitOfMeasurement();
            textUomName.setText(uom.getName());
            textUomSymbol.setText(uom.getSymbol());
            textUomDefinition.setText(uom.getDefinition());
            final ObjectMapper mapper = ObjectMapperFactory.get();
            try {
                Polygon oa = entity.getObservedArea();
                textObservedArea.setText(mapper.writeValueAsString(oa));
            } catch (IOException ex) {
                LOGGER.error("Failed to load fields.", ex);
            }
            if (entity.getPhenomenonTime() != null) {
                textPhenomenonTime.setText(entity.getPhenomenonTime().toString());
            }
            if (entity.getResultTime() != null) {
                textResultTime.setText(entity.getResultTime().toString());
            }

        }

        @Override
        public void saveFields() {
            entity.setName(textName.getText());
            entity.setDescription(textDescription.getText());
            entity.setObservationType(textObservationType.getText());
            UnitOfMeasurement uom = entity.getUnitOfMeasurement();
            uom.setName(textUomName.getText());
            uom.setSymbol(textUomSymbol.getText());
            uom.setDefinition(textUomDefinition.getText());
        }

        @Override
        public void init(Datastream entity, GridPane gridProperties, Accordion accordionLinks, Label labelId) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textName = addFieldTo(gridProperties, i, "Name", new TextField(), false);
            textDescription = addFieldTo(gridProperties, ++i, "Description", new TextArea(), true);
            textObservationType = addFieldTo(gridProperties, ++i, "ObservationType", new TextField(), false);
            textUomName = addFieldTo(gridProperties, ++i, "UoM: Name", new TextField(), false);
            textUomSymbol = addFieldTo(gridProperties, ++i, "UoM: Symbol", new TextField(), false);
            textUomDefinition = addFieldTo(gridProperties, ++i, "UoM: Definition", new TextField(), false);
            textObservedArea = addFieldTo(gridProperties, ++i, "ObservedArea", new TextField(), false);
            textObservedArea.setDisable(true);
            textPhenomenonTime = addFieldTo(gridProperties, ++i, "PhenomenonTime", new TextField(), false);
            textPhenomenonTime.setDisable(true);
            textResultTime = addFieldTo(gridProperties, ++i, "ResultTime", new TextField(), false);
            textResultTime.setDisable(true);

            try {
                TitledPane tp = new TitledPane("Thing", FactoryEntityPanel.getThingPane(entity.getThing()));
                accordionLinks.getPanes().add(tp);
                tp = new TitledPane("Sensor", FactoryEntityPanel.getSensorPane(entity.getSensor()));
                accordionLinks.getPanes().add(tp);
                tp = new TitledPane("ObservedProperty", FactoryEntityPanel.getObsPropPane(entity.getObservedProperty()));
                accordionLinks.getPanes().add(tp);
                tp = new TitledPane("Observations", createCollectionPaneFor(entity.observations().query()));
                accordionLinks.getPanes().add(tp);
            } catch (IOException | ServiceFailureException ex) {
                LOGGER.error("Failed to create panel.", ex);
            }
        }
    }

    public static class GuiControllerMultiDatastream implements EntityGuiController<MultiDatastream> {

        /**
         * The logger for this class.
         */
        private static final Logger LOGGER = LoggerFactory.getLogger(GuiControllerThing.class);
        private Label labelId;
        private MultiDatastream entity;
        private TextField textName;
        private TextArea textDescription;
        private TextField textObservationType;
        private TextArea textUoms;
        private TextField textObservedArea;
        private TextField textPhenomenonTime;
        private TextField textResultTime;

        @Override
        public void loadFields() {
            if (entity.getId() != null) {
                labelId.setText(entity.getId().toString());
            }
            textName.setText(entity.getName());
            textDescription.setText(entity.getDescription());
            textObservationType.setText(entity.getObservationType());

            final ObjectMapper mapper = ObjectMapperFactory.get();
            try {
                List<UnitOfMeasurement> uoms = entity.getUnitOfMeasurements();
                textUoms.setText(mapper.writeValueAsString(uoms));
            } catch (IOException ex) {
                LOGGER.error("Failed to load fields.", ex);
            }
            try {
                Polygon oa = entity.getObservedArea();
                textObservedArea.setText(mapper.writeValueAsString(oa));
            } catch (IOException ex) {
                LOGGER.error("Failed to load fields.", ex);
            }
            if (entity.getPhenomenonTime() != null) {
                textPhenomenonTime.setText(entity.getPhenomenonTime().toString());
            }
            if (entity.getResultTime() != null) {
                textResultTime.setText(entity.getResultTime().toString());
            }

        }

        @Override
        public void saveFields() {
            entity.setName(textName.getText());
            entity.setDescription(textDescription.getText());
            entity.setObservationType(textObservationType.getText());
            final ObjectMapper mapper = ObjectMapperFactory.get();
            try {
                List<UnitOfMeasurement> properties = mapper.readValue(textUoms.getText(), TYPE_LIST_UOM);
                entity.setUnitOfMeasurements(properties);
            } catch (IOException ex) {
                LOGGER.error("Not valid json.", ex);
            }
        }

        @Override
        public void init(MultiDatastream entity, GridPane gridProperties, Accordion accordionLinks, Label labelId) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textName = addFieldTo(gridProperties, i, "Name", new TextField(), false);
            textDescription = addFieldTo(gridProperties, ++i, "Description", new TextArea(), true);
            textObservationType = addFieldTo(gridProperties, ++i, "ObservationType", new TextField(), false);
            textUoms = addFieldTo(gridProperties, ++i, "UoMs", new TextArea(), false);
            textObservedArea = addFieldTo(gridProperties, ++i, "ObservedArea", new TextField(), false);
            textObservedArea.setDisable(true);
            textPhenomenonTime = addFieldTo(gridProperties, ++i, "PhenomenonTime", new TextField(), false);
            textPhenomenonTime.setDisable(true);
            textResultTime = addFieldTo(gridProperties, ++i, "ResultTime", new TextField(), false);
            textResultTime.setDisable(true);

            try {
                TitledPane tp = new TitledPane("Thing", FactoryEntityPanel.getThingPane(entity.getThing()));
                accordionLinks.getPanes().add(tp);
                tp = new TitledPane("Sensor", FactoryEntityPanel.getSensorPane(entity.getSensor()));
                accordionLinks.getPanes().add(tp);
                tp = new TitledPane("ObservedProperties", createCollectionPaneFor(entity.observedProperties().query()));
                accordionLinks.getPanes().add(tp);
                tp = new TitledPane("Observations", createCollectionPaneFor(entity.observations().query()));
                accordionLinks.getPanes().add(tp);
            } catch (IOException | ServiceFailureException ex) {
                LOGGER.error("Failed to create panel.", ex);
            }
        }
    }

    public static class GuiControllerFeatureOfInterest implements EntityGuiController<FeatureOfInterest> {

        /**
         * The logger for this class.
         */
        private static final Logger LOGGER = LoggerFactory.getLogger(GuiControllerThing.class);
        private Label labelId;
        private FeatureOfInterest entity;
        private TextField textName;
        private TextArea textDescription;
        private TextField textEncodingType;
        private TextArea textFeature;

        @Override
        public void loadFields() {
            if (entity.getId() != null) {
                labelId.setText(entity.getId().toString());
            }
            textName.setText(entity.getName());
            textDescription.setText(entity.getDescription());
            textEncodingType.setText(entity.getEncodingType());
            final ObjectMapper mapper = ObjectMapperFactory.get();
            String json;
            try {
                json = mapper.writeValueAsString(entity.getFeature());
                textFeature.setText(json);
            } catch (JsonProcessingException ex) {
                LOGGER.error("Properties can not be converted to JSON.", ex);
            }
        }

        @Override
        public void saveFields() {
            entity.setName(textName.getText());
            entity.setDescription(textDescription.getText());
            entity.setEncodingType(textEncodingType.getText());
            final ObjectMapper mapper = ObjectMapperFactory.get();
            try {
                GeoJsonObject feature = mapper.readValue(textFeature.getText(), GeoJsonObject.class);
                entity.setFeature(feature);
            } catch (IOException ex) {
                LOGGER.error("Not valid geojson.", ex);
            }
        }

        @Override
        public void init(FeatureOfInterest entity, GridPane gridProperties, Accordion accordionLinks, Label labelId) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textName = addFieldTo(gridProperties, i, "Name", new TextField(), false);
            textDescription = addFieldTo(gridProperties, ++i, "Description", new TextArea(), true);
            textEncodingType = addFieldTo(gridProperties, ++i, "EncodingType", new TextField(), false);
            textFeature = addFieldTo(gridProperties, ++i, "Feature", new TextArea(), false);

            TitledPane tp = new TitledPane("Observations", createCollectionPaneFor(entity.observations().query()));
            accordionLinks.getPanes().add(tp);
        }
    }

    public static class GuiControllerHistoricalLocation implements EntityGuiController<HistoricalLocation> {

        /**
         * The logger for this class.
         */
        private static final Logger LOGGER = LoggerFactory.getLogger(GuiControllerThing.class);
        private Label labelId;
        private HistoricalLocation entity;
        private TextField textTime;

        @Override
        public void loadFields() {
            if (entity.getId() != null) {
                labelId.setText(entity.getId().toString());
            }
            textTime.setText(entity.getTime().toString());
        }

        @Override
        public void saveFields() {
            entity.setTime(ZonedDateTime.parse(textTime.getText()));
        }

        @Override
        public void init(HistoricalLocation entity, GridPane gridProperties, Accordion accordionLinks, Label labelId) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textTime = addFieldTo(gridProperties, i, "Time", new TextField(), false);

            try {
                TitledPane tp = new TitledPane("Thing", FactoryEntityPanel.getThingPane(entity.getThing()));
                accordionLinks.getPanes().add(tp);
                tp = new TitledPane("Locations", createCollectionPaneFor(entity.locations().query()));
                accordionLinks.getPanes().add(tp);
            } catch (IOException | ServiceFailureException ex) {
                LOGGER.error("Failed to create panel.", ex);
            }
        }
    }

    public static class GuiControllerLocation implements EntityGuiController<Location> {

        /**
         * The logger for this class.
         */
        private static final Logger LOGGER = LoggerFactory.getLogger(GuiControllerThing.class);
        private Label labelId;
        private Location entity;
        private TextField textName;
        private TextArea textDescription;
        private TextField textEncodingType;
        private TextArea textLocation;

        @Override
        public void loadFields() {
            if (entity.getId() != null) {
                labelId.setText(entity.getId().toString());
            }
            textName.setText(entity.getName());
            textDescription.setText(entity.getDescription());
            textEncodingType.setText(entity.getEncodingType());
            final ObjectMapper mapper = ObjectMapperFactory.get();
            String json;
            try {
                json = mapper.writeValueAsString(entity.getLocation());
                textLocation.setText(json);
            } catch (JsonProcessingException ex) {
                LOGGER.error("Properties can not be converted to JSON.", ex);
            }
        }

        @Override
        public void saveFields() {
            entity.setName(textName.getText());
            entity.setDescription(textDescription.getText());
            entity.setEncodingType(textEncodingType.getText());
            final ObjectMapper mapper = ObjectMapperFactory.get();
            try {
                GeoJsonObject feature = mapper.readValue(textLocation.getText(), GeoJsonObject.class);
                entity.setLocation(feature);
            } catch (IOException ex) {
                LOGGER.error("Not valid geojson.", ex);
            }
        }

        @Override
        public void init(Location entity, GridPane gridProperties, Accordion accordionLinks, Label labelId) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textName = addFieldTo(gridProperties, i, "Name", new TextField(), false);
            textDescription = addFieldTo(gridProperties, ++i, "Description", new TextArea(), true);
            textEncodingType = addFieldTo(gridProperties, ++i, "EncodingType", new TextField(), false);
            textLocation = addFieldTo(gridProperties, ++i, "Location", new TextArea(), false);

            TitledPane tp = new TitledPane("Things", createCollectionPaneFor(entity.things().query()));
            accordionLinks.getPanes().add(tp);
            tp = new TitledPane("HistoricalLocations", createCollectionPaneFor(entity.historicalLocations().query()));
            accordionLinks.getPanes().add(tp);
        }
    }

    public static class GuiControllerObservation implements EntityGuiController<Observation> {

        /**
         * The logger for this class.
         */
        private static final Logger LOGGER = LoggerFactory.getLogger(GuiControllerThing.class);
        private Label labelId;
        private Observation entity;
        private TextField textPhenomenonTime;
        private TextField textResultTime;
        private TextArea textResult;
        private TextField textResultQuality;
        private TextField textValidTime;
        private TextArea textParameters;

        @Override
        public void loadFields() {
            if (entity.getId() != null) {
                labelId.setText(entity.getId().toString());
            }
            textPhenomenonTime.setText(entity.getPhenomenonTime().toString());
            if (entity.getResultTime() != null) {
                textResultTime.setText(entity.getResultTime().toString());
            }
            if (entity.getValidTime() != null) {
                textValidTime.setText(entity.getValidTime().toString());
            }
            final ObjectMapper mapper = ObjectMapperFactory.get();
            String json;
            try {
                json = mapper.writeValueAsString(entity.getResult());
                textResult.setText(json);
            } catch (JsonProcessingException ex) {
                LOGGER.error("Properties can not be converted to JSON.", ex);
            }
            try {
                json = mapper.writeValueAsString(entity.getResultQuality());
                textResultQuality.setText(json);
            } catch (JsonProcessingException ex) {
                LOGGER.error("Properties can not be converted to JSON.", ex);
            }
            try {
                json = mapper.writeValueAsString(entity.getParameters());
                textParameters.setText(json);
            } catch (JsonProcessingException ex) {
                LOGGER.error("Properties can not be converted to JSON.", ex);
            }

        }

        @Override
        public void saveFields() {
            entity.setPhenomenonTime(TimeObject.parse(textPhenomenonTime.getText()));
            entity.setResultTime(ZonedDateTime.parse(textResultTime.getText()));
            entity.setValidTime(Interval.parse(textValidTime.getText()));
            final ObjectMapper mapper = ObjectMapperFactory.get();
            try {
                JsonNode tree = mapper.readTree(textResult.getText());
                entity.setResult(tree);
            } catch (IOException ex) {
                LOGGER.error("Not valid json.", ex);
            }
            try {
                JsonNode tree = mapper.readTree(textResultQuality.getText());
                entity.setResultQuality(tree);
            } catch (IOException ex) {
                LOGGER.error("Not valid json.", ex);
            }
            try {
                Map<String, Object> map = mapper.readValue(textParameters.getText(), TYPE_MAP_STRING_OBJECT);
                entity.setParameters(map);
            } catch (IOException ex) {
                LOGGER.error("Not valid json.", ex);
            }
        }

        @Override
        public void init(Observation entity, GridPane gridProperties, Accordion accordionLinks, Label labelId) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textPhenomenonTime = addFieldTo(gridProperties, i, "PhenomenonTime", new TextField(), false);
            textResultTime = addFieldTo(gridProperties, ++i, "ResultTime", new TextField(), false);
            textResult = addFieldTo(gridProperties, ++i, "Result", new TextArea(), true);
            textResultQuality = addFieldTo(gridProperties, ++i, "ResultQuality", new TextField(), false);
            textValidTime = addFieldTo(gridProperties, ++i, "ValidTime", new TextField(), false);
            textParameters = addFieldTo(gridProperties, ++i, "Parameters", new TextArea(), true);

            try {
                TitledPane tp = new TitledPane("Datastream", FactoryEntityPanel.getDatastreamPane(entity.getDatastream()));
                accordionLinks.getPanes().add(tp);
                tp = new TitledPane("MultiDatastream", FactoryEntityPanel.getMultiDatastreamPane(entity.getMultiDatastream()));
                accordionLinks.getPanes().add(tp);
                tp = new TitledPane("FeatureOfInterest", FactoryEntityPanel.getFeatureOfInterestPane(entity.getFeatureOfInterest()));
                accordionLinks.getPanes().add(tp);
            } catch (IOException | ServiceFailureException ex) {
                LOGGER.error("Failed to create panel.", ex);
            }
        }
    }

    public static class GuiControllerObsProp implements EntityGuiController<ObservedProperty> {

        /**
         * The logger for this class.
         */
        private static final Logger LOGGER = LoggerFactory.getLogger(GuiControllerThing.class);
        private Label labelId;
        private ObservedProperty entity;

        private TextField textName;
        private TextField textDefinition;
        private TextArea textDescription;

        @Override
        public void loadFields() {
            if (entity.getId() != null) {
                labelId.setText(entity.getId().toString());
            }
            textName.setText(entity.getName());
            textDefinition.setText(entity.getDefinition());
            textDescription.setText(entity.getDescription());
        }

        @Override
        public void saveFields() {
            entity.setName(textName.getText());
            entity.setDefinition(textDefinition.getText());
            entity.setDescription(textDescription.getText());
        }

        @Override
        public void init(ObservedProperty entity, GridPane gridProperties, Accordion accordionLinks, Label labelId) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textName = addFieldTo(gridProperties, i, "Name", new TextField(), false);
            textDefinition = addFieldTo(gridProperties, ++i, "Definition", new TextField(), false);
            textDescription = addFieldTo(gridProperties, ++i, "Description", new TextArea(), true);

            TitledPane tp = new TitledPane("Datastreams", createCollectionPaneFor(entity.datastreams().query()));
            accordionLinks.getPanes().add(tp);
            tp = new TitledPane("MultiDatastreams", createCollectionPaneFor(entity.multiDatastreams().query()));
            accordionLinks.getPanes().add(tp);
        }
    }

    public static class GuiControllerSensor implements EntityGuiController<Sensor> {

        /**
         * The logger for this class.
         */
        private static final Logger LOGGER = LoggerFactory.getLogger(GuiControllerThing.class);
        private Label labelId;
        private Sensor entity;

        private TextField textName;
        private TextArea textDescription;
        private TextField textEncodingType;
        private TextArea textMetadata;

        @Override
        public void loadFields() {
            if (entity.getId() != null) {
                labelId.setText(entity.getId().toString());
            }
            textName.setText(entity.getName());
            textDescription.setText(entity.getDescription());
            textEncodingType.setText(entity.getEncodingType());
            final ObjectMapper mapper = ObjectMapperFactory.get();
            try {
                textMetadata.setText(mapper.writeValueAsString(entity.getMetadata()));
            } catch (JsonProcessingException ex) {
                LOGGER.error("Metadata can not be converted to JSON.", ex);
            }
        }

        @Override
        public void saveFields() {
            entity.setName(textName.getText());
            entity.setDescription(textDescription.getText());
            entity.setEncodingType(textEncodingType.getText());
            final ObjectMapper mapper = ObjectMapperFactory.get();
            try {
                JsonNode json = mapper.readTree(textMetadata.getText());
                entity.setMetadata(json);
            } catch (IOException ex) {
                LOGGER.error("Not valid json.", ex);
            }
        }

        @Override
        public void init(Sensor entity, GridPane gridProperties, Accordion accordionLinks, Label labelId) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textName = addFieldTo(gridProperties, i, "Name", new TextField(), false);
            textDescription = addFieldTo(gridProperties, ++i, "Description", new TextArea(), true);
            textEncodingType = addFieldTo(gridProperties, ++i, "EncodingType", new TextField(), false);
            textMetadata = addFieldTo(gridProperties, ++i, "Metadata", new TextArea(), true);

            TitledPane tp = new TitledPane("Datastreams", createCollectionPaneFor(entity.datastreams().query()));
            accordionLinks.getPanes().add(tp);
            tp = new TitledPane("MultiDatastreams", createCollectionPaneFor(entity.multiDatastreams().query()));
            accordionLinks.getPanes().add(tp);
        }
    }

    public static class GuiControllerThing implements EntityGuiController<Thing> {

        /**
         * The logger for this class.
         */
        private static final Logger LOGGER = LoggerFactory.getLogger(GuiControllerThing.class);
        private Label labelId;
        private Thing entity;
        private TextField textName;
        private TextArea textDescription;
        private TextArea textProperties;

        @Override
        public void loadFields() {
            if (entity.getId() != null) {
                labelId.setText(entity.getId().toString());
            }
            textName.setText(entity.getName());
            textDescription.setText(entity.getDescription());
            final ObjectMapper mapper = ObjectMapperFactory.get();
            String props;
            try {
                props = mapper.writeValueAsString(entity.getProperties());
                textProperties.setText(props);
            } catch (JsonProcessingException ex) {
                LOGGER.error("Properties can not be converted to JSON.", ex);
            }

        }

        @Override
        public void saveFields() {
            entity.setName(textName.getText());
            entity.setDescription(textDescription.getText());
            final ObjectMapper mapper = ObjectMapperFactory.get();
            try {
                Map<String, Object> properties = mapper.readValue(textProperties.getText(), TYPE_MAP_STRING_OBJECT);
                entity.setProperties(properties);
            } catch (IOException ex) {
                LOGGER.error("Not valid json.", ex);
            }
        }

        @Override
        public void init(Thing entity, GridPane gridProperties, Accordion accordionLinks, Label labelId) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textName = addFieldTo(gridProperties, i, "Name", new TextField(), false);
            textDescription = addFieldTo(gridProperties, ++i, "Description", new TextArea(), true);
            textProperties = addFieldTo(gridProperties, ++i, "Properties", new TextArea(), true);

            TitledPane tp = new TitledPane("Datastreams", createCollectionPaneFor(entity.datastreams().query()));
            accordionLinks.getPanes().add(tp);
            tp = new TitledPane("MultiDatastreams", createCollectionPaneFor(entity.multiDatastreams().query()));
            accordionLinks.getPanes().add(tp);
            tp = new TitledPane("Locations", createCollectionPaneFor(entity.locations().query()));
            accordionLinks.getPanes().add(tp);
            tp = new TitledPane("HistoricalLocations", createCollectionPaneFor(entity.historicalLocations().query()));
            accordionLinks.getPanes().add(tp);
        }
    }

    public static <T extends Node> T addFieldTo(GridPane gp, int row, String title, T node, boolean fillHeight) {
        gp.getRowConstraints().add(new RowConstraints(Region.USE_PREF_SIZE, Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE, Priority.NEVER, VPos.BASELINE, fillHeight));
        gp.add(new Label(title), 0, row);
        gp.add(node, 1, row);
        return node;
    }

    public static Pane createCollectionPaneFor(Query<? extends Entity<?>> query) {
        try {
            FXMLLoader loader = new FXMLLoader(EntityGuiController.class.getResource("/fxml/Collection.fxml"));
            AnchorPane content = (AnchorPane) loader.load();
            ControllerCollection controller = loader.<ControllerCollection>getController();
            controller.setQuery(query, false);
            return content;
        } catch (IOException ex) {
            LoggerFactory.getLogger(EntityGuiController.class).error("Failed to load Collection Pane.", ex);
        }
        return null;
    }

}
