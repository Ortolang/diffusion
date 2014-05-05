package fr.ortolang.diffusion.test.bench;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses(value=SampleOneCollectionBench.class)
public class BenchSuite {
	
	public static final String SERVER_ADDRESS = "localhost";
	public static final String SERVER_PORT = "8080";
	public static final String APPLICATION_NAME = "diffusion";
	public static final String APPLICATION_REST_PREFIX = "rest";
	public static final String USERID = "user1";
	public static final String PASSWORD = "tagada";

}
