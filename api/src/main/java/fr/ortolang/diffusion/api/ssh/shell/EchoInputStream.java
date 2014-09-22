package fr.ortolang.diffusion.api.ssh.shell;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EchoInputStream extends FilterInputStream {

	private static Logger logger = Logger.getLogger(EchoInputStream.class.getName());

	private OutputStream echo;

	public EchoInputStream(InputStream in, OutputStream echo) {
		super(in);
		this.echo = echo;
	}

	@Override
	public int read() throws IOException {
		int c = super.read();
		logger.log(Level.FINE, "byte read : " + c);
		if ( c == 13 ) {
			echo.write('\r');
		} else if ( c == 127 ) {
			echo.write('\b');
			logger.log(Level.INFO, "Value of character \\b" + (int) 'b');
		} else {
			echo.write(c);
		}
		echo.flush();
		return c;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		b[off] = (byte) read();
		return 1;
	}

	@Override
	public int read(byte[] b) throws IOException {
		b[0] = (byte) read();
		return 1;
	}
}
