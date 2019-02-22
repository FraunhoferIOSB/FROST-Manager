package de.fraunhofer.iosb.ilt.sensorthingsmanager.auth;

import de.fraunhofer.iosb.ilt.configurable.AbstractConfigurable;
import de.fraunhofer.iosb.ilt.configurable.annotations.ConfigurableField;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorBoolean;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorString;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
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
public class AuthBasic extends AbstractConfigurable<Void, Void> implements AuthMethod {

    /**
     * The logger for this class.
     */
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthBasic.class);

    @ConfigurableField(editor = EditorString.class, label = "Username", description = "The username to use for authentication")
    @EditorString.EdOptsString()
    private String username;

    @ConfigurableField(editor = EditorString.class, label = "Password", description = "The password to use for authentication")
    @EditorString.EdOptsString()
    private String password;

    @ConfigurableField(editor = EditorBoolean.class, label = "IgnoreSslErrors", description = "Ignore SSL certificate errors. This is a bad idea unless you know what you are doing.")
    @EditorBoolean.EdOptsBool()
    private boolean ignoreSslErrors;

    @Override
    public void setAuth(SensorThingsService service) {
        try {

            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            URL url = service.getEndpoint();
            credsProvider.setCredentials(
                    new AuthScope(url.getHost(), url.getPort()),
                    new UsernamePasswordCredentials(username, password));

            HttpClientBuilder clientBuilder = HttpClients.custom()
                    .useSystemProperties()
                    .setDefaultCredentialsProvider(credsProvider);

            if (ignoreSslErrors) {
                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(new SSLContextBuilder().loadTrustMaterial((X509Certificate[] chain, String authType) -> true).build());
                clientBuilder.setSSLSocketFactory(sslsf);
            }

            CloseableHttpClient httpclient = clientBuilder.build();

            service.setClient(httpclient);
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException ex) {
            LOGGER.error("Failed to initialise basic auth.", ex);
        }
    }

}
