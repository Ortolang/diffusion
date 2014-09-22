package fr.ortolang.diffusion.runtime.task;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

public class ImportWorkspaceTest {
	
	@Test
	public void testImportBag () {
		
		try {
			Mockery context = new Mockery();
			final Path bagtest = Paths.get(getClass().getClassLoader().getResource("bagit2.zip").getPath());
			final BinaryStoreService store = context.mock(BinaryStoreService.class);
			final RuntimeService runtime = context.mock(RuntimeService.class);
			
			context.checking(new Expectations() {{
	            oneOf(store).get(with(equal("tada")));
	            will(returnValue(Files.newInputStream(bagtest)));
	            oneOf(runtime).notifyTaskExecution(with(any(Task.class)));
	        }});
	
			
			ImportWorkspaceTask task = new ImportWorkspaceTask();
			task.setBinaryStore(store);
			task.setRuntime(runtime);
			task.setKey("taskid");
			task.setProcessKey("processid");
			task.setProcessStep(1);
			task.setParam(ImportWorkspaceTask.BAG_HASH_PARAM_NAME, "tada");
			
			task.run();
			
			System.out.println(task.getLog());
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}		

}
