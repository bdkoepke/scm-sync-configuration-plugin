package hudson.plugins.scm_sync_configuration.scms;

import hudson.scm.SubversionSCM;
import hudson.scm.SubversionSCM.DescriptorImpl.Credential;
import org.kohsuke.stapler.StaplerRequest;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.*;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Level;

public class ScmSyncSubversionSCM extends SCM {
    private static final String SCM_URL_PREFIX = "scm:svn:";

    ScmSyncSubversionSCM() {
        super("Subversion", "svn/config.jelly", "hudson.scm.SubversionSCM", "/hudson/plugins/scm_sync_configuration/ScmSyncConfigurationPlugin/scms/svn/url-help.jelly");
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    @SuppressWarnings("SameReturnValue")
    private static <T> T logNull(Level l, String format, Object... o) {
        LOGGER.log(l, String.format(format, o));
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T logIfNull(T x, Object... o) {
        return x == null ? (T) logNull(Level.SEVERE, "No credentials are stored in Hudson for realm [%s] !", o) : x;
    }

    @Override
    public String createScmUrlFromRequest(final StaplerRequest req) {
        String repoURL = req.getParameter("repositoryUrl");
        return repoURL == null ? null : SCM_URL_PREFIX + repoURL;
    }

    @Override
    public String extractScmUrlFrom(final String scmUrl) {
        return scmUrl.substring(SCM_URL_PREFIX.length());
    }

    @Override
    public SCMCredentialConfiguration extractScmCredentials(final String scmUrl) {
        LOGGER.info("Extracting SVN Credentials for url : " + scmUrl);
        final String realm = retrieveRealmFor(scmUrl);
        if (realm == null)
            return logNull(Level.WARNING,
                    "No credential (realm) found for url [%s] : it seems that you should enter your credentials in the UI at " +
                            "<a target='_blank' href='../../scm/SubversionSCM/enterCredential?%s'>this url</a>",
                    scmUrl, scmUrl);
        LOGGER.fine(String.format("Extracted realm from %s is [%s]", scmUrl, realm));
        final SubversionSCM.DescriptorImpl subversionDescriptor = cast(getSCMDescriptor());
        try {
            final Credential credential = getCredential(realm, subversionDescriptor);
            final String kind = scmUrl.startsWith("svn+ssh") ?
                    ISVNAuthenticationManager.SSH :
                    ISVNAuthenticationManager.PASSWORD;
            return credential == null ? null : valueOf(credential.createSVNAuthentication(kind));
        } catch (SecurityException | NoSuchFieldException e) {
            LOGGER.log(Level.SEVERE, "'credentials' field not readable on SubversionSCM.DescriptorImpl !");
        } catch (IllegalArgumentException | IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, String.format("'credentials' field not accessible on %s !", String.valueOf(subversionDescriptor)));
        } catch (SVNException e) {
            LOGGER.log(Level.WARNING, String.format("Error creating SVN authentication from realm [%s] !", realm), e);
        }
        return null;
    }

    private Credential getCredential(String realm, SubversionSCM.DescriptorImpl subversionDescriptor) throws NoSuchFieldException, IllegalAccessException {
        final Field credentialField = SubversionSCM.DescriptorImpl.class.getDeclaredField("credentials");
        credentialField.setAccessible(true);
        final Map<String, Credential> credentials = cast(credentialField.get(subversionDescriptor));
        return logIfNull(credentials.get(realm), realm);
    }

    @SuppressWarnings("deprecation")
    private String retrieveRealmFor(String scmURL) {
        try {
            final String[] realms = new String[]{null};
            final SVNRepository repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(scmURL));
            repository.setTunnelProvider(SVNWCUtil.createDefaultOptions(true));
            repository.setAuthenticationManager(createDefaultSVNAuthenticationManager(realms));
            repository.testConnection();
            return realms[0];
        } catch (SVNException e) {
            LOGGER.throwing(SVNRepositoryFactory.class.getName(), "create", e);
            return logNull(Level.SEVERE, "Error while creating SVNRepository : %s", e.getMessage());
        }
    }

    private static DefaultSVNAuthenticationManager createDefaultSVNAuthenticationManager(final String[] realms) {
        return new DefaultSVNAuthenticationManager(SVNWCUtil.getDefaultConfigurationDirectory(), true, "", "", null, "") {
            @Override
            public SVNAuthentication getFirstAuthentication(String kind, String realm, SVNURL url) throws SVNException {
                realms[0] = realm;
                return super.getFirstAuthentication(kind, realm, url);
            }

            @Override
            public SVNAuthentication getNextAuthentication(String kind, String realm, SVNURL url) throws SVNException {
                realms[0] = realm;
                return super.getNextAuthentication(kind, realm, url);
            }

            @Override
            public void acknowledgeAuthentication(boolean accepted, String kind, String realm, SVNErrorMessage errorMessage, SVNAuthentication authentication) throws SVNException {
                realms[0] = realm;
                super.acknowledgeAuthentication(accepted, kind, realm, errorMessage, authentication);
            }
        };
    }

    /**
     * Ugly method to convert a SVN authentication into a SCMCredentialConfiguration
     */
    private static SCMCredentialConfiguration valueOf(SVNAuthentication auth) {
        if (auth instanceof SVNPasswordAuthentication) {
            SVNPasswordAuthentication passAuth = cast(auth);
            return new SCMCredentialConfiguration(passAuth.getUserName(), passAuth.getPassword());
        }
        if (auth instanceof SVNSSHAuthentication) {
            SVNSSHAuthentication sshAuth = cast(auth);
            return new SCMCredentialConfiguration(sshAuth.getUserName(), sshAuth.getPassword(), sshAuth.getPassphrase(), sshAuth.getPrivateKey());
        }
        if (auth instanceof SVNSSLAuthentication) {
            SVNSSLAuthentication sslAuth = cast(auth);
            return new SCMCredentialConfiguration(sslAuth.getUserName(), sslAuth.getPassword());
        }
        if (auth instanceof SVNUserNameAuthentication) {
            SVNUserNameAuthentication unameAuth = cast(auth);
            return new SCMCredentialConfiguration(unameAuth.getUserName());
        }
        LOGGER.severe("Unsupported SVNAuthentication method specified.");
        return null;
    }
}
