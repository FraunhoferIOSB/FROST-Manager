/*
 * Copyright (C) 2019 Fraunhofer IOSB
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.sensorthingsmanager.auth;

import de.fraunhofer.iosb.ilt.configurable.Configurable;
import de.fraunhofer.iosb.ilt.frostclient.SensorThingsService;

/**
 * A method for authenticating users to a service.
 */
public interface AuthMethod extends Configurable<Void, Void> {

    public void setAuth(SensorThingsService service);
}
