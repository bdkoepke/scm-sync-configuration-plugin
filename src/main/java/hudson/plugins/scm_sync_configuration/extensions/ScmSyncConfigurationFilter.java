package hudson.plugins.scm_sync_configuration.extensions;

import hudson.Extension;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationDataProvider;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * @author fcamblor
 *         Very important class in the plugin : it is the entry point allowing to decide what files should be
 *         synchronized or not during current thread execution
 */
@Extension
public class ScmSyncConfigurationFilter implements Filter {

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        try {
            // In the beginning of every http request, we should create a new threaded transaction
            final ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
            plugin.startThreadedTransaction();
            // Providing current ServletRequest in ScmSyncConfigurationDataProvider's thread local
            // in order to be able to access it from everywhere inside this call
            ScmSyncConfigurationDataProvider.provideRequestDuring((HttpServletRequest) request, new Callable<Void>() {
                public Void call() throws Exception {
                    try {
                        // Handling "normally" http request
                        chain.doFilter(request, response);
                    } finally {
                        // In the end of http request, we should commit current transaction
                        plugin.getTransaction().commit();
                    }
                    return null;
                }
            });
        } catch (ServletException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void destroy() {
    }
}
