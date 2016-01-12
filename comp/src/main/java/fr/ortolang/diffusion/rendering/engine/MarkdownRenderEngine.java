package fr.ortolang.diffusion.rendering.engine;

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
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tika.io.IOUtils;
import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;

import fr.ortolang.diffusion.rendering.RenderEngine;

public class MarkdownRenderEngine implements RenderEngine {

    private static final Logger LOGGER = Logger.getLogger(MarkdownRenderEngine.class.getName());
    private static final List<String> SUPPORTED_MIME_TYPES = Arrays.asList("text/x-web-markdown", "text/x-markdown", "text/markdown");
    public static final String ID = "md";

    private PegDownProcessor processor;

    public MarkdownRenderEngine() {
        processor = new PegDownProcessor(Extensions.WIKILINKS);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName(Locale locale) {
        if ( locale.equals(Locale.ENGLISH) ) {
            return "Markdown";
        }
        return "Markdown";
    }

    @Override
    public String getDescription(Locale locale) {
        if ( locale.equals(Locale.ENGLISH) ) {
            return "This viewer engine is able to parse markdown based text files to produce HTML. It uses pegdown markdown processor.";
        }
        return "Ce moteur de transformation est capable d'interpréter le texte au format markdown afin de produire du HTML. Il utilise la librairie pegdown.";
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
      //TODO make the processor multithread to avoid synchronizing treatment...
        try ( InputStream is = Files.newInputStream(input); OutputStream os = Files.newOutputStream(output, StandardOpenOption.WRITE) ) {
            LOGGER.log(Level.FINE, "Starting markdown rendering");
            String html = processor.markdownToHtml(IOUtils.toCharArray(is, "UTF-8"));
            IOUtils.copy(new StringReader(html), os);
            LOGGER.log(Level.FINE, "Markdown rendering done.");
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "unable to perform markdown rendering", e);
            return false;
        }
    }

}
