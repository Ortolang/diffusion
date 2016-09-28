package fr.ortolang.diffusion.referential;

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
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.event.entity.Event;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import org.jboss.ejb3.annotation.SecurityDomain;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RunAs;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(name = "ReferentialMDB", activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/notification"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue="eventtype LIKE 'membership.profile.%'")})
@SecurityDomain("ortolang")
@RunAs("system")
public class ReferentialListenerBean implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(ReferentialListenerBean.class.getName());

    @EJB
    IndexingService indexing;
    @EJB
    MembershipService membership;

    @Override
    @PermitAll
    public void onMessage(Message message) {
        try {
            OrtolangEvent event = new Event();
            event.fromJMSMessage(message);
            Profile profile = membership.systemReadProfile(event.getFromObject());
//            if ("membership.profile.create".equals(event.getType()) && profile.isComplete()) {
//                String refEntityName = (StringUtils.stripAccents(profile.getGivenName()) + "_" + StringUtils.stripAccents(profile.getFamilyName())).toLowerCase();
//                JsonObjectBuilder builder = Json.createObjectBuilder();
//                builder.add("schema", "http://www.ortolang.fr/schema/person/02#");
//                builder.add("type", "Person");
//                builder.add("id", refEntityName);
//                builder.add("fullname", profile.getFullName());
//                builder.add("firstname", profile.getGivenName());
//                builder.add("lastname", profile.getFamilyName());
//                builder.add("username", "${" + profile.getId() + "}");
//                builder.add("title", profile.getInfo("civility") == null ? "" : profile.getInfo("civility").getValue());
//                builder.add("organization", profile.getInfo("organisation") == null ? "" : profile.getInfo("organisation").getValue());
//                try {
//                    ReferentialEntity entity = referential.systemCreateEntity(refEntityName, ReferentialEntityType.PERSON, builder.build().toString(), profile.getId());
//                    membership.systemSetProfileReferentialId(profile.getId(), entity.getKey());
//                } catch (ReferentialServiceException | KeyAlreadyExistsException e) {
//                    LOGGER.log(Level.SEVERE, "Unable to create referential entity", e);
//                }
//            }
            if (profile.getReferentialId() != null) {
                indexing.index(profile.getReferentialId());
            }
        } catch (OrtolangException | MembershipServiceException | KeyNotFoundException | IndexingServiceException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

}

