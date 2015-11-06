package fr.ortolang.diffusion.thumbnail.generator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import fr.ortolang.diffusion.thumbnail.util.ImageResizer;

public class VideoThumbnailGenerator implements ThumbnailGenerator {

    private static List<String> acceptedMIMETypes = Arrays.asList(
            "video/mp4",
            "video/quicktime",
            "video/x-msvideo",
            "video/x-ms-wmv",
            "video/mpeg",
            "video/x-matroska",
            "video/ogg",
            "video/theora",
            "video/webm",
            "video/x-flv"
    );

    public void generate(File input, File output, int width, int height) throws ThumbnailGeneratorException {
        FFmpegFrameGrabber g = new FFmpegFrameGrabber(input);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            g.start();
            Frame f = g.grabKeyFrame();
            Java2DFrameConverter converter = new Java2DFrameConverter();
            ImageIO.write(converter.getBufferedImage(f), "jpg", baos);
            g.stop();

            ImageResizer resizer = new ImageResizer(width, height);
            resizer.setInputImage(new ByteArrayInputStream(baos.toByteArray()));
            resizer.writeOutput(output);
        } catch (Exception e) {
            throw new ThumbnailGeneratorException("unable to generate video preview", e);
        }
    }

    public List<String> getAcceptedMIMETypes() {
        return acceptedMIMETypes;
    }
}
