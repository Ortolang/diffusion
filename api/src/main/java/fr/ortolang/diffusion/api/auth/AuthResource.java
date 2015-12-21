package fr.ortolang.diffusion.api.auth;

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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import fr.ortolang.diffusion.api.content.ContentResource;

@Path("/auth")
public class AuthResource {
    
    private static final Logger LOGGER = Logger.getLogger(ContentResource.class.getName());
    
    public static final String REDIRECT_PATH_PARAM_NAME = "redirect";
    
    @Context
    private UriInfo uriInfo;
    
    @GET
    public Response authenticate(@CookieParam(REDIRECT_PATH_PARAM_NAME) String credirect, @QueryParam(REDIRECT_PATH_PARAM_NAME) String qredirect) {
        LOGGER.log(Level.INFO, "GET /content/auth");
        UriBuilder builder = uriInfo.getBaseUriBuilder();
        if (credirect != null && credirect.length() > 0) {
            LOGGER.log(Level.FINE, "redirecting to path found in cookie : " + credirect);
            builder.path(credirect);
        } else if (qredirect != null && qredirect.length() > 0) {
            LOGGER.log(Level.FINE, "redirecting to path found in query : " + qredirect);
            builder.path(qredirect);
        }
        return Response.seeOther(builder.build()).build();
    }

}
