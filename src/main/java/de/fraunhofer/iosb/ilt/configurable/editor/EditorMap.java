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

import java.util.HashMap;
import java.util.Map;

/**
 * An editor for a collection of editors, of various types, that can be selected
 * from a list.
 *
 * @author Hylke van der Schaaf
 * @param <C> The class type that provides context at runtime.
 * @param <D> The class type that provides context while editing.
 * @param <T> The type of object returned by getValue.
 */
public class EditorMap<C, D, T> extends AbstractEditorMap<C, D, Map<String, T>, T> {

    public EditorMap() {
        super();
    }

    public EditorMap(int columns) {
        super(columns);
    }

    @Override
    public Map<String, T> getValue() {
        Map<String, T> result = new HashMap<>();
        for (String name : value) {
            Item<C, D, T> item = options.get(name);
            T val = item.editor.getValue();
            result.put(name, val);
        }
        return result;
    }

    public double getDouble(String name, double deflt) {
        Item<C, D, T> item = options.get(name);
        Object result = item.editor.getValue();
        if (result instanceof Number) {
            return ((Number) result).doubleValue();
        }
        return deflt;
    }

    public long getLong(String name, long deflt) {
        Item<C, D, T> item = options.get(name);
        Object result = item.editor.getValue();
        if (result instanceof Number) {
            return ((Number) result).longValue();
        }
        return deflt;
    }

    public Map<String, Long> getLongMap() {
        Map<String, Long> result = new HashMap<>();
        for (String name : value) {
            result.put(name, getLong(name, 0));
        }
        return result;
    }
}
