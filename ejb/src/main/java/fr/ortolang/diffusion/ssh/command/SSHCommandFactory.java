package fr.ortolang.diffusion.ssh.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;

import fr.ortolang.diffusion.ssh.SSHServiceException;

/**
 * A factory to register commands and instantiate them allowing sshd to respond with the right command.<br/>
 * <br/>
 * Be careful that registered command are in the classpath of the SSH service.
 * 
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 24 September 2009
 */
public class SSHCommandFactory implements CommandFactory {
	private static Logger logger = Logger.getLogger(SSHCommandFactory.class.getName());
	private HashMap<String, String> commands;

	public SSHCommandFactory() {
		commands = new HashMap<String, String>();
	}

	public Command createCommand(String command) {
		logger.log(Level.FINE, "searching a suitable command for expression : " + command);

		// TODO parse the command better
		String[] parts = command.split(" ");
		String commandName = parts[0];
		String[] commandArgs = new String[0];

		if (parts.length > 1) {
			commandArgs = Arrays.copyOfRange(parts, 1, parts.length);
		}

		if (commands.containsKey(commandName)) {
			String className = commands.get(commandName);
			logger.log(Level.FINE,"command found : " + className + " building new one...");

			try {
				Class commandClass = Class.forName(className);
				SSHCommand sshCommand = (SSHCommand) commandClass.getConstructor(String[].class).newInstance(new Object[] { commandArgs });

				return sshCommand;
			} catch (Exception e) {
				logger.log(Level.WARNING,"error in command creation ", e);

				return new NoOPCommand();
			}
		} else {
			logger.log(Level.FINE,"no command found");

			return new NoOPCommand();
		}
	}

	public void registerCommand(String name, String className) throws SSHServiceException {
		if (commands.containsKey(name)) {
			throw new SSHServiceException("A command with name " + name + " is already registered");
		}

		commands.put(name, className);
	}
}
