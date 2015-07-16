package hudson.plugins.scm_sync_configuration.extensions;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.model.WeightedMessage;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;

import java.io.File;

@Extension
public class ScmSyncConfigurationItemListener extends ItemListener {

    @Override
    public void onLoaded() {
        super.onLoaded();
        // After every plugin is loaded, let's init ScmSyncConfigurationPlugin
        // Init is needed after plugin loads since it relies on scm implementations plugins loaded
        ScmSyncConfigurationPlugin.getInstance().init();
    }

    @Override
    public void onDeleted(final Item item) {
        super.onDeleted(item);

        final ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
        {
            final ScmSyncStrategy strategy = plugin.getStrategyForSaveable(item, null);
            final WeightedMessage message = strategy.getCommitMessageFactory().getMessageWhenItemDeleted(item);
            plugin.getTransaction().defineCommitMessage(message);
        }
        final String path = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(item.getRootDir());
        plugin.getTransaction().registerPathForDeletion(path);
    }

    @Override
    public void onRenamed(final Item item, final String oldName, final String newName) {
        super.onRenamed(item, oldName, newName);
        final File oldDir;
        final File newDir;
        {
            final File parentDir = item.getRootDir().getParentFile();
            oldDir = new File(parentDir.getAbsolutePath() + File.separator + oldName);
            newDir = new File(parentDir.getAbsolutePath() + File.separator + newName);
        }
        final ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
        final String oldPath = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(oldDir);
        final String newPath = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(newDir);
        {
            final ScmSyncStrategy strategy = plugin.getStrategyForSaveable(item, null);
            final WeightedMessage message = strategy.getCommitMessageFactory().getMessageWhenItemRenamed(item, oldPath, newPath);
            plugin.getTransaction().defineCommitMessage(message);
        }
        plugin.getTransaction().registerRenamedPath(oldPath, newPath);
    }
}
