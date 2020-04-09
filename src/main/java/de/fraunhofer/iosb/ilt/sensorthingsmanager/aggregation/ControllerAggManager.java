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
package de.fraunhofer.iosb.ilt.sensorthingsmanager.aggregation;

import de.fraunhofer.iosb.ilt.configurable.ConfigurationException;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorMap;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.ButtonTableCell;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.utils.DateTimePicker;
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.ObservedProperty;
import de.fraunhofer.iosb.ilt.sta.model.TimeObject;
import de.fraunhofer.iosb.ilt.sta.model.ext.UnitOfMeasurement;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

/**
 *
 * @author scf
 */
public class ControllerAggManager implements Initializable {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerAggManager.class);

    private static final ObservableValue<Void> OBSERVABLE_VOID = new ObservableValueBase<Void>() {
        @Override
        public Void getValue() {
            return null;
        }
    };

    @FXML
    private BorderPane paneAddLevel;

    @FXML
    private Button buttonReload;

    @FXML
    private Button buttonAddLevel;

    @FXML
    private Button buttonApplyChanges;

    @FXML
    private TableView<AggregationBase> table;

    @FXML
    private ProgressBar progressBar;

    private SensorThingsService service;

    private AggregationData data;

    private TableColumn<AggregationBase, String> baseColumn;
    private TableColumn<AggregationBase, AggregationBase> buttonColumn;
    private Map<AggregationLevel, TableColumn<AggregationBase, Boolean>> columnsByLevel = new HashMap<>();
    private EditorMap<?> levelEditor;
    private SensorThingsUtils utils = new SensorThingsUtils();
    private Instant lastPickedStart;
    private Instant lastPickedEnd;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buttonReload.setDisable(true);

        AggregationLevel level = new AggregationLevel(ChronoUnit.HOURS, 1);
        levelEditor = level.getConfigEditor(null, null);
        Node node = levelEditor.getGuiFactoryFx().getNode();
        paneAddLevel.setCenter(node);
    }

    public void setService(SensorThingsService service) {
        this.service = service;
        buttonReload.setDisable(false);
    }

    private void addMenuToColumn(final TableColumn<AggregationBase, Boolean> column, final AggregationLevel level) {
        MenuItem addAll = new MenuItem("Check All");
        addAll.setOnAction((event) -> {
            setAllOnColumn(column, level, true);
        });
        MenuItem removeAll = new MenuItem("Un-Check All");
        removeAll.setOnAction((event) -> {
            setAllOnColumn(column, level, false);
        });
        MenuItem addSelected = new MenuItem("Check Selected");
        addSelected.setOnAction((event) -> {
            setSelectedOnColumn(column, level, true);
        });
        MenuItem removeSelected = new MenuItem("Un-Check Selected");
        removeSelected.setOnAction((event) -> {
            setSelectedOnColumn(column, level, false);
        });
        ContextMenu menu = new ContextMenu(addAll, removeAll, addSelected, removeSelected);

        column.setContextMenu(menu);
    }

    private void setAllOnColumn(TableColumn<AggregationBase, Boolean> column, final AggregationLevel level, boolean enabled) {
        for (AggregationBase item : column.getTableView().getItems()) {
            item.getFxProperties().getLevelProperty(level).set(enabled);
        }
    }

    private void setSelectedOnColumn(TableColumn<AggregationBase, Boolean> column, final AggregationLevel level, boolean enabled) {
        for (AggregationBase item : column.getTableView().getSelectionModel().getSelectedItems()) {
            item.getFxProperties().getLevelProperty(level).set(enabled);
        }
    }

    private TableColumn<AggregationBase, Boolean> getColumnForLevel(final AggregationLevel level) {
        TableColumn<AggregationBase, Boolean> column = columnsByLevel.get(level);
        if (column == null) {
            LOGGER.info("Creating column {}.", level);
            column = new TableColumn(level.toString());
            column.setCellFactory((TableColumn<AggregationBase, Boolean> param) -> {
                CheckBoxTableCell<AggregationBase, Boolean> cell = new CheckBoxTableCell<>();
                cell.setAlignment(Pos.CENTER);
                return cell;
            });
            column.setCellValueFactory((TableColumn.CellDataFeatures<AggregationBase, Boolean> param) -> param.getValue().getFxProperties().getLevelProperty(level));
            column.setEditable(true);
            addMenuToColumn(column, level);
            columnsByLevel.put(level, column);
        }
        return column;
    }

    private void reCalculateBase(AggregationBase base, Instant start, Instant end) {
        try {
            Datastream baseDs = base.getBaseDatastream();
            if (baseDs == null) {
                LOGGER.error("No base Datastream for {}", base.getBaseName());
            }
            Observation dummy = new Observation("Dummy", baseDs);
            dummy.setPhenomenonTime(new TimeObject(Interval.of(start, end)));

            service.create(dummy);
            service.delete(dummy);
        } catch (ServiceFailureException ex) {
            LOGGER.error("Failed to create or delete dummy observation!", ex);
        }
    }

    private void reCalculateBase(AggregationBase base) {
        DateTimePicker startTime = new DateTimePicker(lastPickedStart);
        DateTimePicker endTime = new DateTimePicker(lastPickedEnd);

        GridPane pane = new GridPane();
        int row = 0;
        pane.add(new Text(base.getBaseName()), 0, row, 2, 1);
        pane.add(new Text("From"), 0, ++row);
        pane.add(startTime, 1, row);

        pane.add(new Text("To"), 0, ++row);
        pane.add(endTime, 1, row);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setResizable(true);
        dialog.setTitle("Re-Calculate which Period?");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.APPLY);
        dialog.getDialogPane().setContent(pane);
        dialog.getDialogPane().setExpandableContent(new Text("This will create a new Observation in the given Datastream, and directly delete it again."));
        Optional<ButtonType> confirmation = dialog.showAndWait();

        Instant startDateTime = startTime.getValue().toInstant();
        lastPickedStart = startDateTime;
        Instant endDateTime = endTime.getValue().toInstant();
        lastPickedEnd = endDateTime;

        if (confirmation.isPresent() && confirmation.get() == ButtonType.APPLY) {
            LOGGER.info("Re-Calculating from {} to {} for {} ({})", startDateTime, endDateTime, base.getBaseName(), base.getBaseDatastream());
            reCalculateBase(base, startDateTime, endDateTime);
        } else {
            LOGGER.info("Cancelled...  {} to {} for {}", startDateTime, endDateTime, base.getBaseName());
        }
    }

    @FXML
    private void actionReload(ActionEvent event) {
        if (service == null) {
            return;
        }
        table.setVisible(false);
        progressBar.setVisible(true);

        final AggregationData myData = new AggregationData(service, true, true);

        Task<AggregationData> task = new Task<AggregationData>() {
            @Override
            protected AggregationData call() throws Exception {
                AggregationData.ProgressListener pl = (double progress1) -> {
                    updateProgress(progress1, 1);
                };
                myData.addProgressListener(pl);
                myData.getAggregationBases();
                myData.removeProgressListener(pl);
                return myData;
            }
        };
        task.setOnSucceeded((WorkerStateEvent event1) -> {
            fillTableAndShow(task.getValue());
        });
        task.setOnFailed((WorkerStateEvent event1) -> {
            new Alert(Alert.AlertType.ERROR, "Loading failed: " + event1.toString(), ButtonType.CLOSE).show();
        });
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(task.progressProperty());
        new Thread(task).start();
    }

    private void fillTableAndShow(AggregationData myData) {
        data = myData;
        baseColumn = new TableColumn("Base Name");
        baseColumn.setCellValueFactory((TableColumn.CellDataFeatures<AggregationBase, String> param) -> param.getValue().getFxProperties().getBaseNameProperty());
        buttonColumn = new TableColumn<>("↺");
        buttonColumn.setCellValueFactory((TableColumn.CellDataFeatures<AggregationBase, AggregationBase> param) -> new ReadOnlyObjectWrapper<>(param.getValue()));
        buttonColumn.setCellFactory((final TableColumn<AggregationBase, AggregationBase> param) -> new ButtonTableCell<AggregationBase, AggregationBase>("↺") {
            @Override
            public void onAction(TableRow<AggregationBase> row) {
                reCalculateBase(row.getItem());
            }
        });
        columnsByLevel.clear();
        for (AggregationBase base : data.getAggregationBases()) {
            for (AggregateCombo combo : base.getCombos()) {
                getColumnForLevel(combo.level);
            }
        }
        table.getColumns().clear();
        table.getColumns().add(baseColumn);
        table.getColumns().add(buttonColumn);
        table.getColumns().addAll(columnsByLevel.values());
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setItems(FXCollections.observableArrayList(data.getAggregationBases()));

        progressBar.setVisible(false);
        table.setVisible(true);
    }

    @FXML
    private void actionAddLevel(ActionEvent event) {
        AggregationLevel level = new AggregationLevel();
        try {
            level.configure(levelEditor.getConfig(), null, null, null);
        } catch (ConfigurationException ex) {
            LOGGER.error("Failed to configure aggregation level.");
        }
        TableColumn<AggregationBase, Boolean> column = getColumnForLevel(level);
        if (table.getColumns().contains(column)) {
            LOGGER.info("Column {} already exists.", level);
        } else {
            table.getColumns().add(column);
        }
    }

    @FXML
    private void actionApplyChanges(ActionEvent event) throws ServiceFailureException {
        List<String> changeLog = generateChangeLog();
        StringBuilder changeString = new StringBuilder();
        for (String line : changeLog) {
            changeString.append(line);
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setResizable(true);
        dialog.setTitle("Apply changes?");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.APPLY);
        TextArea textArea = new TextArea(changeString.toString());
        textArea.setEditable(false);
        textArea.setWrapText(false);
        dialog.getDialogPane().setContent(textArea);

        Optional<ButtonType> confirmation = dialog.showAndWait();
        if (confirmation.isPresent() && confirmation.get() == ButtonType.APPLY) {
            LOGGER.info("Working");
            applyChangesTask(changeLog.size());
        } else {
            LOGGER.info("Cancelled");
        }
    }

    private void applyChangesTask(int count) {
        final SimpleDoubleProperty progress = new SimpleDoubleProperty(0);
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                applyChanges(progress, count);
                return null;
            }
        };
        task.setOnSucceeded((WorkerStateEvent event1) -> {
            actionReload(null);
        });
        task.setOnFailed((WorkerStateEvent event1) -> {
            new Alert(Alert.AlertType.ERROR, "Update Failed: " + event1.toString(), ButtonType.CLOSE).show();
        });
        table.setVisible(false);
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(progress);
        progressBar.setVisible(true);
        new Thread(task).start();
    }

    private void createAggregate(AggregationBase base, AggregationLevel level) throws ServiceFailureException {
        LOGGER.info("Creating {} for {}.", level, base.getBaseName());
        Datastream baseDs = base.getBaseDatastream();
        ObservedProperty op = baseDs.getObservedProperty();
        UnitOfMeasurement uom = baseDs.getUnitOfMeasurement();
        utils.findOrCreateAggregateOps(service, op);

        List<ObservedProperty> ops = new ArrayList<>();
        ops.add(op);
        ops.addAll(utils.aggregateProperties.get(op));
        List<UnitOfMeasurement> uoms = new ArrayList<>();
        for (int i = 0; i < ops.size(); i++) {
            uoms.add(uom);
        }

        Map<String, Object> aggProps = new HashMap<>();
        aggProps.put(Utils.KEY_AGGREGATE_FOR, "/Datastreams(" + baseDs.getId().getUrl() + ")");
        aggProps.put(Utils.KEY_AGGREGATE_AMOUNT, level.amount);
        aggProps.put(Utils.KEY_AGGREGATE_UNIT, level.unit);
        String mdsName = base.getBaseName() + " " + level.toPostFix();
        String mdsDesc = baseDs.getDescription() + " aggregated per " + level.amount + " " + level.unit;
        utils.findOrCreateMultiDatastream(service, mdsName, mdsDesc, uoms, baseDs.getThing(), ops, baseDs.getSensor(), aggProps);
    }

    private void deleteAggregate(AggregationBase base, AggregationLevel level) {
        AggregateCombo combo = base.getCombosByLevel().get(level);
        if (combo == null) {
            LOGGER.error("Can not delete {} for {}, no such combo.", level, base.getBaseName());
            return;
        }
        LOGGER.error("Deleting {} for {}", level, base.getBaseName());
        try {
            service.delete(combo.target);
        } catch (ServiceFailureException ex) {
            LOGGER.error("Failed to delete {}", combo.target);
            LOGGER.error("Failed to delete:", ex);
        }
    }

    private void applyChanges(SimpleDoubleProperty progress, int changeCount) throws ServiceFailureException {
        int nr = 0;
        for (AggregationBase base : data.getAggregationBases()) {
            Map<AggregationLevel, AggregateCombo> presentLevels = base.getCombosByLevel();
            Map<AggregationLevel, Boolean> wantedLevels = base.getWantedLevels();
            for (Map.Entry<AggregationLevel, Boolean> wantedEntry : wantedLevels.entrySet()) {
                AggregationLevel level = wantedEntry.getKey();
                boolean wanted = wantedEntry.getValue();
                if (wanted && !presentLevels.containsKey(level)) {
                    nr++;
                    createAggregate(base, level);

                } else if (!wanted && presentLevels.containsKey(level)) {
                    nr++;
                    deleteAggregate(base, level);
                }
            }

            progress.set(1.0 * nr / changeCount);
        }
    }

    private List<String> generateChangeLog() {
        List<String> changeLog = new ArrayList<>();
        for (AggregationBase base : data.getAggregationBases()) {
            Map<AggregationLevel, AggregateCombo> presentLevels = base.getCombosByLevel();
            Map<AggregationLevel, Boolean> wantedLevels = base.getWantedLevels();
            for (Map.Entry<AggregationLevel, Boolean> wantedEntry : wantedLevels.entrySet()) {
                AggregationLevel level = wantedEntry.getKey();
                boolean wanted = wantedEntry.getValue();
                if (wanted && !presentLevels.containsKey(level)) {
                    changeLog.add("Create " + level.toString() + " for " + base.getBaseName() + '\n');

                } else if (!wanted && presentLevels.containsKey(level)) {
                    changeLog.add("DELETE " + level.toString() + " for " + base.getBaseName() + '\n');

                }
            }
        }
        return changeLog;
    }
}
