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
import de.fraunhofer.iosb.ilt.configurable.ConfigEditor;
import de.fraunhofer.iosb.ilt.configurable.ConfigurationException;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorNull;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;

/**
 *
 * @author scf
 */
public class AuthNone implements AuthMethod {

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
        // Do Nothing.
    }

}
