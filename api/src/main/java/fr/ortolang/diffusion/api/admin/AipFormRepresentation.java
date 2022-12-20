package fr.ortolang.diffusion.api.admin;

import java.io.InputStream;

import javax.ws.rs.FormParam;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class AipFormRepresentation {

    @FormParam("aip")
    @PartType("application/octet-stream")
    private InputStream aip = null;

    public InputStream getAip() {
        return aip;
    }

    public void setAip(InputStream aip) {
        this.aip = aip;
    }

}
