/*
 * Copyright (C) 2016 Hylke van der Schaaf
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

import java.awt.BorderLayout;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Spinner;

/**
 *
 * @author Hylke van der Schaaf
 * @param <C> The class type that provides context at runtime.
 * @param <D> The class type that provides context while editing.
 * @param <T> The type this editor selects.
 */
public class EditorEnum<C, D, T extends Enum<T>> extends EditorDefault<C, D, T> {

    private final Class<T> sourceType;
    private final T deflt;
    private T value;
    /**
     * Flag indicating we are in JavaFX mode.
     */
    private Boolean fx;
    // Swing components
    private JComboBox<T> swComboBox;
    private ComboBoxModel<T> swModel;
    private JPanel swComponent;
    // FX Nodes
    private Spinner<T> fxNode;

    public EditorEnum(Class<T> sourceType, T deflt, String label, String description) {
        this.sourceType = sourceType;
        this.deflt = deflt;
        this.value = deflt;
        setLabel(label);
        setDescription(description);
    }

    @Override
    public void setConfig(JsonElement config, C context, final D edtCtx) {
        if (config != null && config.isJsonPrimitive()) {
            JsonPrimitive prim = config.getAsJsonPrimitive();
            if (prim.isString()) {
                value = Enum.valueOf(sourceType, config.getAsString());
            } else if (prim.isNumber()) {
                T[] list = sourceType.getEnumConstants();
                int ord = prim.getAsInt();
                if (ord >= 0 && ord < list.length) {
                    value = list[ord];
                }
            }
        } else {
            value = deflt;
        }
        fillComponent();
    }

    @Override
    public JsonElement getConfig() {
        readComponent();
        return new JsonPrimitive(value.name());
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
            createComponent();
        }
        return fxNode;
    }

    private void createComponent() {
        if (fx) {
            fxNode = new Spinner<>(FXCollections.observableArrayList(sourceType.getEnumConstants()));
        } else {
            swModel = new DefaultComboBoxModel<>(sourceType.getEnumConstants());
            swComboBox = new JComboBox<>(swModel);
            swComponent = new JPanel(new BorderLayout());
            swComponent.add(swComboBox, BorderLayout.CENTER);
            swComponent.add(getHelpButton(), BorderLayout.WEST);
        }
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
            fxNode.getValueFactory().setValue(value);
        } else {
            swComboBox.setSelectedItem(value);
        }
    }

    private void readComponent() {
        if (fx == null) {
            return;
        }
        if (fx) {
            value = fxNode.getValue();
        } else {
            int index = swComboBox.getSelectedIndex();
            value = swModel.getElementAt(index);
        }
    }

    @Override
    public T getValue() {
        readComponent();
        return value;
    }

}
