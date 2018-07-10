package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 *
 * @author scf
 */
public class ServerListEntry {

    private String name;

    private String config;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public ServerListEntry setName(String name) {
        this.name = name;
        return this;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public JsonElement getJsonElement() {
        if (config == null || config.isEmpty()) {
            return null;
        }
        JsonElement json = new JsonParser().parse(config);
        return json;
    }

    public ServerListEntry setJsonElement(JsonElement json) {
        this.config = new GsonBuilder().create().toJson(json);
        return this;
    }

    @Override
    public String toString() {
        return name;
    }

}
