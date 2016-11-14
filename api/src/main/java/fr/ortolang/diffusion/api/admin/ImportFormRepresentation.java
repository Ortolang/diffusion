package fr.ortolang.diffusion.api.admin;

import java.io.InputStream;

import javax.ws.rs.FormParam;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class ImportFormRepresentation {

    @FormParam("dump")
    @PartType("application/octet-stream")
    private InputStream dump = null;

    public InputStream getDump() {
        return dump;
    }

    public void setDump(InputStream dump) {
        this.dump = dump;
    }

}
