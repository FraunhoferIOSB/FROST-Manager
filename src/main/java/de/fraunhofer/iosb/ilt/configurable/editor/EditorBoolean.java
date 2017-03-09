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
package de.fraunhofer.iosb.ilt.configurable.editor;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;

/**
 * An editor for boolean values.
 *
 * @author Hylke van der Schaaf
 * @param <C> The class type that provides context at runtime.
 * @param <D> The class type that provides context while editing.
 */
public final class EditorBoolean<C, D> extends EditorDefault<C, D, Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditorBoolean.class.getName());
    private final boolean deflt;
    private boolean value;
    /**
     * Flag indicating we are in JavaFX mode.
     */
    private Boolean fx;
    // Swing components
    private JCheckBox swComponent;
    // FX Nodes
    private CheckBox fxNode;

    public EditorBoolean(boolean deflt) {
        this.value = deflt;
        this.deflt = deflt;
    }

    public EditorBoolean(boolean deflt, String label, String description) {
        this.value = deflt;
        this.deflt = deflt;
        setLabel(label);
        setDescription(description);
    }

    @Override
    public void setConfig(JsonElement config, C context, final D edtCtx) {
        try {
            value = config.getAsBoolean();
        } catch (ClassCastException | IllegalStateException e) {
            value = deflt;
            LOGGER.trace("", e);
            LOGGER.debug("Value is not a boolean: {}.", config.toString());
        }
        fillComponent();
    }

    @Override
    public JsonElement getConfig() {
        if (swComponent != null) {
            value = swComponent.isSelected();
        }
        if (fxNode != null) {
            value = fxNode.isSelected();
        }
        return new JsonPrimitive(value);
    }

    private void setFx(boolean fxMode) {
        if (fx != null && fx != fxMode) {
            throw new IllegalStateException("Can not switch between swing and FX mode.");
        }
        fx = fxMode;
    }

    @Override
    public JComponent getComponent() {
        setFx(false);
        if (swComponent == null) {
            createComponent();
        }
        return swComponent;
    }

    @Override
    public Node getNode() {
        setFx(true);
        if (fxNode == null) {
            createNode();
        }
        return fxNode;
    }

    private void createNode() {
        fxNode = new CheckBox();
        fillComponent();
    }

    private void createComponent() {
        swComponent = new JCheckBox();
        fillComponent();
    }

    /**
     * Ensure the component represents the current value.
     */
    private void fillComponent() {
        if (fx == null) {
            return;
        }
        if (fx) {
            fxNode.setSelected(value);
        } else {
            swComponent.setSelected(value);
        }
    }

    public boolean isSelected() {
        if (fx == null) {
            return value;
        }
        return fx ? fxNode.isSelected() : swComponent.isSelected();
    }

    @Override
    public Boolean getValue() {
        return value;
    }

}
