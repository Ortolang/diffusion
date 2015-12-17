package fr.ortolang.diffusion.rendering.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import fr.ortolang.diffusion.rendering.RenderEngine;

public class TEIRenderEngine implements RenderEngine {

    private static final Logger LOGGER = Logger.getLogger(TEIRenderEngine.class.getName());
    private static final List<String> SUPPORTED_MIME_TYPES = Arrays.asList("application/xml");
    private static final String ID = "xml";

    private TransformerFactory tFactory;

    public TEIRenderEngine() {
        tFactory = TransformerFactory.newInstance();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName(Locale locale) {
        if (locale.equals(Locale.ENGLISH)) {
            return "XML";
        }
        return "XML";
    }

    @Override
    public String getDescription(Locale locale) {
        if (locale.equals(Locale.ENGLISH)) {
            return "This viewer engine is able to apply the TEI BoilerPlate xsl stylsheet to an XML document.";
        }
        return "Ce moteur de transformation est capable d'appliquer la feuille de style xsl de TEI BoilerPlate à un document XML.";
    }

    @Override
    public boolean canRender(String mimetype) {
        if (SUPPORTED_MIME_TYPES.contains(mimetype)) {
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean render(Path input, Path output) {
        try (InputStream is = Files.newInputStream(input); OutputStream os = Files.newOutputStream(output, StandardOpenOption.WRITE)) {
            LOGGER.log(Level.FINE, "Starting XSLT rendering");
            String media = null, title = null, charset = null;
            //TODO load TEIBoilerPlate online Stylesheet
            Source stylesheet = tFactory.getAssociatedStylesheet(new StreamSource(is), media, title, charset);
            Transformer transformer = tFactory.newTransformer(stylesheet);
            transformer.transform(new StreamSource(is), new StreamResult(os));
            LOGGER.log(Level.FINE, "XSLT rendering done");
            return true;
        } catch (IOException | TransformerException e) {
            LOGGER.log(Level.WARNING, "unable to perform xslt rendering", e);
            return false;
        }
    }

}
