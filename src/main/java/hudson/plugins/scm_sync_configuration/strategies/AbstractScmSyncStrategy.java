package hudson.plugins.scm_sync_configuration.strategies;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Saveable;
import hudson.plugins.scm_sync_configuration.model.MessageWeight;
import hudson.plugins.scm_sync_configuration.model.WeightedMessage;
import hudson.plugins.scm_sync_configuration.strategies.model.ConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractScmSyncStrategy implements ScmSyncStrategy {

    private static final Function<String, File> PATH_TO_FILE_IN_HUDSON = new Function<String, File>() {
        @SuppressWarnings("deprecation")
        public File apply(@Nullable String path) {
            return new File(Hudson.getInstance().getRootDir() + File.separator + path);
        }
    };
    private final ConfigurationEntityMatcher configEntityMatcher;
    private final List<PageMatcher> pageMatchers;

    protected AbstractScmSyncStrategy(final ConfigurationEntityMatcher configEntityMatcher, final List<PageMatcher> pageMatchers) {
        this.configEntityMatcher = configEntityMatcher;
        this.pageMatchers = pageMatchers;
    }

    protected ConfigurationEntityMatcher createConfigEntityMatcher() {
        return configEntityMatcher;
    }

    public boolean isSaveableApplicable(final Saveable saveable, final File file) {
        return createConfigEntityMatcher().matches(saveable, file);
    }

    @SuppressWarnings("deprecation")
    private PageMatcher getPageMatcherMatching(final String url) {
        final String rootURL = Hudson.getInstance().getRootUrlFromRequest();
        final String cleanedURL = url.startsWith(rootURL) ? url.substring(rootURL.length()) : url;
        for (final PageMatcher pm : pageMatchers)
            if (pm.getUrlRegex().matcher(cleanedURL).matches())
                return pm;
        return null;
    }

    @SuppressWarnings("deprecation")
    public List<File> createInitializationSynchronizedFileset() {
        final File hudsonRoot = Hudson.getInstance().getRootDir();
        final String[] matchingFilePaths = createConfigEntityMatcher().matchingFilesFrom(hudsonRoot);
        return new ArrayList<>(Collections2.transform(Arrays.asList(matchingFilePaths), PATH_TO_FILE_IN_HUDSON));
    }

    public boolean isCurrentUrlApplicable(final String url) {
        return getPageMatcherMatching(url) != null;
    }

    public List<String> getSyncIncludes() {
        return createConfigEntityMatcher().getIncludes();
    }

    public CommitMessageFactory getCommitMessageFactory() {
        return new DefaultCommitMessageFactory();
    }

    protected static class DefaultCommitMessageFactory implements CommitMessageFactory {
        @Override
        public WeightedMessage getMessageWhenSaveableUpdated(final Saveable s) {
            return new WeightedMessage("Modification on configuration(s)", MessageWeight.MINIMAL);
        }

        @Override
        public WeightedMessage getMessageWhenItemRenamed(final Item item, final String oldPath, final String newPath) {
            return new WeightedMessage("Item renamed", MessageWeight.MINIMAL);
        }

        @Override
        public WeightedMessage getMessageWhenItemDeleted(final Item item) {
            return new WeightedMessage("File hierarchy deleted", MessageWeight.MINIMAL);
        }
    }
}
