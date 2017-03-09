/*
 * (C) Copyright 2016 Fraunhofer IOSB, Fraunhoferstr. 1, D 76131 Karlsruhe,
 * Germany. Alle Rechte vorbehalten.
 */
package de.fraunhofer.iosb.ilt.configurable.editor;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

/**
 * An editor that does not edit anything. For cases where you want a class to be
 * configurable, but not actually have an editor.
 *
 * @author Hylke van der Schaaf
 * @param <C> The class type that provides context at runtime.
 * @param <D> The class type that provides context while editing.
 */
public class EditorNull<C, D> extends EditorDefault<C, D, Void> {

    private JComponent swComponent;
    private BorderPane fxNode;

    public EditorNull() {
    }

    public EditorNull(String label, String description) {
        setLabel(label);
        setDescription(description);
    }

    @Override
    public void setConfig(JsonElement config, Object context, Object edtCtx) {
        // Nothing to configure
    }

    @Override
    public JsonElement getConfig() {
        return JsonNull.INSTANCE;
    }

    @Override
    public JComponent getComponent() {
        if (swComponent == null) {
            swComponent = new JPanel(new BorderLayout());
        }
        return swComponent;
    }

    @Override
    public Node getNode() {
        if (fxNode == null) {
            fxNode = new BorderPane();
        }
        return fxNode;
    }

    @Override
    public Void getValue() {
        return null;
    }

}
