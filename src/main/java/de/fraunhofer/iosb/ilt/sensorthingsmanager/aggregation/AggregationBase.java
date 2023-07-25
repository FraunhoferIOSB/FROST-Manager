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

import de.fraunhofer.iosb.ilt.frostclient.model.Entity;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scf
 */
public class AggregationBase {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregationBase.class);

    private final String baseName;
    private Entity baseDatastream;
    private Entity baseMultiDatastream;

    private final Set<AggregateCombo> combos = new TreeSet<>();
    private final Map<AggregationLevel, AggregateCombo> combosByLevel = new HashMap<>();
    private final Map<AggregationLevel, Boolean> wantedLevels = new HashMap<>();

    private AggregationBaseFx fxProperties;

    public AggregationBase(String baseName) {
        this.baseName = baseName;
    }

    public AggregationBase(String baseName, Entity baseDatastream, Entity baseMultiDatastream) {
        this.baseName = baseName;
        this.baseDatastream = baseDatastream;
        this.baseMultiDatastream = baseMultiDatastream;
    }

    public String getBaseName() {
        return baseName;
    }

    public AggregationBaseFx getFxProperties() {
        if (fxProperties == null) {
            fxProperties = new AggregationBaseFx(this);
        }
        return fxProperties;
    }

    protected void toggleLevel(final AggregationLevel level, boolean toValue) {
        if (toValue) {
            LOGGER.info("Adding level {} to base {}.", level, baseName);
            wantedLevels.put(level, true);
        } else {
            LOGGER.info("Removing level {} from base {}.", level, baseName);
            wantedLevels.put(level, false);
        }
    }

    public Set<AggregateCombo> getCombos() {
        return combos;
    }

    public Map<AggregationLevel, AggregateCombo> getCombosByLevel() {
        return combosByLevel;
    }

    public void addCombo(AggregateCombo combo) {
        AggregationLevel level = combo.level;

        combos.add(combo);

        AggregateCombo old = combosByLevel.put(level, combo);
        if (old != null) {
            LOGGER.warn("Multiple combos of level {} found for base {}.", level, getBaseName());
        }
        if (fxProperties != null) {
            fxProperties.comboAdded(level);
        }
    }

    public Map<AggregationLevel, Boolean> getWantedLevels() {
        return wantedLevels;
    }

    public Entity getBaseDatastream() {
        return baseDatastream;
    }

    public void setBaseDatastream(Entity baseDatastream) {
        this.baseDatastream = baseDatastream;
    }

    public Entity getBaseMultiDatastream() {
        return baseMultiDatastream;
    }

    public void setBaseMultiDatastream(Entity baseMultiDatastream) {
        this.baseMultiDatastream = baseMultiDatastream;
    }

}
