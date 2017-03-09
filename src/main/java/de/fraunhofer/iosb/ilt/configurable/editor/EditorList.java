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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import de.fraunhofer.iosb.ilt.configurable.ConfigEditor;
import de.fraunhofer.iosb.ilt.configurable.EditorFactory;
import de.fraunhofer.iosb.ilt.configurable.Styles;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

/**
 * An editor for a list of editors, all of the same type.
 *
 * @author Hylke van der Schaaf
 * @param <C> The class type that provides context at runtime.
 * @param <D> The class type that provides context while editing.
 * @param <T> The type of editors that edit the items in the list.
 * @param <U> The type of items in the list.
 */
public class EditorList<C, D, U, T extends ConfigEditor<C, D, U>> extends EditorDefault<C, D, List<U>> implements Iterable<T> {

    private final EditorFactory<T> factory;
    private final List<T> value = new ArrayList<>();
    private C context;
    private D edtCtx;
    /**
     * Flag indicating we are in JavaFX mode.
     */
    private Boolean fx;
    // Swing components
    private JPanel swComponent;
    private JPanel swListHolder;
    // FX Nodes
    private BorderPane fxPaneRoot;
    private GridPane fxPaneList;

    public EditorList(EditorFactory<T> factory) {
        this.factory = factory;
    }

    public EditorList(EditorFactory<T> factory, String label, String description) {
        this.factory = factory;
        setLabel(label);
        setDescription(description);
    }

    @Override
    public void setConfig(JsonElement config, C context, D edtCtx) {
        this.context = context;
        this.edtCtx = edtCtx;
        value.clear();
        if (config != null && config.isJsonArray()) {
            JsonArray asArray = config.getAsJsonArray();
            for (JsonElement subConf : asArray) {
                T item = factory.createEditor();
                item.setConfig(subConf, context, edtCtx);
                value.add(item);
            }
        }
        fillComponent();
    }

    @Override
    public JsonElement getConfig() {
        JsonArray result = new JsonArray();
        for (T item : value) {
            result.add(item.getConfig());
        }
        return result;
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
    public Pane getNode() {
        setFx(true);
        if (fxPaneRoot == null) {
            createPane();
        }
        return fxPaneRoot;
    }

    private void createPane() {
        BorderPane controls = new BorderPane();
        Label addLabel = new Label("Add item");
        addLabel.setAlignment(Pos.BASELINE_RIGHT);
        controls.setCenter(addLabel);

        Button addButton = new Button("+");
        addButton.setOnAction(event -> addItem());
        controls.setRight(addButton);

        fxPaneList = new GridPane();
        fxPaneRoot = new BorderPane();
        fxPaneRoot.setStyle(Styles.STYLE_BORDER);
        fxPaneRoot.setTop(controls);
        fxPaneRoot.setCenter(fxPaneList);
        fillComponent();
    }

    private void createComponent() {
        JPanel controls = new JPanel(new BorderLayout());
        controls.add(new JLabel("Add item", SwingConstants.RIGHT), BorderLayout.CENTER);
        JButton addButton = new JButton("+");
        addButton.addActionListener(event -> addItem());
        controls.add(addButton, BorderLayout.EAST);
        swListHolder = new JPanel(new GridBagLayout());
        swComponent = new JPanel(new BorderLayout());
        swComponent.setBorder(new EtchedBorder());
        swComponent.add(controls, BorderLayout.NORTH);
        swComponent.add(swListHolder, BorderLayout.CENTER);
        fillComponent();
    }

    public void addItem() {
        final T item = factory.createEditor();
        item.setConfig(null, context, edtCtx);
        value.add(item);
        fillComponent();
    }

    public void removeItem(T item) {
        value.remove(item);
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
            fxPaneList.getChildren().clear();
            if (value.isEmpty()) {
                fxPaneList.add(new Label("No items added."), 0, 0);
            }
            int row = 0;
            for (final T item : value) {
                Node pane = item.getNode();
                GridPane.setConstraints(pane, 0, row, 1, 1, HPos.LEFT, VPos.BASELINE, Priority.ALWAYS, Priority.SOMETIMES);

                Button removeButton = new Button("-");
                removeButton.setOnAction(event -> removeItem(item));
                GridPane.setConstraints(removeButton, 1, row, 1, 1, HPos.RIGHT, VPos.TOP, Priority.NEVER, Priority.NEVER);

                fxPaneList.getChildren().addAll(pane, removeButton);
            }
        } else {
            swListHolder.removeAll();
            if (value.isEmpty()) {
                swListHolder.add(new JLabel("No items added."));
            }
            GridBagConstraints gbc;
            int row = 0;
            Insets insets = new Insets(1, 1, 1, 1);
            for (final T item : value) {
                gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = row;
                gbc.weightx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = insets;
                swListHolder.add(item.getComponent(), gbc);

                JButton removeButton = new JButton("-");
                removeButton.addActionListener(event -> removeItem(item));
                gbc = new GridBagConstraints();
                gbc.gridx = 1;
                gbc.gridy = row;
                gbc.insets = insets;
                swListHolder.add(removeButton, gbc);
                row++;
            }
            swListHolder.invalidate();
            swComponent.revalidate();
        }
    }

    @Override
    public Iterator<T> iterator() {
        return value.iterator();
    }

    @Override
    public List<U> getValue() {
        List<U> valList = new ArrayList<>();
        for (T val : this) {
            valList.add(val.getValue());
        }
        return valList;
    }

}
