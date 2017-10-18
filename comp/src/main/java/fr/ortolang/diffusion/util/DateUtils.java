package fr.ortolang.diffusion.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import fr.ortolang.diffusion.oai.format.Constant;

public class DateUtils {

	public static boolean isThisDateValid(String dateToValidate){
		if(dateToValidate == null){
			return false;
		}
		if (!dateToValidate.matches(Constant.w3cdtfPattern)) {
			return false;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(Constant.w3cdtfFormat);
		sdf.setLenient(false);
		try {
			//if not valid, it will throw ParseException
			Date date = sdf.parse(dateToValidate);
		} catch (ParseException e) {
			return false;
		}
		return true;
	}
}
