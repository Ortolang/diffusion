package fr.ortolang.diffusion.ftp;

import java.util.Map;

import javax.ejb.Local;

@Local
public interface FtpServiceAdmin {

    public Map<String, String> getServiceInfos() throws FtpServiceException;
    
}
