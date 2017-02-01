package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Entity;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;
import de.fraunhofer.iosb.ilt.sta.query.Query;
import javafx.beans.value.ChangeListener;
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
    private BorderPane paneSelected;
    @FXML
    private ListView<EntityListEntry> entityList;
    private final ObservableList<EntityListEntry> entities = FXCollections.observableArrayList();
    private EntityList<? extends Entity<?>> currentQueryList;

    private Query query;

    @FXML
    private void actionButtonReload(ActionEvent event) {
        try {
            currentQueryList = query.list();
            loadEntities();
        } catch (ServiceFailureException ex) {
            LOGGER.error("Failed to fetch entity list.", ex);
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
            currentQueryList = query.list();
            loadAllEntities();
        } catch (ServiceFailureException ex) {
            LOGGER.error("Failed to fetch entity list.", ex);
        }
    }

    @FXML
    private void actionDelete(ActionEvent event) {
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

    private void loadEntities() {
        entities.clear();
        for (Entity<?> entity : currentQueryList) {
            entities.add(new EntityListEntry().setEntity(entity));
        }
        buttonNext.setDisable(!currentQueryList.hasNextLink());
        buttonDelete.setDisable(true);
    }

    private void loadAllEntities() {
        int i = 0;
        entities.clear();
        for (Iterator<? extends Entity<?>> it = currentQueryList.fullIterator(); it.hasNext();) {
            Entity<?> entity = it.next();
            entities.add(new EntityListEntry().setEntity(entity));
            i++;
            if (i >= 500) {
                LOGGER.warn("Aborted loading all entities after {}.", entities.size());
                break;
            }
        }
        buttonNext.setDisable(!currentQueryList.hasNextLink());
        buttonDelete.setDisable(true);
    }

    private void entitySelected(EntityListEntry newValue) {
        if (newValue == null) {
            buttonDelete.setDisable(true);
            return;
        }
        try {
            Node pane = FactoryEntityPanel.getPane(newValue.getEntity());
            paneSelected.setCenter(pane);
            buttonDelete.setDisable(false);
        } catch (IOException ex) {
            LOGGER.error("Failed to create pane for entity.", ex);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        entityList.setItems(entities);
        entityList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<EntityListEntry>() {
            @Override
            public void changed(ObservableValue<? extends EntityListEntry> observable, EntityListEntry oldValue, EntityListEntry newValue) {
                entitySelected(newValue);
            }
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
     * @param canEdit flag if editing (add/delete) is allowed.
     * @return this ControllerCollection.
     */
    public ControllerCollection setQuery(Query query, boolean canEdit) {
        this.query = query;
        buttonDelete.setVisible(canEdit);
        return this;
    }
}
