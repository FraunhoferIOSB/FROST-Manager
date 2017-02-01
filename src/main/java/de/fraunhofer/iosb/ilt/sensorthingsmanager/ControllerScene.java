package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.fraunhofer.iosb.ilt.sta.jackson.ObjectMapperFactory;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class ControllerScene implements Initializable {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerScene.class);
    @FXML
    private SplitPane mainSplit;
    @FXML
    private ToggleButton buttonServers;
    private double[] dividerPositions;

    @FXML
    private Button buttonAdd;
    @FXML
    private Button buttonUpdate;
    @FXML
    private Button buttonConnect;
    @FXML
    private Button buttonDelete;
    @FXML
    private TextField serverName;
    @FXML
    private TextField serverUrl;
    @FXML
    private ListView<ServerListEntry> serverList;
    private ObservableList<ServerListEntry> servers;
    @FXML
    private TabPane serverTabs;
    @FXML
    private Pane paneServers;

    @FXML
    private void toggleServersAction(ActionEvent event) {
        checkServersButton();
    }

    private void checkServersButton() {
        if (buttonServers.isSelected()) {
            mainSplit.getItems().add(0, paneServers);
            mainSplit.setDividerPositions(dividerPositions);
        } else {
            dividerPositions = mainSplit.getDividerPositions();
            mainSplit.getItems().remove(paneServers);
        }
    }

    @FXML
    private void actionServerAdd(ActionEvent event) {
        servers.add(new ServerListEntry().setName(serverName.getText()).setUrl(serverUrl.getText()));
        sortServers();
        saveServerList();
    }

    @FXML
    private void actionServerUpdate(ActionEvent event) {
        ServerListEntry server = serverList.getSelectionModel().getSelectedItem();
        if (server == null) {
            return;
        }
        server.setName(serverName.getText()).setUrl(serverUrl.getText());
        sortServers();
        saveServerList();
    }

    @FXML
    private void actionServerDelete(ActionEvent event) {
        int selectedIndex = serverList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < servers.size()) {
            servers.remove(selectedIndex);
        }
        saveServerList();
    }

    @FXML
    private void actionServerSelected(ServerListEntry server) {
        if (server == null) {
            buttonDelete.setDisable(true);
            return;
        }
        serverName.setText(server.getName());
        serverUrl.setText(server.getUrl());
        buttonDelete.setDisable(false);
    }

    @FXML
    private void actionServerConnect(ActionEvent event) {
        try {
            String name = serverName.getText();
            if (name.isEmpty()) {
                name = serverUrl.getText();
            }
            LOGGER.info("Connecting to {} at {}.", serverName.getText(), serverUrl.getText());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Server.fxml"));
            AnchorPane content = (AnchorPane) loader.load();
            ControllerServer controller = loader.<ControllerServer>getController();
            controller.setServerEntry(
                    new ServerListEntry()
                    .setName(serverName.getText())
                    .setUrl(serverUrl.getText()));

            Tab tab = new Tab(name);
            tab.setContent(content);
            serverTabs.getTabs().add(tab);

            buttonServers.setSelected(false);
            checkServersButton();
        } catch (IOException ex) {
            LOGGER.error("Failed to create server tab.", ex);
        }
    }

    private void nameChanged(String newServerName) {
        int selectedIndex = serverList.getSelectionModel().getSelectedIndex();
        boolean empty = newServerName.isEmpty();
        buttonAdd.setDisable(empty);
        buttonUpdate.setDisable(empty || selectedIndex < 0);
        buttonDelete.setDisable(empty);

    }

    private void urlChanged(String newServerUrl) {
        boolean empty = newServerUrl.isEmpty();
        buttonConnect.setDisable(empty);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        serverName.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            nameChanged(newValue);
        });
        serverUrl.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            urlChanged(newValue);
        });
        servers = FXCollections.observableArrayList();
        loadServerList();
        serverList.setItems(servers);
        serverList.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends ServerListEntry> observable, ServerListEntry oldValue, ServerListEntry newValue) -> {
            actionServerSelected(newValue);
        });
    }

    private void sortServers() {
        servers.sort(new Comparator<ServerListEntry>() {
            @Override
            public int compare(ServerListEntry o1, ServerListEntry o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
            }
        });
    }

    private void loadServerList() {
        try {
            File serversFile = new File("servers.json");
            final ObjectMapper mapper = ObjectMapperFactory.get();
            List<ServerListEntry> serversFromFile = mapper.readValue(serversFile, new TypeReference<List<ServerListEntry>>() {
            });
            servers.addAll(serversFromFile);
            sortServers();
        } catch (IOException ex) {
            LOGGER.error("Failed to read server list.", ex);
        }
    }

    private void saveServerList() {
        try {
            File serversFile = new File("servers.json");
            final ObjectMapper mapper = ObjectMapperFactory.get();
            mapper.writeValue(serversFile, servers);
        } catch (IOException ex) {
            LOGGER.error("Failed to save server list.", ex);
        }
    }
}
