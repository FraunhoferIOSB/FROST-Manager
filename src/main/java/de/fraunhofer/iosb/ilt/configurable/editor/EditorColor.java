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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

/**
 *
 * @author Hylke van der Schaaf
 * @param <C> The class type that provides context at runtime.
 * @param <D> The class type that provides context while editing.
 */
public class EditorColor<C, D> extends EditorDefault<C, D, Color> {

    private boolean editAlpla = true;
    private int red;
    private int green;
    private int blue;
    private int alpha = 255;
    /**
     * Flag indicating we are in JavaFX mode.
     */
    private Boolean fx;
    // Swing components
    private JPanel swComponent;
    private SpinnerNumberModel swModelAlpha;
    private SpinnerNumberModel swModelBlue;
    private SpinnerNumberModel swModelGreen;
    private SpinnerNumberModel swModelRed;
    // FX Nodes
    private ColorPicker fxNode;

    public EditorColor(Color deflt) {
        this.red = deflt.getRed();
        this.green = deflt.getGreen();
        this.blue = deflt.getBlue();
        this.alpha = deflt.getAlpha();
    }

    public EditorColor(Color deflt, boolean editAlpha) {
        this(deflt);
        this.editAlpla = editAlpha;
    }

    public EditorColor(final Color deflt, final boolean editAlpha, final String label, final String description) {
        this(deflt);
        this.editAlpla = editAlpha;
        setLabel(label);
        setDescription(description);
    }

    private static int getInt(JsonObject confObj, int dflt, String... names) {
        for (final String name : names) {
            final JsonElement element = confObj.get(name);
            if (element != null && element.isJsonPrimitive()) {
                return element.getAsInt();
            }
        }
        return dflt;
    }

    @Override
    public void setConfig(JsonElement config, C context, D edtCtx) {
        if (config.isJsonObject()) {
            JsonObject confObj = config.getAsJsonObject();
            red = getInt(confObj, red, "r", "red");
            green = getInt(confObj, green, "g", "green");
            blue = getInt(confObj, blue, "b", "blue");
            alpha = getInt(confObj, alpha, "a", "alpha");
        }
        fillComponent();
    }

    private void readComponent() {
        if (fx == null) {
            return;
        }
        if (fx) {
            javafx.scene.paint.Color color = fxNode.getValue();
            red = (int) (color.getRed() * 255);
            green = (int) (color.getGreen() * 255);
            blue = (int) (color.getBlue() * 255);
            alpha = (int) ((1 - color.getOpacity()) * 255);
        } else {
            red = swModelRed.getNumber().intValue();
            green = swModelGreen.getNumber().intValue();
            blue = swModelBlue.getNumber().intValue();
            alpha = swModelAlpha.getNumber().intValue();
        }
    }

    @Override
    public JsonElement getConfig() {
        readComponent();
        JsonObject config = new JsonObject();
        config.add("r", new JsonPrimitive(red));
        config.add("g", new JsonPrimitive(green));
        config.add("b", new JsonPrimitive(blue));
        if (editAlpla && alpha != 255) {
            config.add("a", new JsonPrimitive(alpha));
        }
        return config;
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
        fxNode = new ColorPicker();
        fxNode.setOnAction(event -> readComponent());

        fillComponent();
    }

    private void createComponent() {
        swModelAlpha = new SpinnerNumberModel(0, 0, 255, 1);
        swModelBlue = new SpinnerNumberModel(0, 0, 255, 1);
        swModelGreen = new SpinnerNumberModel(0, 0, 255, 1);
        swModelRed = new SpinnerNumberModel(0, 0, 255, 1);

        swComponent = new JPanel(new GridBagLayout());
        swComponent.add(new JSpinner(swModelRed), new GridBagConstraints());
        swComponent.add(new JSpinner(swModelGreen), new GridBagConstraints());
        swComponent.add(new JSpinner(swModelBlue), new GridBagConstraints());
        swComponent.add(new JSpinner(swModelAlpha), new GridBagConstraints());

        JButton button = new JButton("…");
        button.setMargin(new java.awt.Insets(0, 2, 0, 2));
        button.addActionListener((ActionEvent e) -> openPicker());
        swComponent.add(button, new GridBagConstraints());

        fillComponent();
    }

    private void openPicker() {
        if (fx == null) {
            return;
        }
        if (fx) {
            return;
        }
        Color newColor = JColorChooser.showDialog(
                swComponent,
                "Choose Color",
                new Color(red, green, blue, alpha));
        if (newColor != null) {
            red = newColor.getRed();
            green = newColor.getGreen();
            blue = newColor.getBlue();
            if (this.editAlpla) {
                this.alpha = newColor.getAlpha();
            }
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
            javafx.scene.paint.Color color = javafx.scene.paint.Color.color(255.0 / red, 255.0 / green, 255.0 / blue, 255.0 / (1 - alpha));
            fxNode.setValue(color);
        } else {
            swModelRed.setValue(red);
            swModelGreen.setValue(green);
            swModelBlue.setValue(blue);
            if (this.editAlpla) {
                swModelAlpha.setValue(alpha);
            }
            swComponent.setBackground(new Color(this.red, this.green, this.blue));
        }
    }

    @Override
    public Color getValue() {
        readComponent();
        return new Color(red, green, blue, alpha);
    }
}
