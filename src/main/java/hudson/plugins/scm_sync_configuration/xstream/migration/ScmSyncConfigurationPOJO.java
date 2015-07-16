package hudson.plugins.scm_sync_configuration.xstream.migration;

import hudson.plugins.scm_sync_configuration.scms.SCM;

import java.util.List;

/**
 * Generic interface for ScmSyncConfiguration POJOs
 *
 * @author fcamblor
 */
public interface ScmSyncConfigurationPOJO {
    String getScmRepositoryUrl();

    void setScmRepositoryUrl(final String scmRepositoryUrl);

    SCM getScm();

    void setScm(final SCM scm);

    boolean isNoUserCommitMessage();

    void setNoUserCommitMessage(final boolean noUserCommitMessage);

    boolean isDisplayStatus();

    void setDisplayStatus(final boolean displayStatus);

    String getCommitMessagePattern();

    void setCommitMessagePattern(final String commitMessagePattern);

    List<String> getManualSynchronizationIncludes();

    void setManualSynchronizationIncludes(final List<String> manualSynchronizationIncludes);
}
