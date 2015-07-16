package hudson.plugins.scm_sync_configuration.strategies.model;

import hudson.model.Saveable;

import java.io.File;
import java.util.List;

public interface ConfigurationEntityMatcher {
    boolean matches(final Saveable saveable, final File file);

    String[] matchingFilesFrom(final File rootDirectory);

    List<String> getIncludes();
}
