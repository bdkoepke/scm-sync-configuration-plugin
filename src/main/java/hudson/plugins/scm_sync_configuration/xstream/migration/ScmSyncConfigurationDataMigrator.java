package hudson.plugins.scm_sync_configuration.xstream.migration;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * Migrator from old GlobalBuildStats POJO to later GlobalBuildStats POJO
 *
 * @param <T>
 * @param <F>
 * @author fcamblor
 */
public interface ScmSyncConfigurationDataMigrator<T extends ScmSyncConfigurationPOJO, F extends ScmSyncConfigurationPOJO> {
    F migrate(final T pojo);

    F readScmSyncConfigurationPOJO(final HierarchicalStreamReader reader);
}
