package fr.ortolang.diffusion.client;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import fr.ortolang.diffusion.client.cmd.FindCommand;

public class FindCommandTest {

	@Test
	public void findAll() {
		FindCommand cmd = new FindCommand();
		List<String> argList = new ArrayList<String>();
		argList.add("-U");
		argList.add("root");
		argList.add("-P");
		argList.add("tagada54");
        argList.add("-w");
        argList.add("8bf85d87-71f5-48f6-835d-6794a232efc1");
        cmd.execute(argList.toArray(new String[0]));
	}

	@Test
	public void findWithFilter() {
		FindCommand cmd = new FindCommand();
		List<String> argList = new ArrayList<String>();
		argList.add("-U");
		argList.add("root");
		argList.add("-P");
		argList.add("tagada54");
        argList.add("-w");
        argList.add("8bf85d87-71f5-48f6-835d-6794a232efc1");
        argList.add("-f");
        argList.add(".*\\.trs");
        cmd.execute(argList.toArray(new String[0]));
	}
}
