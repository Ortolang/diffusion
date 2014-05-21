import java.util.regex.Pattern;

import org.junit.Test;


public class TestHttpRewrite {
	
	@Test
	public void testRewrite() {
		Pattern regexp = Pattern.compile("(?<=/rest/objects).*|(?<=/rest/core/collections).*");
		String path1 = "/rest/objects/guest";
		
		System.out.println(regexp.matcher(path1).groupCount());
		System.out.println(regexp.matcher(path1).group());
		System.out.println(regexp.matcher(path1));
	}

}
