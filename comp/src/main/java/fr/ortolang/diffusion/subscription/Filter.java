package fr.ortolang.diffusion.subscription;

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

import fr.ortolang.diffusion.OrtolangEvent;

import java.util.Map;

public class Filter {

    private String fromPattern;

    private String typePattern;

    private String throwedByPattern;

    private Map<String, String> argumentsPatterns;

    public Filter() {
        fromPattern = "";
        typePattern = "";
        throwedByPattern = "";
        argumentsPatterns = null;
    }

    public boolean matches(OrtolangEvent event) {
        if (fromPattern.length() != 0 && !event.getFromObject().matches(fromPattern) ||
                typePattern.length() != 0 && !event.getType().matches(typePattern) ||
                throwedByPattern.length() != 0 && !event.getThrowedBy().matches(throwedByPattern)) {
            return false;
        }
        if (argumentsPatterns != null) {
            for (Map.Entry<String, String> argumentsPattern : argumentsPatterns.entrySet()) {
                Object o = event.getArguments().get(argumentsPattern.getKey());
                if (o instanceof String) {
                    String value = (String) o;
                    if (!value.matches(argumentsPattern.getValue())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public String getFromPattern() {
        return fromPattern;
    }

    public void setFromPattern(String fromPattern) {
        this.fromPattern = fromPattern;
    }

    public String getTypePattern() {
        return typePattern;
    }

    public void setTypePattern(String typePattern) {
        this.typePattern = typePattern;
    }

    public String getThrowedByPattern() {
        return throwedByPattern;
    }

    public void setThrowedByPattern(String throwedByPattern) {
        this.throwedByPattern = throwedByPattern;
    }

    public Map<String, String> getArgumentsPatterns() {
        return argumentsPatterns;
    }

    public void setArgumentsPatterns(Map<String, String> argumentsPatterns) {
        this.argumentsPatterns = argumentsPatterns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Filter filter = (Filter) o;

        if (!fromPattern.equals(filter.fromPattern)) return false;
        if (!typePattern.equals(filter.typePattern)) return false;
        if (!throwedByPattern.equals(filter.throwedByPattern)) return false;
        return !(argumentsPatterns != null ? !argumentsPatterns.equals(filter.argumentsPatterns) : filter.argumentsPatterns != null);

    }

    @Override
    public int hashCode() {
        int result = fromPattern.hashCode();
        result = 31 * result + typePattern.hashCode();
        result = 31 * result + throwedByPattern.hashCode();
        result = 31 * result + (argumentsPatterns != null ? argumentsPatterns.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Filter{" +
                "typePattern='" + typePattern + '\'' +
                ", fromPattern='" + fromPattern + '\'' +
                ", throwedByPattern='" + throwedByPattern + '\'' +
                (argumentsPatterns != null ? ", argumentsPatterns=" + argumentsPatterns : "") +
                '}';
    }
}
