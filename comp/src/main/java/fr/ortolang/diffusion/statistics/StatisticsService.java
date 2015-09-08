package fr.ortolang.diffusion.statistics;

import java.util.Map;
import java.util.Set;

import fr.ortolang.diffusion.OrtolangService;

public interface StatisticsService extends OrtolangService {
    
    public static final String SERVICE_NAME = "statistics";
    
    public Set<String> list() throws StatisticsServiceException;
    
    public String read(String name) throws StatisticsServiceException;
    
    public String probe(String name) throws StatisticsServiceException;
    
    public Map<String, String> history(String name, long from, long to) throws StatisticsServiceException;
    

}
