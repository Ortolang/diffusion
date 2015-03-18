package fr.ortolang.diffusion.client.cmd;


public abstract class Command {
	
	public abstract void execute(String[] args) throws Exception;
	
}
