package de.fraunhofer.iosb.ilt.sensorthingsmanager;

/**
 *
 * @author scf
 */
public class ServerListEntry {

    private String name;
    private String Url;

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

    @Override
    public String toString() {
        return name;
    }

}
