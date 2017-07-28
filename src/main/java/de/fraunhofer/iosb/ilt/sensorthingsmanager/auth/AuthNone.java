package de.fraunhofer.iosb.ilt.sensorthingsmanager.auth;

import com.google.gson.JsonElement;
import de.fraunhofer.iosb.ilt.configurable.ConfigEditor;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorNull;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;

/**
 *
 * @author scf
 */
public class AuthNone implements AuthMethod {

    @Override
    public void configure(JsonElement config, Object context, Object edtCtx) {
        // Nothing to configure
    }

    @Override
    public ConfigEditor<?> getConfigEditor(Object context, Object edtCtx) {
        return new EditorNull();
    }

    @Override
    public void setAuth(SensorThingsService service) {
        // Do Nothing.
    }

}
