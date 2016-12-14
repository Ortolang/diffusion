package fr.ortolang.diffusion.notification;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RunAs;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import fr.ortolang.diffusion.membership.entity.Profile;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.event.entity.Event;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.entity.Group;
import fr.ortolang.diffusion.membership.entity.ProfileData;
import fr.ortolang.diffusion.message.MessageService;
import fr.ortolang.diffusion.message.MessageServiceException;
import fr.ortolang.diffusion.message.entity.Thread;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.template.MessageResolverMethod;
import fr.ortolang.diffusion.template.TemplateEngine;
import fr.ortolang.diffusion.template.TemplateEngineException;

@MessageDriven(name = "NotificationListener", activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/notification"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
@SecurityDomain("ortolang")
@RunAs("system")
public class SMTPNotificationListenerBean implements MessageListener {

    private static final Locale DEFAULT_LOCALE = Locale.FRANCE;
    private static final Logger LOGGER = Logger.getLogger(SMTPNotificationListenerBean.class.getName());
    private static final ClassLoader TEMPLATE_ENGINE_CL = SMTPNotificationListenerBean.class.getClassLoader();

    private static final String EVENT_CREATE_THREAD = Event.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "create");
    private static final String EVENT_UPDATE_THREAD = Event.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "update");
    private static final String EVENT_DELETE_THREAD = Event.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "delete");
    private static final String EVENT_ANSWERED_THREAD = Event.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "answered");
    private static final String EVENT_POST_THREAD = Event.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "post");
    
    private static final String NOTIFY_WORKSPACE_OWNER = Event.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "notify-owner");
    private static final String NOTIFY_WORKSPACE_MEMBERS = Event.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "notify-members");
    private static final String NOTIFY_WORKSPACE_ADDED_MEMBER = Event.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "notify-added-member");

    @EJB
    private MessageService service;
    @EJB
    private MembershipService membership;
    @EJB
    private CoreService core;
    @EJB
    private AuthorisationService authorisation;
    @Resource(name = "java:jboss/mail/Default")
    private Session session;

    @Override
    @PermitAll
    public void onMessage(Message message) {
        try {
            OrtolangEvent event = new Event();
            event.fromJMSMessage(message);
            if (event.getType().equals(EVENT_CREATE_THREAD)) {
                LOGGER.log(Level.FINE, "received create thread event, starting notification");
                String wskey = event.getArguments().get("wskey");
                String title = event.getArguments().get("title");

                Workspace ws = core.systemReadWorkspace(wskey);
                Set<String> receivers = new HashSet<String>();
                receivers.addAll(getGroupMembers(ws.getMembers()));
                receivers.addAll(getGroupMembers(MembershipService.MODERATORS_GROUP_KEY));
                receivers.addAll(getGroupMembers(MembershipService.PUBLISHERS_GROUP_KEY));
                receivers.addAll(getGroupMembers(MembershipService.REVIEWERS_GROUP_KEY));
                receivers.remove(event.getThrowedBy());

                notify(ws.getAlias(), "thread.create", event.getFromObject(), title, receivers);
            } else if (event.getType().equals(EVENT_UPDATE_THREAD)) {
                LOGGER.log(Level.FINE, "received update thread event, starting notification");
                String wskey = event.getArguments().get("wskey");
                String title = event.getArguments().get("title");

                Set<String> receivers = new HashSet<String>();
                String[] observers = service.systemReadThread(event.getFromObject()).getObservers();
                String wsalias = core.systemReadWorkspace(wskey).getAlias();
                receivers.addAll(Arrays.asList(observers));
                receivers.remove(event.getThrowedBy());

                notify(wsalias, "thread.update", event.getFromObject(), title, receivers);
            } else if (event.getType().equals(EVENT_DELETE_THREAD)) {
                LOGGER.log(Level.FINE, "received delete thread event, starting notification");
                String wskey = event.getArguments().get("wskey");
                String title = event.getArguments().get("title");

                Set<String> receivers = new HashSet<String>();
                String[] observers = service.systemReadThread(event.getFromObject()).getObservers();
                String wsalias = core.systemReadWorkspace(wskey).getAlias();
                receivers.addAll(Arrays.asList(observers));
                receivers.remove(event.getThrowedBy());

                notify(wsalias, "thread.delete", event.getFromObject(), title, receivers);
            } else if (event.getType().equals(EVENT_ANSWERED_THREAD)) {
                LOGGER.log(Level.FINE, "received answered thread event, starting notification");
                String wskey = event.getArguments().get("wskey");
                String body = event.getArguments().get("body");

                Set<String> receivers = new HashSet<String>();
                String[] observers = service.systemReadThread(event.getFromObject()).getObservers();
                String wsalias = core.systemReadWorkspace(wskey).getAlias();
                receivers.addAll(Arrays.asList(observers));
                receivers.remove(event.getThrowedBy());

                notify(wsalias, "thread.answered", event.getFromObject(), body, receivers);
            } else if (event.getType().equals(EVENT_POST_THREAD)) {
                LOGGER.log(Level.FINE, "received post message in thread event, starting notification");
                String wskey = event.getArguments().get("wskey");
                String body = event.getArguments().get("body");
                
                Set<String> receivers = new HashSet<String>();
                String[] observers = service.systemReadThread(event.getFromObject()).getObservers();
                String wsalias = core.systemReadWorkspace(wskey).getAlias();
                receivers.addAll(Arrays.asList(observers));
                receivers.remove(event.getThrowedBy());

                notify(wsalias, "thread.post", event.getFromObject(), body, receivers);
            } else if (event.getType().equals(NOTIFY_WORKSPACE_OWNER)) {
                LOGGER.log(Level.FINE, "received notify workspace owner, starting notification");
                String wskey = event.getFromObject();
                String email = event.getArguments().get("email");
                String body = event.getArguments().get("message");
                
                String wsalias = core.systemReadWorkspace(wskey).getAlias();
                String owner = authorisation.getPolicyOwner(wskey);
                
                Set<String> receivers = new HashSet<String>();
                receivers.add(owner);
                
                String senderName = "";
                if ( !event.getThrowedBy().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER)) {
                    senderName = membership.systemReadProfile(event.getThrowedBy()).getFullName();
                }
                
                notify(senderName, email, wsalias, "workspace.notify", event.getFromObject(), body, receivers);
            } else if (event.getType().equals(NOTIFY_WORKSPACE_MEMBERS)) {
                LOGGER.log(Level.FINE, "received notify workspace members, starting notification");
                String wskey = event.getFromObject();
                String email = event.getArguments().get("email");
                String body = event.getArguments().get("message");
                
                Workspace ws = core.systemReadWorkspace(wskey);
                Set<String> receivers = new HashSet<String>();
                receivers.addAll(getGroupMembers(ws.getMembers()));
                
                String senderName = "";
                if ( !event.getThrowedBy().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER)) {
                    senderName = membership.systemReadProfile(event.getThrowedBy()).getFullName();
                }
                
                notify(senderName, email, ws.getAlias(), "workspace.notify", event.getFromObject(), body, receivers);
            } else if (event.getType().equals(NOTIFY_WORKSPACE_ADDED_MEMBER)) {
                LOGGER.log(Level.FINE, "received notify workspace added member, starting notification");

                String wskey = event.getFromObject();
                String body = event.getArguments().get("message");
                String member = event.getArguments().get("member");

                Workspace ws = core.systemReadWorkspace(wskey);
                Set<String> receivers = new HashSet<String>();
                receivers.add(member);

                String senderName = "";
                String email = "";
                if ( !event.getThrowedBy().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER)) {
                    Profile sender = membership.systemReadProfile(event.getThrowedBy());
                    senderName = sender.getFullName();
                    email = sender.getEmail();
                }

                notify(senderName, email, ws.getAlias(), "workspace.add-member", event.getFromObject(), body, receivers);
            }

        } catch (OrtolangException | MembershipServiceException | KeyNotFoundException | CoreServiceException | MessageServiceException | AuthorisationServiceException e) {
            LOGGER.log(Level.WARNING, "unable to handle notification message", e);
        } 
    }

    private List<String> getGroupMembers(String gkey) throws MembershipServiceException, KeyNotFoundException {
        Group group = membership.systemReadGroup(gkey);
        return Arrays.asList(group.getMembers());
    }
    
    
    private void notify(String wsalias, String type, String key, String title, Set<String> profiles) {
        String senderName = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.SMTP_SENDER_NAME);
        String senderEmail = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.SMTP_SENDER_EMAIL);
        this.notify(senderName, senderEmail, wsalias, type, key, title, profiles);
    }

    private void notify(String senderName, String senderEmail, String wsalias, String type, String key, String title, Set<String> profiles) {
        String marketUrl = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.MARKET_SERVER_URL);
        
        for (String profile : profiles) {
            try {
                Profile recipient = membership.systemReadProfile(profile);
                String recipientEmail = recipient.getEmail();
                if (recipientEmail == null || recipientEmail.length() == 0) {
                    LOGGER.log(Level.WARNING, "No email found for profile: " + profile + ", unable to notify");
                    continue;
                }
                Locale locale = DEFAULT_LOCALE;
                ProfileData recipientLanguageData = recipient.getInfo("language");
                if (recipientLanguageData != null) {
                    locale = new Locale(recipientLanguageData.getValue());
                }
                Map<String, Object> model = new HashMap<>();
                Object[] args = new Object[] { marketUrl, wsalias, senderName, key, title};
                model.put("userType", "user");
                model.put("title", type + ".title");
                model.put("body", type + ".body");
                model.put("msg", new MessageResolverMethod(locale));
                model.put("marketUrl", marketUrl);
                model.put("args", args);
                String subject = MessageFormat.format(ResourceBundle.getBundle("notification", locale).getString(type + ".subject"), args);
                String message = TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process("notification", model);

                MimeMessage msg = new MimeMessage(session);
                msg.setSubject(subject);
                msg.setRecipient(RecipientType.TO, new InternetAddress(recipientEmail));
                if ( senderName.length() > 0 ) {
                    msg.setFrom(new InternetAddress(senderEmail, senderName));
                } else {
                    msg.setFrom(new InternetAddress(senderEmail));
                }
                msg.setContent(message, "text/html; charset=\"UTF-8\"");
                Transport.send(msg);
            } catch (UnsupportedEncodingException | MessagingException | TemplateEngineException | MembershipServiceException | KeyNotFoundException e) {
                LOGGER.log(Level.WARNING, "Error while sending mail to profile: " + profile + " for thread activity notification", e);
            }
        }
    }

}
