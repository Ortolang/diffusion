package fr.ortolang.diffusion.tool.job;

import java.util.List;

import fr.ortolang.diffusion.tool.job.entity.ToolJob;

public interface ToolJobService {

	public void submit(String id, String name, int priority);
	
	public ToolJob read(String id);
	
	public void cancel(String id);
	
	public List<ToolJob> list();

}
