package fr.ortolang.diffusion.runtime.engine.task;

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

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBTransactionRolledbackException;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.entity.Group;
import fr.ortolang.diffusion.membership.entity.ProfileData;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.template.MessageResolverMethod;
import fr.ortolang.diffusion.template.TemplateEngine;
import fr.ortolang.diffusion.template.TemplateEngineException;

public class NotifyTask extends RuntimeEngineTask {

    private static final Logger LOGGER = Logger.getLogger(NotifyTask.class.getName());
    private static final ClassLoader TEMPLATE_ENGINE_CL = NotifyTask.class.getClassLoader();

    public static final Locale DEFAULT_LOCALE = Locale.FRANCE;
    public static final String NAME = "Notify";

    private Expression userid;
    private Expression groupid;
    private Expression userType;
    private Expression titleKey;
    private Expression subjectKey;
    private Expression bodyKey;

    public NotifyTask() {
    }

    public Expression getUserType() {
        return userType;
    }

    public void setUserType(Expression userType) {
        this.userType = userType;
    }

    public Expression getUserId() {
        return userid;
    }

    public void setUserId(Expression userid) {
        this.userid = userid;
    }

    public Expression getGroupId() {
        return groupid;
    }

    public void setGroupId(Expression groupid) {
        this.groupid = groupid;
    }

    public Expression getTitleKey() {
        return titleKey;
    }

    public void setTitleKey(Expression titleKey) {
        this.titleKey = titleKey;
    }

    public Expression getSubjectKey() {
        return subjectKey;
    }

    public void setSubjectKey(Expression subjectKey) {
        this.subjectKey = subjectKey;
    }

    public Expression getBodyKey() {
        return bodyKey;
    }

    public void setBodyKey(Expression bodyKey) {
        this.bodyKey = bodyKey;
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        try {
            String wsalias = (String) execution.getVariable("wsalias");

            String marketUrl = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.MARKET_SERVER_URL);
            String senderName = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.SMTP_SENDER_NAME);
            String senderEmail = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.SMTP_SENDER_EMAIL);

            List<String> recipients = new ArrayList<String>();
            String user = (String) userid.getValue(execution);
            if ( user != null && user.length() > 0 ) {
                LOGGER.log(Level.FINE, "Searching email for user: " + user);
                try {
                    String useremail = getMembershipService().systemReadProfileEmail(user);
                    if (useremail == null || useremail.length() == 0) {
                        LOGGER.log(Level.INFO, "No email found for user: " + user + ", unable to notify");
                    } else {
                        LOGGER.log(Level.FINE, "Email found for user: " + user + ", adding to recipients list");
                        recipients.add(useremail);
                    }
                } catch (MembershipServiceException | KeyNotFoundException e ) {
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "error while trying to load email for user: " + user));
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "error while trying to load email for user: " + user, e));
                }
            }
            String group = (String) groupid.getValue(execution);
            if ( group != null && group.length() > 0 ) {
                LOGGER.log(Level.FINE, "Loading Group with group: " + group);
                try {
                    Group g = getMembershipService().readGroup(group);
                    for (String member : g.getMembers()) {
                        String memberemail = getMembershipService().systemReadProfileEmail(member);
                        if (memberemail == null || memberemail.length() == 0) {
                            LOGGER.log(Level.INFO, "No email found for group member: " + member + ", unable to notify");
                        } else {
                            LOGGER.log(Level.FINE, "Email found for group member [" + member + ", adding to recipients list");
                            recipients.add(memberemail);
                        }
                    }
                } catch (MembershipServiceException | KeyNotFoundException | AccessDeniedException e ) {
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "error while trying to load emails for group: " + group));
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "error while trying to load emails for group: " + group, e));
                } 
            }
            
            for ( String recipient : recipients ) {
                try {
                    Locale locale = getUserLocale(user);
                    Map<String, Object> model = new HashMap<>(execution.getVariables());
                    Object[] args = new Object[] { marketUrl, wsalias, senderName, model.get("reason") };
                    model.put("userType", getUserType().getValue(execution));
                    model.put("title", getTitleKey().getValue(execution));
                    model.put("body", getBodyKey().getValue(execution));
                    model.put("msg", new MessageResolverMethod(locale));
                    model.put("marketUrl", marketUrl);
                    model.put("wsalias", wsalias);
                    model.put("args", args);
                    String subject = getMessage("submit.subject", locale, args);
                    String message = TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process("notification", model);
                    notify(senderName, senderEmail, recipient, subject, message);
                } catch (UnsupportedEncodingException | MessagingException | TemplateEngineException | MembershipServiceException | KeyNotFoundException  e) {
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to notify recipient: " + recipient));
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "unable to notify recipient: " + recipient, e));
                }
            }

        } catch (SecurityException | IllegalStateException | EJBTransactionRolledbackException e) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Unexpected error occured: " + e.getMessage()));
            throw new RuntimeEngineTaskException("unexpected error occurred", e);
        }

    }

    private Locale getUserLocale(String userid) throws MembershipServiceException, KeyNotFoundException, RuntimeEngineTaskException {
        ProfileData recipientLanguageData = getMembershipService().systemGetProfileInfo(userid, "language");
        if (recipientLanguageData != null) {
            return new Locale(recipientLanguageData.getValue());
        } else {
            return DEFAULT_LOCALE;
        }
    }

    private String getMessage(String key, Locale locale, Object... arguments) {
        return MessageFormat.format(ResourceBundle.getBundle("notification", locale).getString(key), arguments);
    }

    private void notify(String senderName, String senderEmail, String receiverEmail, String subject, String message) throws MessagingException, UnsupportedEncodingException,
            RuntimeEngineTaskException {
        Message msg = new MimeMessage(getMailSession());
        msg.setSubject(subject);
        msg.setRecipient(RecipientType.TO, new InternetAddress(receiverEmail));
        msg.setFrom(new InternetAddress(senderEmail, senderName));
        msg.setContent(message, "text/html; charset=\"UTF-8\"");
        Transport.send(msg);
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

}
