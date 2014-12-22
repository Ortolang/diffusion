package fr.ortolang.diffusion.tool.invoke;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.hamcrest.Description;
import org.jmock.api.Action;
import org.jmock.api.Invocation;

public class ToolJobInvocationUtilsMock implements Action {

	private static final String DEFAULT_FILES_FOLDER = "src/test/resources/";
	    	    
    @Override
	public void describeTo(Description description) {
        description.appendText("returns ")
                   .appendValueList("", ", ", "", "path of dataobject");
    }
    
    @Override
	public Object invoke(Invocation invocation) throws Throwable {
    	String fileName = "test-fr.txt";
		Path inputFile = Paths.get(DEFAULT_FILES_FOLDER + "test-fr.txt");
		Path path = Paths.get(invocation.getParameter(0).toString(), fileName);
		Files.copy(inputFile, path, StandardCopyOption.REPLACE_EXISTING);
		System.out.println(invocation.getParameter(0) + ", " + invocation.getParameter(1));
		return (Path) path;
    }

}
