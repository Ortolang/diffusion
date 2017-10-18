package fr.ortolang.diffusion.util;

import org.junit.Test;
import org.springframework.util.Assert;

public class DateUtilsTest {

	@Test
	public void validDate() {
		Assert.isTrue(DateUtils.isThisDateValid("2016-01-11"));
	}
	
	public void invalidDate() {
		Assert.isTrue(!DateUtils.isThisDateValid("2016-01-11/2016-10-12"));
	}
}
