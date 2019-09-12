/*
 * Copyright (C) 2018 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.sensorthingsmanager.utils;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;

/**
 *
 * @author scf
 * @param <S> The type of the TableView generic type (i.e. S ==
 * TableView&lt;S&gt;). This should also match with the first generic type in
 * TableColumn.
 * @param <T> The type of the item contained within the Cell.
 */
public abstract class ButtonTableCell<S, T> extends TableCell<S, T> {

    private final String buttonText;
    private final Button button;

    public ButtonTableCell(String buttonText) {
        this.buttonText = buttonText;
        button = new Button(buttonText);
        button.setOnAction((ActionEvent event) -> {
            onAction(getTableRow());
        });
    }

    public abstract void onAction(TableRow<S> row);

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty) {
            setGraphic(button);
        }
    }

}
