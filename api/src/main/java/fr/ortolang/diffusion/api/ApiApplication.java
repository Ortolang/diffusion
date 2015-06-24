package fr.ortolang.diffusion.api;

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

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import fr.ortolang.diffusion.api.content.ContentResource;
import fr.ortolang.diffusion.api.filter.ContentTypeSetterPreProcessorInterceptor;
import fr.ortolang.diffusion.api.form.FormResource;
import fr.ortolang.diffusion.api.format.MetadataFormatResource;
import fr.ortolang.diffusion.api.group.GroupResource;
import fr.ortolang.diffusion.api.mapper.*;
import fr.ortolang.diffusion.api.object.ObjectResource;
import fr.ortolang.diffusion.api.organization.OrganizationResource;
import fr.ortolang.diffusion.api.preview.PreviewResource;
import fr.ortolang.diffusion.api.profile.ProfileResource;
import fr.ortolang.diffusion.api.runtime.RuntimeResource;
import fr.ortolang.diffusion.api.workspace.WorkspaceResource;

@ApplicationPath("/*")
public class ApiApplication extends Application {
	
	private HashSet<Class<?>> classes = new HashSet<Class<?>>();

	public ApiApplication() {
		classes.add(ObjectResource.class);
		classes.add(WorkspaceResource.class);
		classes.add(ProfileResource.class);
		classes.add(GroupResource.class);
		classes.add(RuntimeResource.class);
		classes.add(FormResource.class);
		classes.add(MetadataFormatResource.class);
		classes.add(PreviewResource.class);
		classes.add(OrganizationResource.class);
		classes.add(ContentResource.class);
	}

	@Override
	public Set<Class<?>> getClasses() {
		return classes;
	}

	@Override
	public Set<Object> getSingletons() {
		HashSet<Object> set = new HashSet<Object>();
		set.add(new ContentTypeSetterPreProcessorInterceptor());
		set.add(new AccessDeniedExceptionMapper());
		set.add(new BrowserServiceExceptionMapper());
		set.add(new KeyAlreadyExistsExceptionMapper());
		set.add(new KeyNotFoundExceptionMapper());
		set.add(new AliasNotFoundExceptionMapper());
		set.add(new InvalidPathExceptionMapper());
		set.add(new PropertyNotFoundExceptionMapper());
		set.add(new SearchServiceExceptionMapper());
		set.add(new SecurityServiceExceptionMapper());
		set.add(new CollectionNotEmptyExceptionMapper());
		return set;
	}
}