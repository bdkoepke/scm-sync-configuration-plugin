package hudson.plugins.scm_sync_configuration.model;

import hudson.plugins.scm_sync_configuration.scms.SCM;
import org.apache.commons.lang.builder.ToStringBuilder;

public class ScmContext {

    private final String scmRepositoryUrl;
    private final SCM scm;
    private final String commitMessagePattern;

    public ScmContext(final SCM scm, final String scmRepositoryUrl) {
        this(scm, scmRepositoryUrl, "[message]");
    }

    public ScmContext(final SCM scm, final String scmRepositoryUrl, final String commitMessagePattern) {
        this.scm = scm;
        this.scmRepositoryUrl = scmRepositoryUrl;
        this.commitMessagePattern = commitMessagePattern;
    }

    public String getScmRepositoryUrl() {
        return scmRepositoryUrl;
    }

    public SCM getScm() {
        return scm;
    }

    public String getCommitMessagePattern() {
        return commitMessagePattern;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("scm", scm)
                .append("scmRepositoryUrl", scmRepositoryUrl)
                .append("commitMessagePattern", commitMessagePattern)
                .toString();
    }
}
