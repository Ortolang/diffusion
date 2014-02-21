package fr.ortolang.diffusion.notification;


public interface NotificationService {
	
	public static final String SERVICE_NAME = "notification";
    
    public void throwEvent(String fromObject, String throwedBy, String objectType, String eventType, String args) throws NotificationServiceException;
    

}
