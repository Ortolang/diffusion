package fr.ortolang.diffusion.client.cmd;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class OrtolangCli {
	
	private static OrtolangCli instance;
	private Map<String, String> commands = new HashMap<String, String>();
	
	private OrtolangCli() {
		commands.put("delete-workspace", DeleteWorkspaceCommand.class.getName());
		commands.put("import-workspace", ImportWorkspaceCommand.class.getName());
	}
	
	public static OrtolangCli getInstance() {
		if (instance == null) {
			instance = new OrtolangCli();
		}
		return instance;
	}
	
	public void parse(String[] args) {
		if ( args.length > 0 ) {
			String command = args[0];
			if ( commands.containsKey(command) ) {
				System.out.println("Executing command: " + command);
				try {
					Command instance = (Command) Class.forName(commands.get(command)).newInstance();
					instance.execute(Arrays.copyOfRange(args, 1, args.length));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("Command not found");
				help();
			}
		} else {
			help();
		}
	}
	
	private void help() {
		System.out.println("Ortolang CLI availables commands :");
		for (String command : commands.keySet()) {
			System.out.println("\t " + command);
		}
		System.exit(0);
	}
	
	public static void main(String[] args) {
		OrtolangCli.getInstance().parse(args);
	}

}
