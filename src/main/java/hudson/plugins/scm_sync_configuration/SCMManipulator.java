package hudson.plugins.scm_sync_configuration;

import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.command.remove.RemoveScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class providing atomic scm commands and wrapping calls to maven scm api
 * with logging informations
 *
 * @author fcamblor
 */
public class SCMManipulator {
    private static final Logger LOGGER = Logger.getLogger(SCMManipulator.class.getName());

    private final ScmManager scmManager;
    private ScmRepository scmRepository = null;
    private String scmSpecificFilename = null;

    public SCMManipulator(final ScmManager scmManager) {
        this.scmManager = scmManager;
    }

    /**
     * Will check if everything is settled up (useful before a scm manipulation)
     */
    public boolean scmConfigurationSettledUp(final ScmContext scmContext, final boolean resetScmRepository) {
        final String scmRepositoryUrl = scmContext.getScmRepositoryUrl();
        final SCM scm = scmContext.getScm();
        if (scmRepositoryUrl == null || scm == null)
            return false;
        if (resetScmRepository)
            return expectScmRepositoryInitiated();

        LOGGER.info("Creating scmRepository connection data ..");
        this.scmRepository = scm.getConfiguredRepository(this.scmManager, scmRepositoryUrl);
        try {
            this.scmSpecificFilename = this.scmManager.getProviderByRepository(this.scmRepository).getScmSpecificFilename();
        } catch (NoSuchScmProviderException e) {
            LOGGER.throwing(ScmManager.class.getName(), "getScmSpecificFilename", e);
            LOGGER.severe("[getScmSpecificFilename] Error while getScmSpecificFilename : " + e.getMessage());
            return false;
        }
        return expectScmRepositoryInitiated();
    }

    private boolean expectScmRepositoryInitiated() {
        final boolean scmRepositoryInitiated = this.scmRepository != null;
        if (!scmRepositoryInitiated)
            LOGGER.warning("SCM Repository has not yet been initiated !");
        return scmRepositoryInitiated;
    }

    public void update(final File root) throws ScmException {
        this.scmManager.update(scmRepository, new ScmFileSet(root));
    }

    public boolean checkout(final File checkoutDirectory) {
        if (!expectScmRepositoryInitiated())
            return false;

        // Checkouting sources
        LOGGER.fine(String.format("Checkouting SCM files into [%s] ...", checkoutDirectory.getAbsolutePath()));
        try {
            final CheckOutScmResult result = scmManager.checkOut(this.scmRepository, new ScmFileSet(checkoutDirectory));
            if (!result.isSuccess()) {
                LOGGER.severe("[checkout] Error during checkout : " + result.getProviderMessage() + " || " + result.getCommandOutput());
                return false;
            }
        } catch (ScmException e) {
            LOGGER.throwing(ScmManager.class.getName(), "checkOut", e);
            LOGGER.severe("[checkout] Error during checkout : " + e.getMessage());
            return false;
        }

        LOGGER.fine("Checkouted SCM files into [" + checkoutDirectory.getAbsolutePath() + "] !");
        return true;
    }

    public List<File> deleteHierarchy(final File hierarchyToDelete) {
        if (!expectScmRepositoryInitiated())
            return null;

        final File enclosingDirectory = hierarchyToDelete.getParentFile();
        LOGGER.fine("Deleting SCM hierarchy [" + hierarchyToDelete.getAbsolutePath() + "] from SCM ...");

        File commitFile;
        for (commitFile = hierarchyToDelete; !commitFile.isDirectory(); commitFile = commitFile.getParentFile())
            //noinspection UnnecessaryContinue
            continue;

        try {
            {
                final ScmFileSet deleteFileSet = new ScmFileSet(enclosingDirectory, hierarchyToDelete);
                final RemoveScmResult removeResult = this.scmManager.remove(this.scmRepository, deleteFileSet, "");
                if (!removeResult.isSuccess()) {
                    LOGGER.severe("[deleteHierarchy] Problem during remove : " + removeResult.getProviderMessage());
                    return null;
                }
            }

            final List<File> filesToCommit = new ArrayList<>();
            filesToCommit.add(commitFile);
            return refineUpdatedFilesInScmResult(filesToCommit);
        } catch (ScmException e) {
            LOGGER.throwing(ScmManager.class.getName(), "remove", e);
            LOGGER.severe("[deleteHierarchy] Hierarchy deletion aborted : " + e.getMessage());
            return null;
        }
    }

    public List<File> addFile(final File scmRoot, final String filePathRelativeToScmRoot) {
        final List<File> synchronizedFiles = new ArrayList<>();
        if (!expectScmRepositoryInitiated())
            return synchronizedFiles;

        LOGGER.fine(String.format("Adding SCM file [%s] ...", filePathRelativeToScmRoot));
        try {
            // Split every directory leading through modifiedFilePathRelativeToHudsonRoot
            // and try add it in the scm
            final String[] pathChunks = filePathRelativeToScmRoot.split("\\\\|/");
            final StringBuilder currentPath = new StringBuilder();
            for (int i = 0; i < pathChunks.length; i++) {
                currentPath.append(pathChunks[i]);
                if (i != pathChunks.length - 1)
                    currentPath.append(File.separator);
                final File currentFile = new File(currentPath.toString());

                // Trying to add current path to the scm ...
                AddScmResult addResult = this.scmManager.add(this.scmRepository, new ScmFileSet(scmRoot, currentFile));
                // If current has not yet been synchronized, addResult.isSuccess() should be true
                if (!addResult.isSuccess()) {
                    // If addResult.isSuccess() is false, it isn't an error if it is related to path chunks (except for latest one) :
                    // if pathChunk is already synchronized, addResult.isSuccess() will be false.
                    Level logLevel = (i == pathChunks.length - 1) ? Level.SEVERE : Level.FINE;
                    LOGGER.log(logLevel, "Error while adding SCM file : " + addResult.getCommandOutput());
                    continue;
                }

                if (i != pathChunks.length - 1 ||
                        !new File(String.format("%s%s%s", scmRoot.getAbsolutePath(), File.separator, currentPath.toString())).isDirectory())
                    continue;

                addResult = this.scmManager.add(this.scmRepository, new ScmFileSet(scmRoot, currentPath.toString() + "/**/*"));
                if (addResult.isSuccess()) {
                    synchronizedFiles.addAll(refineUpdatedFilesInScmResult(addResult.getAddedFiles()));
                    continue;
                }

                LOGGER.severe("Error while adding SCM files in directory : " + addResult.getCommandOutput());
            }
        } catch (IOException e) {
            LOGGER.throwing(ScmFileSet.class.getName(), "init<>", e);
            LOGGER.warning("[addFile] Error while creating ScmFileset : " + e.getMessage());
            return synchronizedFiles;
        } catch (org.apache.maven.scm.ScmException e) {
            LOGGER.throwing(ScmManager.class.getName(), "add", e);
            LOGGER.warning("[addFile] Error while adding file : " + e.getMessage());
            return synchronizedFiles;
        }

        if (!synchronizedFiles.isEmpty())
            LOGGER.fine(String.format("Added SCM files : %s",
                    Arrays.toString(synchronizedFiles.toArray(new File[synchronizedFiles.size()])) + " !"));
        return synchronizedFiles;
    }

    private List<File> refineUpdatedFilesInScmResult(List updatedFiles) {
        List<File> refinedUpdatedFiles = new ArrayList<>();

        // Cannot use directly a List<ScmFile> or List<File> here, since result type will depend upon
        // current scm api version
        for (final Object scmFile : updatedFiles) {
            if (scmFile instanceof File) {
                final String checkoutScmDir = ScmSyncConfigurationBusiness.getCheckoutScmDirectoryAbsolutePath();
                String scmPath = ((File) scmFile).getAbsolutePath();
                if (scmPath.startsWith(checkoutScmDir))
                    scmPath = scmPath.substring(checkoutScmDir.length() + 1);
                refinedUpdatedFiles.add(new File(scmPath));
            } else if (scmFile instanceof ScmFile) {
                refinedUpdatedFiles.add(new File(((ScmFile) scmFile).getPath()));
            } else {
                LOGGER.severe("Unhandled AddScmResult.addedFiles type : " + scmFile.getClass().getName());
            }
        }

        return refinedUpdatedFiles;
    }

    public boolean checkinFiles(final File scmRoot, final String commitMessage) {
        if (!expectScmRepositoryInitiated())
            return false;

        LOGGER.fine("Checking in SCM files ...");
        final ScmFileSet fileSet = new ScmFileSet(scmRoot);
        try {
            final CheckInScmResult result = this.scmManager.checkIn(this.scmRepository, fileSet, commitMessage);
            if (!result.isSuccess()) {
                LOGGER.severe("[checkinFiles] Problem during SCM commit : " + result.getCommandOutput());
                return false;
            }
        } catch (ScmException e) {
            LOGGER.throwing(ScmManager.class.getName(), "checkIn", e);
            LOGGER.severe("[checkinFiles] Error while checkin : " + e.getMessage());
            return false;
        }

        LOGGER.fine("Checked in SCM files !");
        return true;
    }

    public String getScmSpecificFilename() {
        return scmSpecificFilename;
    }
}
