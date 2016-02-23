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

import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.entity.Group;
import fr.ortolang.diffusion.membership.entity.ProfileData;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.runtime.entity.Process;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.template.TemplateEngine;
import fr.ortolang.diffusion.template.TemplateEngineException;

public class NotifyTask extends RuntimeEngineTask {

    private static final Logger LOGGER = Logger.getLogger(NotifyTask.class.getName());
    private static final ClassLoader TEMPLATE_ENGINE_CL = NotifyTask.class.getClassLoader();

    public static final String NAME = "Notify";
    public static final String ACTION_SUBMIT = "submit";
    public static final String ACTION_REJECT = "reject";
    public static final String ACTION_ACCEPT = "accept";
    public static final String ACTION_REMIND = "remind";
    private Expression action;

    public NotifyTask() {
    }

    public Expression getAction() {
        return action;
    }

    public void setAction(Expression action) {
        this.action = action;
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        
//        try {
//            LOGGER.log(Level.FINE, "User Transaction Status: " + getUserTransaction().getStatus());
//            if (getUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION) {
//                LOGGER.log(Level.FINE, "START User Transaction");
//                getUserTransaction().begin();
//            }
//        } catch (Exception e) {
//            LOGGER.log(Level.SEVERE, "unable to start new user transaction", e);
//        }
        
        try {
            String initier = execution.getVariable(Process.INITIER_VAR_NAME, String.class);
            String lang = "fr";
            switch ((String) action.getValue(execution)) {
            case ACTION_SUBMIT:
                LOGGER.log(Level.FINE, "Notifying initier and moderators for new publication submission");
                try {
                    String initierEmail = getMembershipService().systemReadProfileEmail(initier);
                    if (initierEmail == null || initierEmail.length() == 0) {
                        throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to notify initier [" + initier + "], no email provided in profile"));
                    } else {
                        ProfileData language = getMembershipService().systemGetProfileInfo(initier, "language");
                        if ( language != null ) {
                            lang = language.getValue();
                        }
                        String subject = TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process(lang + "/notif.submit.subject", execution.getVariables());
                        String message = TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process(lang + "/notif.submit.body.initier.txt", execution.getVariables());
                        notify("ORTOLANG Service", "noreply@ortolang.fr", initierEmail, subject, message);
                    }
                } catch (MembershipServiceException | KeyNotFoundException | UnsupportedEncodingException | MessagingException | RuntimeEngineTaskException | TemplateEngineException e) {
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to notify initier: " + e.getMessage()));
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "error during notifying initier",  e));
                }
                try {
                    Group moderators = getMembershipService().readGroup(MembershipService.MODERATOR_GROUP_KEY);
                    for (String moderator : moderators.getMembers()) {
                        String moderatorEmail = getMembershipService().systemReadProfileEmail(moderator);
                        if (moderatorEmail == null || moderatorEmail.length() == 0) {
                            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to notify moderator [" + moderator
                                    + "], no email provided in profile"));
                        } else {
                            ProfileData language = getMembershipService().systemGetProfileInfo(moderator, "language");
                            if ( language != null ) {
                                lang = language.getValue();
                            }
                            String subject = TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process(lang + "/notif.submit.subject", execution.getVariables());
                            String message = TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process(lang + "/notif.submit.body.moderator.txt", execution.getVariables());
                            notify("ORTOLANG Service", "noreply@ortolang.fr", moderatorEmail, subject, message);
                        }
                    }
                } catch (AccessDeniedException | MembershipServiceException | KeyNotFoundException | UnsupportedEncodingException | MessagingException | RuntimeEngineTaskException | TemplateEngineException e) {
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to notify moderators: " + e.getMessage()));
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "error during notifying moderators",  e));
                }
                break;
            case ACTION_ACCEPT:
                LOGGER.log(Level.FINE, "Notifying initier publication accepted");
                try {
                    String initierEmail = getMembershipService().systemReadProfileEmail(initier);
                    if (initierEmail == null || initierEmail.length() == 0) {
                        throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to notify initier [" + initier + "], no email provided in profile"));
                    } else {
                        ProfileData language = getMembershipService().systemGetProfileInfo(initier, "language");
                        if ( language != null ) {
                            lang = language.getValue();
                        }
                        String subject = TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process(lang + "/notif.accept.subject", execution.getVariables());
                        String message = TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process(lang + "/notif.accept.body.txt", execution.getVariables());
                        notify("ORTOLANG Service", "noreply@ortolang.fr", initierEmail, subject, message);
                    }
                } catch (MembershipServiceException | KeyNotFoundException | UnsupportedEncodingException | MessagingException | RuntimeEngineTaskException | TemplateEngineException e) {
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to notify initier: " + e.getMessage()));
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "error during notifying initier",  e));
                }
                break;
            case ACTION_REJECT:
                LOGGER.log(Level.FINE, "Notifying initier publication rejected");
                try {
                    String initierEmail = getMembershipService().systemReadProfileEmail(initier);
                    if (initierEmail == null || initierEmail.length() == 0) {
                        throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to notify initier [" + initier + "], no email provided in profile"));
                    } else {
                        ProfileData language = getMembershipService().systemGetProfileInfo(initier, "language");
                        if ( language != null ) {
                            lang = language.getValue();
                        }
                        String subject = TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process(lang + "/notif.reject.subject", execution.getVariables());
                        String message = TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process(lang + "/notif.reject.body.txt", execution.getVariables());
                        notify("ORTOLANG Service", "noreply@ortolang.fr", initierEmail, subject, message);
                    }
                } catch (MembershipServiceException | KeyNotFoundException | UnsupportedEncodingException | MessagingException | RuntimeEngineTaskException | TemplateEngineException e) {
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to notify initier: " + e.getMessage()));
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "error during notifying initier",  e));
                }
                break;
            case ACTION_REMIND:
                LOGGER.log(Level.FINE, "Remind moderators for submitted publication query");
                try {
                    Group moderators = getMembershipService().readGroup(MembershipService.MODERATOR_GROUP_KEY);
                    for (String moderator : moderators.getMembers()) {
                        String moderatorEmail = getMembershipService().systemReadProfileEmail(moderator);
                        if (moderatorEmail == null || moderatorEmail.length() == 0) {
                            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to notify moderator [" + moderator
                                    + "], no email provided in profile"));
                        } else {
                            ProfileData language = getMembershipService().systemGetProfileInfo(moderator, "language");
                            if ( language != null ) {
                                lang = language.getValue();
                            }
                            String subject = TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process(lang + "/notif.remind.subject", execution.getVariables());
                            String message = TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process(lang + "/notif.remind.body.txt", execution.getVariables());
                            notify("ORTOLANG Service", "noreply@ortolang.fr", moderatorEmail, subject, message);
                        }
                    }
                } catch (AccessDeniedException | MembershipServiceException | KeyNotFoundException | UnsupportedEncodingException | MessagingException | RuntimeEngineTaskException | TemplateEngineException e) {
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to notify moderators: " + e.getMessage()));
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "error during notifying moderators",  e));
                }
                break;
            default:
                LOGGER.log(Level.FINE, "Unable to understand this action: " + (String) action.getValue(execution));
            }
        } catch (SecurityException | IllegalStateException | EJBTransactionRolledbackException e) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Unexpected error occured: " + e.getMessage()));
            throw new RuntimeEngineTaskException("unexpected error occurred", e);
        }
        
//        try {
//            LOGGER.log(Level.FINE, "COMMIT Active User Transaction.");
//            getUserTransaction().commit();
//            getUserTransaction().begin();
//        } catch (Exception e) {
//            LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
//        }
    }

    private void notify(String senderName, String senderEmail, String receiverEmail, String subject, String message) throws MessagingException, UnsupportedEncodingException,
            RuntimeEngineTaskException {
        Message msg = new MimeMessage(getMailSession());
        msg.setSubject(subject);
        msg.setRecipient(RecipientType.TO, new InternetAddress(receiverEmail));
        msg.setFrom(new InternetAddress(senderEmail, senderName));
        msg.setContent(message, "text/plain; charset=\"UTF-8\"");
        Transport.send(msg);
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

}
