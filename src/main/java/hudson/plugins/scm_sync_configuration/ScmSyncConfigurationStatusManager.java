package hudson.plugins.scm_sync_configuration;

import hudson.model.Hudson;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

public class ScmSyncConfigurationStatusManager {

    public static final String LOG_SUCCESS_FILENAME = "scm-sync-configuration.success.log";
    public static final String LOG_FAIL_FILENAME = "scm-sync-configuration.fail.log";
    private static final Logger LOGGER = Logger.getLogger(ScmSyncConfigurationStatusManager.class.getName());
    private final File fail;
    private final File success;

    public ScmSyncConfigurationStatusManager() {
        fail = new File(Hudson.getInstance().getRootDir().getAbsolutePath() + File.separator + LOG_FAIL_FILENAME);
        success = new File(Hudson.getInstance().getRootDir().getAbsolutePath() + File.separator + LOG_SUCCESS_FILENAME);
    }

    private static String readFile(final File f) {
        if (f.exists()) {
            try {
                return FileUtils.fileRead(f);
            } catch (IOException e) {
                LOGGER.severe(String.format("Unable to read file %s : %s", f.getAbsolutePath(), e.getMessage()));
            }
        }
        return null;
    }

    private static void writeFile(final File f, final String data) {
        try {
            FileUtils.fileWrite(f.getAbsolutePath(), data);
        } catch (IOException e) {
            LOGGER.severe(String.format("Unable to write file %s : %s", f.getAbsolutePath(), e.getMessage()));
        }
    }

    private static void appendFile(final File f, final String data) {
        try {
            FileUtils.fileAppend(f.getAbsolutePath(), data);
        } catch (IOException e) {
            LOGGER.severe(String.format("Unable to append file %s : %s", f.getAbsolutePath(), e.getMessage()));
        }
    }

    public String getLastFail() {
        return readFile(fail);
    }

    public String getLastSuccess() {
        return readFile(success);
    }

    public void signalSuccess() {
        writeFile(success, new Date().toString());
    }

    public void signalFailed(String description) {
        appendFile(fail, String.format("%s : %s<br/>", new Date().toString(), description));
    }

    public void purgeFailLogs() {
        //noinspection ResultOfMethodCallIgnored
        fail.delete();
    }
}
