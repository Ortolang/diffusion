package fr.ortolang.diffusion.api.referentiel;

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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fr.ortolang.diffusion.api.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.referentiel.ReferentielService;
import fr.ortolang.diffusion.referentiel.ReferentielServiceException;
import fr.ortolang.diffusion.referentiel.entity.OrganizationEntity;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import org.jboss.resteasy.annotations.GZIP;

/**
 * @resourceDescription Operations on Referentiels
 */
@Path("/referentiels")
@Produces({ MediaType.APPLICATION_JSON })
public class ReferentielEntityResource {

    private static final Logger LOGGER = Logger.getLogger(ReferentielEntityResource.class.getName());

    @EJB
    private ReferentielService referentiel;

    @GET
    @Path("/organizations")
    @GZIP
    public Response list() throws ReferentielServiceException {
        LOGGER.log(Level.INFO, "GET /organizations");

        List<OrganizationEntity> refs = referentiel.listOrganizationEntities();

        GenericCollectionRepresentation<OrganizationEntityRepresentation> representation = new GenericCollectionRepresentation<OrganizationEntityRepresentation> ();
        for(OrganizationEntity ref : refs) {
            representation.addEntry(OrganizationEntityRepresentation.fromReferentielEntity(ref));
        }
        representation.setOffset(0);
        representation.setSize(refs.size());
        representation.setLimit(refs.size());

        return Response.ok(representation).build();
    }

    @GET
    @Path("/organizations/{key}")
    @GZIP
    public Response get(@PathParam(value = "key") String key) throws ReferentielServiceException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "GET /referentiels/" + key);

        OrganizationEntityRepresentation representation = OrganizationEntityRepresentation.fromReferentielEntity(referentiel.readOrganizationEntity(key));

        return Response.ok(representation).build();
    }

}
