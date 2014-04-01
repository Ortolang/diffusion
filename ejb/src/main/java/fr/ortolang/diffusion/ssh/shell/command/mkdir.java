package fr.ortolang.diffusion.ssh.shell.command;

import javax.ejb.EJB;

import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import fr.ortolang.diffusion.core.CoreService;

/**
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 6 october 2009
 */
public class mkdir {
	
	@EJB
	private static CoreService core;
	
	public static void invoke(Interpreter env, CallStack callstack, String newDir) {
		invoke(env, callstack, newDir, "no name", "no description");
	}
	
	public static void invoke(Interpreter env, CallStack callstack, String newDir, String name, String description) {
		try {
			String pwd = (String) env.eval("bsh.cwd");
			String path = pwd + "/" + newDir;
			core.createCollection(path, name, description);
		} catch (EvalError e) {
			e.printStackTrace();
		} catch (Exception e) {
			env.getErr().print(e.getMessage());
		}
	}
}