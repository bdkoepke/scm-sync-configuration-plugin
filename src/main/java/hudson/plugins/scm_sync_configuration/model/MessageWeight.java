package hudson.plugins.scm_sync_configuration.model;

/**
 * @author fcamblor
 *         Message weight should be used to prioritize messages into a Scm Transaction
 */
public enum MessageWeight {
    MINIMAL(0), NORMAL(1), IMPORTANT(2), MORE_IMPORTANT(3);

    private final int weight;

    MessageWeight(int weight) {
        this.weight = weight;
    }

    public boolean heavierThan(final MessageWeight ms) {
        return this.weight > ms.weight;
    }
}
