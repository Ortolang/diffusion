package fr.ortolang.diffusion.indexing;

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

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangIndexableService;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.referential.ReferentialService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrtolangIndexableContentParser {

    private static final Pattern ORTOLANG_KEY_MATCHER = Pattern.compile("\"\\$\\{([\\w:.\\-]+)}\"");
    private static final int startReferentialKeyPostion = 2;
    private static final int startReferentialIdPosition = 2 + ReferentialService.SERVICE_NAME.length() + 1;

    public static String parse(String json) throws IndexableContentParsingException {
        try {
            RegistryService registry = (RegistryService) OrtolangServiceLocator.lookup(RegistryService.SERVICE_NAME, RegistryService.class);
            Matcher matcher = ORTOLANG_KEY_MATCHER.matcher(json);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String key = matcher.group(1);
                OrtolangObjectIdentifier identifier = registry.lookup(key);
                OrtolangIndexableService service = OrtolangServiceLocator.findIndexableService(identifier.getService());
                List<OrtolangIndexableContent> indexableContents = service.getIndexableContent(key);
                if (!indexableContents.isEmpty()) {
                    matcher.appendReplacement(sb, indexableContents.get(0).getContent());
                }
            }
            matcher.appendTail(sb);
            return sb.toString();
        } catch (IndexingServiceException | OrtolangException | RegistryServiceException | KeyNotFoundException e) {
            throw new IndexableContentParsingException("Unexpected error while parsing indexable content", e);
        }
    }
    
    public static String extractKey(String expression) {
    	return expression.substring(startReferentialKeyPostion, expression.length()-1);
    }

    public static String extractReferentialId(String expression) {
        return expression.substring(startReferentialIdPosition, expression.length()-1);
    }
}
