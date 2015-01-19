package fr.ortolang.diffusion.client.auth;

public abstract class KeycloakAuthManager {
	
	public abstract boolean exists(String user);
	
	public abstract boolean check(String user);
	
	public abstract void refresh(String user);
	
	public abstract void redirect(String user);
	
	public abstract void create(String user, String code);
	
	public abstract void revoke(String user);

}
