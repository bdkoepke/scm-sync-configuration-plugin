package hudson.plugins.scm_sync_configuration.strategies.impl;

import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.strategies.AbstractScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.model.ConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PatternsEntityMatcher;

import java.util.ArrayList;
import java.util.List;

public class ManualIncludesScmSyncStrategy extends AbstractScmSyncStrategy {
    private static final List<PageMatcher> PAGE_MATCHERS = new ArrayList<>();

    public ManualIncludesScmSyncStrategy() {
        super(null, PAGE_MATCHERS);
    }

    @Override
    protected ConfigurationEntityMatcher createConfigEntityMatcher() {
        final List<String> manualSynchronizationIncludes = ScmSyncConfigurationPlugin.getInstance().getManualSynchronizationIncludes();
        return new PatternsEntityMatcher(manualSynchronizationIncludes == null ?
                new String[0] :
                manualSynchronizationIncludes.toArray(new String[manualSynchronizationIncludes.size()]));
    }
}
