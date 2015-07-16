package hudson.plugins.scm_sync_configuration;

import org.kohsuke.stapler.Stapler;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.Callable;

public class ScmSyncConfigurationDataProvider {
    private static final ThreadLocal<HttpServletRequest> CURRENT_REQUEST = new ThreadLocal<>();
    private static final String COMMENT_SESSION_KEY = "__commitMessage";

    static String retrieveComment() {
        return retrieveObject();
    }

    @SuppressWarnings("unchecked")
    private static <T> T retrieveObject() {
        final HttpServletRequest request = currentRequest();
        // Sometimes, request can be null : when hudson starts for instance !
        return request == null ?
                null :
                (T) request.getSession().getAttribute(ScmSyncConfigurationDataProvider.COMMENT_SESSION_KEY);
    }

    public static void provideRequestDuring(HttpServletRequest request, Callable<Void> callable) throws Exception {
        try {
            CURRENT_REQUEST.set(request);
            callable.call();
        } finally {
            CURRENT_REQUEST.set(null);
        }
    }

    private static HttpServletRequest currentRequest() {
        return Stapler.getCurrentRequest() == null ? CURRENT_REQUEST.get() : Stapler.getCurrentRequest();
    }
}
