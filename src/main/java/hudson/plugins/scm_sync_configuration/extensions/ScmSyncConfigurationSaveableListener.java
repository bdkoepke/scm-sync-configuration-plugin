package hudson.plugins.scm_sync_configuration.extensions;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.model.WeightedMessage;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;

@Extension
public class ScmSyncConfigurationSaveableListener extends SaveableListener {

    @Override
    public void onChange(final Saveable o, final XmlFile file) {
        super.onChange(o, file);

        final ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
        {
            final ScmSyncStrategy strategy = plugin.getStrategyForSaveable(o, file.getFile());
            final WeightedMessage message = strategy.getCommitMessageFactory().getMessageWhenSaveableUpdated(o);
            plugin.getTransaction().defineCommitMessage(message);
        }
        final String path = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(file.getFile());
        plugin.getTransaction().registerPath(path);
    }
}