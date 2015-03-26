package fr.ortolang.diffusion.api.ssh.shell;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EchoInputStream extends FilterInputStream {

	private static final Logger LOGGER = Logger.getLogger(EchoInputStream.class.getName());

	private OutputStream echo;

	public EchoInputStream(InputStream in, OutputStream echo) {
		super(in);
		this.echo = echo;
	}

	@Override
	public int read() throws IOException {
		int c = super.read();
		LOGGER.log(Level.FINE, "byte read : " + c);
		if ( c == 13 ) {
			echo.write('\r');
		} else if ( c == 127 ) {
			echo.write('\b');
			LOGGER.log(Level.INFO, "Value of character \\b" + (int) 'b');
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
