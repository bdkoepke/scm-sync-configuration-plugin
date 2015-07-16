package hudson.plugins.scm_sync_configuration.scms;

public class SCMCredentialConfiguration {
    private final String username;
    private final String password;
    private final String privateKey;
    private final String passphrase;

    public SCMCredentialConfiguration(final String username, final String password, final String passPhrase, final char[] privateKey) {
        this.username = username;
        this.password = password;
        this.passphrase = passPhrase;
        this.privateKey = privateKey == null ? null : String.valueOf(privateKey);
    }

    public SCMCredentialConfiguration(final String username, final String password) {
        this(username, password, null, null);
    }

    public SCMCredentialConfiguration(final String username) {
        this(username, null);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPassphrase() {
        return passphrase;
    }
}
