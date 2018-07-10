package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import de.fraunhofer.iosb.ilt.configurable.AbstractConfigurable;
import de.fraunhofer.iosb.ilt.configurable.annotations.ConfigurableField;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorString;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorSubclass;
import de.fraunhofer.iosb.ilt.sensorthingsmanager.auth.AuthMethod;

/**
 *
 * @author scf
 */
public class Server extends AbstractConfigurable<Void, Void> {

    @ConfigurableField(editor = EditorString.class,
            label = "Name",
            description = "The name of the server as it should appear in the server list.")
    @EditorString.EdOptsString(dflt = "New Server")
    private String name;

    @ConfigurableField(editor = EditorString.class,
            label = "Url",
            description = "The name of the server as it should appear in the server list.")
    @EditorString.EdOptsString(dflt = "https://server.de/FROST-Server/v1.0")
    private String url;

    @ConfigurableField(editor = EditorSubclass.class,
            label = "Auth",
            description = "The method to use for authentication",
            optional = false)
    @EditorSubclass.EdOptsSubclass(iface = AuthMethod.class, nameField = "authClass")
    private AuthMethod authMethod;

    /**
     * @return the name
     */
    public String getName() {
        if (!name.isEmpty()) {
            return name;
        }
        return url;
    }

    /**
     * @param name the name to set
     * @return This ServerListEntry
     */
    public Server setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param Url the url to set
     * @return This ServerListEntry
     */
    public Server setUrl(String Url) {
        this.url = Url;
        return this;
    }

    public AuthMethod getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(AuthMethod authMethod) {
        this.authMethod = authMethod;
    }

    @Override
    public String toString() {
        return name;
    }

}
