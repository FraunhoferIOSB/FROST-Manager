package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
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
import de.fraunhofer.iosb.ilt.sta.model.TimeObject;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;
import de.fraunhofer.iosb.ilt.sta.model.ext.UnitOfMeasurement;
import de.fraunhofer.iosb.ilt.sta.query.Query;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import org.geojson.GeoJsonObject;
import org.geojson.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

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

    public EntityType getType();

    /**
     *
     * @param service The service the entity belongs to.
     * @param entity the entity.
     * @param gridProperties the grid for entity properties.
     * @param accordionLinks The accordion for navigation properties.
     * @param labelId The label that shows the entity id.
     * @param editable is the entity editable.
     */
    public void init(SensorThingsService service, T entity, GridPane gridProperties, Accordion accordionLinks, Label labelId, boolean editable);

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
            if (entity == null) {
                return;
            }
            if (entity.getId() != null) {
                labelId.setText(entity.getId().toString());
            }
            textName.setText(entity.getName());
            textDescription.setText(entity.getDescription());
            textObservationType.setText(entity.getObservationType());
            UnitOfMeasurement uom = entity.getUnitOfMeasurement();
            if (uom == null) {
                uom = new UnitOfMeasurement();
                entity.setUnitOfMeasurement(uom);
            }
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
        public EntityType getType() {
            return EntityType.DATASTREAM;
        }

        @Override
        public void init(SensorThingsService service, Datastream entity, GridPane gridProperties, Accordion accordionLinks, Label labelId, boolean editable) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textName = addFieldTo(gridProperties, i, "Name", new TextField(), false, editable);
            textDescription = addFieldTo(gridProperties, ++i, "Description", new TextArea(), true, editable);
            textObservationType = addFieldTo(gridProperties, ++i, "ObservationType", new TextField(), false, editable);
            textUomName = addFieldTo(gridProperties, ++i, "UoM: Name", new TextField(), false, editable);
            textUomSymbol = addFieldTo(gridProperties, ++i, "UoM: Symbol", new TextField(), false, editable);
            textUomDefinition = addFieldTo(gridProperties, ++i, "UoM: Definition", new TextField(), false, editable);
            textObservedArea = addFieldTo(gridProperties, ++i, "ObservedArea", new TextField(), false, false);
            textPhenomenonTime = addFieldTo(gridProperties, ++i, "PhenomenonTime", new TextField(), false, false);
            textResultTime = addFieldTo(gridProperties, ++i, "ResultTime", new TextField(), false, false);

            if (accordionLinks != null) {
                try {
                    accordionLinks.getPanes().add(createEditableEntityPane(entity, entity.getThing(), service.things().query(), entity::setThing));
                    accordionLinks.getPanes().add(createEditableEntityPane(entity, entity.getSensor(), service.sensors().query(), entity::setSensor));
                    accordionLinks.getPanes().add(createEditableEntityPane(entity, entity.getObservedProperty(), service.observedProperties().query(), entity::setObservedProperty));
                    accordionLinks.getPanes().add(new TitledPane("Observations", createCollectionPaneFor(entity.observations().query())));
                } catch (IOException | ServiceFailureException ex) {
                    LOGGER.error("Failed to create panel.", ex);
                } catch (NullPointerException e) {
                    // Happens when entity is new.
                    LOGGER.trace("Failed to create panel.", e);
                }
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
        public EntityType getType() {
            return EntityType.MULTIDATASTREAM;
        }

        @Override
        public void init(SensorThingsService service, MultiDatastream entity, GridPane gridProperties, Accordion accordionLinks, Label labelId, boolean editable) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textName = addFieldTo(gridProperties, i, "Name", new TextField(), false, editable);
            textDescription = addFieldTo(gridProperties, ++i, "Description", new TextArea(), true, editable);
            textObservationType = addFieldTo(gridProperties, ++i, "ObservationType", new TextField(), false, editable);
            textUoms = addFieldTo(gridProperties, ++i, "UoMs", new TextArea(), false, editable);
            textObservedArea = addFieldTo(gridProperties, ++i, "ObservedArea", new TextField(), false, false);
            textPhenomenonTime = addFieldTo(gridProperties, ++i, "PhenomenonTime", new TextField(), false, false);
            textResultTime = addFieldTo(gridProperties, ++i, "ResultTime", new TextField(), false, false);

            if (accordionLinks != null) {
                try {
                    accordionLinks.getPanes().add(createEditableEntityPane(entity, entity.getThing(), service.things().query(), entity::setThing));
                    accordionLinks.getPanes().add(createEditableEntityPane(entity, entity.getSensor(), service.sensors().query(), entity::setSensor));

                    TitledPane tp = new TitledPane("ObservedProperties", createCollectionPaneFor(entity.observedProperties().query()));
                    accordionLinks.getPanes().add(tp);
                    tp = new TitledPane("Observations", createCollectionPaneFor(entity.observations().query()));
                    accordionLinks.getPanes().add(tp);
                } catch (IOException | ServiceFailureException ex) {
                    LOGGER.error("Failed to create panel.", ex);
                } catch (NullPointerException e) {
                    // Happens when entity is new.
                }
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
            if (entity == null) {
                return;
            }
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
        public EntityType getType() {
            return EntityType.FEATURE_OF_INTEREST;
        }

        @Override
        public void init(SensorThingsService service, FeatureOfInterest entity, GridPane gridProperties, Accordion accordionLinks, Label labelId, boolean editable) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textName = addFieldTo(gridProperties, i, "Name", new TextField(), false, editable);
            textDescription = addFieldTo(gridProperties, ++i, "Description", new TextArea(), true, editable);
            textEncodingType = addFieldTo(gridProperties, ++i, "EncodingType", new TextField(), false, editable);
            textFeature = addFieldTo(gridProperties, ++i, "Feature", new TextArea(), false, editable);

            if (accordionLinks != null) {
                try {
                    TitledPane tp = new TitledPane("Observations", createCollectionPaneFor(entity.observations().query()));
                    accordionLinks.getPanes().add(tp);
                } catch (NullPointerException e) {
                    // Happens when entity is new.
                }
            }
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
            if (entity == null) {
                return;
            }
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
        public EntityType getType() {
            return EntityType.HISTORICAL_LOCATION;
        }

        @Override
        public void init(SensorThingsService service, HistoricalLocation entity, GridPane gridProperties, Accordion accordionLinks, Label labelId, boolean editable) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textTime = addFieldTo(gridProperties, i, "Time", new TextField(), false, editable);

            if (accordionLinks != null) {
                try {
                    accordionLinks.getPanes().add(createEditableEntityPane(entity, entity.getThing(), service.things().query(), entity::setThing));
                    TitledPane tp = new TitledPane("Locations", createCollectionPaneFor(entity.locations().query()));
                    accordionLinks.getPanes().add(tp);
                } catch (IOException | ServiceFailureException ex) {
                    LOGGER.error("Failed to create panel.", ex);
                } catch (NullPointerException ex) {
                    // Happens when entity is new.
                }
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
            if (entity == null) {
                return;
            }
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
        public EntityType getType() {
            return EntityType.LOCATION;
        }

        @Override
        public void init(SensorThingsService service, Location entity, GridPane gridProperties, Accordion accordionLinks, Label labelId, boolean editable) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textName = addFieldTo(gridProperties, i, "Name", new TextField(), false, editable);
            textDescription = addFieldTo(gridProperties, ++i, "Description", new TextArea(), true, editable);
            textEncodingType = addFieldTo(gridProperties, ++i, "EncodingType", new TextField(), false, editable);
            textLocation = addFieldTo(gridProperties, ++i, "Location", new TextArea(), false, editable);

            if (accordionLinks != null) {
                try {
                    TitledPane tp = new TitledPane("Things", createCollectionPaneFor(entity.things().query()));
                    accordionLinks.getPanes().add(tp);
                    tp = new TitledPane("HistoricalLocations", createCollectionPaneFor(entity.historicalLocations().query()));
                    accordionLinks.getPanes().add(tp);
                } catch (NullPointerException e) {
                    // Happens when entity is new.
                }
            }
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
            if (entity == null) {
                return;
            }
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
        public EntityType getType() {
            return EntityType.OBSERVATION;
        }

        @Override
        public void init(SensorThingsService service, Observation entity, GridPane gridProperties, Accordion accordionLinks, Label labelId, boolean editable) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textPhenomenonTime = addFieldTo(gridProperties, i, "PhenomenonTime", new TextField(), false, editable);
            textResultTime = addFieldTo(gridProperties, ++i, "ResultTime", new TextField(), false, editable);
            textResult = addFieldTo(gridProperties, ++i, "Result", new TextArea(), true, editable);
            textResultQuality = addFieldTo(gridProperties, ++i, "ResultQuality", new TextField(), false, editable);
            textValidTime = addFieldTo(gridProperties, ++i, "ValidTime", new TextField(), false, editable);
            textParameters = addFieldTo(gridProperties, ++i, "Parameters", new TextArea(), true, editable);

            if (accordionLinks != null) {
                try {
                    accordionLinks.getPanes().add(createEditableEntityPane(entity, entity.getDatastream(), service.datastreams().query(), entity::setDatastream));
                    accordionLinks.getPanes().add(createEditableEntityPane(entity, entity.getMultiDatastream(), service.multiDatastreams().query(), entity::setMultiDatastream));
                    accordionLinks.getPanes().add(createEditableEntityPane(entity, entity.getFeatureOfInterest(), service.featuresOfInterest().query(), entity::setFeatureOfInterest));
                } catch (IOException | ServiceFailureException ex) {
                    LOGGER.error("Failed to create panel.", ex);
                }
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
            if (entity == null) {
                return;
            }
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
        public EntityType getType() {
            return EntityType.OBSERVED_PROPERTY;
        }

        @Override
        public void init(SensorThingsService service, ObservedProperty entity, GridPane gridProperties, Accordion accordionLinks, Label labelId, boolean editable) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textName = addFieldTo(gridProperties, i, "Name", new TextField(), false, editable);
            textDefinition = addFieldTo(gridProperties, ++i, "Definition", new TextField(), false, editable);
            textDescription = addFieldTo(gridProperties, ++i, "Description", new TextArea(), true, editable);

            if (accordionLinks != null) {
                try {
                    TitledPane tp = new TitledPane("Datastreams", createCollectionPaneFor(entity.datastreams().query()));
                    accordionLinks.getPanes().add(tp);
                    tp = new TitledPane("MultiDatastreams", createCollectionPaneFor(entity.multiDatastreams().query()));
                    accordionLinks.getPanes().add(tp);
                } catch (NullPointerException e) {
                    // Happens when entity is new.
                }
            }
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
            if (entity == null) {
                return;
            }
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
        public EntityType getType() {
            return EntityType.SENSOR;
        }

        @Override
        public void init(SensorThingsService service, Sensor entity, GridPane gridProperties, Accordion accordionLinks, Label labelId, boolean editable) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textName = addFieldTo(gridProperties, i, "Name", new TextField(), false, editable);
            textDescription = addFieldTo(gridProperties, ++i, "Description", new TextArea(), true, editable);
            textEncodingType = addFieldTo(gridProperties, ++i, "EncodingType", new TextField(), false, editable);
            textMetadata = addFieldTo(gridProperties, ++i, "Metadata", new TextArea(), true, editable);

            if (accordionLinks != null) {
                try {
                    TitledPane tp = new TitledPane("Datastreams", createCollectionPaneFor(entity.datastreams().query()));
                    accordionLinks.getPanes().add(tp);
                    tp = new TitledPane("MultiDatastreams", createCollectionPaneFor(entity.multiDatastreams().query()));
                    accordionLinks.getPanes().add(tp);
                } catch (NullPointerException e) {
                    // Happens when entity is new.
                }
            }
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
            if (entity == null) {
                return;
            }
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
        public EntityType getType() {
            return EntityType.THING;
        }

        @Override
        public void init(SensorThingsService service, Thing entity, GridPane gridProperties, Accordion accordionLinks, Label labelId, boolean editable) {
            this.labelId = labelId;
            this.entity = entity;
            int i = 0;
            textName = addFieldTo(gridProperties, i, "Name", new TextField(), false, editable);
            textDescription = addFieldTo(gridProperties, ++i, "Description", new TextArea(), true, editable);
            textProperties = addFieldTo(gridProperties, ++i, "Properties", new TextArea(), true, editable);

            if (accordionLinks != null) {
                try {
                    TitledPane tp = new TitledPane("Datastreams", createCollectionPaneFor(entity.datastreams().query()));
                    accordionLinks.getPanes().add(tp);
                    tp = new TitledPane("MultiDatastreams", createCollectionPaneFor(entity.multiDatastreams().query()));
                    accordionLinks.getPanes().add(tp);
                    ChildSetter<Location> locationChildSetter = new ChildSetter<Location>() {
                        @Override
                        public void setChild(Location child) {
                            throw new UnsupportedOperationException("This setter only deals with multiple childs.");
                        }

                        @Override
                        public void setChildren(List<Location> children) {
                            EntityList<Location> locations = entity.getLocations();
                            locations.clear();
                            for (Location child : children) {
                                locations.add(child.withOnlyId());
                            }
                        }
                    };
                    tp = new TitledPane("Locations", createCollectionPaneFor(entity.locations().query(), true, locationChildSetter));
                    accordionLinks.getPanes().add(tp);
                    tp = new TitledPane("HistoricalLocations", createCollectionPaneFor(entity.historicalLocations().query()));
                    accordionLinks.getPanes().add(tp);
                } catch (NullPointerException e) {
                    // Happens when entity is new.
                }
            }
        }
    }

    public static interface ChildSetter<C extends Entity<C>> {

        public void setChild(C child);

        default public void setChildren(List<C> children) {
            // Does nothing by default.
        }
    }

    public static <C extends Entity<C>, P extends Entity<P>> TitledPane createEditableEntityPane(
            final P parentEntity,
            final C childEntity,
            final Query<C> childQuery,
            final ChildSetter<C> setter) throws IOException {

        EntityType type = EntityType.singleForClass(childQuery.getEntityType().getType());
        String paneTitle;
        if (childEntity == null) {
            paneTitle = type.getName() + ": None selected";
        } else {
            paneTitle = childEntity.getType().getName() + ": " + childEntity.toString();
        }
        Node pane = FactoryEntityPanel.getPane(childQuery.getService(), type, childEntity, false);
        TitledPane tp = new TitledPane(paneTitle, pane);
        Button edit = new Button("🔧");
        tp.setGraphic(edit);
        edit.setOnAction((ActionEvent event) -> {
            Optional<List<C>> result = entitySearchDialog(childQuery, false);
            if (result.isPresent() && !result.get().isEmpty()) {
                C newChild = result.get().get(0);
                setter.setChild(newChild);
                try {
                    tp.setContent(FactoryEntityPanel.getPane(childQuery.getService(), type, childEntity, false));
                } catch (IOException ex) {
                    LoggerFactory.getLogger(EntityGuiController.class).error("Failed to load Collection Pane.", ex);
                }
                tp.setText(newChild.getType().getName() + ": " + newChild.toString());
            }
        });

        return tp;
    }

    public static <T extends Node> T addFieldTo(GridPane gp, int row, String title, T node, boolean fillHeight, boolean editable) {
        gp.getRowConstraints().add(new RowConstraints(Region.USE_PREF_SIZE, Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE, Priority.NEVER, VPos.BASELINE, fillHeight));
        gp.add(new Label(title), 0, row);
        gp.add(node, 1, row);
        if (node instanceof TextArea) {
            ((TextArea) node).setPrefRowCount(4);
        }
        if (node instanceof TextInputControl) {
            ((TextInputControl) node).setEditable(editable);
        } else {
            node.setDisable(!editable);
        }
        return node;
    }

    public static <C extends Entity<C>> Pane createCollectionPaneFor(Query<C> query) {
        return createCollectionPaneFor(query, false, null);
    }

    public static <C extends Entity<C>> Pane createCollectionPaneFor(Query<C> query, boolean canLinkNew, ChildSetter<C> childSetter) {
        try {
            FXMLLoader loader = new FXMLLoader(EntityGuiController.class.getResource("/fxml/Collection.fxml"));
            AnchorPane content = (AnchorPane) loader.load();
            ControllerCollection controller = loader.<ControllerCollection>getController();
            controller.setQuery(query, true, canLinkNew, false);
            if (childSetter != null) {
                controller.setChildSetter(childSetter);
            }
            return content;
        } catch (IOException ex) {
            LoggerFactory.getLogger(EntityGuiController.class).error("Failed to load Collection Pane.", ex);
        }
        return null;
    }

    public static <T extends Entity<T>> Optional<List<T>> entitySearchDialog(Query<T> query, boolean multiSelect) {
        try {
            FXMLLoader loader = new FXMLLoader(EntityGuiController.class.getResource("/fxml/Collection.fxml"));
            AnchorPane content = (AnchorPane) loader.load();
            final ControllerCollection<T> controller = loader.<ControllerCollection<T>>getController();
            controller.setQuery(query, false, false, multiSelect);

            Dialog<List<T>> dialog = new Dialog<>();
            dialog.setHeight(800);
            if (multiSelect) {
                dialog.setTitle("Choose one or more " + EntityType.singleForClass(query.getEntityType().getType()).getName());
            } else {
                dialog.setTitle("Choose a " + EntityType.singleForClass(query.getEntityType().getType()).getName());
            }
            dialog.setResizable(true);
            dialog.getDialogPane().setContent(content);
            ButtonType buttonTypeOk = new ButtonType("Set", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);
            ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().add(buttonTypeCancel);
            dialog.setResultConverter((ButtonType button) -> {
                if (button == buttonTypeOk) {
                    if (multiSelect) {
                        return controller.getSelectedEntities();
                    }
                    List<T> list = new ArrayList<>();
                    list.add(controller.getSelectedEntity());
                    return list;
                }
                return null;
            });
            return dialog.showAndWait();

        } catch (IOException ex) {
            LoggerFactory.getLogger(EntityGuiController.class).error("Failed to load Tab.", ex);
            return Optional.empty();
        }
    }
}
