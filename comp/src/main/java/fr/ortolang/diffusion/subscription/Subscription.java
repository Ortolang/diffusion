package fr.ortolang.diffusion.subscription;

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

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class Subscription {

    private String username;

    private Broadcaster broadcaster;

    private Set<Filter> filters;

    Subscription(String username, Broadcaster broadcaster) {
        this.username = username;
        this.broadcaster = broadcaster;
        filters = new HashSet<>();
    }

    String getUsername() {
        return username;
    }

    void broadcast(Object o) {
        broadcaster.broadcast(o);
    }

    void destroy() {
        broadcaster.destroy();
    }

    boolean hasAtmosphereResources() {
        return !broadcaster.getAtmosphereResources().isEmpty();
    }

    boolean isConnected() {
        boolean closed = true;
        for (AtmosphereResource atmosphereResource : broadcaster.getAtmosphereResources()) {
            if (!atmosphereResource.transport().equals(AtmosphereResource.TRANSPORT.CLOSE)) {
                closed = false;
            }
        }
        return !closed;
    }

    Collection<Filter> getFilters() {
        return filters;
    }

    boolean addFilter(Filter filter) {
        return filters.add(filter);
    }

    boolean removeFilter(Filter filter) {
        return filters.remove(filter);
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "broadcaster=" + broadcaster.getID() +
                " (" + filters.size() + " filter" + (filters.size() > 1 ? "s)" : ")") +
                '}';
    }
}
