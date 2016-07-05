package fr.ortolang.diffusion.store.binary;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BinaryStoreVolumeMapper {

    private BinaryStoreVolumeMapper() {
    }

    private static final String PREFIX = "volume";

    private static Map<String, String> mapping = new HashMap<String, String> ();
    private static List<String> volumes = new ArrayList<String> ();

    static {
        int n = Integer.parseInt("10", 16);
        int v = 1;
        while ( n <= Integer.parseInt("ff", 16) ) {
            volumes.add(PREFIX + v);
            for (int i=0; i<31; i++) {
                if ( n <= Integer.parseInt("ff", 16) ) {
                    mapping.put(Integer.toHexString(n), PREFIX + v);
                    n++;
                }
            }
            v++;
        }
    }

    public static String getVolume(String digit) throws VolumeNotFoundException {
        if ( mapping.containsKey(digit) ) {
            return mapping.get(digit);
        } else {
            throw new VolumeNotFoundException("No volume mapped for digit: " + digit);
        }
    }

    public static List<String> listVolumes() {
        return volumes;
    }

    public static void main(String[] args) throws VolumeNotFoundException {
        System.out.println(BinaryStoreVolumeMapper.getVolume("b1"));
    }

}
