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

import fr.ortolang.diffusion.api.admin.AdminResource;
import fr.ortolang.diffusion.api.auth.AuthResource;
import fr.ortolang.diffusion.api.config.ConfigResource;
import fr.ortolang.diffusion.api.content.ContentResource;
import fr.ortolang.diffusion.api.event.EventFeedResource;
import fr.ortolang.diffusion.api.filter.CacheFilter;
import fr.ortolang.diffusion.api.filter.ContentTypeSetterPreProcessorInterceptor;
import fr.ortolang.diffusion.api.form.FormResource;
import fr.ortolang.diffusion.api.format.MetadataFormatResource;
import fr.ortolang.diffusion.api.group.GroupResource;
import fr.ortolang.diffusion.api.mapper.AccessDeniedExceptionMapper;
import fr.ortolang.diffusion.api.mapper.AliasAlreadyExistsExceptionMapper;
import fr.ortolang.diffusion.api.mapper.AliasNotFoundExceptionMapper;
import fr.ortolang.diffusion.api.mapper.BrowserServiceExceptionMapper;
import fr.ortolang.diffusion.api.mapper.CollectionNotEmptyExceptionMapper;
import fr.ortolang.diffusion.api.mapper.ContentSearchExceptionMapper;
import fr.ortolang.diffusion.api.mapper.ContentSearchNotFoundExceptionMapper;
import fr.ortolang.diffusion.api.mapper.CoreServiceExceptionMapper;
import fr.ortolang.diffusion.api.mapper.EJBAccessExceptionMapper;
import fr.ortolang.diffusion.api.mapper.HandleNotFoundExceptionMapper;
import fr.ortolang.diffusion.api.mapper.InvalidPathExceptionMapper;
import fr.ortolang.diffusion.api.mapper.KeyAlreadyExistsExceptionMapper;
import fr.ortolang.diffusion.api.mapper.KeyNotFoundExceptionMapper;
import fr.ortolang.diffusion.api.mapper.MetadataFormatExceptionMapper;
import fr.ortolang.diffusion.api.mapper.OaiServiceExceptionMapper;
import fr.ortolang.diffusion.api.mapper.PathAlreadyExistsExceptionMapper;
import fr.ortolang.diffusion.api.mapper.PathNotFoundExceptionMapper;
import fr.ortolang.diffusion.api.mapper.PropertyNotFoundExceptionMapper;
import fr.ortolang.diffusion.api.mapper.RootNotFoundExceptionMapper;
import fr.ortolang.diffusion.api.mapper.SearchServiceExceptionMapper;
import fr.ortolang.diffusion.api.mapper.SecurityServiceExceptionMapper;
import fr.ortolang.diffusion.api.mapper.SubscriptionServiceExceptionMapper;
import fr.ortolang.diffusion.api.message.MessageResource;
import fr.ortolang.diffusion.api.oai.OaiResource;
import fr.ortolang.diffusion.api.oaipmh.OAIPMHServlet;
import fr.ortolang.diffusion.api.object.ObjectResource;
import fr.ortolang.diffusion.api.profile.ProfileResource;
import fr.ortolang.diffusion.api.referential.ReferentialResource;
import fr.ortolang.diffusion.api.runtime.RuntimeResource;
import fr.ortolang.diffusion.api.search.SearchResource;
import fr.ortolang.diffusion.api.seo.SeoResource;
import fr.ortolang.diffusion.api.statistics.StatisticsResource;
import fr.ortolang.diffusion.api.thumbnail.ThumbnailResource;
import fr.ortolang.diffusion.api.workspace.WorkspaceResource;

@ApplicationPath("/*")
public class ApiApplication extends Application {

    private Set<Class<?>> classes = new HashSet<>();

    public ApiApplication() {
        classes.add(ObjectResource.class);
        classes.add(WorkspaceResource.class);
        classes.add(ProfileResource.class);
        classes.add(GroupResource.class);
        classes.add(RuntimeResource.class);
        classes.add(FormResource.class);
        classes.add(MetadataFormatResource.class);
        classes.add(ReferentialResource.class);
        classes.add(AdminResource.class);
        classes.add(AuthResource.class);
        classes.add(ContentResource.class);
        classes.add(ThumbnailResource.class);
        classes.add(ConfigResource.class);
        classes.add(StatisticsResource.class);
        classes.add(SearchResource.class);
        classes.add(OAIPMHServlet.class);
        classes.add(EventFeedResource.class);
        classes.add(SeoResource.class);
        classes.add(MessageResource.class);
        classes.add(OaiResource.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> set = new HashSet<>();
        set.add(new ContentTypeSetterPreProcessorInterceptor());
        set.add(new CacheFilter());
        set.add(new AccessDeniedExceptionMapper());
        set.add(new BrowserServiceExceptionMapper());
        set.add(new ContentSearchExceptionMapper());
        set.add(new ContentSearchNotFoundExceptionMapper());
        set.add(new KeyAlreadyExistsExceptionMapper());
        set.add(new KeyNotFoundExceptionMapper());
        set.add(new AliasNotFoundExceptionMapper());
        set.add(new AliasAlreadyExistsExceptionMapper());
        set.add(new InvalidPathExceptionMapper());
        set.add(new PropertyNotFoundExceptionMapper());
        set.add(new SearchServiceExceptionMapper());
        set.add(new SecurityServiceExceptionMapper());
        set.add(new CollectionNotEmptyExceptionMapper());
        set.add(new CoreServiceExceptionMapper());
        set.add(new SubscriptionServiceExceptionMapper());
        set.add(new EJBAccessExceptionMapper());
        set.add(new PathAlreadyExistsExceptionMapper());
        set.add(new PathNotFoundExceptionMapper());
        set.add(new RootNotFoundExceptionMapper());
        set.add(new AuthorizationRequestFilter());
        set.add(new MetadataFormatExceptionMapper());
        set.add(new HandleNotFoundExceptionMapper());
        set.add(new OaiServiceExceptionMapper());
        return set;
    }
}
