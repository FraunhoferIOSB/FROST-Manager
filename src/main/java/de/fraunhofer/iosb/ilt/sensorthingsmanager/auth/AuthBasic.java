package de.fraunhofer.iosb.ilt.sensorthingsmanager.auth;

import com.google.gson.JsonElement;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorBoolean;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorMap;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorString;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.LoggerFactory;

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
    private EditorBoolean editorIgnoreSslErrors;

    @Override
    public void configure(JsonElement config, Object context, Object edtCtx) {
        getConfigEditor(context, edtCtx).setConfig(config);
    }

    @Override
    public EditorMap<?> getConfigEditor(Object context, Object edtCtx) {
        if (configEditor == null) {
            configEditor = new EditorMap();

            editorUsername = new EditorString("username", 1, "Username", "The username to use for authentication.");
            configEditor.addOption("username", editorUsername, false);

            editorPassword = new EditorString("*****", 1, "Password", "The password to use for authentication.");
            configEditor.addOption("password", editorPassword, false);

            editorIgnoreSslErrors = new EditorBoolean(false, "Ignore SSL Errors", "Ignore SSL certificate errors. This is a bad idea unless you know what you are doing.");
            configEditor.addOption("ignoreSslErrors", editorIgnoreSslErrors, true);
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

            HttpClientBuilder clientBuilder = HttpClients.custom()
                    .useSystemProperties()
                    .setDefaultCredentialsProvider(credsProvider);

            if (editorIgnoreSslErrors.getValue()) {
                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(new SSLContextBuilder().loadTrustMaterial((X509Certificate[] chain, String authType) -> true).build());
                clientBuilder.setSSLSocketFactory(sslsf);
            }

            CloseableHttpClient httpclient = clientBuilder.build();

            service.setClient(httpclient);
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException | MalformedURLException ex) {
            LOGGER.error("Failed to initialise basic auth.", ex);
        }
    }

}
