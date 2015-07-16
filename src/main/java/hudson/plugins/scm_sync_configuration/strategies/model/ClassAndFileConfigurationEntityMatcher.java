package hudson.plugins.scm_sync_configuration.strategies.model;

import hudson.model.Saveable;

import java.io.File;

public class ClassAndFileConfigurationEntityMatcher extends PatternsEntityMatcher {
    private final Class<? extends Saveable> saveableClazz;

    public ClassAndFileConfigurationEntityMatcher(final Class<? extends Saveable> clazz, final String[] patterns) {
        super(patterns);
        this.saveableClazz = clazz;
    }

    public boolean matches(final Saveable saveable, final File file) {
        return saveableClazz.isAssignableFrom(saveable.getClass()) && (file == null || super.matches(saveable, file));
    }

}
