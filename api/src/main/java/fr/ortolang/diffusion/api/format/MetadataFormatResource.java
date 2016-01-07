package fr.ortolang.diffusion.api.format;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.api.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import org.jboss.resteasy.annotations.GZIP;

/**
 * @resourceDescription Operations on Objects
 */
@Path("/metadataformats")
@Produces({ MediaType.APPLICATION_JSON })
public class MetadataFormatResource {

    private static final Logger LOGGER = Logger.getLogger(MetadataFormatResource.class.getName());

    @EJB
    private CoreService core;
    @EJB
    private BinaryStoreService store;

    @GET
    @GZIP
    public Response listMetadataFormat(@QueryParam(value = "name") String name) throws CoreServiceException {
        LOGGER.log(Level.INFO, "GET /metadataformats");

        GenericCollectionRepresentation<MetadataFormat> representation = new GenericCollectionRepresentation<MetadataFormat>();
        if(name!=null) {
            MetadataFormat md = core.getMetadataFormat(name);

            representation.addEntry(md);
            representation.setSize(1);
            representation.setLimit(1);
        } else {
            List<MetadataFormat> mdfs = core.listMetadataFormat();

            for (MetadataFormat mdf : mdfs) {
                representation.addEntry(mdf);
            }

            representation.setSize(mdfs.size());
            representation.setLimit(mdfs.size());
        }

        representation.setOffset(0);
        return Response.ok(representation).build();
    }

    @GET
    @Path("/download")
    public void download(final @QueryParam(value = "id") String id, final @QueryParam(value = "name") String name, @Context HttpServletResponse response) throws OrtolangException, CoreServiceException, DataNotFoundException, IOException, BinaryStoreServiceException {
        LOGGER.log(Level.INFO, "GET /metadataformats/download");

        MetadataFormat format = null;
        if ( id != null && id.length() > 0 ) {
            format = core.findMetadataFormatById(id);
        } else if ( name != null && name.length() > 0 ) {
            format = core.getMetadataFormat(name);
        } else {
            throw new DataNotFoundException("either id or name must be provided in order to find metadata format");
        }
        if ( format != null ) {
            response.setHeader("Content-Disposition", "attachment; filename=" + format.getName());
            response.setContentType(format.getMimeType());
            response.setContentLength((int)format.getSize());
            InputStream input = store.get(format.getSchema());
            try {
                IOUtils.copy(input, response.getOutputStream());
            } finally {
                IOUtils.closeQuietly(input);
            }
        } else {
            throw new DataNotFoundException("unable to find a metadata format for this name or this id");
        }

    }

}
