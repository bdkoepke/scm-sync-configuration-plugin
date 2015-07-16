package hudson.plugins.scm_sync_configuration.transactions;

/**
 * This ScmTransaction implementation should be aimed at commiting changes immediately
 *
 * @author fcamblor
 */
public class AtomicTransaction extends ScmTransaction {

    public AtomicTransaction(final boolean synchronousCommit) {
        super(synchronousCommit);
    }

    @Override
    public void registerPath(final String path) {
        super.registerPath(path);
        // We should commit transaction after every change
        commit();
    }

    @Override
    public void registerPathForDeletion(final String path) {
        super.registerPathForDeletion(path);
        // We should commit transaction after every change
        commit();
    }

    @Override
    public void registerRenamedPath(final String oldPath, final String newPath) {
        super.registerRenamedPath(oldPath, newPath);
        // We should commit transaction after every change
        commit();
    }

}
