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

import de.fraunhofer.iosb.ilt.frostclient.dao.BaseDao;
import de.fraunhofer.iosb.ilt.frostclient.exception.ServiceFailureException;
import de.fraunhofer.iosb.ilt.frostclient.exception.StatusCodeException;
import de.fraunhofer.iosb.ilt.frostclient.model.Entity;
import de.fraunhofer.iosb.ilt.frostclient.model.EntitySet;
import java.io.PrintWriter;
import java.io.StringWriter;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scf
 */
public class Utils {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    private Utils() {
        // Utility class, not to be instantiated.
    }

    /**
     * Returns true if the given string is null, or empty.
     *
     * @param string the string to check.
     * @return true if string == null || string.isEmpty()
     */
    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public static void showAlert(Alert.AlertType type, String title, String text, Throwable exception) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(text);

        if (exception != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.append(exception.getMessage() + "\n");
            exception.printStackTrace(pw);

            String exceptionText = sw.toString();
            if (exception instanceof StatusCodeException) {
                StatusCodeException sce = (StatusCodeException) exception;
                exceptionText = "Server Returned:" + sce.getReturnedContent() + "\n\nException:\n" + exceptionText;
            }
            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(false);

            alert.getDialogPane().setExpandableContent(textArea);
        }
        alert.showAndWait();
    }

    public static void deleteAll(BaseDao doa) throws ServiceFailureException {
        boolean more = true;
        int count = 0;
        while (more) {
            EntitySet entities = doa.query().count(true).list();
            if (entities.getCount() > 0) {
                LOGGER.info("{} to go.", entities.getCount());
            } else {
                more = false;
            }
            for (Entity entity : entities) {
                doa.delete(entity);
                count++;
            }
        }
        LOGGER.info("Deleted {} using {}.", count, doa.getClass().getName());
    }

}
