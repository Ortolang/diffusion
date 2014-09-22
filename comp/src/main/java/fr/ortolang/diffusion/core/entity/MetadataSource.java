package fr.ortolang.diffusion.core.entity;

import java.util.Set;

public interface MetadataSource {
	
	public Set<MetadataElement> getMetadatas();
	
	public void setMetadatas(Set<MetadataElement> metadatas);
	
	public boolean addMetadata(MetadataElement metadata);
	
	public boolean removeMetadata(MetadataElement metadata);
	
	public boolean containsMetadata(MetadataElement metadata);
	
	public boolean containsMetadataName(String name);
	
	public boolean containsMetadataKey(String key);
	
	public MetadataElement findMetadataByName(String name);

}
