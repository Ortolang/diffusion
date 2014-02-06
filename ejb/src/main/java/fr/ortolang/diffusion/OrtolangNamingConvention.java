package fr.ortolang.diffusion;

public class OrtolangNamingConvention {
	
    public static final String SERVICE_PREFIX = "digital-service-";
    public static final String LOCAL_SERVICE_PREFIX = "local-digital-service-";
    
    public static String getJNDINameForService(String service) {
        return SERVICE_PREFIX + service;
    }

    public static String getJNDINameForLocalService(String service) {
        return LOCAL_SERVICE_PREFIX + service;
    }

    public static String getServiceNameFromJNDI(String jndiName) {
        return jndiName.substring(SERVICE_PREFIX.length());
    }
    
    public static String buildEventType(String serviceName, String resourceName, String eventName) {
    	StringBuffer event = new StringBuffer();
    	if ( serviceName != null && serviceName.length() > 0 ) {
    		event.append(serviceName + ".");
    	}
    	if ( resourceName != null && resourceName.length() > 0 ) {
    		event.append(resourceName + ".");
    	}
    	event.append(eventName);
    	return event.toString();
    }
    
}