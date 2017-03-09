package de.fraunhofer.iosb.ilt.sensorthingsmanager.auth;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import de.fraunhofer.iosb.ilt.configurable.editor.EditorMap;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorString;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;

/**
 *
 * @author scf
 */
public class AuthBasic implements AuthMethod {

    /**
     * The logger for this class.
     */
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthBasic.class);
    private EditorMap configEditor;
    private EditorString editorUsername;
    private EditorString editorPassword;

    @Override
    public void configure(JsonElement config, Object context, Object edtCtx) {
        getConfigEditor(context, edtCtx).setConfig(config, context, edtCtx);
    }

    @Override
    public EditorMap<Object, Object, ?> getConfigEditor(Object context, Object edtCtx) {
        if (configEditor == null) {
            configEditor = new EditorMap();
            editorUsername = new EditorString("username", 1, "Username", "The username to use for authentication.");
            configEditor.addOption("username", editorUsername, false);
            editorPassword = new EditorString("*****", 1, "Password", "The password to use for authentication.");
            configEditor.addOption("password", editorPassword, false);
        }
        return configEditor;
    }

    @Override
    public void setAuth(SensorThingsService service) {
        try {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            URL url = service.getEndpoint().toURL();
            credsProvider.setCredentials(
                    new AuthScope(url.getHost(), url.getPort()),
                    new UsernamePasswordCredentials(editorUsername.getValue(), editorPassword.getValue()));
            CloseableHttpClient httpclient = HttpClients.custom()
                    .setDefaultCredentialsProvider(credsProvider)
                    .build();
            service.setClient(httpclient);
        } catch (MalformedURLException ex) {
            LOGGER.error("Failed to initialise basic auth.", ex);
        }
    }

}
