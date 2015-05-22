package fr.ortolang.diffusion.core.preview;



public interface PreviewService {
	
	public static final String SERVICE_NAME = "preview";
	public static final String PREVIEW_MIMETYPE = "image/jpeg";
	public static final String GENERATORS_CONFIG_PARAMS = "preview.generators";
	public static final int SMALL_IMAGE_WIDTH = 120;
	public static final int SMALL_IMAGE_HEIGHT = 120;
	public static final int LARGE_IMAGE_WIDTH = 300;
	public static final int LARGE_IMAGE_HEIGHT = 300;

	public void generate(String key) throws PreviewServiceException;

}
