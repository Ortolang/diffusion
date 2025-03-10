package fr.ortolang.diffusion.core.indexing;

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.core.OrtolangItemType;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.indexing.OrtolangIndexableContent;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrtolangItemIndexableContent extends OrtolangIndexableContent {

    private static final Logger LOGGER = Logger.getLogger(OrtolangItemIndexableContent.class.getName());

    public static final String[] ORTOLANG_ITEM_MAPPING;

    public static final String INDEX = "item";

    public static final String INDEX_ALL = "item-all";
    public static final int DEFAULT_ABSTRACT_LENGHT = 200;

    private OrtolangItemType ortolangItemType;

    static {
        ORTOLANG_ITEM_MAPPING = new String[] {
                "key",
                "type=keyword",
                "snapshot",
                "type=keyword",
                "alias",
                "type=keyword",
                "tag",
                "type=keyword",
                "schema",
                "type=keyword",
                "type",
                "type=keyword",
                "title",
                "type=nested",
                "description",
                "type=nested",
                "keywords",
                "type=nested",
                "bibliographicCitation",
                "type=nested",
                "datasize",
                "type=long",
                "publications",
                "type=text",
                "website",
                "type=keyword,index=no",
                "image",
                "type=keyword,index=no",
                "preview",
                "type=keyword,index=no",
                "statusOfUse",
                "type=object",
//                "statusOfUse.content",
//                "type=keyword,index=no",
                "conditionsOfUse",
                "type=nested",
                "license",
                "type=object",
                "derogation",
                "type=text,index=no",
                "copyright",
                "type=text,index=no",
                "producers",
                "type=nested",
                "sponsors",
                "type=nested",
                "contributors",
                "type=nested",
                "relations",
                "type=nested",
                "commercialLinks",
                "type=nested",
                "creationLocations",
                "type=nested",
                "originDate",
                "type=keyword",
                "publicationDate",
                "type=date",
                "corporaType",
                "type=object",
//                "corporaType.content",
//                "type=keyword,index=no",
//                "corporaType.id",
//                "type=keyword",
//                "corporaType.labels",
//                "type=nested",
                "corporaLanguages",
                "type=nested",
//                "corporaLanguages.content",
//                "type=keyword,index=no",
                "corporaStudyLanguages",
                "type=nested",
                "corporaStyles",
                "type=nested",
                "annotationLevels",
                "type=nested",
//                "annotationLevels.content",
//                "type=keyword,index=no",
                "corporaFormats",
                "type=nested",
//                "corporaFormats.content",
//                "type=keyword,index=no",
                "corporaFileEncodings",
                "type=nested",
//                "corporaFileEncodings.content",
//                "type=keyword,index=no",
                "corporaDataTypes",
                "type=object",
                "corporaDataTypes.content",
                "type=keyword,index=no",
                "corporaLanguageType",
                "type=nested",
//                "corporaLanguageType.content",
//                "type=keyword,index=no",
                "wordCount",
                "type=long", // keywoard ?
                "linguisticDataType",
                "type=text",
                "discourseTypes",
                "type=text",
                "linguisticSubjects",
                "type=text",
                "programmingLanguages",
                "type=nested",
                "operatingSystems",
                "type=nested",
                "toolSupport",
                "type=nested",
                "navigationLanguages",
                "type=nested",
                "toolLanguages",
                "type=nested",
                "toolFunctionalities",
                "type=nested",
                "toolInputData",
                "type=nested",
                "toolOutputData",
                "type=nested",
                "toolFileEncodings",
                "type=nested",
                "toolId",
                "type=keyword,index=no",
                "toolUrl",
                "type=keyword,index=no",
                "toolHelp",
                "type=text,index=no",
                "lexiconInputType",
                "type=nested",
                "lexiconInputLanguages",
                "type=nested",
                "lexiconInputCount",
                "type=long", // keyword ?
                "lexiconDescriptionTypes",
                "type=nested",
                "lexiconDescriptionLanguages",
                "type=nested",
                "lexiconLanguageType",
                "type=nested",
                "lexiconFormats",
                "type=nested",
                "applicationUrl",
                "type=keyword,index=no",
                "terminoType",
                "type=nested",
                "terminoStructureType",
                "type=nested",
                "terminoDescriptionTypes",
                "type=nested",
                "terminoLanguageType",
                "type=nested",
                "terminoInputLanguages",
                "type=nested",
                "terminoDomains",
                "type=nested",
                "terminoFormat",
                "type=nested",
                "terminoUsage",
                "type=nested",
                "terminoOrigin",
                "type=nested",
                "terminoInputCount",
                "type=nested",
                "terminoVersion",
                "type=keyword",
                "terminoControled",
                "type=boolean",
                "terminoValidated",
                "type=boolean",
                "terminoApproved",
                "type=boolean",
                "terminoChecked",
                "type=boolean",
                "parts",
                "type=object" // nested ?
        };
    }

    public OrtolangItemIndexableContent(MetadataObject metadata, Collection collection, String alias, String snapshot, String tag, int rating, boolean archive, boolean latest) throws IndexingServiceException, OrtolangException {
        super();
        try {
            BinaryStoreService binary = (BinaryStoreService) OrtolangServiceLocator.lookup(BinaryStoreService.SERVICE_NAME, BinaryStoreService.class);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> content = mapper.readValue(binary.getFile(metadata.getStream()), new TypeReference<Map<String, Object>>(){});

            String metadataType = (String) content.get(OrtolangItemType.METADATA_KEY);
            ortolangItemType = OrtolangItemType.fromMetadataType(metadataType);
            setType(ortolangItemType.getSection());
            
            if (latest) {
                setIndex(INDEX);
                // For latest: key equals workspace alias
                setId(alias);
            } else {
                setIndex(INDEX_ALL);
                setId(alias + "-" + tag);
            }
            content.put("key", collection.getKey());
            content.put("tag", tag);
            content.put("snapshot", snapshot);
            content.put("alias", alias);
            content.put("rank", rating);
            content.put("archive", archive);

            List<Map<String,String>> descriptionArray = (List<Map<String,String>>) content.get("description");
            for(Map<String,String> descriptionObj : descriptionArray) {
            	descriptionObj.put("abstract", sumUp(descriptionObj.get("value")));
            }
            
            setContent(content);
        } catch (IOException | BinaryStoreServiceException | DataNotFoundException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }
    
    /**
     * Sum up the description of the item.
     * @param description the full description
     * @return a sum up
     */
    private String sumUp(String description) {
    	return new StringBuffer()
    			.append(description.substring(0, (description.length()>DEFAULT_ABSTRACT_LENGHT) ? DEFAULT_ABSTRACT_LENGHT : description.length()))
    			.append((description.length()>DEFAULT_ABSTRACT_LENGHT) ? "..." : "").toString();
    }
}
