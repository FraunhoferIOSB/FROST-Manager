package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Entity;
import de.fraunhofer.iosb.ilt.sta.model.Id;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;
import de.fraunhofer.iosb.ilt.sta.query.Query;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
 * @param <T> The entity type of the collection.
 */
public class ControllerCollection<T extends Entity<T>> implements Initializable {

    public static interface EntityFactory<T extends Entity<T>> {

        public T createEntity();
    }
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

    private String orderby = "";
    @FXML
    private BorderPane paneSelected;
    @FXML
    private TableView<EntityListEntry<T>> entityTable;
    @FXML
    private TableColumn<EntityListEntry<T>, String> columnId;
    @FXML
    private TableColumn<EntityListEntry<T>, String> columnName;

    private final ObservableList<EntityListEntry<T>> entities = FXCollections.observableArrayList();
    private EntityList<T> currentQueryList;

    private Query<T> query;
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
    private EntityGuiController.ChildSetter<T> childSetter;
    private boolean canMultiSelect = false;
    private EntityFactory<T> entityFactory;
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
        currentQueryList.fetchNext();
        loadEntities();
    }

    @FXML
    private void actionButtonAll(ActionEvent event) {
        try {
            addOptionsToQuery();
            currentQueryList = query.top(500).list();
            loadAllEntities();
        } catch (ServiceFailureException ex) {
            LOGGER.error("Failed to fetch entity list.", ex);
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
    private void actionDelete(ActionEvent event) {
        if (!canDelete) {
            return;
        }
        ObservableList<EntityListEntry<T>> selectedItems = entityTable.getSelectionModel().getSelectedItems();
        List<EntityListEntry<T>> toDelete = new ArrayList<>();
        for (EntityListEntry<T> selectedItem : selectedItems) {
            toDelete.add(selectedItem);
        }
        String what = selectedItems.size() == 1 ? "Item " + selectedItems.get(0).getEntity().getId().toString() : "all " + selectedItems.size() + " items";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + what + " ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            try {
                // ToDo: Add progress dialog.
                for (EntityListEntry<T> selectedItem : toDelete) {
                    Entity entity = selectedItem.getEntity();
                    if (entity.getId() == null) {
                        // entity doesn't exist yet.
                        entities.remove(selectedItem);
                    } else {
                        LOGGER.info("Deleting " + entity);
                        entity.getService().delete(entity);
                        entities.remove(selectedItem);
                    }
                }
            } catch (ServiceFailureException ex) {
                LOGGER.warn("Failed to delete entity.", ex);
            }
        }
    }

    @FXML
    private void actionNew(ActionEvent event) {
        if (!canCreate) {
            return;
        }
        EntityListEntry newItem = new EntityListEntry().setEntity(entityFactory.createEntity());
        entities.add(newItem);
        entityTable.getSelectionModel().select(newItem);
    }

    @FXML
    private void actionAdd(ActionEvent event) {
        if (childSetter == null) {
            new Alert(Alert.AlertType.ERROR, "No childSetter defined.", ButtonType.CLOSE).showAndWait();
            return;
        }
        Class<T> entityClass = query.getEntityClass();
        Query<T> allQuery = new Query<>(query.getService(), entityClass);
        Optional<List<T>> result = EntityGuiController.entitySearchDialog(allQuery, true, orderby);
        if (result.isPresent() && !result.get().isEmpty()) {
            List<T> newChildren = result.get();
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
        for (T entity : currentQueryList) {
            entities.add(new EntityListEntry<T>().setEntity(entity));
        }
        buttonNext.setDisable(!currentQueryList.hasNextLink());
        buttonDelete.setDisable(true);
    }

    private void loadAllEntities() {
        int i = 0;
        entities.clear();
        for (Iterator<T> it = currentQueryList.fullIterator(); it.hasNext();) {
            T entity = it.next();
            entities.add(new EntityListEntry<T>().setEntity(entity));
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
        buttonNext.setDisable(!currentQueryList.hasNextLink());
        buttonDelete.setDisable(true);
    }

    private void entitySelected(EntityListEntry<T> newValue) {
        if (newValue == null) {
            buttonDelete.setDisable(true);
            return;
        }
        try {
            T entity = newValue.getEntity();
            Node pane = FactoryEntityPanel.getPane(query.getService(), entity.getType(), entity, showNavigationProperties);
            paneSelected.setCenter(pane);
            buttonDelete.setDisable(false);
        } catch (IOException ex) {
            LOGGER.error("Failed to create pane for entity.", ex);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        columnId.setCellValueFactory(
                (TableColumn.CellDataFeatures<EntityListEntry<T>, String> param)
                -> new ReadOnlyObjectWrapper<>(param.getValue().getEntity().getId().toString()));
        columnName.setCellValueFactory(
                (TableColumn.CellDataFeatures<EntityListEntry<T>, String> param) -> {
                    T entity = param.getValue().getEntity();
                    Id id = entity.getId();
                    String entityString = entity.toString();
                    if (entityString.startsWith(id.toString())) {
                        entityString = entityString.substring(id.toString().length()).trim();
                    }
                    return new ReadOnlyObjectWrapper<>(entityString);
                });
        entityTable.setItems(entities);
        entityTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        entityTable.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends EntityListEntry<T>> observable, EntityListEntry<T> oldValue, EntityListEntry<T> newValue) -> {
                    entitySelected(newValue);
                });
    }

    /**
     * @return the query
     */
    public Query getQuery() {
        return query;
    }

    /**
     * @param query the query to set.
     * @param entityFactory The factory to use to generate new entities.
     * @param orderBy The ordering to use.
     * @return this ControllerCollection.
     */
    public ControllerCollection setQuery(Query query, EntityFactory<T> entityFactory, String orderBy) {
        this.query = query;
        this.canCreate = true;
        this.canDelete = true;
        this.orderby = orderBy;
        this.entityFactory = entityFactory;
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
     * @return this ControllerCollection.
     */
    public ControllerCollection setQuery(Query query, boolean showNavigationProperties, boolean canDelete, boolean canLinkNew, boolean multiSelect, String orderBy) {
        this.query = query;
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

    public void setChildSetter(EntityGuiController.ChildSetter<T> childSetter) {
        this.childSetter = childSetter;
    }

    /**
     * @return the currently selected entity for single-select mode, or the last
     * selected entity for multi select mode.
     */
    public T getSelectedEntity() {
        EntityListEntry<T> item = entityTable.getSelectionModel().getSelectedItem();
        if (item == null) {
            return null;
        }
        return item.getEntity();
    }

    /**
     * @return the currently selected entity for single-select mode, or the last
     * selected entity for multi select mode.
     */
    public List<T> getSelectedEntities() {
        ObservableList<EntityListEntry<T>> items = entityTable.getSelectionModel().getSelectedItems();
        if (items == null) {
            return null;
        }
        List<T> values = new ArrayList<>();
        for (EntityListEntry<T> item : items) {
            values.add(item.getEntity());
        }
        return values;
    }
}
