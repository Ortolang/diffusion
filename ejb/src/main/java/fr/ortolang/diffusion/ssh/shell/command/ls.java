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
public class ls {
	
	@EJB
	private static CoreService core;
	
	public static void invoke(Interpreter env, CallStack callstack) {
		try {
			//TODO depending on the current virtual path, ls should use different service to list elements...
			String pwd = (String) env.eval("bsh.cwd");
			//String[] childs = core.listChildren(pwd);
			String[] childs = new String[] {"fake", "list", "of", "childs"};
			env.print("\r\nTotal " + childs.length);
			for (String child : childs) {
				env.print("\r\n" + child);
			}
		} catch (EvalError e) {
			e.printStackTrace();
		} catch (Exception e) {
			env.getErr().print(e.getMessage());
		}
	}
}