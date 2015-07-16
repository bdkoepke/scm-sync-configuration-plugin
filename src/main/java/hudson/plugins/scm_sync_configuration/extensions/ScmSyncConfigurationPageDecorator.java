package hudson.plugins.scm_sync_configuration.extensions;

import hudson.Extension;
import hudson.model.PageDecorator;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import org.kohsuke.stapler.bind.JavaScriptMethod;

@SuppressWarnings("unused")
@Extension
class ScmSyncConfigurationPageDecorator extends PageDecorator {

    private ScmSyncConfigurationPlugin getScmSyncConfigPlugin() {
        return ScmSyncConfigurationPlugin.getInstance();
    }

    @JavaScriptMethod
    public void purgeScmSyncConfigLogs() {
        getScmSyncConfigPlugin().purgeFailLogs();
    }
}