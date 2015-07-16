package hudson.plugins.scm_sync_configuration.scms;

import org.kohsuke.stapler.StaplerRequest;

public class ScmSyncGitSCM extends SCM {
    private static final String SCM_URL_PREFIX = "scm:git:";

    ScmSyncGitSCM() {
        super("Git", "git/config.jelly", "hudson.plugins.git.GitSCM", "/hudson/plugins/scm_sync_configuration/ScmSyncConfigurationPlugin/scms/git/url-help.jelly");
    }

    public String createScmUrlFromRequest(final StaplerRequest req) {
        final String repoURL = req.getParameter("gitRepositoryUrl");
        return repoURL == null ? null : SCM_URL_PREFIX + repoURL;
    }

    public String extractScmUrlFrom(final String scmUrl) {
        return scmUrl.substring(SCM_URL_PREFIX.length());
    }

    public SCMCredentialConfiguration extractScmCredentials(final String scmUrl) {
        return null;
    }
}
