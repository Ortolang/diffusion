package fr.ortolang.diffusion.api.download;

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

import fr.ortolang.diffusion.security.authentication.TicketHelper;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class DownloadServlet extends HttpServlet {

	private static final Logger LOGGER = Logger.getLogger(DownloadServlet.class.getName());

	@EJB
	private BinaryStoreService binaryStore;

	@Override
	public void init() throws ServletException {
		LOGGER.log(Level.FINE, "DownloadServlet Initialized");
		super.init();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String filename = null;
		try {
			String[] uriSplit = request.getRequestURI().split("/");
			String hash = uriSplit[uriSplit.length - 1];
			TicketHelper.Ticket ticket = TicketHelper.decodeTicket(request.getParameter("t"));
			byte[] c = Base64.decodeBase64(request.getParameter("c"));
			String content = new String(c);
			StringTokenizer st = new StringTokenizer(content, ":");
			filename = st.nextToken();
			int size = Integer.parseInt(st.nextToken());
			String contentType = st.nextToken();
			LOGGER.log(Level.INFO, "User " + ticket.getUsername() + " trying to download file '" + filename + "' (" + hash + ")");
			LOGGER.log(Level.FINE, "Validating ticket " + ticket);
			if (!ticket.validate(hash)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid ticket");
			}
			response.setHeader("Content-Disposition", "attachment; filename=" + filename);
			response.setContentType(contentType);
			response.setContentLength(size);
			response.setDateHeader("Expires", System.currentTimeMillis() + 315360000000L);
			InputStream input = binaryStore.get(hash);
			try {
				IOUtils.copy(input, response.getOutputStream());
			} finally {
				IOUtils.closeQuietly(input);
			}
		} catch (BinaryStoreServiceException | DataNotFoundException e) {
			LOGGER.log(Level.SEVERE, "An error occurred when trying to download " + filename, e);
			throw new ServletException(e);
		}
	}

	@Override
	protected long getLastModified(HttpServletRequest request) {
		LOGGER.log(Level.FINE, "Getting last modification");
		String[] uriSplit = request.getRequestURI().split("/");
		String hash = uriSplit[uriSplit.length - 1];
		try {
			long lmd = binaryStore.getFile(hash).lastModified();
			return (lmd / 1000 * 1000);
		} catch (BinaryStoreServiceException | DataNotFoundException e) {
			LOGGER.log(Level.SEVERE, "An error occurred when trying to get last modification date for hash: " + hash, e);
			return -1;
		}
	}
}
