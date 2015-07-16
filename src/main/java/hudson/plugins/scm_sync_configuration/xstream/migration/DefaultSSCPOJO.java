package hudson.plugins.scm_sync_configuration.xstream.migration;

import hudson.plugins.scm_sync_configuration.scms.SCM;

import java.util.List;

public class DefaultSSCPOJO implements ScmSyncConfigurationPOJO {
    private String scmRepositoryUrl;
    private SCM scm;
    private boolean noUserCommitMessage;
    private boolean displayStatus;
    private String commitMessagePattern;
    private List<String> manualSynchronizationIncludes;

    @Override
    public String getScmRepositoryUrl() {
        return scmRepositoryUrl;
    }

    @Override
    public void setScmRepositoryUrl(final String scmRepositoryUrl) {
        this.scmRepositoryUrl = scmRepositoryUrl;
    }

    @Override
    public SCM getScm() {
        return scm;
    }

    @Override
    public void setScm(SCM scm) {
        this.scm = scm;
    }

    @Override
    public boolean isNoUserCommitMessage() {
        return noUserCommitMessage;
    }

    @Override
    public void setNoUserCommitMessage(final boolean noUserCommitMessage) {
        this.noUserCommitMessage = noUserCommitMessage;
    }

    @Override
    public boolean isDisplayStatus() {
        return displayStatus;
    }

    @Override
    public void setDisplayStatus(final boolean displayStatus) {
        this.displayStatus = displayStatus;
    }

    @Override
    public String getCommitMessagePattern() {
        return commitMessagePattern;
    }

    @Override
    public void setCommitMessagePattern(final String commitMessagePattern) {
        this.commitMessagePattern = commitMessagePattern;
    }

    @Override
    public List<String> getManualSynchronizationIncludes() {
        return this.manualSynchronizationIncludes;
    }

    @Override
    public void setManualSynchronizationIncludes(final List<String> manualSynchronizationIncludes) {
        this.manualSynchronizationIncludes = manualSynchronizationIncludes;
    }
}
