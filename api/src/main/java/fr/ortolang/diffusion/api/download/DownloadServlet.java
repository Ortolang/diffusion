package fr.ortolang.diffusion.api.download;

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

    private static Logger logger = Logger.getLogger(DownloadServlet.class.getName());

    @EJB
    private BinaryStoreService binaryStore;

    @Override
    public void init() throws ServletException {
        logger.log(Level.INFO, "DownloadServlet Initialized");
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
            logger.log(Level.INFO, "User " + ticket.getUsername() + " trying to download file '" + filename + "' (" + hash + ")");
            logger.log(Level.FINE, "Validating ticket " + ticket);
            if (!ticket.validate(hash)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid ticket");
            }
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            response.setContentType(contentType);
            response.setContentLength(size);
            InputStream input = binaryStore.get(hash);
            try {
                IOUtils.copy(input, response.getOutputStream());
            } finally {
                IOUtils.closeQuietly(input);
            }
        } catch (BinaryStoreServiceException | DataNotFoundException e) {
            logger.log(Level.SEVERE, "An error occurred when trying to download " + filename, e);
        }
    }
}
