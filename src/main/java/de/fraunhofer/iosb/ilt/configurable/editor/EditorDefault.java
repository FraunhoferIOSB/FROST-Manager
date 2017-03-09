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

import de.fraunhofer.iosb.ilt.configurable.ConfigEditor;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.html.HTMLEditorKit;

/**
 *
 * @author Hylke van der Schaaf
 * @param <C> The class type that provides context at runtime.
 * @param <D> The class type that provides context while editing.
 * @param <T> The type of object returned by getValue.
 */
public abstract class EditorDefault<C, D, T> implements ConfigEditor<C, D, T> {

    /**
     * The human readable label for this editor.
     */
    private String label = "";
    /**
     * The longer description for this editor.
     */
    private String description = "";
    /**
     * The help frame.
     */
    private static JFrame helpFrame;
    private static JTextPane textPane;

    /**
     * The human readable label for this editor.
     *
     * @return the label
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * The human readable label for this editor.
     *
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * The longer description for this editor.
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * The longer description for this editor.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    protected JButton getHelpButton() {
        JButton button = new JButton("?");
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));
        button.addActionListener((ActionEvent e) -> popupDescription());
        return button;
    }

    public void popupDescription() {
        getHelpFrame();
        textPane.setText("<html><body>" + description + "</body></html>");
        helpFrame.setSize(400, 600);
        helpFrame.setVisible(true);
    }

    public static JFrame getHelpFrame() {
        if (helpFrame == null) {
            helpFrame = new JFrame("Help");
            helpFrame.setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);

            JScrollPane jScrollPane1 = new JScrollPane();
            textPane = new JTextPane();
            textPane.setEditorKit(new HTMLEditorKit());
            jScrollPane1.setViewportView(textPane);

            helpFrame.getContentPane().add(jScrollPane1, java.awt.BorderLayout.CENTER);
        }
        return helpFrame;
    }
}
