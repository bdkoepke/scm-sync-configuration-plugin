package hudson.plugins.scm_sync_configuration.strategies.impl;

import hudson.XmlFile;
import hudson.model.Item;
import hudson.model.Saveable;
import hudson.plugins.scm_sync_configuration.model.MessageWeight;
import hudson.plugins.scm_sync_configuration.model.WeightedMessage;
import hudson.plugins.scm_sync_configuration.strategies.AbstractScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.model.ConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PatternsEntityMatcher;

import java.util.ArrayList;
import java.util.List;

public class BasicPluginsConfigScmSyncStrategy extends AbstractScmSyncStrategy {

    private static final List<PageMatcher> PAGE_MATCHERS = new ArrayList<>();

    private static final String[] PATTERNS = new String[]{
            "hudson*.xml",
            "scm-sync-configuration.xml"
    };

    private static final ConfigurationEntityMatcher CONFIG_ENTITY_MATCHER = new PatternsEntityMatcher(PATTERNS);

    public BasicPluginsConfigScmSyncStrategy() {
        super(CONFIG_ENTITY_MATCHER, PAGE_MATCHERS);
    }

    public CommitMessageFactory getCommitMessageFactory() {
        return new CommitMessageFactory() {
            @Override
            public WeightedMessage getMessageWhenSaveableUpdated(final Saveable s, final XmlFile file) {
                return new WeightedMessage("Plugin configuration files updated", MessageWeight.MINIMAL);
            }

            @Override
            public WeightedMessage getMessageWhenItemRenamed(final Item item, final String oldPath, final String newPath) {
                return new WeightedMessage("Plugin configuration files renamed", MessageWeight.MINIMAL);
            }

            @Override
            public WeightedMessage getMessageWhenItemDeleted(final Item item) {
                return new WeightedMessage("Plugin configuration files deleted", MessageWeight.MINIMAL);
            }
        };
    }
}
