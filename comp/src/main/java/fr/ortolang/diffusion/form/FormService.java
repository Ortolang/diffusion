package fr.ortolang.diffusion.form;

import java.util.List;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.form.entity.Form;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface FormService extends OrtolangService {
	
	public static final String SERVICE_NAME = "form";
	public static final String[] OBJECT_TYPE_LIST = new String[] { Form.OBJECT_TYPE };
	public static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { 
		{ Form.OBJECT_TYPE, "read,update,delete" }};
	
	public void importForms() throws FormServiceException;
	
	public List<Form> listForms() throws FormServiceException;
	
	public void createForm(String key, String name, String definition) throws FormServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public Form readForm(String key) throws FormServiceException, KeyNotFoundException;
	
	public void updateForm(String key, String name, String definition) throws FormServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void deleteForm(String key) throws FormServiceException, KeyNotFoundException, AccessDeniedException;

}
