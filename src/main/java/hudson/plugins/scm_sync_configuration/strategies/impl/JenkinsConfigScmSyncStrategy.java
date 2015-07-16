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

public class JenkinsConfigScmSyncStrategy extends AbstractScmSyncStrategy {
    private static final List<PageMatcher> PAGE_MATCHERS = new ArrayList<>();
    private static final String[] PATTERNS = new String[]{"config.xml"};
    private static final ConfigurationEntityMatcher CONFIG_ENTITY_MATCHER = new PatternsEntityMatcher(PATTERNS);

    static {
        PAGE_MATCHERS.add(new PageMatcher("^configure$"));
        PAGE_MATCHERS.add(new PageMatcher("^(.+/)?view/[^/]+/configure$"));
        PAGE_MATCHERS.add(new PageMatcher("^newView$"));
    }

    public JenkinsConfigScmSyncStrategy() {
        super(CONFIG_ENTITY_MATCHER, PAGE_MATCHERS);
    }

    public CommitMessageFactory getCommitMessageFactory() {
        return new CommitMessageFactory() {
            @Override
            public WeightedMessage getMessageWhenSaveableUpdated(final Saveable s, final XmlFile file) {
                return new WeightedMessage(
                        "Jenkins configuration files updated",
                        MessageWeight.NORMAL);
            }

            @Override
            public WeightedMessage getMessageWhenItemRenamed(final Item item, final String oldPath, final String newPath) {
                throw new IllegalStateException("Jenkins configuration files should never be renamed !");
            }

            @Override
            public WeightedMessage getMessageWhenItemDeleted(final Item item) {
                throw new IllegalStateException("Jenkins configuration files should never be deleted !");
            }
        };
    }
}
