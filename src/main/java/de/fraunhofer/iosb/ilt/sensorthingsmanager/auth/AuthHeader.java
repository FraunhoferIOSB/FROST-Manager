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

import de.fraunhofer.iosb.ilt.configurable.AbstractConfigurable;
import de.fraunhofer.iosb.ilt.configurable.annotations.ConfigurableField;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorString;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import de.fraunhofer.iosb.ilt.sta.service.TokenManager;
import org.apache.http.HttpRequest;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 *
 * @author scf
 */
public class AuthHeader extends AbstractConfigurable<Void, Void> implements AuthMethod {

    @ConfigurableField(editor = EditorString.class, label = "Header Name", description = "The name of the authentication header to use.")
    @EditorString.EdOptsString()
    private String headerName;

    @ConfigurableField(editor = EditorString.class, label = "Header Value", description = "The value of the authentication header to use.")
    @EditorString.EdOptsString()
    private String headerValue;

    @Override
    public void setAuth(SensorThingsService service) {
        service.setTokenManager(new TokenManager() {
            @Override
            public void addAuthHeader(HttpRequest hr) {
                hr.addHeader(headerName, headerValue);
            }

            @Override
            public TokenManager setHttpClient(CloseableHttpClient chc) {
                // We don't need a HTTPClient.
                return this;
            }

            @Override
            public CloseableHttpClient getHttpClient() {
                // We don't need a HTTPClient.
                return null;
            }
        });
    }

}
