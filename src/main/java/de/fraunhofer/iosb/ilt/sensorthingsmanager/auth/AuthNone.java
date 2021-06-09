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
package de.fraunhofer.iosb.ilt.sensorthingsmanager.auth;

import com.google.gson.JsonElement;
import de.fraunhofer.iosb.ilt.configurable.AnnotatedConfigurable;
import de.fraunhofer.iosb.ilt.configurable.ConfigEditor;
import de.fraunhofer.iosb.ilt.configurable.annotations.ConfigurableField;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorBoolean;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorNull;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication type for no authentication. Does add the option to ignore SSL
 * certificate errors, for when using self-signed certificates for testing
 * purposes.
 *
 * @author scf
 */
public class AuthNone implements AnnotatedConfigurable<Void, Void>, AuthMethod {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthNone.class);

    @ConfigurableField(editor = EditorBoolean.class,
            label = "IgnoreSslErrors",
            description = "Ignore SSL certificate errors. This is a bad idea unless you know what you are doing.")
    @EditorBoolean.EdOptsBool()
    private boolean ignoreSslErrors;

    @Override
    public void configure(JsonElement config, Void context, Void edtCtx, ConfigEditor<?> ce) {
        // Nothing to configure
    }

    @Override
    public ConfigEditor<?> getConfigEditor(Void context, Void edtCtx) {
        return new EditorNull();
    }

    @Override
    public void setAuth(SensorThingsService service) {
        try {
            URL url = service.getEndpoint();

            HttpClientBuilder clientBuilder = HttpClients.custom()
                    .useSystemProperties();

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
