package de.fraunhofer.iosb.ilt.sensorthingsmanager.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
            mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
            mapper.registerModule(new JavaTimeModule());
            // Write any date/time values as ISO-8601 formated strings.
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }

        return mapper;
    }

}
