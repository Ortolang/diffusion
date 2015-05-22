package fr.ortolang.diffusion.core.preview.util;

import java.awt.Image;
import java.awt.image.ImageObserver;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageResizerObserver implements ImageObserver {

	private final static Logger LOGGER = Logger.getLogger(ImageResizerObserver.class.getName());
	private Thread toNotify;
	public volatile boolean ready = false;

	public ImageResizerObserver(Thread toNotify) {
		this.toNotify = toNotify;
		ready = false;
	}

	public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
		LOGGER.log(Level.FINE, "image bits treated info flag " + infoflags);
		if ((infoflags & ImageObserver.ALLBITS) > 0) {
			ready = true;
			LOGGER.log(Level.FINE, "all bits have been treated");
			toNotify.notify();
			return true;
		}
		return false;
	}
}