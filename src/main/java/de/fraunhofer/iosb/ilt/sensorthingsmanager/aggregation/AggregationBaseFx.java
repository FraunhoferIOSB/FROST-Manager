/*
 * Copyright (C) 2019 Fraunhofer IOSB
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
package de.fraunhofer.iosb.ilt.sensorthingsmanager.aggregation;

import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

/**
 * JavaFX parts for AggregationBase.
 *
 * @author scf
 */
public class AggregationBaseFx {

    private final AggregationBase base;
    private final StringProperty baseName;

    private final Map<AggregationLevel, AggregateCombo> combosByLevel;
    private final Map<AggregationLevel, BooleanProperty> levelToggles = new HashMap<>();

    public AggregationBaseFx(AggregationBase base) {
        this.base = base;
        this.baseName = new SimpleStringProperty(base.getBaseName());
        combosByLevel = base.getCombosByLevel();
    }

    public StringProperty getBaseNameProperty() {
        return baseName;
    }

    public BooleanProperty getLevelProperty(final AggregationLevel level) {
        BooleanProperty property = levelToggles.get(level);
        if (property == null) {
            boolean hasProp = combosByLevel.containsKey(level);
            property = new SimpleBooleanProperty(hasProp);
            levelToggles.put(level, property);
            property.addListener(
                    (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                        base.toggleLevel(level, newValue);
                    });
        }
        return property;
    }

    public Map<AggregationLevel, BooleanProperty> getLevelToggles() {
        return levelToggles;
    }

    public void comboAdded(AggregationLevel level) {
        BooleanProperty presentProp = levelToggles.get(level);
        if (presentProp != null) {
            presentProp.set(true);
        }

    }
}
