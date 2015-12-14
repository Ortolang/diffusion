package fr.ortolang.diffusion.seo;

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

import com.orientechnologies.orient.core.record.impl.ODocument;
import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.store.json.JsonStoreService;
import fr.ortolang.diffusion.store.json.JsonStoreServiceException;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Local(SeoService.class)
@Singleton(name = SeoService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class SeoServiceBean implements SeoService {

    @EJB
    private JsonStoreService json;

    private static final Logger LOGGER = Logger.getLogger(SeoServiceBean.class.getName());

    private static final String[] OBJECT_TYPE_LIST = new String[] { };

    private static final String[] OBJECT_PERMISSIONS_LIST = new String[] { };

    private Map<String, String> marketTypes;

    public SeoServiceBean() {
        marketTypes = new HashMap<>();
        for (MarketSection marketSection : MarketSection.values()) {
            if (marketSection.mdValue != null) {
                marketTypes.put(marketSection.mdValue, marketSection.marketType);
            }
        }
    }

    @Override
    public String generateSiteMap() throws JsonStoreServiceException, ParserConfigurationException, TransformerException, SeoServiceException {
        LOGGER.log(Level.INFO, "Start generating Site Map");
        Document document = generateSiteMapDocument();

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);

        transformer.transform(source, result);
        return writer.toString();
    }

    private Document generateSiteMapDocument() throws ParserConfigurationException, SeoServiceException, JsonStoreServiceException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        Element urlset = doc.createElement("urlset");
        urlset.setAttribute("xmlns", "http://www.sitemaps.org/schemas/sitemap/0.9");

        String marketServerUrl = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.MARKET_SERVER_URL);

        generateMarketSectionEntries(urlset, doc, marketServerUrl);
        generateWorkspacesEntries(urlset, doc, marketServerUrl);

        doc.appendChild(urlset);

        return doc;
    }

    private void generateMarketSectionEntries(Element urlset, Document doc, String marketServerUrl) throws SeoServiceException {
        String loc;
        for (MarketSection marketSection : MarketSection.values()) {
            loc = marketServerUrl + marketSection.path;
            Element url = buildSiteMapEntry(loc, null, ChangeFrequency.ALWAYS, marketSection.priority, doc);
            urlset.appendChild(url);
        }
    }

    private void generateWorkspacesEntries(Element urlset, Document doc, String marketServerUrl) throws JsonStoreServiceException, SeoServiceException {
        List<ODocument> workspaces = json.systemSearch("SELECT key, lastModificationDate as lastModificationDate, meta_ortolang-item-json.type as type, meta_ortolang-workspace-json.wsalias as alias, meta_ortolang-workspace-json.snapshotName as snapshotName FROM collection WHERE status = 'published' AND meta_ortolang-item-json.type IS NOT null AND meta_ortolang-workspace-json.wsalias IS NOT null");

        Map<String, ODocument> workspacesLatest = new HashMap<>();
        // Find latest version of each published resource
        for (ODocument workspace : workspaces) {
            String alias = workspace.rawField("alias");
            if (workspacesLatest.containsKey(alias)) {
                if ((long) workspacesLatest.get(alias).rawField("lastModificationDate") < (long) workspace.rawField("lastModificationDate")) {
                    workspacesLatest.put(alias, workspace);
                }
            } else {
                workspacesLatest.put(alias, workspace);
            }
        }

        for (ODocument workspace : workspaces) {

            boolean latest = workspacesLatest.containsValue(workspace);
            String marketType = marketTypes.get((String) workspace.rawField("type"));
            if (marketType == null) {
                continue;
            }
            String locLatest =  marketServerUrl + "market/" + marketType + "/" + workspace.rawField("alias");
            String loc = locLatest + "/" + workspace.rawField("snapshotName");
            String priority = latest ? "0.7" : "0.5";

            Element url = buildSiteMapEntry(loc, null, ChangeFrequency.WEEKLY, priority, doc);
            urlset.appendChild(url);

            // Add an entry for the latest version without the snapshotName in the url
            if (latest) {
                loc = locLatest;
                priority = "0.8";
                Element urlLatest = buildSiteMapEntry(loc, null, ChangeFrequency.ALWAYS, priority, doc);
                urlset.appendChild(urlLatest);
            }
        }
    }

    @Override
    public String getServiceName() {
        return SeoService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        Map<String, String> infos = new HashMap<String, String>();
        try {
            Document document = generateSiteMapDocument();
            int sitemapEntriesNumber = document.getChildNodes().item(0).getChildNodes().getLength();
            infos.put(INFO_SITEMAP_ENTRIES_ALL, String.valueOf(sitemapEntriesNumber));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "unable to collect info: " + INFO_SITEMAP_ENTRIES_ALL, e);
        }
        return infos;
    }

    @Override
    public String[] getObjectTypeList() {
        return OBJECT_TYPE_LIST;
    }

    @Override
    public String[] getObjectPermissionsList(String type) throws OrtolangException {
        return OBJECT_PERMISSIONS_LIST;
    }

    @Override
    public OrtolangObject findObject(String key) throws OrtolangException {
        throw new OrtolangException("This service does not manage any object");
    }

    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        throw new OrtolangException("This service does not manage any object");
    }

    private Element buildSiteMapEntry(String loc, String lastmod, ChangeFrequency changefreq, String priority, Document doc) throws SeoServiceException {
        Element url = doc.createElement("url");

        if (loc == null || loc.length() == 0) {
            throw new SeoServiceException("Cannot generate site map entry: 'loc' is mandatory");
        }
        Element locElement = doc.createElement("loc");
        locElement.appendChild(doc.createTextNode(loc));
        url.appendChild(locElement);

        if (lastmod != null) {
            Element lastmodElement = doc.createElement("lastmod");
            lastmodElement.appendChild(doc.createTextNode(lastmod));
            url.appendChild(lastmodElement);
        }

        if (changefreq != null) {
            Element changefreqElement = doc.createElement("changefreq");
            changefreqElement.appendChild(doc.createTextNode(changefreq.name().toLowerCase()));
            url.appendChild(changefreqElement);
        }

        if (priority != null) {
            Element priorityElement = doc.createElement("priority");
            priorityElement.appendChild(doc.createTextNode(priority));
            url.appendChild(priorityElement);
        }
        return url;
    }

    private enum MarketSection {

        INDEX("", "1.0", null, null),
        HOME("market/home", "0.9", null, null),
        CORPORA("market/corpora", "0.9", "Corpus", "corpora"),
        LEXICONS("market/lexicons", "0.9", "Lexique", "lexicons"),
        TOOLS("market/tools", "0.9", "Outil", "tools"),
        APPLICATIONS("market/applications", "0.9", "Application", null),
        INFORMATION("information", "0.9", null, null),
        LEGAL_NOTICES("legal-notices", "0.3", null, null);

        private final String path;
        private final String mdValue;
        private final String priority;
        private final String marketType;

        MarketSection(String path, String priority, String mdValue, String marketType) {
            this.path = path;
            this.priority = priority;
            this.mdValue = mdValue;
            this.marketType = marketType;
        }
    }

    private enum ChangeFrequency {
        ALWAYS,
        HOURLY,
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY,
        NEVER
    }
}
