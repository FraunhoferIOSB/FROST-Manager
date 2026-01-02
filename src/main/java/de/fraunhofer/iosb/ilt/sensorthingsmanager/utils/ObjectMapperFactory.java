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
package de.fraunhofer.iosb.ilt.sensorthingsmanager.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.fraunhofer.iosb.ilt.frostclient.json.SimpleJsonMapper;

/**
 * Factory for ObjectMapper instances. Keeps track of configuration.
 *
 * @author Nils Sommer
 *
 */
public final class ObjectMapperFactory {

    private static ObjectMapper mapper;

    private ObjectMapperFactory() {
    }

    /**
     * Get a preconfigured, long living instance of {@link ObjectMapper} with
     * all custom modules needed.
     *
     * @return the object mapper
     */
    public static ObjectMapper get() {
        if (mapper == null) {
            mapper = SimpleJsonMapper.getSimpleObjectMapper().copy();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }

        return mapper;
    }

}
