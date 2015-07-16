package hudson.plugins.scm_sync_configuration;

import hudson.model.Hudson;

import java.io.File;

public class JenkinsFilesHelper {
    @SuppressWarnings("deprecation")
    public static String buildPathRelativeToHudsonRoot(final File file) {
        final File hudsonRoot = Hudson.getInstance().getRootDir();
        if (!file.getAbsolutePath().startsWith(hudsonRoot.getAbsolutePath()))
            throw new IllegalArgumentException(String.format("Err ! File [%s] seems not to reside in [%s] !", file.getAbsoluteFile(), hudsonRoot.getAbsolutePath()));
        return file
                .getAbsolutePath()
                .substring(hudsonRoot.getAbsolutePath().length() + 1)
                .replaceAll("\\\\", "/");
    }

    @SuppressWarnings("deprecation")
    public static File buildFileFromPathRelativeToHudsonRoot(final String pathRelativeToHudsonRoot) {
        final File hudsonRoot = Hudson.getInstance().getRootDir();
        return new File(String.format("%s%s%s",
                hudsonRoot.getAbsolutePath(),
                File.separator,
                pathRelativeToHudsonRoot));
    }
}
