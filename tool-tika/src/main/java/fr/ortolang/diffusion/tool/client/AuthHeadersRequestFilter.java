package fr.ortolang.diffusion.tool.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import org.apache.commons.codec.binary.Base64;

public class AuthHeadersRequestFilter implements ClientRequestFilter {

	private final String username;
    private final String password;

    public AuthHeadersRequestFilter(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        String token = username + ":" + password;
        String base64Token = new String(Base64.encodeBase64(token.getBytes(StandardCharsets.UTF_8)));
        requestContext.getHeaders().add("Authorization", "Basic " + base64Token);
    }
}
