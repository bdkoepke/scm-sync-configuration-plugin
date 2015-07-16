package hudson.plugins.scm_sync_configuration;

import org.apache.maven.scm.manager.ScmManager;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

public class SCMManagerFactory {
    private static final SCMManagerFactory INSTANCE = new SCMManagerFactory();
    private PlexusContainer plexus = null;

    private SCMManagerFactory() {
    }

    public static SCMManagerFactory getInstance() {
        return INSTANCE;
    }

    public void start() {
        if (plexus == null)
            this.plexus = new DefaultPlexusContainer();
    }

    public ScmManager createScmManager() throws ComponentLookupException {
        return (ScmManager) this.plexus.lookup(ScmManager.ROLE);
    }

    public void stop() {
        this.plexus.dispose();
        this.plexus = null;
    }
}
