package fr.ortolang.diffusion.ssh.shell.command;

import java.io.IOException;

import bsh.CallStack;
import bsh.Interpreter;

/**
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 6 october 2009
 */
public class exit {
	
	public static void invoke(Interpreter env, CallStack callstack) {
		try {
			env.getIn().close();
		} catch (IOException e) {
			env.getErr().print(e.getMessage());
		}
	}

}
