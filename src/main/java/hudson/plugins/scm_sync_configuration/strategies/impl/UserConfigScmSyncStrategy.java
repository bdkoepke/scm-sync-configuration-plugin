package hudson.plugins.scm_sync_configuration.strategies.impl;

import hudson.model.Item;
import hudson.model.Saveable;
import hudson.model.User;
import hudson.plugins.scm_sync_configuration.model.MessageWeight;
import hudson.plugins.scm_sync_configuration.model.WeightedMessage;
import hudson.plugins.scm_sync_configuration.strategies.AbstractScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.model.ClassAndFileConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.ConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;

import java.util.ArrayList;
import java.util.List;

public class UserConfigScmSyncStrategy extends AbstractScmSyncStrategy {
    // Don't miss to take into account view urls since we can configure a job through a view !
    private static final List<PageMatcher> PAGE_MATCHERS = new ArrayList<>();
    // Only saving config.xml file located in user directory
    private static final String[] PATTERNS = new String[]{
            "users/*/config.xml"
    };
    private static final ConfigurationEntityMatcher CONFIG_ENTITY_MANAGER = new ClassAndFileConfigurationEntityMatcher(User.class, PATTERNS);

    static {
        PAGE_MATCHERS.add(new PageMatcher("^securityRealm/addUser$"));
        PAGE_MATCHERS.add(new PageMatcher("^securityRealm/user/[^/]+/configure$"));
    }

    public UserConfigScmSyncStrategy() {
        super(CONFIG_ENTITY_MANAGER, PAGE_MATCHERS);
    }

    public CommitMessageFactory getCommitMessageFactory() {
        return new CommitMessageFactory() {
            @Override
            public WeightedMessage getMessageWhenSaveableUpdated(final Saveable s) {
                return new WeightedMessage(
                        String.format("User [%s] configuration updated", ((User) s).getDisplayName()),
                        // Job config update message should be considered as "important", especially
                        // more important than the plugin descriptors Saveable updates
                        MessageWeight.IMPORTANT);
            }

            @Override
            public WeightedMessage getMessageWhenItemRenamed(final Item item, final String oldPath, final String newPath) {
                return new WeightedMessage(
                        String.format("User [%s] configuration renamed from [%s] to [%s]", item.getName(), oldPath, newPath),
                        // Job config rename message should be considered as "important", especially
                        // more important than the plugin descriptors Saveable renames
                        MessageWeight.MORE_IMPORTANT);
            }

            @Override
            public WeightedMessage getMessageWhenItemDeleted(Item item) {
                return new WeightedMessage(
                        String.format("User [%s] hierarchy deleted", item.getName()),
                        // Job config deletion message should be considered as "important", especially
                        // more important than the plugin descriptors Saveable deletions
                        MessageWeight.MORE_IMPORTANT);
            }
        };
    }
}
