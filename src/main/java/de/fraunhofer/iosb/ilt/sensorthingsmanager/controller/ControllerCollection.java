package de.fraunhofer.iosb.ilt.sensorthingsmanager.controller;

import de.fraunhofer.iosb.ilt.frostclient.SensorThingsService;
import de.fraunhofer.iosb.ilt.frostclient.exception.ServiceFailureException;
import de.fraunhofer.iosb.ilt.frostclient.exception.StatusCodeException;
import de.fraunhofer.iosb.ilt.frostclient.model.Entity;
import de.fraunhofer.iosb.ilt.frostclient.model.EntitySet;
import de.fraunhofer.iosb.ilt.frostclient.model.EntityType;
import de.fraunhofer.iosb.ilt.frostclient.model.PrimaryKey;
import de.fraunhofer.iosb.ilt.frostclient.model.property.EntityPropertyMain;
import de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypePrimitive;
import static de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsSensingV11.EP_NAME;
import de.fraunhofer.iosb.ilt.frostclient.query.Query;
import de.fraunhofer.iosb.ilt.frostclient.utils.ParserUtils;
import de.fraunhofer.iosb.ilt.frostclient.utils.StringHelper;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.controller.gui.helper.ChildSetter;
import static de.fraunhofer.iosb.ilt.sensorthingsmanager.controller.gui.helper.entitySearchDialog;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.Utils;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scf
 */
public class ControllerCollection implements Initializable {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerCollection.class);
    @FXML
    private Button buttonReload;
    @FXML
    private Button buttonNext;
    @FXML
    private Button buttonAll;
    @FXML
    private Button buttonDelete;
    @FXML
    private Button buttonNew;
    @FXML
    private Button buttonAdd;

    @FXML
    private ToggleButton buttonFilter;
    private String filter = "";

    @FXML
    private ToggleButton buttonSelect;
    private String select = "";

    @FXML
    private ToggleButton buttonOrder;
    private String orderby = "";

    @FXML
    private BorderPane paneSelected;

    @FXML
    private TableView<EntityListEntry> entityTable;

    @FXML
    private TableColumn<EntityListEntry, String> columnName;
    private TableColumn<EntityListEntry, ?> columnId;

    private final ObservableList<EntityListEntry> entities = FXCollections.observableArrayList();
    private EntitySet currentQueryList;

    private SensorThingsService service;
    private Query query;
    private EntityType entityType;

    /**
     * Can new entities be created.
     */
    private boolean canCreate = false;
    /**
     * Can entities be deleted.
     */
    private boolean canDelete = false;
    /**
     * Can existing entities not in this collection be linked into this
     * collection.
     */
    private boolean canLinkNew = false;
    /**
     * The setter used to link newly selected items to the owner of the
     * collection.
     */
    private ChildSetter childSetter;
    private boolean canMultiSelect = false;

    /**
     * Should navigation properties of the selected Entity be shown, or just
     * entityProperties.
     */
    private boolean showNavigationProperties = true;

    @FXML
    private void actionButtonReload(ActionEvent event) {
        try {
            addOptionsToQuery();
            currentQueryList = query.list();
            loadEntities();
        } catch (ServiceFailureException ex) {
            LOGGER.error("Failed to fetch entity list.", ex);
            Utils.showAlert(
                    Alert.AlertType.ERROR,
                    "Failed to reload",
                    "Reloading the set of entities failed.",
                    ex);
        } catch (IllegalArgumentException ex) {
            // Happens with new entities.
            LOGGER.trace("Failed to fetch entity list.", ex);
        }
    }

    @FXML
    private void actionButtonNext(ActionEvent event) {
        if (currentQueryList == null || !currentQueryList.hasNextLink()) {
            return;
        }
        try {
            currentQueryList.fetchNext();
        } catch (StatusCodeException ex) {
            LOGGER.error("Failed to fetch next set of entities.", ex);
            Utils.showAlert(
                    Alert.AlertType.ERROR,
                    "Failed to fetch",
                    "Fetching the next set of entities failed for url: " + ex.getUrl(),
                    ex);
        } catch (ServiceFailureException ex) {
            LOGGER.error("Failed to fetch entity list.", ex);
            Utils.showAlert(
                    Alert.AlertType.ERROR,
                    "Failed to fetch",
                    "Fetching the set of all entities failed.",
                    ex);
        }
        loadEntities();
    }

    @FXML
    private void actionButtonAll(ActionEvent event) {
        try {
            addOptionsToQuery();
            currentQueryList = query.top(500).list();
            loadAllEntities();
        } catch (StatusCodeException ex) {
            LOGGER.error("Failed to fetch entity list.", ex);
            Utils.showAlert(
                    Alert.AlertType.ERROR,
                    "Failed to fetch",
                    "Fetching the set of all entities failed for url: " + ex.getUrl(),
                    ex);
        } catch (ServiceFailureException ex) {
            LOGGER.error("Failed to fetch entity list.", ex);
            Utils.showAlert(
                    Alert.AlertType.ERROR,
                    "Failed to fetch",
                    "Fetching the set of all entities failed.",
                    ex);
        }
    }

    @FXML
    private void actionButtonFilter(ActionEvent event) {
        LOGGER.info("Filter button clicked.");
        if (buttonFilter.isSelected()) {
            TextInputDialog textInputDialog = new TextInputDialog(filter);
            textInputDialog.setHeaderText("Set filter");
            textInputDialog.setResizable(true);
            Optional<String> filterOptional = textInputDialog.showAndWait();
            if (filterOptional.isPresent()) {
                this.filter = filterOptional.get();
            } else {
                this.filter = "";
            }
        }
    }

    @FXML
    private void actionButtonSelect(ActionEvent event) {
        LOGGER.info("Select button clicked.");
        if (buttonSelect.isSelected()) {
            TextInputDialog textInputDialog = new TextInputDialog(select);
            textInputDialog.setHeaderText("Set select");
            textInputDialog.setResizable(true);
            Optional<String> selectOptional = textInputDialog.showAndWait();
            if (selectOptional.isPresent()) {
                this.select = selectOptional.get();
            } else {
                this.select = "";
            }
        }
    }

    @FXML
    private void actionButtonOrder(ActionEvent event) {
        LOGGER.info("Order button clicked.");
        if (buttonOrder.isSelected()) {
            TextInputDialog textInputDialog = new TextInputDialog(orderby);
            textInputDialog.setHeaderText("Set OrderBy");
            textInputDialog.setResizable(true);
            Optional<String> orderByOptional = textInputDialog.showAndWait();
            if (orderByOptional.isPresent()) {
                this.orderby = orderByOptional.get();
            } else {
                this.orderby = "";
            }
        }
    }

    @FXML
    private void actionDelete(ActionEvent event) {
        if (!canDelete) {
            return;
        }
        ObservableList<EntityListEntry> selectedItems = entityTable.getSelectionModel().getSelectedItems();
        List<EntityListEntry> toDelete = new ArrayList<>();
        for (EntityListEntry selectedItem : selectedItems) {
            toDelete.add(selectedItem);
        }
        String what = selectedItems.size() == 1 ? "Item " + ParserUtils.formatKeyValuesForUrl(selectedItems.get(0).getEntity()) : "all " + selectedItems.size() + " items";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + what + " ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            try {
                // ToDo: Add progress dialog.
                for (EntityListEntry selectedItem : toDelete) {
                    Entity entity = selectedItem.getEntity();
                    if (entity.primaryKeyFullySet()) {
                        LOGGER.info("Deleting " + entity);
                        entity.getService().delete(entity);
                        entities.remove(selectedItem);
                    } else {
                        // entity doesn't exist yet.
                        entities.remove(selectedItem);
                    }
                }
            } catch (ServiceFailureException ex) {
                LOGGER.warn("Failed to delete entity.", ex);
                Utils.showAlert(
                        Alert.AlertType.ERROR,
                        "Failed to delete",
                        "Failed to delete an entity.",
                        ex);
            }
        }
    }

    @FXML
    private void actionNew(ActionEvent event) {
        if (!canCreate) {
            return;
        }
        EntityListEntry newItem = new EntityListEntry().setEntity(new Entity(entityType));
        entities.add(newItem);
        entityTable.getSelectionModel().select(newItem);
    }

    @FXML
    private void actionAdd(ActionEvent event) {
        if (childSetter == null) {
            new Alert(Alert.AlertType.ERROR, "No childSetter defined.", ButtonType.CLOSE).showAndWait();
            return;
        }
        Query allQuery = service.query(entityType);
        Optional<List<Entity>> result = entitySearchDialog(allQuery, true, orderby);
        if (result.isPresent() && !result.get().isEmpty()) {
            List<Entity> newChildren = result.get();
            childSetter.setChildren(newChildren);
        }
    }

    private void addOptionsToQuery() {
        if (buttonFilter.isSelected() && !filter.isEmpty()) {
            query.filter(filter);
        } else {
            query.filter("");
        }
        if (buttonSelect.isSelected() && !select.isEmpty()) {
            query.select(select);
        } else {
            query.select();
        }
        if (!orderby.isEmpty()) {
            query.orderBy(orderby);
        }
    }

    public void setOrderby(String orderby) {
        this.orderby = orderby;
    }

    private void loadEntities() {
        entities.clear();
        for (Entity entity : currentQueryList.toList()) {
            entities.add(new EntityListEntry().setEntity(entity));
        }
        createIdColumn();
        buttonNext.setDisable(!currentQueryList.hasNextLink());
        buttonDelete.setDisable(true);
        entityTable.sort();
    }

    private void loadAllEntities() {
        int i = 0;
        entities.clear();
        for (Entity entity : currentQueryList) {
            entities.add(new EntityListEntry().setEntity(entity));
            i++;
            if (i >= 500) {
                LOGGER.warn("Warning after {}. Total: {}.", i, entities.size());
                Optional<ButtonType> result = new Alert(Alert.AlertType.WARNING, "Already loaded " + entities.size() + " Entities.\nContinue loading?", ButtonType.CANCEL, ButtonType.YES).showAndWait();
                if (!result.isPresent() || result.get() != ButtonType.YES) {
                    break;
                }
                i = 0;
            }
        }
        createIdColumn();
        buttonNext.setDisable(!currentQueryList.hasNextLink());
        buttonDelete.setDisable(true);
        entityTable.sort();
    }

    private void entitySelected(EntityListEntry newValue) {
        if (newValue == null) {
            buttonDelete.setDisable(true);
            return;
        }
        try {
            Entity entity = newValue.getEntity();
            Node pane = FactoryEntityPanel.getPane(service, entityType, entity, showNavigationProperties);
            paneSelected.setCenter(pane);
            buttonDelete.setDisable(false);
        } catch (IOException ex) {
            LOGGER.error("Failed to create pane for entity.", ex);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        columnName.setCellValueFactory((TableColumn.CellDataFeatures<EntityListEntry, String> param) -> {
            Entity entity = param.getValue().getEntity();
            if (entityType.hasProperty(EP_NAME)) {
                String name = entity.getProperty(EP_NAME);
                if (!StringHelper.isNullOrEmpty(name)) {
                    return new ReadOnlyObjectWrapper<>(name);
                }
            }
            for (EntityPropertyMain prop : entity.getEntityType().getEntityProperties()) {
                if (prop.getType() == TypePrimitive.EDM_STRING) {
                    Object propValue = entity.getProperty(prop);
                    return new ReadOnlyObjectWrapper<>(Objects.toString(propValue));
                }
            }
            return new ReadOnlyObjectWrapper<>(entity.getEntityType().entityName);
        });
        entityTable.setItems(entities);
        entityTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        entityTable.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends EntityListEntry> observable, EntityListEntry oldValue, EntityListEntry newValue) -> {
                    entitySelected(newValue);
                });
    }

    private void createIdColumn() {
        if (entities.isEmpty() || columnId != null) {
            return;
        }
        final Entity entity = entities.get(0).getEntity();
        if (!entity.primaryKeyFullySet()) {
            return;
        }
        final PrimaryKey pk = entity.getPrimaryKey();
        final List<EntityPropertyMain> keyProps = pk.getKeyProperties();
        final EntityPropertyMain keyProp0 = keyProps.get(0);
        boolean pkIsNumeric = false;

        String idColumnName = null;
        for (var prop : keyProps) {
            if (idColumnName == null) {
                idColumnName = "";
            } else {
                idColumnName += ",";
            }
            idColumnName += prop.getJsonName();
        }

        if (keyProps.size() == 1 && keyProp0.getType().getName().equals(TypePrimitive.EDM_UNTYPED_NAME)) {
            final Object pkVal = entity.getPrimaryKeyValues()[0];
            if (pkVal instanceof Number n) {
                pkIsNumeric = true;
            }
        } else if (keyProps.size() == 1 && keyProp0.getType().getName().startsWith("Edm.Int")) {
            pkIsNumeric = true;
        }
        if (pkIsNumeric) {
            TableColumn<EntityListEntry, Number> column = new TableColumn<>(idColumnName);
            columnId = column;
            column.setCellValueFactory((TableColumn.CellDataFeatures<EntityListEntry, Number> param) -> {
                final Object pkValue = param.getValue().getEntity().getPrimaryKeyValues()[0];
                if (pkValue == null) {
                    return new ReadOnlyObjectWrapper<>(-1);
                }
                return new ReadOnlyObjectWrapper<>((Number) pkValue);
            });

        } else {
            TableColumn<EntityListEntry, String> column = new TableColumn<>(idColumnName);
            columnId = column;
            column.setCellValueFactory((TableColumn.CellDataFeatures<EntityListEntry, String> param) -> {
                final Entity cellEntity = param.getValue().getEntity();
                if (!cellEntity.primaryKeyFullySet()) {
                    return new ReadOnlyObjectWrapper<>("");
                }
                String keyValue = ParserUtils.formatKeyValuesForUrl(cellEntity);
                return new ReadOnlyObjectWrapper<>(keyValue);
            });

        }
        entityTable.getColumns().add(0, columnId);
    }

    /**
     * @return the query
     */
    public Query getQuery() {
        return query;
    }

    /**
     * @param query the query to use to fetch entities. This includes the
     * service and the EntityType.
     * @param orderBy The ordering to use.
     * @return this ControllerCollection.
     */
    public ControllerCollection setQuery(Query query, String orderBy) {
        this.query = query;
        this.service = query.getService();
        this.entityType = query.getEntityType();
        this.canCreate = true;
        this.canDelete = true;
        this.orderby = orderBy;
        buttonDelete.setVisible(canDelete);
        buttonNew.setVisible(canCreate);
        buttonAdd.setVisible(canLinkNew);
        return this;
    }

    /**
     * @param query the query to set.
     * @param showNavigationProperties Should navigation properties of the
     * selected Entity be shown, or just entityProperties.
     * @param canDelete Can entities be deleted from this collection.
     * @param canLinkNew Can new entities be linked into this collection.
     * @param multiSelect Can more than one entity be selected.
     * @param orderBy The ordering to use for the query.
     * @return this ControllerCollection.
     */
    public ControllerCollection setQuery(Query query, boolean showNavigationProperties, boolean canDelete, boolean canLinkNew, boolean multiSelect, String orderBy) {
        this.query = query;
        this.service = query.getService();
        this.entityType = query.getEntityType();
        this.canCreate = false;
        this.canLinkNew = canLinkNew;
        this.canDelete = canDelete;
        this.showNavigationProperties = showNavigationProperties;
        this.canMultiSelect = multiSelect;
        this.orderby = orderBy;
        buttonDelete.setVisible(canDelete);
        buttonNew.setVisible(canCreate);
        buttonAdd.setVisible(canLinkNew);
        entityTable.getSelectionModel().setSelectionMode(canMultiSelect ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
        return this;
    }

    public void setChildSetter(ChildSetter childSetter) {
        this.childSetter = childSetter;
    }

    /**
     * @return the currently selected entity for single-select mode, or the last
     * selected entity for multi select mode.
     */
    public Entity getSelectedEntity() {
        EntityListEntry item = entityTable.getSelectionModel().getSelectedItem();
        if (item == null) {
            return null;
        }
        return item.getEntity();
    }

    /**
     * @return the currently selected entity for single-select mode, or the last
     * selected entity for multi select mode.
     */
    public List<Entity> getSelectedEntities() {
        ObservableList<EntityListEntry> items = entityTable.getSelectionModel().getSelectedItems();
        if (items == null) {
            return null;
        }
        List<Entity> values = new ArrayList<>();
        items.stream().forEach(i -> values.add(i.getEntity()));
        return values;
    }
}
