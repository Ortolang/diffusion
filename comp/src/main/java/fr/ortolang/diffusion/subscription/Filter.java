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
import java.util.regex.Pattern;

public class Filter {

    private Pattern fromPattern;

    private Pattern typePattern;

    private Pattern throwedByPattern;

    private Map<String, Pattern> argumentsPatterns;

    public Filter() {
        fromPattern = null;
        typePattern = null;
        throwedByPattern = null;
        argumentsPatterns = null;
    }

    public boolean matches(OrtolangEvent event) {
        if (fromPattern != null && !fromPattern.matcher(event.getFromObject()).matches() ||
                typePattern != null && !typePattern.matcher(event.getType()).matches() ||
                throwedByPattern != null && !throwedByPattern.matcher(event.getThrowedBy()).matches()) {
            return false;
        }
        if (argumentsPatterns != null) {
            for (Map.Entry<String, Pattern> argumentsPattern : argumentsPatterns.entrySet()) {
                Object o = event.getArguments().get(argumentsPattern.getKey());
                if (o instanceof String) {
                    String value = (String) o;
                    if (!argumentsPattern.getValue().matcher(value).matches()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public Pattern getFromPattern() {
        return fromPattern;
    }

    public void setFromPattern(Pattern fromPattern) {
        this.fromPattern = fromPattern;
    }

    public Pattern getTypePattern() {
        return typePattern;
    }

    public void setTypePattern(Pattern typePattern) {
        this.typePattern = typePattern;
    }

    public Pattern getThrowedByPattern() {
        return throwedByPattern;
    }

    public void setThrowedByPattern(Pattern throwedByPattern) {
        this.throwedByPattern = throwedByPattern;
    }

    public Map<String, Pattern> getArgumentsPatterns() {
        return argumentsPatterns;
    }

    public void setArgumentsPatterns(Map<String, Pattern> argumentsPatterns) {
        this.argumentsPatterns = argumentsPatterns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Filter filter = (Filter) o;

        if (fromPattern != null ? !fromPattern.pattern().equals(filter.fromPattern.pattern()) : filter.fromPattern != null) return false;
        if (typePattern != null ? !typePattern.pattern().equals(filter.typePattern.pattern()) : filter.typePattern != null) return false;
        if (throwedByPattern != null ? !throwedByPattern.pattern().equals(filter.throwedByPattern.pattern()) : filter.throwedByPattern != null)
            return false;
        if (argumentsPatterns != null ? argumentsPatterns.size() != filter.argumentsPatterns.size() : filter.argumentsPatterns != null) {
            return false;
        }
        if (argumentsPatterns != null) {
            for (Map.Entry<String, Pattern> patternEntry : argumentsPatterns.entrySet()) {
                if (patternEntry.getValue() != null ? !patternEntry.getValue().pattern().equals(filter.argumentsPatterns.get(patternEntry.getKey()).pattern()) : filter.argumentsPatterns.get(patternEntry.getKey()) != null) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = fromPattern != null ? fromPattern.pattern().hashCode() : 0;
        result = 31 * result + (typePattern != null ? typePattern.pattern().hashCode() : 0);
        result = 31 * result + (throwedByPattern != null ? throwedByPattern.pattern().hashCode() : 0);
        if (argumentsPatterns != null) {
            for (Pattern pattern : argumentsPatterns.values()) {
                result = 31 * result + (pattern != null ? pattern.pattern().hashCode() : 0);
            }
        }
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
