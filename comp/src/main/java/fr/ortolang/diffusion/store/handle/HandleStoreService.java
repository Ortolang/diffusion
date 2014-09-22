package fr.ortolang.diffusion.store.handle;


public interface HandleStoreService {
	
	public static final String SERVICE_NAME = "handle-store";
	
	public String create(String suffix, String... values) throws HandleStoreServiceException;
	
	public String read(String suffix) throws HandleStoreServiceException;
	
	public boolean exists(String suffix) throws HandleStoreServiceException;
	
	public void delete(String suffix) throws HandleStoreServiceException;

}
