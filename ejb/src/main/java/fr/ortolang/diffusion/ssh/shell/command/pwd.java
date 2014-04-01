package fr.ortolang.diffusion.ssh.shell.command;

import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;

/**
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 6 october 2009
 */
public class pwd {

	public static void invoke(Interpreter env, CallStack callstack) {
		try {
			String pwd = (String) env.eval("bsh.cwd");
			env.print("\r\n" + pwd);
		} catch (EvalError e) {
			e.printStackTrace();
		}
	}

}
