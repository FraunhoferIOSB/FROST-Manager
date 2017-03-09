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
package de.fraunhofer.iosb.ilt.configurable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for Reflections.
 *
 * @author Hylke van der Schaaf
 */
public class Reflection {

    private static final Logger LOGGER = LoggerFactory.getLogger(Reflection.class.getName());
    private static Reflections reflections;

    public static synchronized Reflections getReflections() {
        if (reflections == null) {
            ConfigurationBuilder config = new ConfigurationBuilder().useParallelExecutor().setUrls(ClasspathHelper.forClassLoader());
            try {
                reflections = new Reflections(config);
            } finally {
                final ExecutorService executorService = config.getExecutorService();
                if (executorService != null) {
                    executorService.shutdown();
                }
            }
        }
        return reflections;
    }

    /**
     * Returns all subtypes of the given class or interface.
     *
     * @param parent The class or interface to get subtypes of.
     * @return
     */
    public static Set<Class> getSubtypesOf(Class parent) {
        return getReflections().getSubTypesOf(parent);
    }

    /**
     * Returns all subtypes of the given class or interface. Optionally
     * filtering out all interfaces.
     *
     * @param parent The class or interface to find subclasses of.
     * @param interfaces Should interfaces be included.
     * @return
     */
    public static List<Class> getSubtypesOf(Class parent, boolean interfaces) {
        Set<Class> types = getReflections().getSubTypesOf(parent);
        List<Class> result = new ArrayList<>();
        for (Class subtype : types) {
            if (interfaces || !subtype.isInterface()) {
                result.add(subtype);
            }
        }
        return result;
    }
}
