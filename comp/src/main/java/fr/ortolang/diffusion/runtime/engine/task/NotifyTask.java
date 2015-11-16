package fr.ortolang.diffusion.runtime.engine.task;

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
                        //TODO maybe notify user in its profile feed
                    } else {
                        ProfileData language = getMembershipService().systemGetProfileInfo(initier, "language");
                        if ( language != null ) {
                            lang = language.getValue();
                        }
                        String subject = TemplateEngine.getInstance().process(lang + "/notif.submit.subject", execution.getVariables());
                        String message = TemplateEngine.getInstance().process(lang + "/notif.submit.body.initier.txt", execution.getVariables());
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
                          //TODO maybe notify user in its profile feed
                        } else {
                            ProfileData language = getMembershipService().systemGetProfileInfo(moderator, "language");
                            if ( language != null ) {
                                lang = language.getValue();
                            }
                            String subject = TemplateEngine.getInstance().process(lang + "/notif.submit.subject", execution.getVariables());
                            String message = TemplateEngine.getInstance().process(lang + "/notif.submit.body.moderator.txt", execution.getVariables());
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
                        String subject = TemplateEngine.getInstance().process(lang + "/notif.accept.subject", execution.getVariables());
                        String message = TemplateEngine.getInstance().process(lang + "/notif.accept.body.txt", execution.getVariables());
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
                        String subject = TemplateEngine.getInstance().process(lang + "/notif.reject.subject", execution.getVariables());
                        String message = TemplateEngine.getInstance().process(lang + "/notif.reject.body.txt", execution.getVariables());
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
                            String subject = TemplateEngine.getInstance().process(lang + "/notif.remind.subject", execution.getVariables());
                            String message = TemplateEngine.getInstance().process(lang + "/notif.remind.body.txt", execution.getVariables());
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
