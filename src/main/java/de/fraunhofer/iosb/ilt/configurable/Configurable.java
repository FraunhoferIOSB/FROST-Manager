/*
 * Copyright (C) 2015 Hylke van der Schaaf
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published
 *  by the Free Software Foundation, in version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.configurable;

import com.google.gson.JsonElement;

/**
 *
 * @author hylke
 * @param <C> The class type that provides context at runtime.
 * @param <D> The class type that provides context while editing.
 */
public interface Configurable<C, D> {

    /**
     * Configure the instance using the given configuration.
     *
     * @param config The configuration to use for this instance.
     * @param context the object that defines the context at runtime.
     * @param edtCtx the object that defines the context while editing.
     */
    public void configure(JsonElement config, C context, D edtCtx);

    /**
     * Returns an generic editor for any instance of this class. This editor can
     * be given a configuration separate from the configuration of the class
     * used to generate this editor.
     *
     * @param context the object that defines the context at runtime.
     * @param edtCtx the object that defines the context while editing.
     * @return A generic editor for any instance of this class.
     */
    public ConfigEditor<C, D, ?> getConfigEditor(C context, D edtCtx);
}
