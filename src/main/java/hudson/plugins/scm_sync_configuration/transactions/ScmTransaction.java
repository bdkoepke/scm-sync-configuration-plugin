package hudson.plugins.scm_sync_configuration.transactions;

import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.model.ChangeSet;
import hudson.plugins.scm_sync_configuration.model.WeightedMessage;

/**
 * @author fcamblor
 */
public abstract class ScmTransaction {
    private final ChangeSet changeset;
    // Flag allowing to say if transaction will be asynchronous (default) or synchronous
    // Synchronous commit are useful during tests execution
    private final boolean synchronousCommit;

    protected ScmTransaction(final boolean synchronousCommit) {
        this.changeset = new ChangeSet();
        this.synchronousCommit = synchronousCommit;
    }

    public void defineCommitMessage(final WeightedMessage weightedMessage) {
        this.changeset.defineMessage(weightedMessage);
    }

    public void commit() {
        ScmSyncConfigurationPlugin.getInstance().commitChangeset(changeset);
        if (!synchronousCommit)
            return;
        // Synchronous transactions should wait for latest commit future to be fully processed
        // before going further
        try {
            ScmSyncConfigurationPlugin.getInstance().getLatestCommitFuture().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void registerPath(final String path) {
        this.changeset.registerPath(path);
    }

    public void registerPathForDeletion(final String path) {
        this.changeset.registerPathForDeletion(path);
    }

    public void registerRenamedPath(final String oldPath, final String newPath) {
        this.changeset.registerRenamedPath(oldPath, newPath);
    }
}
