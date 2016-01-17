package io.mangoo.admin;

import java.util.Objects;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Base64;

import com.google.common.base.Charsets;
import com.google.inject.Inject;

import io.mangoo.configuration.Config;
import io.mangoo.enums.AdminRoute;
import io.mangoo.enums.Default;
import io.mangoo.enums.Template;
import io.mangoo.interfaces.MangooFilter;
import io.mangoo.routing.Response;
import io.mangoo.routing.bindings.Request;
import io.undertow.util.Headers;

/**
 * Authentication filter for administrative URLs
 *
 * @author svenkubiak
 *
 */
public class AdminFilter implements MangooFilter {
    private final Config config;

    @Inject
    public AdminFilter(Config config) {
        this.config = Objects.requireNonNull(config,  "config can not be null");
    }

    @Override
    public Response execute(Request request, Response response) {
        final String url = Optional.ofNullable(request.getURI()).orElse("").replace("/", "");
        if (isURLEnabled(url)) {
            if (config.isAdminAuthenticationEnabled() && !isAuthenticated(request)) {
                  return Response.withUnauthorized()
                          .andHeader(Headers.WWW_AUTHENTICATE, "Basic realm=Administration authentication")
                          .andEmptyBody()
                          .end();
            }

            return response;
        }

        return Response.withNotFound()
                .andBody(Template.DEFAULT.notFound())
                .end();
    }

    /**
     * Checks via a basic HTTP authentication if user is authenticated
     *
     * @param request The current HTTP request
     * @return True if credentials are valid, false otherwise
     */
    private boolean isAuthenticated(Request request) {
        String username = null;
        String password = null;
        String authInfo = request.getHeader(Headers.AUTHORIZATION);
        if (StringUtils.isNotBlank(authInfo)) {
            authInfo = authInfo.replace("Basic", "");
            authInfo = authInfo.trim();
            authInfo = new String(Base64.decode(authInfo), Charsets.UTF_8);

            final String [] credentials = authInfo.split(":");
            if (credentials != null && credentials.length == Default.BASICAUTH_CREDENTIALS_LENGTH.toInt()) {
                username = credentials[0];
                password = credentials[1];
            }
        }

        return StringUtils.isNotBlank(username) &&
               StringUtils.isNotBlank(password) &&
               config.getAdminAuthenticationUser().equals(username) &&
               config.getAdminAuthenticationPassword().equals(DigestUtils.sha512Hex(password));
    }

    /**
     * Checks if an administrative URL is enabled
     *
     * @param url The URL to check
     * @return True when enabled via application.yaml, false otherwise
     */
    private boolean isURLEnabled(String url) {
        Objects.requireNonNull(url, "url can not be null");
        
        boolean enabled;
        if ('/' != url.charAt(0)) {
            url = '/' + url;
        }
        
        switch (AdminRoute.fromString(url)) {
        case ROUTES:
            enabled = config.isAdminRoutesEnabled();
            break;
        case CONFIG:
            enabled = config.isAdminConfigEnabled();
            break;
        case HEALTH:
            enabled = config.isAdminHealthEnabled();
            break;
        case CACHE:
            enabled = config.isAdminCacheEnabled();
            break;
        case METRICS:
            enabled = config.isAdminMetricsEnabled();
            break;
        case SCHEDULER:
            enabled = config.isAdminSchedulerEnabled();
            break;
        case SYSTEM:
            enabled = config.isAdminSystemEnabled();
            break;    
        case MEMORY:
            enabled = config.isAdminMemoryEnabled();
            break;             
        default:
            enabled = false;
        }

        return enabled;
    }
}