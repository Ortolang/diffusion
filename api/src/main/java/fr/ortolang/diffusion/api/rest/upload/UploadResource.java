package fr.ortolang.diffusion.api.rest.upload;

import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.api.rest.workspace.WorkspaceResource;

@Path("/upload")
@Produces({ MediaType.APPLICATION_JSON })
public class UploadResource {

	private Logger logger = Logger.getLogger(WorkspaceResource.class.getName());

	private static DiskFileItemFactory factory;
	private static ServletFileUpload upload;

	@Context
	private UriInfo uriInfo;

	private static DiskFileItemFactory getDiskFileItemFactory() {
		if (factory == null) {
			factory = new DiskFileItemFactory(0, Paths.get(OrtolangConfig.getInstance().getProperty("home"), "upload").toFile());
			factory.setFileCleaningTracker(null);
		}
		return factory;
	}

	private static ServletFileUpload getServletFileUpload() {
		if (upload == null) {
			upload = new ServletFileUpload(getDiskFileItemFactory());
			upload.setFileSizeMax(5368709120l);
		}
		return upload;
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response upload(@Context HttpServletRequest request) throws FileUploadException {
		logger.log(Level.INFO, "uploading files");
		List<FileItem> items = getServletFileUpload().parseRequest(request);
		for (FileItem item : items) {
			if (item.isFormField()) {
				// TODO throw an error because we cannot handle field but only files...
			} else {
				// TODO Process the file but maybe use it
			}
		}
		return Response.ok().build();
	}

	class UploadProgressListener implements ProgressListener {

		private long unit = -1;

		public void update(long pBytesRead, long pContentLength, int pItems) {
			long reads = pBytesRead / 1024 * 100;
			if (unit == reads) {
				return;
			}
			unit = reads;
			System.out.println("We are currently reading item " + pItems);
			if (pContentLength == -1) {
				System.out.println("So far, " + pBytesRead + " bytes have been read.");
			} else {
				System.out.println("So far, " + pBytesRead + " of " + pContentLength + " bytes have been read.");
			}
		}
	};

}
