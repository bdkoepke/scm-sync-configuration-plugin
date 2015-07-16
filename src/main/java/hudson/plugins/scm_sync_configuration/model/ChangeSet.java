package hudson.plugins.scm_sync_configuration.model;

import com.google.common.io.Files;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;
import hudson.plugins.scm_sync_configuration.exceptions.LoggableException;
import hudson.plugins.scm_sync_configuration.utils.Checksums;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author fcamblor
 *         POJO representing a Changeset built during a scm transaction
 */
public class ChangeSet {
    private final Map<Path, byte[]> pathContents;
    private final List<Path> pathsToDelete;
    private WeightedMessage message = null;

    public ChangeSet() {
        pathContents = new HashMap<>();
        pathsToDelete = new ArrayList<>();
    }

    public void registerPath(final String path) {
        final Path pathToRegister;
        {
            final File hudsonFile = JenkinsFilesHelper.buildFileFromPathRelativeToHudsonRoot(path);
            pathToRegister = new Path(hudsonFile);
        }

        if (pathToRegister.isDirectory()) {
            pathContents.put(pathToRegister, new byte[0]);
            return;
        }

        String method = "fileAndByteArrayContentAreEqual";
        Class c = Checksums.class;
        try {
            if (pathContents.containsKey(pathToRegister) && Checksums.fileAndByteArrayContentAreEqual(pathToRegister.getHudsonFile(), pathContents.get(pathToRegister)))
                return;
            method = "toByteArray";
            c = Files.class;
            pathContents.put(pathToRegister, Files.toByteArray(pathToRegister.getHudsonFile()));
        } catch (IOException e) {
            throw new LoggableException(String.format("Changeset path <%s> registration failed", path), c, method, e);
        }
    }

    public void registerRenamedPath(final String oldPath, final String newPath) {
        registerPathForDeletion(oldPath);
        registerPath(newPath);
    }

    public void registerPathForDeletion(final String path) {
        pathsToDelete.add(new Path(path, new Path(path).getScmFile().isDirectory()));
    }

    public boolean isEmpty() {
        return pathContents.isEmpty() && pathsToDelete.isEmpty();
    }

    public Map<Path, byte[]> getPathContents() {
        final Map<Path, byte[]> filteredPathContents = new HashMap<>(pathContents);
        final List<Path> filteredPaths = new ArrayList<>();

        for (final Path pathToAdd : filteredPathContents.keySet())
            for (final Path pathToDelete : pathsToDelete)
                if (pathToDelete.contains(pathToAdd))
                    filteredPaths.add(pathToAdd);
        for (final Path path : filteredPaths)
            filteredPathContents.remove(path);
        return filteredPathContents;
    }

    public List<Path> getPathsToDelete() {
        return Collections.unmodifiableList(pathsToDelete);
    }

    public void defineMessage(final WeightedMessage weightedMessage) {
        if (this.message == null || weightedMessage.getWeight().heavierThan(message.getWeight()))
            this.message = weightedMessage;
    }

    public String getMessage() {
        return this.message.getMessage();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final Path path : getPathContents().keySet())
            sb.append(String.format("    A %s%n", path.toString()));
        for (final Path path : getPathsToDelete())
            sb.append(String.format("    D %s%n", path.toString()));
        return sb.toString();
    }
}
