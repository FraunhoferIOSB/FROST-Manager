/*
 * Copyright (C) 2015 Hylke van der Schaaf
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, in version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.configurable;

import javax.swing.JComponent;

import com.google.gson.JsonElement;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * Interface defining configuration editors.
 *
 * @author hylke
 * @param <C> The class type that provides context.
 * @param <D> The class type that provides context while editing.
 * @param <T> The type of object returned by getValue.
 */
public interface ConfigEditor<C, D, T> {

    /**
     * Load the given configuration into this editor.
     *
     * @param config the configuration to load into this editor.
     * @param context the object that defines the context at runtime.
     * @param edtCtx the object that defines the context while editing.
     */
    public void setConfig(JsonElement config, C context, D edtCtx);

    /**
     * Get the current (edited) state of the configuration.
     *
     * @return The current (edited) configuration.
     */
    public JsonElement getConfig();

    /**
     * Get the value configured in the editor.
     *
     * @return the value configured in the editor.
     */
    public T getValue();

    /**
     * Get the swing Component that represents the editor.
     *
     * @return The swing Component that represents the editor.
     */
    public JComponent getComponent();

    /**
     * Get the JavaFX Node that represents the editor.
     *
     * @return The JavaFX Node that represents the editor.
     */
    public Node getNode();

    /**
     * Get the human-readable label to use for this editor. Can return an empty
     * string.
     *
     * @return The label to use for this editor.
     */
    default String getLabel() {
        return "";
    }

    /**
     * Get the description for this editor. Can return an empty string.
     *
     * @return The description to use for this editor.
     */
    default String getDescription() {
        return "";
    }
}
