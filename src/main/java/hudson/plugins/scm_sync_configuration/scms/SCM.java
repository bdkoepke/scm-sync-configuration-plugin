package hudson.plugins.scm_sync_configuration.scms;

import hudson.model.Descriptor;
import hudson.model.Hudson;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.ScmProviderRepositoryWithHost;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.util.StringUtils;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class SCM {
    static final Logger LOGGER = Logger.getLogger(SCM.class.getName());
    private static final List<SCM> SCM_IMPLEMENTATIONS = new ArrayList<>();

    static {
        SCM_IMPLEMENTATIONS.add(new ScmSyncNoSCM());
        SCM_IMPLEMENTATIONS.add(new ScmSyncSubversionSCM());
        SCM_IMPLEMENTATIONS.add(new ScmSyncGitSCM());
    }

    transient final private String title;
    transient final private String scmClassName;
    transient final private String configPage;
    transient final private String repositoryUrlHelpPath;

    SCM(final String title, final String configPage, final String scmClassName, final String repositoryUrlHelpPath) {
        this.title = title;
        this.configPage = configPage;
        this.scmClassName = scmClassName;
        this.repositoryUrlHelpPath = repositoryUrlHelpPath;
    }

    public static SCM valueOf(final Class<? extends SCM> clazz) {
        return valueOf(getId(clazz));
    }

    public static SCM valueOf(final String scmId) {
        for (SCM scm : SCM_IMPLEMENTATIONS)
            if (scmId.equals(scm.getId()))
                return scm;
        return null;
    }

    public static SCM[] values() {
        return SCM_IMPLEMENTATIONS.toArray(new SCM[SCM_IMPLEMENTATIONS.size()]);
    }

    private static String getId(final Class<? extends SCM> clazz) {
        return clazz.getName();
    }

    public String getTitle() {
        return this.title;
    }

    private String getSCMClassName() {
        return this.scmClassName;
    }

    @SuppressWarnings("deprecation")
    Descriptor getSCMDescriptor() {
        return Hudson.getInstance().getDescriptorByName(getSCMClassName());
    }

    public String getRepositoryUrlHelpPath() {
        return this.repositoryUrlHelpPath;
    }

    public ScmRepository getConfiguredRepository(final ScmManager scmManager, final String scmRepositoryURL) {
        LOGGER.info("Creating SCM repository object for url : " + scmRepositoryURL);
        final ScmRepository repository;
        try {
            repository = scmManager.makeScmRepository(scmRepositoryURL);
        } catch (ScmRepositoryException | NoSuchScmProviderException e) {
            LOGGER.throwing(ScmManager.class.getName(), "makeScmRepository", e);
            LOGGER.severe("Error creating ScmRepository : " + e.getMessage());
            return null;
        }

        final SCMCredentialConfiguration credentials = extractScmCredentials(extractScmUrlFrom(scmRepositoryURL));
        if (credentials == null)
            return repository;

        final ScmProviderRepository scmRepo = repository.getProviderRepository();
        LOGGER.info("Populating credentials data into SCM repository object ...");
        if (!StringUtils.isEmpty(credentials.getUsername()))
            scmRepo.setUser(credentials.getUsername());
        if (!StringUtils.isEmpty(credentials.getPassword()))
            scmRepo.setPassword(credentials.getPassword());

        if (!(scmRepo instanceof ScmProviderRepositoryWithHost))
            return repository;

        final ScmProviderRepositoryWithHost repositoryWithHost = (ScmProviderRepositoryWithHost) scmRepo;
        if (!StringUtils.isEmpty(credentials.getPrivateKey()))
            repositoryWithHost.setPrivateKey(credentials.getPrivateKey());
        if (!StringUtils.isEmpty(credentials.getPassphrase()))
            repositoryWithHost.setPassphrase(credentials.getPassphrase());
        return repository;
    }

    public abstract String createScmUrlFromRequest(final StaplerRequest req);

    protected abstract String extractScmUrlFrom(final String scmUrl);

    public abstract SCMCredentialConfiguration extractScmCredentials(final String scmRepositoryURL);

    public String toString() {
        return new ToStringBuilder(this)
                .append("class", getClass().getName())
                .append("title", title)
                .append("scmClassName", scmClassName)
                .append("configPage", configPage)
                .append("repositoryUrlHelpPath", repositoryUrlHelpPath)
                .toString();
    }

    public String getId() {
        return getId(getClass());
    }
}
