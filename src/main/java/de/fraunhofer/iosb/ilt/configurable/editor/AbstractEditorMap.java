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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.MutableComboBoxModel;
import javax.swing.border.EtchedBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.fraunhofer.iosb.ilt.configurable.ConfigEditor;
import de.fraunhofer.iosb.ilt.configurable.Styles;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
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
 * @param <T> The type of object returned by getValue.
 * @param <V> The type of object in the map.
 */
public abstract class AbstractEditorMap<C, D, T, V> extends EditorDefault<C, D, T> implements Iterable<String> {

    protected static final class Item<C, D, V> {

        final ConfigEditor<C, D, V> editor;
        final boolean optional;
        final int colwidth;
        final String name;
        final String label;

        public Item(final String name, final ConfigEditor<C, D, V> editor, final boolean optional, final int colwidth) {
            this.name = name;
            final String edLabel = editor.getLabel();
            if (edLabel == null || edLabel.isEmpty()) {
                label = name;
            } else {
                label = edLabel;
            }
            this.editor = editor;
            this.optional = optional;
            this.colwidth = colwidth;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return label;
        }

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEditorMap.class.getName());
    /**
     * All options
     */
    protected final Map<String, Item<C, D, V>> options = new LinkedHashMap<>();
    /**
     * The names of the selected options.
     */
    protected final Set<String> value = new HashSet<>();
    /**
     * The names of the optional options.
     */
    private final List<String> optionalOptions = new ArrayList<>();
    /**
     * Flag indicating we are in JavaFX mode.
     */
    private Boolean fx;
    // Swing components
    private JPanel swComponent;
    private JPanel swListHolder;
    private JComboBox<Item> swNames;
    private MutableComboBoxModel<Item> swModel;
    // FX Nodes
    private BorderPane fxPaneRoot;
    private GridPane fxPaneList;
    private ComboBox<Item> fxBoxNames;
    /**
     * How many columns we want to have. Defaults to 1.
     */
    private final int columns;

    public AbstractEditorMap() {
        columns = 1;
    }

    public AbstractEditorMap(int columns) {
        this.columns = columns;
    }

    public AbstractEditorMap(int columns, String label, String description) {
        this.columns = columns;
        setLabel(label);
        setDescription(description);
    }

    public void addOption(String name, ConfigEditor editor, boolean optional) {
        if (options.containsKey(name)) {
            throw new IllegalArgumentException("Map already contains an editor for " + name);
        }
        options.put(name, new Item<>(name, editor, optional, 1));
        if (optional) {
            optionalOptions.add(name);
        } else {
            addItem(name);
        }
    }

    public void addOption(String name, ConfigEditor editor, boolean optional, int width) {
        options.put(name, new Item<>(name, editor, optional, width));
        if (optional) {
            optionalOptions.add(name);
        } else {
            addItem(name);
        }
    }

    @Override
    public void setConfig(JsonElement config, C context, D edtCtx) {
        value.clear();

        if (config != null && config.isJsonObject()) {
            final JsonObject asObj = config.getAsJsonObject();
            for (final Map.Entry<String, JsonElement> entry : asObj.entrySet()) {
                final String key = entry.getKey();
                final JsonElement itemConfig = entry.getValue();
                final Item<C, D, V> item = options.get(key);
                if (item == null) {
                    if (!"$type".equals(key)) {
                        LOGGER.debug("Unknown entry {} in configuration.", key);
                    }
                } else {
                    item.editor.setConfig(itemConfig, context, edtCtx);
                    value.add(key);
                }
            }
        }
        for (final Map.Entry<String, Item<C, D, V>> entry : options.entrySet()) {
            final String key = entry.getKey();
            final Item<C, D, V> val = entry.getValue();
            if (!val.optional) {
                value.add(key);
            }
        }
        fillComponent();
    }

    @Override
    public JsonElement getConfig() {
        final JsonObject result = new JsonObject();
        for (final String key : value) {
            final Item<C, D, V> item = options.get(key);
            result.add(key, item.editor.getConfig());
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
        if (!optionalOptions.isEmpty()) {
            controls.setLeft(new Label("List of items:"));
            List<Item> optionals = new ArrayList<>();
            for (final String optionName : optionalOptions) {
                if (!value.contains(optionName)) {
                    optionals.add(options.get(optionName));
                }
            }
            optionals.sort((final Item o1, final Item o2) -> o1.label.compareTo(o2.label));
            fxBoxNames = new ComboBox<>(FXCollections.observableArrayList(optionals));
            controls.setCenter(fxBoxNames);

            Button addButton = new Button("+");
            addButton.setOnAction(event -> addItem());
            controls.setRight(addButton);
        }
        fxPaneList = new GridPane();
        fxPaneRoot = new BorderPane();
        fxPaneRoot.setStyle(Styles.STYLE_BORDER);
        fxPaneRoot.setTop(controls);
        fxPaneRoot.setCenter(fxPaneList);

        fillComponent();
    }

    private void createComponent() {
        JPanel controls = new JPanel(new BorderLayout());
        if (!optionalOptions.isEmpty()) {
            controls.add(new JLabel("List of items:"), BorderLayout.WEST);
            List<Item> optionals = new ArrayList<>();
            for (final String optionName : optionalOptions) {
                if (!value.contains(optionName)) {
                    optionals.add(options.get(optionName));
                }
            }
            optionals.sort((final Item o1, final Item o2) -> o1.label.compareTo(o2.label));
            swModel = new DefaultComboBoxModel<>(optionals.toArray(new Item[optionals.size()]));
            swNames = new JComboBox<>(swModel);
            controls.add(swNames, BorderLayout.CENTER);

            final JButton addButton = new JButton("+");
            addButton.addActionListener(event -> addItem());

            controls.add(addButton, BorderLayout.EAST);
        }

        swListHolder = new JPanel(new GridBagLayout());

        swComponent = new JPanel(new BorderLayout());
        swComponent.setBorder(new EtchedBorder());
        swComponent.add(controls, BorderLayout.NORTH);
        swComponent.add(swListHolder, BorderLayout.CENTER);

        fillComponent();
    }

    private void addItem() {
        if (fx) {
            Item item = fxBoxNames.getSelectionModel().getSelectedItem();
            if (item != null) {
                String key = item.getName();
                addItem(key);
            }
        } else {
            int idx = swNames.getSelectedIndex();
            if (idx >= 0) {
                String key = swNames.getModel().getElementAt(idx).getName();
                addItem(key);
            }
        }
    }

    public void addItem(final String key) {
        value.add(key);
        if (swModel != null) {
            swModel.removeElement(options.get(key));
        }
        if (fxBoxNames != null) {
            fxBoxNames.getItems().remove(options.get(key));
        }
        fillComponent();
    }

    public void removeItem(final String key) {
        final Item<C, D, V> item = options.get(key);
        if (item.optional) {
            value.remove(key);
            if (swModel != null) {
                swModel.addElement(item);
            }
            if (fxBoxNames != null) {
                fxBoxNames.getItems().add(item);
            }
            fillComponent();
        }
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
        } else {
            swListHolder.removeAll();
        }

        GridBagConstraints gbc;
        int row = 0;
        int endCol = -1;
        // Iterate over the options so the order is fixed.
        for (final Map.Entry<String, Item<C, D, V>> entry : options.entrySet()) {
            final String key = entry.getKey();
            if (!value.contains(key)) {
                continue;
            }
            final Item<C, D, V> item = entry.getValue();
            endCol += item.colwidth;
            if (endCol >= columns) {
                endCol = item.colwidth - 1;
                row++;
            }
            final int startCol = endCol - item.colwidth + 1;
            final int width = 3 * item.colwidth - 2;
            final int x0 = startCol * 3;
            final int x1 = x0 + 1;
            final int x2 = x0 + width + 1;

            String label = item.editor.getLabel();
            if (label.isEmpty()) {
                label = key;
            }

            if (fx) {
                addToGridFx(row, x0, label, x1, item, width, x2, key);
            } else {
                addToGridSw(row, x0, label, x1, item, width, x2, key);
            }
        }
        if (!fx) {
            swListHolder.invalidate();
            swComponent.revalidate();
            swComponent.repaint();
        }
    }

    private void addToGridFx(int row, final int x0, String label, final int x1, final Item<C, D, V> item, final int width, final int x2, final String key) {
        Label fxLabel = new Label(label);
        fxLabel.setTooltip(new Tooltip(item.editor.getDescription()));
        GridPane.setConstraints(fxLabel, x0, row, 1, 1, HPos.LEFT, VPos.BASELINE, Priority.NEVER, Priority.NEVER);
        fxPaneList.getChildren().add(fxLabel);

        Node itemPane = item.editor.getNode();
        GridPane.setConstraints(itemPane, x1, row, width, 1, HPos.LEFT, VPos.BASELINE, Priority.SOMETIMES, Priority.NEVER);
        fxPaneList.getChildren().add(itemPane);

        if (!optionalOptions.isEmpty()) {
            Button removeButton = new Button("-");
            removeButton.setDisable(!item.optional);
            removeButton.setOnAction(event -> removeItem(key));
            GridPane.setConstraints(removeButton, x2, row, 1, 1, HPos.LEFT, VPos.BASELINE, Priority.NEVER, Priority.NEVER);
            fxPaneList.getChildren().add(removeButton);
        }
    }

    private void addToGridSw(int row, final int x0, String label, final int x1, final Item<C, D, V> item, final int width, final int x2, final String key) {
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = x0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 3, 1, 1);
        final JLabel jLabel = new JLabel(label);
        jLabel.setToolTipText(item.editor.getDescription());
        swListHolder.add(jLabel, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = x1;
        gbc.gridy = row;
        gbc.gridwidth = width;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        swListHolder.add(item.editor.getComponent(), gbc);
        if (!optionalOptions.isEmpty()) {
            JButton removeButton = new JButton("-");
            removeButton.setEnabled(item.optional);
            removeButton.addActionListener(event -> removeItem(key));
            gbc = new GridBagConstraints();
            gbc.gridx = x2;
            gbc.gridy = row;
            swListHolder.add(removeButton, gbc);
        }
    }

    public void setContentsOn(final Object target) {
        for (String key : value) {
            Object val = options.get(key).editor.getValue();
            if (val == null) {
                continue;
            }

            String methodName = "set" + key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1);
            AbstractEditorMap.callMethodOn(methodName, target, val);
        }
    }

    private static boolean callMethodOn(final String methodName, final Object target, final Object val) {
        final Class<? extends Object> aClass = target.getClass();
        final Class<? extends Object> vClass = val.getClass();
        try {
            Class<? extends Object> current = aClass;
            while (current.getSuperclass() != null) {
                final Method[] declaredMethods = current.getDeclaredMethods();
                for (final Method method : declaredMethods) {
                    final String mName = method.getName();
                    final int pCount = method.getParameterCount();
                    if (pCount == 1 && mName.equals(methodName)) {
                        final Class<?> pt0 = method.getParameterTypes()[0];
                        // unfortunately this does not do autoboxing.
                        final boolean assignable = pt0.isAssignableFrom(vClass);
                        if (assignable) {
                            method.invoke(target, val);
                            return true;
                        } else {
                            LOGGER.debug("Method found, but wrong parameter.");
                        }
                    }
                }
                current = current.getSuperclass();
            }
        } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | UnsupportedOperationException exc) {
            LOGGER.debug("", exc);
        }
        LOGGER.debug("Failed call method {} on {}.", methodName, aClass.getName());
        return false;
    }

    @Override
    public Iterator<String> iterator() {
        return value.iterator();
    }

    public V getValue(final String name) {
        final Item<C, D, V> item = options.get(name);
        return item.editor.getValue();
    }

    /**
     * Checks if the given option is set.
     *
     * @param name The option to check.
     * @return true if the option is set, false otherwise.
     */
    public boolean isOptionSet(final String name) {
        return value.contains(name);
    }

}
