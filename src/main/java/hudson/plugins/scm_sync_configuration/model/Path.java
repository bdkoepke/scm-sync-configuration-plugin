package hudson.plugins.scm_sync_configuration.model;

import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationBusiness;

import java.io.File;

/**
 * @author fcamblor
 *         Paths allows to know if a given path is a directory or not, without using a File object since,
 *         generally, Path will be relative to jenkins root
 */
public class Path {
    private final String path;
    private final boolean isDirectory;

    public Path(final String path) {
        this(JenkinsFilesHelper.buildFileFromPathRelativeToHudsonRoot(path));
    }

    public Path(final File hudsonFile) {
        this(JenkinsFilesHelper.buildPathRelativeToHudsonRoot(hudsonFile), hudsonFile.isDirectory());
    }

    public Path(final String path, final boolean isDirectory) {
        this.path = path;
        this.isDirectory = isDirectory;
    }

    public String getPath() {
        return path;
    }

    public File getHudsonFile() {
        return JenkinsFilesHelper.buildFileFromPathRelativeToHudsonRoot(this.path);
    }

    public File getScmFile() {
        return new File(String.format("%s%s%s",
                ScmSyncConfigurationBusiness.getCheckoutScmDirectoryAbsolutePath(),
                File.separator,
                getPath()));
    }

    public String getFirstNonExistingParentScmPath() {
        File latestNonExistingScmFile = null;
        for (File currentFile = getScmFile(); !currentFile.exists(); currentFile = currentFile.getParentFile())
            latestNonExistingScmFile = currentFile;
        return latestNonExistingScmFile != null ?
                latestNonExistingScmFile.getAbsolutePath().substring(ScmSyncConfigurationBusiness.getCheckoutScmDirectoryAbsolutePath().length() + 1) :
                null;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public boolean contains(final Path p) {
        return this.isDirectory() && p.getPath().startsWith(this.getPath());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Path)) return false;

        final Path path = (Path) o;
        return isDirectory == path.isDirectory && !(this.path != null ? !this.path.equals(path.path) : path.path != null);
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (isDirectory ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return getPath() + (isDirectory() ? "/" : "");
    }
}
