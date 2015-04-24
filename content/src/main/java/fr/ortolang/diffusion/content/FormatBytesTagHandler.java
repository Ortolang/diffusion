package fr.ortolang.diffusion.content;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

@SuppressWarnings("serial")
public class FormatBytesTagHandler extends TagSupport {

	private long value;

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}

	@Override
	public int doStartTag() throws JspException {
		JspWriter out = pageContext.getOut();
		try {
			out.print(parse(value));
		} catch ( IOException e ) {
			throw new JspException(e);
		}
        return SKIP_BODY;
	}
	
	private String parse(long bytes) {
		if (bytes < 1000) {
			return bytes + " B";
		}
		int exp = (int) (Math.log(bytes) / Math.log(1000));
		String pre = "kMGTPE".charAt(exp - 1) + "";
		return String.format("%.1f %sB", bytes / Math.pow(1000, exp), pre);
	}

}
