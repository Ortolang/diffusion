package fr.ortolang.diffusion.runtime.engine.task;

public class GenerateHandlesTask {
	
	//TODO : 
	// à partir de la racine : 
	// Si c'est la racine, l'URL ne pointe pas vers le même 
	// createHandle(handle=/alias/version/currentpath, key, URL=content/alias/version/currentpath)
	// createHandle(handle=/alias/currentpath, key, URL=content/alias/version/currentpath)
	// createHandle(handle=/currentkey, URL=content/key/currentkey)
	// si element est une collection, pour chaque élément de la collection
	//    rappeler la fonction, currentpath = currentpath + /elementname

	//La fonction createHandle ajoute les entrées suivantes : 
	// 1. une entrée d'admin pour le handle
	// 2. une entrée d'URL pour le handle
	
	

}
