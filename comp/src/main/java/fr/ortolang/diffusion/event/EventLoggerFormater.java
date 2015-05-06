package fr.ortolang.diffusion.event;

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

import java.text.SimpleDateFormat;
import java.util.HashMap;

import fr.ortolang.diffusion.event.entity.Event;

public class EventLoggerFormater {
	
	private static HashMap<String, SimpleDateFormat> sdf = new HashMap<String, SimpleDateFormat> (); 
	private static String fieldSeparator = ",";
	
	private static SimpleDateFormat getEventDateFormatter() {
		String key = Thread.currentThread().getId() + "";
		if ( !sdf.containsKey(key) ) {
			sdf.put(key, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSS ZZ"));
		}
		return sdf.get(key);
	}
	
	public static String formatEvent(Event e) {
		StringBuilder builder = new StringBuilder();
		builder.append("[" + getEventDateFormatter().format(e.getDate()) + "]");
		builder.append(fieldSeparator);
		builder.append(e.getFromObject());
		builder.append(fieldSeparator);
		builder.append(e.getObjectType());
		builder.append(fieldSeparator);
		builder.append(e.getType());
		builder.append(fieldSeparator);
		builder.append(e.getThrowedBy());
		builder.append(fieldSeparator);
		builder.append(e.getArguments());
		return builder.toString();
	}

}