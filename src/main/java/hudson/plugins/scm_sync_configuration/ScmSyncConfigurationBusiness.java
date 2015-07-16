package hudson.plugins.scm_sync_configuration;

import com.google.common.io.Files;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.plugins.scm_sync_configuration.exceptions.LoggableException;
import hudson.plugins.scm_sync_configuration.model.*;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.utils.Checksums;
import hudson.security.Permission;
import hudson.util.DaemonThreadFactory;
import org.apache.commons.io.FileUtils;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.manager.ScmManager;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;


public class ScmSyncConfigurationBusiness {
    private static final String WORKING_DIRECTORY_PATH = "/scm-sync-configuration/";
    private static final String CHECKOUT_SCM_DIRECTORY = "checkoutConfiguration";
    private static final Logger LOGGER = Logger.getLogger(ScmSyncConfigurationBusiness.class.getName());
    /**
     * Use of a size 1 thread pool frees us from worrying about accidental thread death and
     * changeset commit concurrency
     */
    private final ExecutorService writer = Executors.newFixedThreadPool(1, new DaemonThreadFactory());
    //  TODO: Refactor this into the plugin object ???
    private final List<Commit> commitsQueue = Collections.synchronizedList(new ArrayList<Commit>());
    private boolean checkoutSucceeded;
    private SCMManipulator scmManipulator;
    private File checkoutScmDirectory = null;
    private ScmSyncConfigurationStatusManager scmSyncConfigurationStatusManager = null;

    public ScmSyncConfigurationBusiness() {
    }

    public static String getCheckoutScmDirectoryAbsolutePath() {
        return Hudson.getInstance().getRootDir().getAbsolutePath() + WORKING_DIRECTORY_PATH + CHECKOUT_SCM_DIRECTORY;
    }

    private static Permission purgeFailLogPermission() {
        // Only administrators should be able to purge logs
        return Hudson.ADMINISTER;
    }

    public ScmSyncConfigurationStatusManager getScmSyncConfigurationStatusManager() {
        return scmSyncConfigurationStatusManager == null ?
                (scmSyncConfigurationStatusManager = new ScmSyncConfigurationStatusManager()) :
                scmSyncConfigurationStatusManager;
    }

    public void init(final ScmContext scmContext) throws ComponentLookupException, PlexusContainerException {
        final ScmManager scmManager = SCMManagerFactory.getInstance().createScmManager();
        this.scmManipulator = new SCMManipulator(scmManager);
        this.checkoutScmDirectory = new File(getCheckoutScmDirectoryAbsolutePath());
        this.checkoutSucceeded = false;
        initializeRepository(scmContext, false);
    }

    public void initializeRepository(final ScmContext scmContext, final boolean deleteCheckoutScmDir) {
        if (scmManipulator == null || !scmManipulator.scmConfigurationSettledUp(scmContext, true))
            return;

        // Let's check if everything is available to checkout sources
        LOGGER.info("Initializing SCM repository for scm-sync-configuration plugin ...");
        // If checkoutScmDirectory was not empty and deleteCheckoutScmDir is asked, reinitialize it !
        if (deleteCheckoutScmDir)
            cleanChekoutScmDirectory();

        // Creating checkout scm directory
        if (!checkoutScmDirectory.exists()) {
            String created = "";
            try {
                FileUtils.forceMkdir(checkoutScmDirectory);
                LOGGER.info("Directory [" + checkoutScmDirectory.getAbsolutePath() + "] created !");
            } catch (IOException ignored) {
                created = " could not not be";
            } finally {
                LOGGER.warning(String.format("Directory [%s]%s created !", checkoutScmDirectory.getAbsolutePath(), created));
            }
        }

        this.checkoutSucceeded = this.scmManipulator.checkout(this.checkoutScmDirectory);
        if (this.checkoutSucceeded)
            LOGGER.info("SCM repository initialization done.");
        signal("Checkout " + this.checkoutScmDirectory, this.checkoutSucceeded);
    }

    public void cleanChekoutScmDirectory() {
        if (checkoutScmDirectory == null || !checkoutScmDirectory.exists())
            return;

        LOGGER.info("Deleting old checkout SCM directory ...");
        try {
            FileUtils.forceDelete(checkoutScmDirectory);
        } catch (IOException e) {
            LOGGER.throwing(FileUtils.class.getName(), "forceDelete", e);
            LOGGER.severe(String.format("Error while deleting [%s] : %s", checkoutScmDirectory.getAbsolutePath(), e.getMessage()));
        }
        // TODO: watch this line...
        this.checkoutSucceeded = false;
    }

    public List<File> deleteHierarchy(ScmContext scmContext, Path hierarchyPath) {
        if (scmManipulator == null || !scmManipulator.scmConfigurationSettledUp(scmContext, false))
            return null;

        final File rootHierarchyTranslatedInScm = hierarchyPath.getScmFile();
        final List<File> filesToCommit = scmManipulator.deleteHierarchy(rootHierarchyTranslatedInScm);
        // Once done, we should delete path in scm if it is a directory
        if (hierarchyPath.isDirectory())
            try {
                FileUtils.deleteDirectory(rootHierarchyTranslatedInScm);
            } catch (IOException e) {
                throw new LoggableException(String.format(
                        "Failed to recursively delete scm directory %s",
                        rootHierarchyTranslatedInScm.getAbsolutePath()),
                        FileUtils.class, "deleteDirectory", e);
            }

        signal(String.format("Delete %s", hierarchyPath), filesToCommit != null);
        return filesToCommit;
    }

    public Future<Void> queueChangeSet(final ScmContext scmContext, ChangeSet changeset, User user, String userMessage) {
        if (scmManipulator == null || !scmManipulator.scmConfigurationSettledUp(scmContext, false)) {
            LOGGER.info(String.format("Queue of changeset %s aborted (scm manipulator not settled !)", changeset.toString()));
            return null;
        }

        final Commit commit = new Commit(changeset, user, userMessage, scmContext);
        LOGGER.finest(String.format("Queuing commit %s to SCM ...", commit.toString()));
        commitsQueue.add(commit);
        return writer.submit(new Callable<Void>() {
            public Void call() throws Exception {
                processCommitsQueue();
                return null;
            }
        });
    }

    private void processCommitsQueue() {
        final File scmRoot = new File(getCheckoutScmDirectoryAbsolutePath());

        // Copying shared commitQueue in order to allow conccurrent modification
        final List<Commit> currentCommitQueue = new ArrayList<>(commitsQueue);
        final List<Commit> checkedInCommits = new ArrayList<>();

        try {
            // Reading commit queue and commiting changeset
            for (final Commit commit : currentCommitQueue) {
                final String logMessage = "Processing commit : " + commit.toString();
                LOGGER.finest(logMessage);

                // Preparing files to add / delete
                final List<File> updatedFiles = new ArrayList<>();
                for (final Map.Entry<Path, byte[]> pathContent : commit.getChangeset().getPathContents().entrySet()) {
                    final Path pathRelativeToJenkinsRoot = pathContent.getKey();
                    final byte[] content = pathContent.getValue();

                    final File fileTranslatedInScm = pathRelativeToJenkinsRoot.getScmFile();
                    if (pathRelativeToJenkinsRoot.isDirectory()) {
                        if (!fileTranslatedInScm.exists()) {
                            // Retrieving non existing parent scm path *before* copying it from jenkins directory
                            String firstNonExistingParentScmPath = pathRelativeToJenkinsRoot.getFirstNonExistingParentScmPath();

                            try {
                                FileUtils.copyDirectory(JenkinsFilesHelper.buildFileFromPathRelativeToHudsonRoot(pathRelativeToJenkinsRoot.getPath()),
                                        fileTranslatedInScm);
                            } catch (IOException e) {
                                throw new LoggableException("Error while copying file hierarchy to SCM checkouted directory", FileUtils.class, "copyDirectory", e);
                            }
                            updatedFiles.addAll(scmManipulator.addFile(scmRoot, firstNonExistingParentScmPath));
                        }
                    } else {
                        // We should remember if file in scm existed or not before any manipulation,
                        // especially writing content
                        boolean fileTranslatedInScmInitiallyExists = fileTranslatedInScm.exists();

                        boolean fileContentModified = writeScmContentOnlyIfItDiffers(pathRelativeToJenkinsRoot, content, fileTranslatedInScm);
                        if (fileTranslatedInScmInitiallyExists) {
                            if (fileContentModified) {
                                // No need to call scmManipulator.addFile() if fileTranslatedInScm already existed
                                updatedFiles.add(fileTranslatedInScm);
                            }
                        } else {
                            updatedFiles.addAll(scmManipulator.addFile(scmRoot, pathRelativeToJenkinsRoot.getPath()));
                        }
                    }
                }
                for (Path path : commit.getChangeset().getPathsToDelete()) {
                    List<File> deletedFiles = deleteHierarchy(commit.getScmContext(), path);
                    updatedFiles.addAll(deletedFiles);
                }

                if (updatedFiles.isEmpty()) {
                    LOGGER.finest("Empty changeset to commit (no changes found on files) => commit skipped !");
                    checkedInCommits.add(commit);
                } else {
                    // Commiting files...
                    boolean result = scmManipulator.checkinFiles(scmRoot, commit.getMessage());

                    if (result) {
                        LOGGER.finest("Commit " + commit.toString() + " pushed to SCM !");
                        checkedInCommits.add(commit);
                    } else {
                        throw new LoggableException("Error while checking in file to scm repository", SCMManipulator.class, "checkinFiles");
                    }

                    signal(logMessage, true);
                }
            }
            // As soon as a commit doesn't goes well, we should abort commit queue processing...
        } catch (LoggableException e) {
            LOGGER.throwing(e.getClazz().getName(), e.getMethodName(), e);
            LOGGER.severe("Error while processing commit queue : " + e.getMessage());
            signal(e.getMessage(), false);
        } finally {
            // We should remove every checkedInCommits
            commitsQueue.removeAll(checkedInCommits);
        }
    }

    private boolean writeScmContentOnlyIfItDiffers(
            final Path pathRelativeToJenkinsRoot,
            final byte[] content,
            final File fileTranslatedInScm)
            throws LoggableException {
        try {
            final boolean contentDiffer = !Checksums.fileAndByteArrayContentAreEqual(fileTranslatedInScm, content);
            if (contentDiffer)
                createScmContent(pathRelativeToJenkinsRoot, content, fileTranslatedInScm);
            return contentDiffer;
        } catch (IOException e) {
            throw new LoggableException("Error while checking content checksum", Checksums.class, "fileAndByteArrayContentAreEqual", e);
        }
    }

    private void createScmContent(
            final Path pathRelativeToJenkinsRoot,
            byte[] content,
            File fileTranslatedInScm)
            throws LoggableException {
        final Stack<File> directoriesToCreate = new Stack<>();
        File directory = fileTranslatedInScm.getParentFile();

        // Eventually, creating non existing enclosing directories
        while (!directory.exists()) {
            directoriesToCreate.push(directory);
            directory = directory.getParentFile();
        }
        while (!directoriesToCreate.empty()) {
            directory = directoriesToCreate.pop();
            if (!directory.mkdir()) {
                throw new LoggableException("Error while creating directory " + directory.getAbsolutePath(), File.class, "mkdir");
            }
        }

        try {
            // Copying content if pathRelativeToJenkinsRoot is a file, or creating the directory if it is a directory
            if (pathRelativeToJenkinsRoot.isDirectory()) {
                if (!fileTranslatedInScm.mkdir()) {
                    throw new LoggableException("Error while creating directory " + fileTranslatedInScm.getAbsolutePath(), File.class, "mkdir");
                }
            } else {
                Files.write(content, fileTranslatedInScm);
            }
        } catch (IOException e) {
            throw new LoggableException("Error while creating file in checkouted directory", Files.class, "write", e);
        }
    }

    public void synchronizeAllConfigs(ScmSyncStrategy[] availableStrategies) {
        List<File> filesToSync = new ArrayList<File>();
        // Building synced files from strategies
        for (ScmSyncStrategy strategy : availableStrategies) {
            filesToSync.addAll(strategy.createInitializationSynchronizedFileset());
        }

        ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
        plugin.startThreadedTransaction();
        try {
            for (File fileToSync : filesToSync) {
                String hudsonConfigPathRelativeToHudsonRoot = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(fileToSync);

                plugin.getTransaction().defineCommitMessage(new WeightedMessage("Repository initialization", MessageWeight.IMPORTANT));
                plugin.getTransaction().registerPath(hudsonConfigPathRelativeToHudsonRoot);
            }
        } finally {
            plugin.getTransaction().commit();
        }
    }

    public boolean scmCheckoutDirectorySettledUp(ScmContext scmContext) {
        return scmManipulator != null && this.scmManipulator.scmConfigurationSettledUp(scmContext, false) && this.checkoutSucceeded;
    }

    public List<File> reloadAllFilesFromScm() throws IOException, ScmException {
        this.scmManipulator.update(new File(getCheckoutScmDirectoryAbsolutePath()));
        return syncDirectories(new File(getCheckoutScmDirectoryAbsolutePath() + File.separator), "");
    }

    private List<File> syncDirectories(File from, String relative) throws IOException {
        List<File> l = new ArrayList<File>();
        for (File f : from.listFiles()) {
            String newRelative = relative + File.separator + f.getName();
            File jenkinsFile = new File(Hudson.getInstance().getRootDir() + newRelative);
            if (f.getName().equals(scmManipulator.getScmSpecificFilename())) {
                // nothing to do
            } else if (f.isDirectory()) {
                if (!jenkinsFile.exists()) {
                    FileUtils.copyDirectory(f, jenkinsFile, new FileFilter() {

                        public boolean accept(File f) {
                            return !f.getName().equals(scmManipulator.getScmSpecificFilename());
                        }

                    });
                    l.add(jenkinsFile);
                } else {
                    l.addAll(syncDirectories(f, newRelative));
                }
            } else {
                if (!jenkinsFile.exists() || !FileUtils.contentEquals(f, jenkinsFile)) {
                    FileUtils.copyFile(f, jenkinsFile);
                    l.add(jenkinsFile);
                }
            }
        }
        return l;
    }

    private void signal(String operation, boolean result) {
        if (result) {
            getScmSyncConfigurationStatusManager().signalSuccess();
        } else {
            getScmSyncConfigurationStatusManager().signalFailed(operation);
        }
    }

    public void purgeFailLogs() {
        Hudson.getInstance().checkPermission(purgeFailLogPermission());
        scmSyncConfigurationStatusManager.purgeFailLogs();
    }

    public boolean canCurrentUserPurgeFailLogs() {
        return Hudson.getInstance().hasPermission(purgeFailLogPermission());
    }
}
