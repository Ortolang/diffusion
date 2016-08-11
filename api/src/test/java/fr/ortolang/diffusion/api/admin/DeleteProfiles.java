package fr.ortolang.diffusion.api.admin;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * *
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 * *
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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.jboss.resteasy.util.Base64;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class DeleteProfiles {

    @Test
    public void deleteProfileWithUpperCaseLetter() {

        try (BufferedReader br = new BufferedReader(new FileReader("PATH/TO/FILE/profiles_todelete.txt")))
        {

            Client client = ClientBuilder.newClient();

            String key;

            while ((key = br.readLine()) != null) {
                System.out.println("Removing profile with key: " + key);
                Response response = client.target("HOST/api/admin/membership/profiles/" + key).request().header("Authorization", "Basic " + Base64.encodeBytes("USERNAME:PASSWORD".getBytes())).delete();
                response.close();
                if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                    System.out.println("Profile not found. Skipping it...");
                    continue;
                }
                if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                    System.out.println("Could not remove profile, response: " + response.getStatusInfo().getStatusCode() + " " + response.getStatusInfo().getReasonPhrase());
                    return;
                }
                Thread.sleep(200);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
