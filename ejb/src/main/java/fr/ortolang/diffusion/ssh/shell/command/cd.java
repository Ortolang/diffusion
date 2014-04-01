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
public class cd {

	@EJB
	private static CoreService core;
	
	public static void invoke(Interpreter env, CallStack callstack) {
		try {
			String dir = (String) env.eval("bsh.cwd");
			invoke(env, callstack, dir);
		} catch (EvalError e) {
			e.printStackTrace();
		}
	}
	
	public static void invoke(Interpreter env, CallStack callstack, String dir) {
		try {
			//SourcePlantResource resource = getBrowserService().findResource(dir);
			//env.eval("bsh.cwd = " + resource.getResourcePath());
			env.getErr().print("This is not implemented !!");
		} catch (Exception e) {
			env.getErr().print(e.getMessage());
		}
	}

}
