package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Optional;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Entity;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;
import de.fraunhofer.iosb.ilt.sta.query.Query;
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
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;

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
    private BorderPane paneSelected;
    @FXML
    private ListView<EntityListEntry<T>> entityList;
    private final ObservableList<EntityListEntry<T>> entities = FXCollections.observableArrayList();
    private EntityList<T> currentQueryList;

    private Query query;
    private boolean canEdit = false;
    private EntityFactory<T> entityFactory;
    /**
     * Should navigation properties of the selected Entity be shown, or just
     * entityProperties.
     */
    private boolean showNavigationProperties = true;

    @FXML
    private void actionButtonReload(ActionEvent event) {
        try {
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
            currentQueryList = query.top(500).list();
            loadAllEntities();
        } catch (ServiceFailureException ex) {
            LOGGER.error("Failed to fetch entity list.", ex);
        }
    }

    @FXML
    private void actionDelete(ActionEvent event) {
        if (!canEdit) {
            return;
        }
        EntityListEntry selectedItem = entityList.getSelectionModel().getSelectedItem();
        Entity entity = selectedItem.getEntity();

        if (entity.getId() == null) {
            // entity doesn't exist yet.
            entities.remove(selectedItem);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + entity.getId().toString() + " ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            try {
                entity.getService().delete(entity);
                entities.remove(selectedItem);
            } catch (ServiceFailureException ex) {
                LOGGER.warn("Failed to delete entity.", ex);
            }
        }
    }

    @FXML
    private void actionNew(ActionEvent event) {
        if (!canEdit) {
            return;
        }
        EntityListEntry newItem = new EntityListEntry().setEntity(entityFactory.createEntity());
        entities.add(newItem);
        entityList.getSelectionModel().select(newItem);
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
        entityList.setItems(entities);
        entityList.getSelectionModel().selectedItemProperty().addListener(
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
     * @return this ControllerCollection.
     */
    public ControllerCollection setQuery(Query query, EntityFactory<T> entityFactory) {
        this.query = query;
        this.canEdit = true;
        this.entityFactory = entityFactory;
        buttonDelete.setVisible(canEdit);
        buttonNew.setVisible(canEdit);
        return this;
    }

    /**
     * @param query the query to set.
     * @param showNavigationProperties Should navigation properties of the
     * selected Entity be shown, or just entityProperties.
     * @return this ControllerCollection.
     */
    public ControllerCollection setQuery(Query query, boolean showNavigationProperties) {
        this.query = query;
        this.canEdit = false;
        this.showNavigationProperties = showNavigationProperties;
        buttonDelete.setVisible(canEdit);
        buttonNew.setVisible(canEdit);
        return this;
    }

    public T getSelectedEntity() {
        EntityListEntry<T> item = entityList.getSelectionModel().getSelectedItem();
        if (item == null) {
            return null;
        }
        return item.getEntity();
    }
}
