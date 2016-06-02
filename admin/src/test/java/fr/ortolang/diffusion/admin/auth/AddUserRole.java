package fr.ortolang.diffusion.admin.auth;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 *
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty ofpu
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.junit.Test;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AddUserRole {

    private static final Logger LOGGER = Logger.getLogger(AddUserRole.class.getName());

    @Test
    public void addUserRoleToAllUsers() throws IOException, KeycloakAdminClient.Failure {

        AccessTokenResponse res = KeycloakAdminClient.getToken();
        UserRepresentation[] users = KeycloakAdminClient.getUsers(res);

        for (UserRepresentation user : users) {
            RoleRepresentation[] roleRepresentations = KeycloakAdminClient.getRealmRoleMapping(res, user);
            for (RoleRepresentation roleRepresentation : roleRepresentations) {
                if (roleRepresentation.getName().equals("user")) {
                    LOGGER.log(Level.INFO, "Add role 'user' to user: " + user.getFirstName() + " " + user.getLastName() + " (" + user.getUsername() + ")");
                    KeycloakAdminClient.addUserRoleMappings(res, user, new RoleRepresentation[]{roleRepresentation});
                    break;
                }
            }
        }
    }
}
