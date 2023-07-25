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
