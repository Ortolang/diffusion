package fr.ortolang.diffusion.statistics;

import java.util.List;
import java.util.Map;

import fr.ortolang.diffusion.OrtolangService;

public interface StatisticsService extends OrtolangService {
    
    public static final String SERVICE_NAME = "statistics";
    
    public List<String> list() throws StatisticsServiceException;
    
    public void probe() throws StatisticsServiceException;
    
    public String[] read(String name) throws StatisticsServiceException, StatisticNameNotFoundException;
    
    public Map<String, String> history(String name, long from, long to) throws StatisticsServiceException, StatisticNameNotFoundException;
    

}
