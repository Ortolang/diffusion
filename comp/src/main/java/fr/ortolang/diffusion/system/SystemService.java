package fr.ortolang.diffusion.system;

import fr.ortolang.diffusion.OrtolangService;

public interface SystemService extends OrtolangService {
    
    public static final String SERVICE_NAME = "system";
    
    public static final String INFO_HOSTNAME = "hostname";
    public static final String INFO_DOMAINNAME = "domainname";
    public static final String INFO_DNS = "dns";
    public static final String INFO_GATEWAY = "gateway";
    
    public static final String INFO_LOAD = "load";
    
    public static final String INFO_MEMTOTAL = "mem.total";
    public static final String INFO_MEMRAM = "mem.ram";
    public static final String INFO_MEMUSED = "mem.used";
    public static final String INFO_MEMFREE = "mem.free";
    
}
