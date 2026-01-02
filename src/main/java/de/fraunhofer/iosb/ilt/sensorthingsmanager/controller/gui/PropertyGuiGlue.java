/*
 * Copyright (C) 2024 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.sensorthingsmanager.controller.gui;

import static de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypePrimitive.EDM_BOOLEAN_NAME;
import static de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypePrimitive.EDM_DATETIMEOFFSET_NAME;
import static de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypePrimitive.EDM_GEOMETRY_NAME;
import static de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypePrimitive.EDM_INT16_NAME;
import static de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypePrimitive.EDM_INT32_NAME;
import static de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypePrimitive.EDM_INT64_NAME;
import static de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypePrimitive.EDM_STRING_NAME;
import static de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypePrimitive.EDM_UNTYPED_NAME;

import de.fraunhofer.iosb.ilt.frostclient.model.ComplexValue;
import de.fraunhofer.iosb.ilt.frostclient.model.PropertyType;
import de.fraunhofer.iosb.ilt.frostclient.model.property.EntityProperty;
import de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypeCollection;
import de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypeComplex;
import de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypePrimitive;
import de.fraunhofer.iosb.ilt.frostclient.model.property.type.TypeSimple;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.layout.GridPane;

/**
 * A Interface for binding properties to GUI elements.
 *
 * @param <T> The exact class of the implementation.
 */
public interface PropertyGuiGlue<T extends PropertyGuiGlue<T>> {

    /**
     * Read the value from the Entity and push it to the GUI element.
     */
    public void entityToGui();

    /**
     * Read the value from the Gui and push it to the Entity.
     */
    public void guiToEntity();

    public boolean isGuiNullOrEmpty();

    public T setEnabled(boolean enabled);

    /**
     * @return the parent "entity" that the value of this editor will be set on.
     */
    public ComplexValue getEntity();

    /**
     *
     * @param entity the parent "entity" that the value of this editor should be
     * loaded from and set on.
     */
    public void setEntity(ComplexValue<? extends ComplexValue> entity);

    static PropertyGuiGlue createGuiElement(ComplexValue entity, EntityProperty property, boolean editable, GridPane gridProperties, AtomicInteger itemCount) {
        return createGuiElement("", entity, property, editable, gridProperties, itemCount);
    }

    static PropertyGuiGlue createGuiElement(String namePrfx, ComplexValue entity, EntityProperty property, boolean editable, GridPane gridProperties, AtomicInteger itemCount) {
        PropertyType pt = property.getType();
        if (pt instanceof TypeComplex ptc) {
            if (ptc.isOpenType()) {
                return new GuiGlueOpenType(entity, property)
                        .init(namePrfx, gridProperties, itemCount, editable);
            } else {
                return new GuiGlueComplex(entity, property)
                        .init(namePrfx, gridProperties, itemCount, editable);
            }
        }
        if (pt instanceof TypeSimple ts) {
            pt = ts.getUnderlyingType();
        }
        if (pt instanceof TypePrimitive ptsp) {
            switch (ptsp.getName()) {
                case EDM_BOOLEAN_NAME:
                    return new GuiGlueSimpleBoolean(entity, property)
                            .init(namePrfx, gridProperties, itemCount, editable);

                case EDM_GEOMETRY_NAME:
                    return new GuiGlueGeometry(entity, property)
                            .init(namePrfx, gridProperties, itemCount, editable);

                case EDM_INT16_NAME:
                case EDM_INT32_NAME:
                case EDM_INT64_NAME:
                    return new GuiGlueSimpleLong(entity, property)
                            .init(namePrfx, gridProperties, itemCount, editable);

                case EDM_DATETIMEOFFSET_NAME:
                    return new GuiGlueSimpleDateTime(entity, property)
                            .init(namePrfx, gridProperties, itemCount, editable);

                case EDM_UNTYPED_NAME:
                    return new GuiGlueUntyped(entity, property)
                            .init(namePrfx, gridProperties, itemCount, editable);

                case EDM_STRING_NAME:
                default:
                    return new GuiGlueSimpleString(entity, property)
                            .init(namePrfx, gridProperties, itemCount, editable);
            }
        }
        if (pt instanceof TypeCollection tc) {
            return new GuiGlueCollection(entity, property, tc.getContaintedType())
                    .init(namePrfx, gridProperties, itemCount, editable);
        }
        return new GuiGlueSimpleString(entity, property)
                .init(namePrfx, gridProperties, itemCount, editable);
    }

}
