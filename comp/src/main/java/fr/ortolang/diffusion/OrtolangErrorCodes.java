package fr.ortolang.diffusion;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * *
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 * *
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

public final class OrtolangErrorCodes {

    public static final String PATH_NOT_FOUND_EXCEPTION = "1";
    public static final String PATH_ALREADY_EXISTS_EXCEPTION = "2";
    public static final String ALIAS_ALREADY_EXISTS_EXCEPTION = "3";
    public static final String INVALID_PATH_EXCEPTION = "4";
    public static final String ROOT_NOT_FOUND_EXCEPTION = "5";
    public static final String ALIAS_NOT_FOUND_EXCEPTION = "6";
    public static final String COLLECTION_NOT_EMPTY_EXCEPTION = "7";
    public static final String ACCESS_DENIED_EXCEPTION = "8";
    public static final String METADATA_FORMAT_EXCEPTION = "9";
    public static final String CORE_SERVICE_EXCEPTION = "10";
    public static final String CONTENT_SEARCH_SERVICE_EXCEPTION = "11";
    public static final String RECORD_NOT_FOUND_EXCEPTION = "12";

    private OrtolangErrorCodes() {
    }
}
