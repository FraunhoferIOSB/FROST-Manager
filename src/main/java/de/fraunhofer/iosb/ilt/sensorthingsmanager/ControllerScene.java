package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorMap;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerScene implements Initializable {

    public static final Charset UTF8 = Charset.forName("UTF-8");
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
    private ListView<ServerListEntry> serverList;
    private ObservableList<ServerListEntry> servers;
    @FXML
    private TabPane serverTabs;
    @FXML
    private Node paneServers;
    @FXML
    private BorderPane paneAuth;
    private ServerListEntry activeItem = new ServerListEntry();

    private EditorMap<?> configEditor;

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

    private Server createConfiguredServer() {
        JsonElement config = configEditor.getConfig();
        Server server = new Server();
        server.configure(config, null, null);
        return server;
    }

    @FXML
    private void actionServerAdd(ActionEvent event) {
        Server server = createConfiguredServer();

        ServerListEntry entry = new ServerListEntry()
                .setName(server.getName())
                .setJsonElement(configEditor.getConfig());
        servers.add(entry);
        sortServers();
        saveServerList();
    }

    @FXML
    private void actionServerUpdate(ActionEvent event) {
        ServerListEntry serverEntry = serverList.getSelectionModel().getSelectedItem();
        if (serverEntry == null) {
            return;
        }
        Server server = createConfiguredServer();
        serverEntry.setName(server.getName())
                .setJsonElement(configEditor.getConfig());
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
        activeItem = server;
        buttonDelete.setDisable(false);
        configEditor.setConfig(server.getJsonElement());
    }

    @FXML
    private void actionServerConnect(ActionEvent event) {
        try {
            Server server = createConfiguredServer();

            String name = server.getName();
            if (name.isEmpty()) {
                name = server.getUrl();
            }
            LOGGER.info("Connecting to {} at {}.", name, server.getUrl());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Server.fxml"));
            AnchorPane content = (AnchorPane) loader.load();
            ControllerServer controller = loader.<ControllerServer>getController();
            controller.setServerEntry(server);

            Tab tab = new Tab(name);
            tab.setContent(content);
            serverTabs.getTabs().add(tab);

            buttonServers.setSelected(false);
            checkServersButton();
        } catch (IOException ex) {
            LOGGER.error("Failed to create server tab.", ex);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configEditor = new Server().getConfigEditor(null, null);
        paneAuth.setCenter(configEditor.getGuiFactoryFx().getNode());

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
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            List<ServerListEntry> serversFromFile = gson.fromJson(new FileReader(serversFile), new TypeReference<List<ServerListEntry>>() {
            }.getType());
            servers.addAll(serversFromFile);
            sortServers();
        } catch (IOException ex) {
            LOGGER.error("Failed to read server list.", ex);
        }
    }

    private void saveServerList() {
        try {
            File serversFile = new File("servers.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String toJson = gson.toJson(servers);
            FileUtils.write(serversFile, toJson, UTF8);
        } catch (IOException ex) {
            LOGGER.error("Failed to save server list.", ex);
        }
    }
}
