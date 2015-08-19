package fr.ortolang.diffusion.membership;

import java.util.Map;

import javax.ejb.Local;

import fr.ortolang.diffusion.registry.KeyNotFoundException;

@Local
public interface MembershipServiceAdmin {
    
    public boolean systemValidateTOTP(String identifier, String totp) throws MembershipServiceException, KeyNotFoundException;
    
    public String systemReadProfileSecret(String identifier) throws MembershipServiceException, KeyNotFoundException;

    public Map<String, String> getServiceInfos() throws MembershipServiceException;

}
