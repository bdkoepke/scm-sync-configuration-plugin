package hudson.plugins.scm_sync_configuration.xstream.migration;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import hudson.plugins.scm_sync_configuration.scms.SCM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public abstract class AbstractMigrator<F extends ScmSyncConfigurationPOJO, T extends ScmSyncConfigurationPOJO> implements ScmSyncConfigurationDataMigrator<F, T> {
    // TODO: enum?
    public enum ScmSyncConfiguration {
        SCM_REPOSITORY_URL_TAG("scmRepositoryUrl"),
        SCM_TAG("scm"),
        SCM_CLASS_ATTRIBUTE("class"),
        SCM_NO_USER_COMMIT_MESSAGE("noUserCommitMessage"),
        SCM_DISPLAY_STATUS("displayStatus"),
        SCM_COMMIT_MESSAGE_PATTERN("commitMessagePattern"),
        SCM_MANUAL_INCLUDES("manualSynchronizationIncludes");

        private static final Map<String, ScmSyncConfiguration> m = new HashMap<>();

        static {
            for (ScmSyncConfiguration s : ScmSyncConfiguration.values())
                m.put(s.getName(), s);
        }

        private final String name;

        ScmSyncConfiguration(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static ScmSyncConfiguration fromName(String name) {
            return m.get(name);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(AbstractMigrator.class.getName());

    public T migrate(final F pojo) {
        final T migratedPojo = createMigratedPojo();
        {
            migratedPojo.setScmRepositoryUrl(migrateScmRepositoryUrl(pojo.getScmRepositoryUrl()));
            migratedPojo.setScm(migrateScm(pojo.getScm()));
        }
        return migratedPojo;
    }

    public T readScmSyncConfigurationPOJO(
            final HierarchicalStreamReader reader) {
        final T pojo = createMigratedPojo();

        String scmRepositoryUrl = null;
        String scmClassAttribute = null;
        String scmContent = null;
        boolean noUserCommitMessage = false;
        boolean displayStatus = true;
        String commitMessagePattern = "[message]";
        List<String> manualIncludes = null;

        while (reader.hasMoreChildren()) {
            reader.moveDown();
            switch (ScmSyncConfiguration.fromName(reader.getNodeName())) {
                case SCM_REPOSITORY_URL_TAG:
                    scmRepositoryUrl = reader.getValue();
                    break;
                case SCM_TAG:
                    scmClassAttribute = reader.getAttribute(ScmSyncConfiguration.SCM_CLASS_ATTRIBUTE.name());
                    scmContent = reader.getValue();
                    break;
                case SCM_NO_USER_COMMIT_MESSAGE:
                    noUserCommitMessage = Boolean.parseBoolean(reader.getValue());
                    break;
                case SCM_DISPLAY_STATUS:
                    displayStatus = Boolean.parseBoolean(reader.getValue());
                    break;
                case SCM_COMMIT_MESSAGE_PATTERN:
                    commitMessagePattern = reader.getValue();
                    break;
                case SCM_MANUAL_INCLUDES:
                    manualIncludes = new ArrayList<>();
                    while (reader.hasMoreChildren()) {
                        reader.moveDown();
                        manualIncludes.add(reader.getValue());
                        reader.moveUp();
                    }
                    break;
                case SCM_CLASS_ATTRIBUTE:
                    // intentional fall-through
                default:
                    IllegalArgumentException iae = new IllegalArgumentException("Unknown tag : " + reader.getNodeName());
                    LOGGER.throwing(this.getClass().getName(), "readScmSyncConfigurationPOJO", iae);
                    LOGGER.severe("Unknown tag : " + reader.getNodeName());
                    throw iae;
            }
            reader.moveUp();
        }

        pojo.setScm(createSCMFrom(scmClassAttribute, scmContent));
        pojo.setScmRepositoryUrl(scmRepositoryUrl);
        pojo.setNoUserCommitMessage(noUserCommitMessage);
        pojo.setDisplayStatus(displayStatus);
        pojo.setCommitMessagePattern(commitMessagePattern);
        pojo.setManualSynchronizationIncludes(manualIncludes);

        return pojo;
    }

    private String migrateScmRepositoryUrl(final String scmRepositoryUrl) {
        return scmRepositoryUrl == null ? null : scmRepositoryUrl;
    }

    private SCM migrateScm(final SCM scm) {
        return scm == null ? null : SCM.valueOf(scm.getClass().getName());
    }

    protected abstract T createMigratedPojo();

    protected abstract SCM createSCMFrom(final String clazz, final String content);
}
