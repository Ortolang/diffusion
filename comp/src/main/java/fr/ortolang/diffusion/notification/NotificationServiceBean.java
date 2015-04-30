package fr.ortolang.diffusion.notification;

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

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Topic;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;

@Local(NotificationService.class)
@Stateless(name = NotificationService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class NotificationServiceBean implements NotificationService {
	
	private static final Logger LOGGER = Logger.getLogger(NotificationServiceBean.class.getName());
	
	@Resource(mappedName = "java:jboss/exported/jms/topic/notification")
	private Topic notificationTopic;
	@Inject
	private JMSContext context;
	
	public NotificationServiceBean() {
	}

	@Override
	public void throwEvent(String fromObject, String throwedBy, String objectType, String eventType) throws NotificationServiceException {
		throwEvent(fromObject, throwedBy, objectType, eventType, null);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void throwEvent(String fromObject, String throwedBy, String objectType, String eventType, Map<String, Object> args) throws NotificationServiceException {
		try {
			Message message = context.createMessage();
			message.setStringProperty(OrtolangEvent.DATE, OrtolangEvent.getEventDateFormatter().format(new Date()));
			message.setStringProperty(OrtolangEvent.THROWED_BY, throwedBy);
			message.setStringProperty(OrtolangEvent.FROM_OBJECT, fromObject);
			message.setStringProperty(OrtolangEvent.OBJECT_TYPE, objectType);
			message.setStringProperty(OrtolangEvent.TYPE, eventType);
			if (args != null) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(args);
				oos.close();
				message.setStringProperty(OrtolangEvent.ARGUMENTS, Base64.encodeBase64String(baos.toByteArray()));
			}
			context.createProducer().send(notificationTopic, message);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "unable to throw event", e);
			throw new NotificationServiceException("unable to throw event", e);
		}
	}
}
