package fr.ortolang.diffusion.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Test;

import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;
import fr.ortolang.diffusion.client.cmd.CopyCommand;

public class CopyCommandTest {

	private static Logger logger = Logger.getLogger(CopyCommandTest.class.toString());
	

	@Test
	public void copy() throws IOException, OrtolangClientException, OrtolangClientAccountException {
		CopyCommand cmd = new CopyCommand();
		List<String> argList = new ArrayList<String>();
		argList.add("-U");
		argList.add("root");
		argList.add("-P");
		argList.add("tagada54");
		argList.add("/Users/cpestel/tmp/test");
		argList.add("/");
		cmd.execute(argList.toArray(new String[0]));
	}
}
