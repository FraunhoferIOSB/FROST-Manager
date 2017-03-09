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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import de.fraunhofer.iosb.ilt.configurable.ConfigEditor;
import de.fraunhofer.iosb.ilt.configurable.Configurable;
import de.fraunhofer.iosb.ilt.configurable.Reflection;
import de.fraunhofer.iosb.ilt.configurable.Styles;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

/**
 * An editor that offers a selection of a class that implements an interface or
 * extends a class.
 *
 * @author Hylke van der Schaaf
 * @param <C> The class type that provides context at runtime.
 * @param <D> The class type that provides context while editing.
 * @param <T> The type of object returned by getValue.
 */
public class EditorSubclass<C, D, T> extends EditorDefault<C, D, T> {

    private static final String KEY_CLASSNAME = "className";
    private static final String KEY_CLASSCONFIG = "classConfig";
    private static final Logger LOGGER = LoggerFactory.getLogger(EditorSubclass.class);
    private String selectLabel = "Available Classes:";
    private final Class<?> iface;
    private boolean merge = false;
    private String nameField = KEY_CLASSNAME;
    private String className = "";
    private String displayName = "";
    private JsonElement classConfig;
    private ConfigEditor classEditor;
    private String prefix = "";
    private C context;
    private D edtCtx;
    /**
     * Flag indicating we are in JavaFX mode.
     */
    private Boolean fx;
    // Swing components
    private JPanel swComponent;
    private JPanel swItemHolder;
    private JComboBox<String> swItems;
    // JavaFX nodes
    private BorderPane fxPaneRoot;
    private BorderPane fxPaneItem;
    private ComboBox<String> fxItems;

    public EditorSubclass(Class<?> iface, boolean merge, String nameField) {
        this.iface = iface;
        this.merge = merge;
        this.nameField = nameField;
    }

    /**
     * @param iface The interface or superclass the presented options should
     * implement or extend.
     * @param label The label to use for this instance.
     * @param description The description describing this instance.
     * @param merge Should the class name be merged into the configuration.
     * @param nameField The name of the field to use for storing the className.
     */
    public EditorSubclass(final Class<? extends T> iface, final String label, final String description, final boolean merge, final String nameField) {
        this.iface = iface;
        this.merge = merge;
        this.nameField = nameField;
        setLabel(label);
        setDescription(description);
    }

    @Override
    public void setConfig(JsonElement config, C context, D edtCtx) {
        this.context = context;
        this.edtCtx = edtCtx;
        if (config.isJsonObject()) {
            JsonObject confObj = config.getAsJsonObject();
            if (merge) {
                JsonElement classNameElem = confObj.get(nameField);
                if (classNameElem != null && classNameElem.isJsonPrimitive()) {
                    className = classNameElem.getAsString();
                }

                classConfig = confObj;
            } else {
                JsonElement classNameElem = confObj.get(KEY_CLASSNAME);
                if (classNameElem != null && classNameElem.isJsonPrimitive()) {
                    className = classNameElem.getAsString();
                }

                classConfig = confObj.get(KEY_CLASSCONFIG);
            }
        }
        if (className == null || className.isEmpty()) {
            LOGGER.info("Empty class name.");
        }
        setClassName(className);
    }

    @Override
    public JsonElement getConfig() {
        readComponent();
        JsonObject result;
        if (merge && classConfig != null && classConfig.isJsonObject()) {
            result = classConfig.getAsJsonObject();
            result.add(nameField, new JsonPrimitive(className));
        } else {
            result = new JsonObject();
            result.add(KEY_CLASSNAME, new JsonPrimitive(className));
            result.add(KEY_CLASSCONFIG, classConfig);
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
            createGui();
        }
        return swComponent;
    }

    @Override
    public Pane getNode() {
        setFx(true);
        if (fxPaneRoot == null) {
            createGui();
        }
        return fxPaneRoot;
    }

    private void createGui() {
        String[] classes = getClasses();

        displayName = className;
        if (!displayName.isEmpty()) {
            displayName = displayName.substring(prefix.length());
        }

        if (fx) {
            createPane(classes);
        } else {
            createComponent(classes);
        }
    }

    private void createComponent(String[] classes) {
        JPanel controls = new JPanel(new BorderLayout());
        controls.add(new JLabel(selectLabel), BorderLayout.WEST);

        swItems = new JComboBox<>(classes);
        controls.add(swItems, BorderLayout.CENTER);
        swItems.setSelectedItem(displayName);

        JButton addButton = new JButton("Set");
        addButton.addActionListener((ActionEvent e) -> {
            setItem();
        });
        controls.add(addButton, BorderLayout.EAST);
        swItemHolder = new JPanel(new BorderLayout());
        swComponent = new JPanel(new BorderLayout());
        swComponent.setBorder(new EtchedBorder());
        swComponent.add(controls, BorderLayout.NORTH);
        swComponent.add(swItemHolder, BorderLayout.CENTER);
        fillComponent();
    }

    private void createPane(String[] classes) {
        BorderPane controls = new BorderPane();
        controls.setLeft(new Label(selectLabel));

        fxItems = new ComboBox<>(FXCollections.observableArrayList(classes));
        fxItems.getSelectionModel().select(displayName);
        controls.setCenter(fxItems);

        Button addButton = new Button("set");
        addButton.setOnAction(event -> setItem());
        controls.setRight(addButton);

        fxPaneItem = new BorderPane();
        fxPaneItem.setPadding(new Insets(0, 0, 0, 5));
        fxPaneRoot = new BorderPane();
        fxPaneRoot.setStyle(Styles.STYLE_BORDER);
        fxPaneRoot.setCenter(fxPaneItem);
        fxPaneRoot.setTop(controls);
        fillComponent();
    }

    private String[] getClasses() {
        List<Class> subtypes = Reflection.getSubtypesOf(iface, false);
        int i = 0;
        String[] result = new String[subtypes.size()];
        for (Class subtype : subtypes) {
            result[i] = subtype.getName();
            i++;
        }
        if (result.length == 0) {
            return result;
        }
        boolean end = false;
        int length = 0;
        while (!end) {
            length++;
            String test = result[0].substring(0, length);
            for (String name : result) {
                if (!name.startsWith(test)) {
                    end = true;
                    break;
                }
            }
            if (!end) {
                prefix = test;
            }
        }
        LOGGER.info("Found prefix to be: {}", prefix);
        if (length > 0) {
            for (i = 0; i < result.length; i++) {
                result[i] = result[i].substring(prefix.length());
            }
        }
        Arrays.sort(result);
        return result;
    }

    private void setItem() {
        if (fx) {
            String cName = fxItems.getSelectionModel().getSelectedItem();
            if (cName != null && !cName.isEmpty()) {
                setClassName(prefix + cName);
            }
        } else {
            int idx = swItems.getSelectedIndex();
            if (idx >= 0) {
                String cName = swItems.getModel().getElementAt(idx);
                setClassName(prefix + cName);
            }
        }
    }

    private void setClassName(String name) {
        className = name;
        displayName = className;
        if (name == null || name.isEmpty()) {
            return;
        }
        displayName = className.substring(prefix.length());
        Class<?> loadedClass = null;
        Object instance = null;
        ClassLoader cl = getClass().getClassLoader();
        try {
            loadedClass = cl.loadClass(className);
        } catch (ClassNotFoundException e) {
            LOGGER.trace("Could not find class {}. Not a full class name?", className);
            LOGGER.trace("Exception loading class.", e);
        }

        if (loadedClass == null) {
            className = findClass(className);
            try {
                loadedClass = cl.loadClass(className);
            } catch (ClassNotFoundException e) {
                LOGGER.warn("Exception loading class.", e);
            }
        }

        if (loadedClass != null) {
            try {
                instance = loadedClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                LOGGER.warn("Exception instantiating class {}.", className);
                LOGGER.trace("Exception instantiating class.", e);
            }
        }

        if (instance != null && instance instanceof Configurable) {
            Configurable confInstance = (Configurable) instance;
            classEditor = confInstance.getConfigEditor(context, edtCtx);
            classEditor.setConfig(classConfig, context, edtCtx);
        } else {
            LOGGER.warn("Class {} is not configurable.", className);
            classEditor = null;
        }

        fillComponent();
    }

    private String findClass(String from) {
        String[] classes = getClasses();
        for (String name : classes) {
            if (name.endsWith(from)) {
                LOGGER.debug("Mapping {} to {}.", from, name);
                return name;
            }
        }
        return from;
    }

    private void fillComponent() {
        if (fx == null) {
            return;
        }
        String label;
        if (className == null || className.isEmpty()) {
            label = "No Class selected.";
        } else {
            label = "Selected: " + className.substring(prefix.length());
        }
        if (fx) {
            fxPaneItem.getChildren().clear();
            fxPaneItem.setTop(new Label(label));
            if (classEditor == null) {
                Label noConf = new Label("Nothing to be configured.");
                fxPaneItem.setCenter(noConf);
            } else {
                fxPaneItem.setCenter(classEditor.getNode());
            }
        } else {
            swItemHolder.removeAll();
            Dimension dim = new Dimension(5, 5);
            swItemHolder.add(new Box.Filler(dim, dim, dim), BorderLayout.WEST);
            swItemHolder.add(new JLabel(label), BorderLayout.NORTH);
            if (classEditor == null) {
                JLabel noConf = new JLabel("Nothing to be configured.");
                swItemHolder.add(noConf, BorderLayout.CENTER);
            } else {
                swItemHolder.add(classEditor.getComponent(), BorderLayout.CENTER);
            }
            swItemHolder.invalidate();
            swComponent.revalidate();
            swComponent.repaint();
        }
    }

    private void readComponent() {
        if (classEditor != null) {
            classConfig = classEditor.getConfig();
        }
    }

    @Override
    public T getValue() {
        readComponent();
        try {
            ClassLoader cl = getClass().getClassLoader();
            Class<?> loadedClass = cl.loadClass(className);
            Object instance = loadedClass.newInstance();

            if (instance instanceof Configurable) {
                Configurable confInstance = (Configurable) instance;
                confInstance.configure(classConfig, context, edtCtx);
            }
            return (T) instance;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOGGER.warn("Exception instantiating class {}.", className);
            LOGGER.trace("Exception instantiating class.", e);
            return null;
        }
    }

    public EditorSubclass<C, D, T> setSelectLabel(String selectLabel) {
        this.selectLabel = selectLabel;
        return this;
    }

}
