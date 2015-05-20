package fr.ortolang.diffusion.core.preview.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

public class ImageResizer {

	private static final Logger LOGGER = Logger.getLogger(ImageResizer.class.getName());

	BufferedImage inputImage;
	private boolean isProcessed = false;
	BufferedImage outputImage;

	private int imageWidth;
	private int imageHeight;
	private int thumbWidth;
	private int thumbHeight;
	private double resizeRatio = 1.0;

	private int scaledWidth;
	private int scaledHeight;
	
	public ImageResizer(int thumbWidth, int thumbHeight) {
		this.thumbWidth = thumbWidth;
		this.thumbHeight = thumbHeight;
	}

	public void setInputImage(File input) throws Exception {
		BufferedImage image = ImageIO.read(input);
		setInputImage(image);
	}

	public void setInputImage(InputStream input) throws Exception {
		BufferedImage image = ImageIO.read(input);
		setInputImage(image);
	}

	public void setInputImage(BufferedImage input) throws Exception {
		if (input == null) {
			throw new Exception("The image reader could not open the file.");
		}

		this.inputImage = input;
		isProcessed = false;
		imageWidth = inputImage.getWidth(null);
		imageHeight = inputImage.getHeight(null);
	}

	public void writeOutput(File output) throws IOException {
		writeOutput(output, "jpg");
	}

	public void writeOutput(File output, String format) throws IOException {
		if (!isProcessed) {
			process();
		}
		ImageIO.write(outputImage, format, output);
	}

	private void process() {
		if (imageWidth == thumbWidth && imageHeight == thumbHeight) {
			outputImage = inputImage;
		} else {
			calcDimensions();
			paint();
		}
		isProcessed = true;
	}

	private void calcDimensions() {
		resizeRatio = Math.min(((double) thumbWidth) / imageWidth, ((double) thumbHeight) / imageHeight);
		scaledWidth = (int) Math.round(imageWidth * resizeRatio);
		scaledHeight = (int) Math.round(imageHeight * resizeRatio);
	}

	private void paint() {
		LOGGER.log(Level.FINE, "starting to paint image");
		outputImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics2D = outputImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		ImageResizerObserver observer = new ImageResizerObserver(Thread.currentThread());
		boolean scalingComplete = graphics2D.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, observer);
		if (!scalingComplete && observer != null) {
			while (!observer.ready) {
				LOGGER.log(Level.FINE, "waiting for image to be completelly drawned");
				try {
					Thread.sleep(400);
				} catch (InterruptedException e) {
				}
			}
		}
		graphics2D.dispose();
		LOGGER.log(Level.FINE, "image painted");
	}
}
