package fr.ortolang.diffusion.thumbnail.util;

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
