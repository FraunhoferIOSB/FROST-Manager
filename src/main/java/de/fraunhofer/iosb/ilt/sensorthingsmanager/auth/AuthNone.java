package de.fraunhofer.iosb.ilt.sensorthingsmanager.auth;

import com.google.gson.JsonElement;
import de.fraunhofer.iosb.ilt.configurable.ConfigEditor;
import de.fraunhofer.iosb.ilt.configurable.ConfigurationException;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorNull;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;

/**
 *
 * @author scf
 */
public class AuthNone implements AuthMethod {

    @Override
    public void configure(JsonElement config, Void context, Void edtCtx, ConfigEditor<?> ce) {
        // Nothing to configure
    }

    @Override
    public ConfigEditor<?> getConfigEditor(Void context, Void edtCtx) {
        return new EditorNull();
    }

    @Override
    public void setAuth(SensorThingsService service) {
        // Do Nothing.
    }

}
