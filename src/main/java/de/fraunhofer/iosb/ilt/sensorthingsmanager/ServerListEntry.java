package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.fraunhofer.iosb.ilt.configurable.Configurable;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorMap;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorSubclass;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.auth.AuthMethod;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.auth.AuthNone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scf
 */
public class ServerListEntry implements Configurable<Object, Object> {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerListEntry.class);
    private String name;
    private String Url;
    private String config;
    private EditorMap configEditor;
    private EditorSubclass<Object, Object, AuthMethod> editorAuthMethod;

    @Override
    public void configure(JsonElement config, Object context, Object edtCtx) {
        getConfigEditor(context, edtCtx).setConfig(config);
    }

    @Override
    public EditorMap<?> getConfigEditor(Object context, Object edtCtx) {
        if (configEditor == null) {
            configEditor = new EditorMap();

            editorAuthMethod = new EditorSubclass<>(context, edtCtx, AuthMethod.class, "Auth", "The method to use for authentication", false, "authClass")
                    .setSelectLabel("");
            configEditor.addOption("authMethod", editorAuthMethod, false);
        }
        return configEditor;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     * @return This ServerListEntry
     */
    public ServerListEntry setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @return the Url
     */
    public String getUrl() {
        return Url;
    }

    /**
     * @param Url the Url to set
     * @return This ServerListEntry
     */
    public ServerListEntry setUrl(String Url) {
        this.Url = Url;
        return this;
    }

    public ServerListEntry updateConfig() {
        JsonElement json = getConfigEditor(null, null).getConfig();
        config = new Gson().toJson(json);
        return this;
    }

    /**
     * @return the config
     */
    public String getConfig() {
        return config;
    }

    /**
     * @param config the config to set
     * @return this
     */
    public ServerListEntry setConfig(String config) {
        this.config = config;
        try {
            JsonElement json = new JsonParser().parse(config);
            configure(json, null, null);
        } catch (Exception e) {
            LOGGER.trace("Failed to parse {}", config);
        }
        return this;
    }

    public AuthMethod getAuthMethod() {
        getConfigEditor(null, null);
        AuthMethod method = editorAuthMethod.getValue();
        if (method == null) {
            return new AuthNone();
        }
        return method;
    }

    @Override
    public String toString() {
        return name;
    }

}
