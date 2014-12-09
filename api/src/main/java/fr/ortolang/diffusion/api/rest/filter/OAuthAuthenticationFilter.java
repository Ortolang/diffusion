package fr.ortolang.diffusion.api.rest.filter;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;

import org.keycloak.adapters.AuthChallenge;
import org.keycloak.adapters.AuthOutcome;
import org.keycloak.adapters.BasicAuthRequestAuthenticator;
import org.keycloak.adapters.BearerTokenRequestAuthenticator;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.jaxrs.JaxrsBearerTokenFilterImpl;
import org.keycloak.jaxrs.JaxrsHttpFacade;

@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class OAuthAuthenticationFilter extends JaxrsBearerTokenFilterImpl {

	private Logger logger = Logger.getLogger(OAuthAuthenticationFilter.class.getName());
	
	@Override
	protected void bearerAuthentication(JaxrsHttpFacade facade, ContainerRequestContext request, KeycloakDeployment resolvedDeployment) {
		
		logger.log(Level.INFO, "Entering bearer authentication filter");
        BearerTokenRequestAuthenticator authenticator = new BearerTokenRequestAuthenticator(resolvedDeployment);
        AuthOutcome outcome = authenticator.authenticate(facade);
        
        if (outcome == AuthOutcome.NOT_ATTEMPTED && resolvedDeployment.isEnableBasicAuth()) {
        	logger.log(Level.INFO, "AuthOutcome NOT_ATTEMPED && BasicAuth enable");
            authenticator = new BasicAuthRequestAuthenticator(resolvedDeployment);
            outcome = authenticator.authenticate(facade);
        }
        
        if (outcome == AuthOutcome.FAILED || outcome == AuthOutcome.NOT_ATTEMPTED) {
        	logger.log(Level.INFO, "AuthOutcome FAILED || AuthOutcome NOT ATTEMPTED");
            AuthChallenge challenge = authenticator.getChallenge();
            boolean challengeSent = challenge.challenge(facade);
            if (!challengeSent) {
            	logger.log(Level.INFO, "Challenge Not Set, don't know what it means");
            	//TODO in this case I don't know what to do ...
            }

            return;
        } else {
            if (verifySslFailed(facade, resolvedDeployment)) {
                return;
            }
        }

        propagateSecurityContext(facade, request, resolvedDeployment, authenticator);
        handleAuthActions(facade, resolvedDeployment);
    }

}
