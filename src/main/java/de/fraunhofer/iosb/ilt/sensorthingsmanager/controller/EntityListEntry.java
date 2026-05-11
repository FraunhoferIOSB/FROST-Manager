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
package de.fraunhofer.iosb.ilt.sensorthingsmanager.controller;

import de.fraunhofer.iosb.ilt.frostclient.model.Entity;

/**
 * The GUI element that represents an Entity in a GUI List.
 */
public class EntityListEntry {

    private Entity entity;
    private boolean changed = false;
    private boolean toLink = false;
    private boolean toUnlink = false;

    @Override
    public String toString() {
        return entity.toString();
    }

    /**
     * @return the entity
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * @param entity the entity to set.
     * @return this EntityListEntry.
     */
    public EntityListEntry setEntity(Entity entity) {
        this.entity = entity;
        return this;
    }

    /**
     * @return the changed flag.
     */
    public boolean isChanged() {
        return changed;
    }

    /**
     * @param changed the value to set the changed flag to.
     * @return this EntityListEntry.
     */
    public EntityListEntry setChanged(boolean changed) {
        this.changed = changed;
        return this;
    }

    public boolean isToLink() {
        return toLink;
    }

    public EntityListEntry setToLink(boolean toLink) {
        this.toLink = toLink;
        return this;
    }

    public boolean isToUnlink() {
        return toUnlink;
    }

    public EntityListEntry setToUnlink(boolean toUnlink) {
        this.toUnlink = toUnlink;
        return this;
    }

}
