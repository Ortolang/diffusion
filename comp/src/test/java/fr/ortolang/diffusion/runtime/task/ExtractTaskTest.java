package fr.ortolang.diffusion.runtime.task;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

public class ExtractTaskTest {
	
	@Test
	public void testExtractTask () {
		
		try {
			Mockery context = new Mockery();
			final Path ziptest = Paths.get(getClass().getClassLoader().getResource("bag.zip").getPath());
			final Path ziptest2 = Paths.get(getClass().getClassLoader().getResource("bagit.zip").getPath());
			final BinaryStoreService store = context.mock(BinaryStoreService.class);
			final RuntimeService runtime = context.mock(RuntimeService.class);
			
			context.checking(new Expectations() {{
				oneOf(store).type(with(equal("tada")));
	            will(returnValue("application/zip"));
	            oneOf(store).get(with(equal("tada")));
	            will(returnValue(Files.newInputStream(ziptest)));
	            oneOf(runtime).notifyTaskExecution(with(any(Task.class)));
	            
	            oneOf(store).type(with(equal("tada2")));
	            will(returnValue("application/zip"));
	            oneOf(store).get(with(equal("tada2")));
	            will(returnValue(Files.newInputStream(ziptest2)));
	            oneOf(runtime).notifyTaskExecution(with(any(Task.class)));
	        }});
	
			
			ExtractTask task = new ExtractTask();
			task.setBinaryStore(store);
			task.setRuntime(runtime);
			task.setKey("taskid");
			task.setProcessKey("processid");
			task.setProcessStep(1);
			task.setParam(ExtractTask.HASH_PARAM_NAME, "tada");
			task.setParam(ExtractTask.EXTRACT_PATH_PARAM_NAME, "bag-path");
			
			task.run();
			
			System.out.println(task.getLog());
			
			ExtractTask task2 = new ExtractTask();
			task2.setBinaryStore(store);
			task2.setRuntime(runtime);
			task2.setKey("taskid2");
			task2.setProcessKey("processid");
			task2.setProcessStep(1);
			task2.setParam(ExtractTask.HASH_PARAM_NAME, "tada2");
			task2.setParam(ExtractTask.EXTRACT_PATH_PARAM_NAME, "bagit-path");
			
			task2.run();
			
			System.out.println(task2.getLog());
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
		
		
	}

}
