package fr.ortolang.diffusion.api.sru.fcs;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import eu.clarin.sru.server.SRUConfigException;
import eu.clarin.sru.server.SRUException;
import eu.clarin.sru.server.fcs.DataView;
import eu.clarin.sru.server.fcs.EndpointDescription;
import eu.clarin.sru.server.fcs.Layer;
import eu.clarin.sru.server.fcs.ResourceInfo;
import eu.clarin.sru.server.fcs.DataView.DeliveryPolicy;
import eu.clarin.sru.server.fcs.utils.SimpleEndpointDescriptionParser;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.content.entity.ContentSearchResource;
import fr.ortolang.diffusion.search.SearchService;

/**
 * Contains information for generating a response of SRU explain
 * operation with endpoint description.
 * 
 * Example:
 * http://localhost:8080/OrtolangEndpoint?operation=explain&x-fcs-endpoint
 * -description=true
 * 
 * @author cpestel
 * 
 */
public class OrtolangEndpointDescription implements EndpointDescription {

    private List<DataView> dataviews;
    private List<URI> capabilities;
    private List<String> languages;

    private List<String> defaultDataviews;
    private List<Layer> layers;
    private Layer textLayer;
    
    private List<AnnotationLayer> annotationLayers;

    private SearchService search;
    
    public OrtolangEndpointDescription(ServletContext context) throws SRUConfigException, OrtolangException {
    	//TODO regarder si le constructeur est appelé à chaque appel de explain
    	search = (SearchService) OrtolangServiceLocator.findService(SearchService.SERVICE_NAME);
    	
        String endpointDesc = context.getInitParameter("fr.ortolang.endpointDescription");
        if (endpointDesc==null || endpointDesc.isEmpty()){
            endpointDesc = "/WEB-INF/endpoint-description.xml";
        }

        try {
            URL url = context.getResource(endpointDesc);
            EndpointDescription simpleEndpointDescription =
                    SimpleEndpointDescriptionParser.parse(url);
            if (simpleEndpointDescription != null) {
                setSupportedLayers(
                        simpleEndpointDescription.getSupportedLayers());
                setAnnotationLayers(new ArrayList<AnnotationLayer>());
                setSupportedDataViews(
                        simpleEndpointDescription.getSupportedDataViews());
                setDefaultDataViews(
                        simpleEndpointDescription.getSupportedDataViews());
//                setCapabilities(simpleEndpointDescription.getCapabilities());
                List<URI> capabilities = new ArrayList<URI>();
                capabilities.add(new URI(OrtolangEndpoint.CAPABILITY_BASIC_SEARCH));
                setCapabilities(capabilities);
            }

        }
        catch (MalformedURLException | URISyntaxException e) {
            throw new SRUConfigException(
                    "error initializing resource info inventory", e);
        }
        setLanguages();
    }

	public void destroy() {
        dataviews = null;
        capabilities = null;
        languages.clear();
	}

    public void setLanguages () {
        languages = new ArrayList<String>();
        languages.add("fra");
    }

    public List<URI> getCapabilities () {
        return capabilities;
    }

    public void setCapabilities (List<URI> list) throws SRUConfigException {
        capabilities = list;
    }

    public List<DataView> getSupportedDataViews () {
        return dataviews;
    }

    public void setSupportedDataViews (List<DataView> list) {
        dataviews = list;
    }

    public List<ResourceInfo> getResourceList (String pid) throws SRUException {

        List<ResourceInfo> resourceList = new ArrayList<ResourceInfo>();

        Map<String, String> title;
        Map<String, String> description;

        List<ContentSearchResource> resources = search.listResources();
        for (ContentSearchResource res : resources) {
        	if (res.getPid() != null) { // Needs to have at least one published root collection
	    		title = new HashMap<String, String>();
	    		title.put("fr", res.getTitle());
	    		description = new HashMap<String, String>();
	    		description.put("fr", res.getDescription());
	
	    		ResourceInfo ri = new ResourceInfo(res.getPid(), title,
	                  description, res.getPid(), languages, dataviews,
	                  this.getSupportedLayers(), null);
	    		resourceList.add(ri);
        	}
        }
        
        return resourceList;
    }

    public List<String> getDefaultDataViews () {
        return defaultDataviews;
    }

    public void setDefaultDataViews (List<DataView> supportedDataViews) {
        defaultDataviews = new ArrayList<String>();
        for (DataView d : supportedDataViews) {
            if (d.getDeliveryPolicy() == DeliveryPolicy.SEND_BY_DEFAULT) {
                defaultDataviews.add(d.getIdentifier());
            }
        }
    }

    public void setSupportedLayers (List<Layer> layers) {
        this.layers = layers;
    }

    public List<Layer> getSupportedLayers () {
        return layers;
    }

    public List<AnnotationLayer> getAnnotationLayers () {
        return annotationLayers;
    }

    public void setAnnotationLayers (List<AnnotationLayer> layers) {
    	annotationLayers = layers;
    }

    public Layer getTextLayer () {
        return textLayer;
    }

    public void setTextLayer (Layer textLayer) {
        this.textLayer = textLayer;
    }

}
