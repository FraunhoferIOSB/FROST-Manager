package de.fraunhofer.iosb.ilt.sensorthingsmanager.controller;

import de.fraunhofer.iosb.ilt.sta.model.Entity;

/**
 *
 * @author scf
 * @param <T> The entity type.
 */
public class EntityListEntry<T extends Entity<T>> {

    private T entity;
    private boolean changed = false;

    @Override
    public String toString() {
        return entity.toString();
    }

    /**
     * @return the entity
     */
    public T getEntity() {
        return entity;
    }

    /**
     * @param entity the entity to set.
     * @return this EntityListEntry.
     */
    public EntityListEntry<T> setEntity(T entity) {
        this.entity = entity;
        return this;
    }

    /**
     * @return the changed
     */
    public boolean isChanged() {
        return changed;
    }

    /**
     * @param changed the changed to set.
     * @return this EntityListEntry.
     */
    public EntityListEntry<T> setChanged(boolean changed) {
        this.changed = changed;
        return this;
    }

}
