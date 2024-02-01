package de.fraunhofer.iosb.ilt.sensorthingsmanager.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.ilt.frostclient.SensorThingsService;
import de.fraunhofer.iosb.ilt.frostclient.model.ComplexValue;
import de.fraunhofer.iosb.ilt.frostclient.model.Entity;
import de.fraunhofer.iosb.ilt.frostclient.model.EntityType;
import de.fraunhofer.iosb.ilt.frostclient.model.Property;
import de.fraunhofer.iosb.ilt.frostclient.model.PropertyType;
import de.fraunhofer.iosb.ilt.frostclient.model.property.EntityProperty;
import de.fraunhofer.iosb.ilt.frostclient.model.property.EntityPropertyMain;
import de.fraunhofer.iosb.ilt.frostclient.model.property.NavigationProperty;
import de.fraunhofer.iosb.ilt.frostclient.model.property.NavigationPropertyEntitySet;
import de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypeComplex;
import de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypePrimitive;
import static de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypePrimitive.EDM_STRING_NAME;
import de.fraunhofer.iosb.ilt.frostclient.models.ext.MapValue;
import de.fraunhofer.iosb.ilt.frostclient.query.Query;
import static de.fraunhofer.iosb.ilt.frostclient.utils.ParserUtils.formatKeyValuesForUrl;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.ObjectMapperFactory;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public interface EntityGuiController {

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
    public void init(SensorThingsService service, Entity entity, GridPane gridProperties, Accordion accordionLinks, Label labelId, boolean editable);

    public static class GuiControllerDefault implements EntityGuiController {

        /**
         * The logger for this class.
         */
        private static final Logger LOGGER = LoggerFactory.getLogger(GuiControllerDefault.class);
        private Label labelId;
        private final EntityType entityType;
        private Entity entity;
        private final List<PropertyGuiGlue> controls = new ArrayList<>();
        private final AtomicInteger itemCount = new AtomicInteger();

        public GuiControllerDefault(EntityType entityType) {
            this.entityType = entityType;
        }

        @Override
        public void loadFields() {
            if (entity == null) {
                return;
            }
            if (entity.primaryKeyFullySet()) {
                labelId.setText(formatKeyValuesForUrl(entity));
            }
            for (PropertyGuiGlue control : controls) {
                control.entityToGui();
            }
        }

        @Override
        public void saveFields() {
            for (PropertyGuiGlue control : controls) {
                control.guiToEntity();
            }
        }

        @Override
        public EntityType getType() {
            return entity.getEntityType();
        }

        @Override
        public void init(SensorThingsService service, Entity entity, GridPane gridProperties, Accordion accordionLinks, Label labelId, boolean editable) {
            this.labelId = labelId;
            this.entity = entity;
            EntityType entityType = entity.getEntityType();
            Set<EntityPropertyMain> entityProperties = entityType.getEntityProperties();
            for (EntityPropertyMain ep : entityProperties) {
                addGuiElement(ep.getJsonName(), ep, editable, gridProperties);
            }

            if (accordionLinks != null) {
                try {
                    for (NavigationProperty<Entity> npe : entityType.getNavigationEntities()) {
                        TitledPane tp = createEditableEntityPane(entity, entity.getProperty(npe), service.query(npe.getEntityType()), "name asc", child -> entity.setProperty(npe, child));
                        accordionLinks.getPanes().add(tp);
                    }
                    for (NavigationPropertyEntitySet nps : entityType.getNavigationSets()) {
                        Pane pane = createCollectionPaneFor(entity.query(nps), "");
                        accordionLinks.getPanes().add(new TitledPane(nps.getName(), pane));
                    }
                } catch (IOException ex) {
                    LOGGER.error("Failed to create panel.", ex);
                } catch (NullPointerException e) {
                    // Happens when entity is new.
                    LOGGER.trace("Failed to create panel.", e);
                }
            }
        }

        private void addGuiElement(String name, EntityPropertyMain property, boolean editable, GridPane gridProperties) {
            PropertyGuiGlue item = PropertyGuiGlue.createGuiElement(entity, property, editable, gridProperties, itemCount);
            PropertyType pt = property.getType();
            controls.add(item);
        }
    }

    public static interface PropertyGuiGlue {

        /**
         * Read the value from the Entity and push it to the GUI element.
         */
        public void entityToGui();

        /**
         * Read the value from the Gui and push it to the Entity.
         */
        public void guiToEntity();

        private static PropertyGuiGlue createGuiElement(ComplexValue entity, EntityProperty property, boolean editable, GridPane gridProperties, AtomicInteger itemCount) {
            PropertyType pt = property.getType();
            if (pt instanceof TypeComplex ptc) {
                if (ptc.isOpenType()) {
                    return new GuiGlueOpenType(entity, property)
                            .init(gridProperties, itemCount, editable);
                } else {
                    return new GuiGlueComplex(entity, property)
                            .init(gridProperties, itemCount, editable);
                }
            } else if (pt instanceof TypePrimitive ptsp) {
                switch (ptsp.getName()) {
                    case EDM_STRING_NAME:
                    default:
                        return new GuiGlueSimpleString(entity, property)
                                .init(gridProperties, itemCount, editable);
                }
            }
            return new GuiGlueSimpleString(entity, property)
                    .init(gridProperties, itemCount, editable);
        }

    }

    public static class GuiGlueComplex implements PropertyGuiGlue {

        private final ComplexValue<? extends ComplexValue> entity;
        private final EntityProperty<ComplexValue> property;
        private final Map<String, PropertyGuiGlue> subProperties = new HashMap<>();
        private ComplexValue value;

        public GuiGlueComplex(ComplexValue entity, EntityProperty<ComplexValue> property) {
            this.entity = entity;
            this.property = property;
        }

        public GuiGlueComplex init(GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
            return init("", gridProperties, itemCount, editable);
        }

        public GuiGlueComplex init(String namePrefix, GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
            PropertyType pt = property.getType();
            if (pt instanceof TypeComplex ptc) {
                value = entity.getProperty(property);
                if (value == null) {
                    // TODO: only instantiate when not empty...
                    value = ptc.instantiate();
                    entity.setProperty(property, value);
                }
                for (Property subProperty : ptc.getProperties()) {
                    if (subProperty instanceof EntityProperty ep) {
                        PropertyGuiGlue subItem = PropertyGuiGlue.createGuiElement(value, ep, editable, gridProperties, itemCount);
                        subProperties.put(subProperty.getJsonName(), subItem);
                    }
                }
            }
            return this;
        }

        @Override
        public void entityToGui() {
            for (PropertyGuiGlue subProp : subProperties.values()) {
                subProp.entityToGui();
            }
        }

        @Override
        public void guiToEntity() {
            for (PropertyGuiGlue subProp : subProperties.values()) {
                subProp.guiToEntity();
            }
        }
    }

    public static class GuiGlueOpenType implements PropertyGuiGlue {

        private static final Logger LOGGER = LoggerFactory.getLogger(GuiGlueOpenType.class);

        private final ComplexValue<MapValue> entity;
        private final EntityProperty<MapValue> property;
        private TextArea field;

        public GuiGlueOpenType(ComplexValue entity, EntityProperty<MapValue> property) {
            this.entity = entity;
            this.property = property;
        }

        public GuiGlueOpenType init(GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
            return init("", gridProperties, itemCount, editable);
        }

        public GuiGlueOpenType init(String namePrefix, GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
            field = addFieldTo(gridProperties, itemCount.getAndIncrement(), namePrefix + property.getName(), new TextArea(), true, editable);
            return this;
        }

        @Override
        public void entityToGui() {
            final ObjectMapper mapper = ObjectMapperFactory.get();
            try {
                final MapValue value = entity.getProperty(property);
                if (value != null) {
                    String textValue = mapper.writeValueAsString(value);
                    field.setText(textValue);
                }
            } catch (JsonProcessingException ex) {
                LOGGER.error("Properties can not be converted to JSON.", ex);
            }

        }

        @Override
        public void guiToEntity() {
            final ObjectMapper mapper = ObjectMapperFactory.get();
            try {
                final String textInput = field.getText();
                if (!Utils.isNullOrEmpty(textInput)) {
                    MapValue properties = mapper.readValue(textInput, MapValue.class);
                    entity.setProperty(property, properties);
                }
            } catch (IOException ex) {
                LOGGER.error("Not valid json.", ex);
            }
        }
    }

    public static class GuiGlueSimpleString implements PropertyGuiGlue {

        private final ComplexValue<?> entity;
        private final EntityProperty<String> property;
        private TextField field;

        public GuiGlueSimpleString(ComplexValue entity, EntityProperty<String> property) {
            this.entity = entity;
            this.property = property;
        }

        public GuiGlueSimpleString init(GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
            return init("", gridProperties, itemCount, editable);
        }

        public GuiGlueSimpleString init(String namePrefix, GridPane gridProperties, AtomicInteger itemCount, boolean editable) {
            field = addFieldTo(gridProperties, itemCount.getAndIncrement(), namePrefix + property, new TextField(), false, editable);
            return this;
        }

        @Override
        public void entityToGui() {
            field.setText(Objects.toString(entity.getProperty(property)));
        }

        @Override
        public void guiToEntity() {
            entity.setProperty(property, field.getText());
        }

    }

    public static interface ChildSetter {

        public void setChild(Entity child);

        default public void setChildren(List<Entity> children) {
            // Does nothing by default.
        }
    }

    public static TitledPane createEditableEntityPane(
            final Entity parentEntity,
            final Entity childEntity,
            final Query childQuery,
            String orderby,
            final ChildSetter setter) throws IOException {

        EntityType type = childQuery.getEntityType();
        String paneTitle;
        if (childEntity == null) {
            paneTitle = type.getEntityName() + ": None selected";
        } else {
            paneTitle = childEntity.getEntityType().getEntityName() + ": " + childEntity.toString();
        }
        Node pane = FactoryEntityPanel.getPane(childQuery.getService(), type, childEntity, false);
        TitledPane tp = new TitledPane(paneTitle, pane);
        Button edit = new Button("🔧");
        tp.setGraphic(edit);
        edit.setOnAction((ActionEvent event) -> {
            Optional<List<Entity>> result = entitySearchDialog(childQuery, false, orderby);
            if (result.isPresent() && !result.get().isEmpty()) {
                Entity newChild = result.get().get(0);
                setter.setChild(newChild);
                try {
                    tp.setContent(FactoryEntityPanel.getPane(childQuery.getService(), type, childEntity, false));
                } catch (IOException ex) {
                    LoggerFactory.getLogger(EntityGuiController.class).error("Failed to load Collection Pane.", ex);
                }
                tp.setText(newChild.getEntityType().getEntityName() + ": " + newChild.toString());
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

    public static Pane createCollectionPaneFor(Query query, String orderBy) {
        return createCollectionPaneFor(query, orderBy, false, null);
    }

    public static Pane createCollectionPaneFor(Query query, String orderBy, boolean canLinkNew, ChildSetter childSetter) {
        try {
            FXMLLoader loader = new FXMLLoader(EntityGuiController.class.getResource("/fxml/Collection.fxml"));
            AnchorPane content = (AnchorPane) loader.load();
            ControllerCollection controller = loader.<ControllerCollection>getController();
            controller.setQuery(query, true, true, canLinkNew, true, orderBy);
            if (childSetter != null) {
                controller.setChildSetter(childSetter);
            }
            return content;
        } catch (IOException ex) {
            LoggerFactory.getLogger(EntityGuiController.class).error("Failed to load Collection Pane.", ex);
        }
        return null;
    }

    public static Optional<List<Entity>> entitySearchDialog(Query query, boolean multiSelect, String orderBy) {
        try {
            FXMLLoader loader = new FXMLLoader(EntityGuiController.class.getResource("/fxml/Collection.fxml"));
            AnchorPane content = (AnchorPane) loader.load();
            final ControllerCollection controller = loader.<ControllerCollection>getController();
            controller.setQuery(query, false, false, false, multiSelect, orderBy);

            Dialog<List<Entity>> dialog = new Dialog<>();
            dialog.setHeight(800);
            if (multiSelect) {
                dialog.setTitle("Choose one or more " + query.getEntityType().getEntityName());
            } else {
                dialog.setTitle("Choose a " + query.getEntityType().getEntityName());
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
                    List<Entity> list = new ArrayList<>();
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
