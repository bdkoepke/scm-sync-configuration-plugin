package hudson.plugins.scm_sync_configuration.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.xstream.migration.AbstractMigrator;
import hudson.plugins.scm_sync_configuration.xstream.migration.ScmSyncConfigurationDataMigrator;
import hudson.plugins.scm_sync_configuration.xstream.migration.ScmSyncConfigurationPOJO;
import hudson.plugins.scm_sync_configuration.xstream.migration.v0.InitialMigrator;
import hudson.plugins.scm_sync_configuration.xstream.migration.v1.V0ToV1Migrator;
import static hudson.plugins.scm_sync_configuration.xstream.migration.AbstractMigrator.ScmSyncConfiguration.*;

import java.util.logging.Logger;

/**
 * XStream converter for ScmSyncConfigurationPlugin XStream data
 * Allows to provide API to migrate from one version to another of persisted scm sync configuration data
 * When creating a new migrator you must :
 * - Create a new package hudson.plugins.scm_sync_configuration.xstream.migration.v[X]
 * - Inside this package, copy/paste every classes located in hudson.plugins.scm_sync_configuration.xstream.migration.v[X-1]
 * - Rename every *V[X-1]* POJOs to *V[X]* POJO
 * - Eventually, change attributes in V[X]ScmSyncConfigurationPOJO (for example, if additionnal attribute has appeared)
 * - Provide implementation for V[X]Migrator.migrate() algorithm
 * - If parsing algorithm has changed, update V[X]Migrator.readScmSyncConfigurationPOJO with the new algorithm (if, for example, new root
 * elements has appeared in XStream file)
 * - Update ScmSyncConfigurationXStreamConverter.MIGRATORS with new provided class
 *
 * @author fcamblor
 */
public class ScmSyncConfigurationXStreamConverter implements Converter {
    private static final String VERSION_ATTRIBUTE = "version";
    private static final Logger LOGGER = Logger.getLogger(ScmSyncConfigurationXStreamConverter.class.getName());

    /**
     * Migrators for old versions of GlobalBuildStatsPlugin data representations
     */
    private static final ScmSyncConfigurationDataMigrator[] MIGRATORS = new ScmSyncConfigurationDataMigrator[]{
            new InitialMigrator(),
            new V0ToV1Migrator()
    };

    /**
     * @return current version number of scm sync configuration plugin
     * data representation in XStream
     */
    private static int getCurrentScmSyncConfigurationVersionNumber() {
        return MIGRATORS.length - 1;
    }

    /**
     * Converter is only applicable on GlobalBuildStatsPlugin data
     */
    public boolean canConvert(final Class type) {
        return ScmSyncConfigurationPlugin.class.isAssignableFrom(type);
    }

    public void marshal(
            final Object source,
            final HierarchicalStreamWriter writer,
            final MarshallingContext context)
    {
        final ScmSyncConfigurationPlugin plugin = (ScmSyncConfigurationPlugin) source;

        // Since "v1", providing version number in scm sync configuration heading tag
        writer.addAttribute(VERSION_ATTRIBUTE, String.valueOf(getCurrentScmSyncConfigurationVersionNumber()));

        if (plugin.getSCM() != null)
            addAttribute(writer, plugin);
        if (plugin.getScmRepositoryUrl() != null)
            addNode(SCM_REPOSITORY_URL_TAG, writer, plugin.getScmRepositoryUrl());

        addNode(SCM_NO_USER_COMMIT_MESSAGE, writer, Boolean.toString(plugin.isNoUserCommitMessage()));
        addNode(SCM_DISPLAY_STATUS, writer, Boolean.toString(plugin.isDisplayStatus()));

        if (plugin.getCommitMessagePattern() != null)
            addNode(SCM_COMMIT_MESSAGE_PATTERN, writer, plugin.getCommitMessagePattern());
        if (plugin.getManualSynchronizationIncludes() != null) {
            writer.startNode(SCM_MANUAL_INCLUDES.name());
            for (String include : plugin.getManualSynchronizationIncludes())
                addNode("include", writer, include);
            writer.endNode();
        }
    }

    private static void addAttribute(final HierarchicalStreamWriter writer, final ScmSyncConfigurationPlugin plugin) {
        writer.startNode(SCM_TAG.name());
        writer.addAttribute(SCM_CLASS_ATTRIBUTE.name(), plugin.getSCM().getId());
        writer.endNode();
    }

    private static void addNode(AbstractMigrator.ScmSyncConfiguration scmDisplayStatus, HierarchicalStreamWriter writer, String s) {
        addNode(scmDisplayStatus.name(), writer, s);
    }

    private static void addNode(String start, HierarchicalStreamWriter writer, String value) {
        writer.startNode(start);
        writer.setValue(value);
        writer.endNode();
    }

    /**
     * Will transform scm sync configuration XStream data representation into
     * current ScmSyncConfigurationPlugin instance
     */
    @SuppressWarnings("deprecation")
    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {

        final ScmSyncConfigurationPlugin plugin = (context.currentObject() == null
                  || !(context.currentObject() instanceof ScmSyncConfigurationPlugin))
                ? new ScmSyncConfigurationPlugin() :
                (ScmSyncConfigurationPlugin) context.currentObject();

        // Retrieving data representation version number
        final String version = reader.getAttribute(VERSION_ATTRIBUTE);
        // Before version 1 (version 0), there wasn't any version in the scm sync configuration
        // configuration file
        int versionNumber = version != null ? Integer.parseInt(version) : 0;

        if (versionNumber != getCurrentScmSyncConfigurationVersionNumber())
            // There will be a data migration ..
            LOGGER.info(String.format(
                    "Your version of persisted ScmSyncConfigurationPlugin data" +
                    " is not up-to-date (v%s < v%s) : data will be migrated !",
                    versionNumber,
                    getCurrentScmSyncConfigurationVersionNumber()));

        // Calling version's reader to read data representation
        final ScmSyncConfigurationPOJO pojo = unsafeGetScmSyncConfigurationPOJO(
                versionNumber,
                MIGRATORS[versionNumber].readScmSyncConfigurationPOJO(reader));
        // Populating latest POJO information into ScmSyncConfigurationPlugin
        plugin.loadData(pojo);
        return plugin;
    }

    // TODO: Figure out another way to do this, this is a very unsafe way to handle this migration...
    @Deprecated
    @SuppressWarnings("unchecked")
    private static ScmSyncConfigurationPOJO unsafeGetScmSyncConfigurationPOJO(int versionNumber, ScmSyncConfigurationPOJO pojo) {
        // Migrating old data into up-to-date data
        // Added "+1" because we take into consideration InitialMigrator
        for (int i = versionNumber + 1; i <= getCurrentScmSyncConfigurationVersionNumber(); i++)
            pojo = MIGRATORS[i].migrate(pojo);
        return pojo;
    }
}
