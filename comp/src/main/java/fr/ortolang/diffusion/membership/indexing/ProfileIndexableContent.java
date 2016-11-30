package fr.ortolang.diffusion.membership.indexing;

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

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.indexing.NotIndexableContentException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.membership.entity.ProfileData;
import fr.ortolang.diffusion.membership.entity.ProfileDataVisibility;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.store.es.OrtolangIndexableContent;

import java.util.HashMap;
import java.util.Map;

public class ProfileIndexableContent extends OrtolangIndexableContent {

    private static final Object[] MAPPING;

    static {
        MAPPING = new String[]{
                "key",
                "type=keyword",
                "givenName",
                "type=string",
                "familyName",
                "type=string",
                "emailHash",
                "type=string,index=no",
                "email",
                "type=string",
                "groups",
                "type=string,index=no",
                "infos",
                "type=object"
        };
    }

    public ProfileIndexableContent(Profile profile) throws OrtolangException, NotIndexableContentException, RegistryServiceException, KeyNotFoundException {
        super(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, profile.getObjectKey());
        Map<String, Object> map = new HashMap<>();
        map.put("key", profile.getKey());
        map.put("givenName", profile.getGivenName());
        map.put("familyName", profile.getFamilyName());
        map.put("emailHash", profile.getEmailHash());
        if (profile.getEmailVisibility().equals(ProfileDataVisibility.EVERYBODY)) {
            map.put("email", profile.getEmail());
        }
        map.put("groups", profile.getGroups());
        Map<String, Object> infos = new HashMap<>();
        for (Map.Entry<String, ProfileData> info : profile.getInfos().entrySet()) {
            if (info.getValue().getVisibility().equals(ProfileDataVisibility.EVERYBODY)) {
                infos.put(info.getKey(), info.getValue().getValue());
            }
        }
        map.put("infos", infos);
        setContent(map);
    }

    @Override
    public Object[] getMapping() {
        return MAPPING;
    }

}
