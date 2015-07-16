package hudson.plugins.scm_sync_configuration;

import hudson.Plugin;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Saveable;
import hudson.model.User;
import hudson.plugins.scm_sync_configuration.extensions.ScmSyncConfigurationFilter;
import hudson.plugins.scm_sync_configuration.model.ChangeSet;
import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.scms.ScmSyncNoSCM;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.impl.*;
import hudson.plugins.scm_sync_configuration.transactions.AtomicTransaction;
import hudson.plugins.scm_sync_configuration.transactions.ScmTransaction;
import hudson.plugins.scm_sync_configuration.transactions.ThreadedTransaction;
import hudson.plugins.scm_sync_configuration.xstream.ScmSyncConfigurationXStreamConverter;
import hudson.plugins.scm_sync_configuration.xstream.migration.ScmSyncConfigurationPOJO;
import hudson.util.PluginServletFilter;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class ScmSyncConfigurationPlugin extends Plugin {
    private static final ScmSyncStrategy[] AVAILABLE_STRATEGIES = new ScmSyncStrategy[]{
            new JenkinsConfigScmSyncStrategy(),
            new BasicPluginsConfigScmSyncStrategy(),
            new JobConfigScmSyncStrategy(),
            new UserConfigScmSyncStrategy(),
            new ManualIncludesScmSyncStrategy()
    };

    private static final Logger LOGGER = Logger.getLogger(ScmSyncConfigurationPlugin.class.getName());
    private transient ScmSyncConfigurationBusiness business;
    /**
     * Flag allowing to process commit synchronously instead of asynchronously (default)
     * Could be useful, particularly during tests execution
     */
    private transient boolean synchronousTransactions = false;
    /**
     * SCM Transaction which is currently used. This transaction is thread scoped and will be, by default,
     * an AtomicTransaction (each time a change is recorded, it will be immediately commited).
     * Every time a transaction will be commited, it will be resetted to null
     */
    private final transient ThreadLocal<ScmTransaction> transaction = new ThreadLocal<>();
    private transient Future<Void> latestCommitFuture;
    private String scmRepositoryUrl;
    private SCM scm;
    private boolean noUserCommitMessage;
    private boolean displayStatus = true;
    // The [message] is a magic string that will be replaced with commit message
    // when commit occurs
    private String commitMessagePattern = "[message]";
    private List<File> filesModifiedByLastReload;
    private List<String> manualSynchronizationIncludes;

    public ScmSyncConfigurationPlugin() {
        // By default, transactions should be asynchronous
        this(false);
    }

    public ScmSyncConfigurationPlugin(final boolean synchronousTransactions) {
        this.synchronousTransactions = synchronousTransactions;
        setBusiness(new ScmSyncConfigurationBusiness());

        try {
            PluginServletFilter.addFilter(new ScmSyncConfigurationFilter());
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("deprecation")
    public static ScmSyncConfigurationPlugin getInstance() {
        return Hudson.getInstance().getPlugin(ScmSyncConfigurationPlugin.class);
    }

    public void purgeFailLogs() {
        business.purgeFailLogs();
    }

    public List<String> getManualSynchronizationIncludes() {
        return manualSynchronizationIncludes;
    }

    @Override
    public void start() throws Exception {
        super.start();

        Hudson.XSTREAM.registerConverter(new ScmSyncConfigurationXStreamConverter());

        this.load();

        // If scm has not been read in scm-sync-configuration.xml, let's initialize it
        // to the "no scm" SCM
        if (this.scm == null) {
            this.scm = SCM.valueOf(ScmSyncNoSCM.class);
            this.scmRepositoryUrl = null;
        }

        // SCMManagerFactory.start() must be called here instead of ScmSyncConfigurationItemListener.onLoaded()
        // because, for some unknown reasons, we reach plexus bootstraping exceptions when
        // calling Embedder.start() when everything is loaded (very strange...)
        SCMManagerFactory.getInstance().start();
    }

    public void loadData(ScmSyncConfigurationPOJO pojo) {
        this.scmRepositoryUrl = pojo.getScmRepositoryUrl();
        this.scm = pojo.getScm();
        this.noUserCommitMessage = pojo.isNoUserCommitMessage();
        this.displayStatus = pojo.isDisplayStatus();
        this.commitMessagePattern = pojo.getCommitMessagePattern();
        this.manualSynchronizationIncludes = pojo.getManualSynchronizationIncludes();
    }

    public void init() {
        try {
            this.business.init(createScmContext());
        } catch (Exception e) {
            throw new RuntimeException("Error during ScmSyncConfiguration initialisation !", e);
        }
    }

    @Override
    public void stop() throws Exception {
        SCMManagerFactory.getInstance().stop();
        super.stop();
    }

    @Override
    public void configure(StaplerRequest req, JSONObject formData)
            throws IOException, ServletException, FormException {
        super.configure(req, formData);

        //TODO: simplify...

        boolean repoInitializationRequired = false;
        boolean configsResynchronizationRequired = false;
        boolean repoCleaningRequired = false;

        this.noUserCommitMessage = formData.getBoolean("noUserCommitMessage");
        this.displayStatus = formData.getBoolean("displayStatus");
        this.commitMessagePattern = req.getParameter("commitMessagePattern");

        final String oldScmRepositoryUrl = this.scmRepositoryUrl;
        final String scmType = req.getParameter("scm");
        if (scmType != null) {
            this.scm = SCM.valueOf(scmType);
            assert this.scm != null;
            final String newScmRepositoryUrl = this.scm.createScmUrlFromRequest(req);

            this.scmRepositoryUrl = newScmRepositoryUrl;

            // If something changed, let's reinitialize repository in working directory !
            repoInitializationRequired = newScmRepositoryUrl != null && !newScmRepositoryUrl.equals(oldScmRepositoryUrl);
            configsResynchronizationRequired = repoInitializationRequired;
            repoCleaningRequired = newScmRepositoryUrl == null && oldScmRepositoryUrl != null;
        }

        if (req.getParameterValues("manualSynchronizationIncludes") != null) {
            final List<String> submittedManualIncludes = new ArrayList<>(Arrays.asList(req.getParameterValues("manualSynchronizationIncludes")));
            final List<String> newManualIncludes = new ArrayList<>(submittedManualIncludes);
            if (this.manualSynchronizationIncludes != null)
                newManualIncludes.removeAll(this.manualSynchronizationIncludes);
            this.manualSynchronizationIncludes = submittedManualIncludes;
            configsResynchronizationRequired = !newManualIncludes.isEmpty();
        } else
            this.manualSynchronizationIncludes = new ArrayList<>();

        // Repo initialization should be made _before_ plugin save, in order to let scm-sync-configuration.xml
        // file synchronizable
        if (repoInitializationRequired)
            this.business.initializeRepository(createScmContext(), true);
        if (configsResynchronizationRequired)
            this.business.synchronizeAllConfigs(AVAILABLE_STRATEGIES);
        if (repoCleaningRequired)
            // Cleaning checkouted repository
            this.business.cleanChekoutScmDirectory();

        // Persisting plugin data
        // Note that save() is made _after_ the synchronizeAllConfigs() because, otherwise, scm-sync-configuration.xml
        // file would be commited _before_ every other jenkins configuration file, which doesn't seem "natural"
        this.save();
    }

    @SuppressWarnings("deprecation")
    private User getCurrentUser() {
        return Hudson.getInstance().getMe();
    }

    public ScmSyncStrategy getStrategyForSaveable(final Saveable s, final File f) {
        for (ScmSyncStrategy strat : AVAILABLE_STRATEGIES)
            if (strat.isSaveableApplicable(s, f))
                return strat;
        // Strategy not found !
        return null;
    }

    private ScmContext createScmContext() {
        return new ScmContext(this.scm, this.scmRepositoryUrl, this.commitMessagePattern);
    }

    public boolean isNoUserCommitMessage() {
        return noUserCommitMessage;
    }

    public SCM[] getScms() {
        return SCM.values();
    }

    public void setBusiness(ScmSyncConfigurationBusiness business) {
        this.business = business;
    }

    public String getScmRepositoryUrl() {
        return scmRepositoryUrl;
    }

    public SCM getSCM() {
        return this.scm;
    }

    public boolean isDisplayStatus() {
        return displayStatus;
    }

    public String getCommitMessagePattern() {
        return commitMessagePattern;
    }

    public void startThreadedTransaction() {
        this.setTransaction(new ThreadedTransaction(synchronousTransactions));
    }

    public void commitChangeset(ChangeSet changeset) {
        try {
            return changeset.isEmpty() ?
                    null :
                    (latestCommitFuture =
                            this.business.queueChangeSet(
                                    createScmContext(),
                                    changeset,
                                    getCurrentUser(),
                                    ScmSyncConfigurationDataProvider.retrieveComment()));
        } finally {
            // Reinitializing transaction once commited
            this.setTransaction(null);
        }
    }

    public ScmTransaction getTransaction() {
        if (transaction.get() == null)
            setTransaction(new AtomicTransaction(synchronousTransactions));
        return transaction.get();
    }

    private void setTransaction(ScmTransaction transactionToRegister) {
        if (transaction.get() != null && transactionToRegister != null)
            LOGGER.warning("Existing threaded transaction will be overriden !");
        transaction.set(transactionToRegister);
    }

    public Future<Void> getLatestCommitFuture() {
        return latestCommitFuture;
    }
}
