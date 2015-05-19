package fr.ortolang.diffusion.admin.action.registry;

import javax.xml.registry.RegistryService;

import com.opensymphony.xwork2.ActionSupport;

@SuppressWarnings("serial")
public class ListEntriesAction extends ActionSupport {
 
    private RegistryService registry;
     
    public String execute() throws Exception {
        return SUCCESS;
    }
 
}