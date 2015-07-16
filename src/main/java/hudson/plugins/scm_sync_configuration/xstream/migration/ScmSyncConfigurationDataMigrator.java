package hudson.plugins.scm_sync_configuration.xstream.migration;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * Migrator from old GlobalBuildStats POJO to later GlobalBuildStats POJO
 *
 * @param <TFROM>
 * @param <TTO>
 * @author fcamblor
 */
public interface ScmSyncConfigurationDataMigrator<TFROM extends ScmSyncConfigurationPOJO, TTO extends ScmSyncConfigurationPOJO> {
    TTO migrate(final TFROM pojo);

    TTO readScmSyncConfigurationPOJO(final HierarchicalStreamReader reader, final UnmarshallingContext context);
}
