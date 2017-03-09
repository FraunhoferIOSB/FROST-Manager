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

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.BorderPane;

/**
 *
 * @author Hylke van der Schaaf
 * @param <C> The class type that provides context at runtime.
 * @param <D> The class type that provides context while editing.
 */
public class EditorString<C, D> extends EditorDefault<C, D, String> {

    private final String deflt;
    private String value;
    private int lines = 5;
    /**
     * Flag indicating we are in JavaFX mode.
     */
    private Boolean fx;
    // Swing components
    private JTextComponent swText;
    private JComponent swComponent;
    // JavaFX Nodes
    private TextInputControl fxNode;

    public EditorString(String deflt, int lines) {
        this.deflt = deflt;
        this.value = deflt;
        this.lines = lines;
    }

    public EditorString(String deflt, int lines, String label, String description) {
        this.deflt = deflt;
        this.value = deflt;
        this.lines = lines;
        setLabel(label);
        setDescription(description);
    }

    @Override
    public void setConfig(JsonElement config, C context, D edtCtx) {
        if (config.isJsonPrimitive()) {
            value = config.getAsJsonPrimitive().getAsString();
        } else {
            value = deflt;
        }
        fillComponent();
    }

    @Override
    public JsonElement getConfig() {
        readComponent();
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
        if (lines == 1) {
            fxNode = new TextField();
        } else {
            TextArea text = new TextArea();
            text.setPrefRowCount(lines);
            fxNode = text;
        }
        fillComponent();
    }

    private void createComponent() {
        if (lines == 1) {
            swText = new JTextField();
            swComponent = swText;
        } else {
            JTextArea textArea = new JTextArea();
            swText = textArea;
            textArea.setRows(lines);
            textArea.setLineWrap(true);
            JScrollPane jScrollPane = new JScrollPane();
            jScrollPane.setViewportView(textArea);
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(jScrollPane, BorderLayout.CENTER);
            swComponent = panel;
        }
        fillComponent();
    }

    /**
     * Ensure the swComponent represents the current value.
     */
    private void fillComponent() {
        if (fx == null) {
            return;
        }
        if (fx) {
            fxNode.setText(value);
        } else {
            swText.setText(value);
        }
    }

    private void readComponent() {
        if (fx == null) {
            return;
        }
        if (fx) {
            value = fxNode.getText();
        } else {
            value = swText.getText();
        }
    }

    @Override
    public String getValue() {
        readComponent();
        return value;
    }
}
