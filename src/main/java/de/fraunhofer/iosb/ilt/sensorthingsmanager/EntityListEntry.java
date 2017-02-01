package de.fraunhofer.iosb.ilt.sensorthingsmanager;

import de.fraunhofer.iosb.ilt.sta.model.Entity;

/**
 *
 * @author scf
 */
public class EntityListEntry {

    private Entity entity;
    private boolean changed = false;

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
    public EntityListEntry setEntity(Entity<?> entity) {
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
    public EntityListEntry setChanged(boolean changed) {
        this.changed = changed;
        return this;
    }

}
