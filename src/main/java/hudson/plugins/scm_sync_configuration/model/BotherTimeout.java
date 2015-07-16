package hudson.plugins.scm_sync_configuration.model;

import org.apache.commons.lang.builder.HashCodeBuilder;

public interface BotherTimeout {
    boolean matchesUrl(final String currentUrl);

    // TODO: Java8 will allow extension methods on interfaces.
    class FACTORY {
        public static BotherTimeout createBotherTimeout(final String type, final String currentURL) {
            switch (type) {
                case "thisConfig":
                    return new CurrentConfig(currentURL);
                case "anyConfigs":
                    return AnyConfig;
                default:
                    throw new IllegalArgumentException(String.format("Invalid bother timeout type : %s", type));
            }
        }

        private static final BotherTimeout AnyConfig = new BotherTimeout() {
            @Override
            public boolean matchesUrl(final String currentUrl) {
                return true;
            }

            @Override
            public boolean equals(final Object that) {
                return this == that;
            }

            @Override
            public int hashCode() {
                return new HashCodeBuilder(17, 23).toHashCode();
            }
        };

        private static class CurrentConfig implements BotherTimeout {
            private final String currentURL;

            private CurrentConfig(String currentURL) {
                this.currentURL = currentURL;
            }

            public String getCurrentURL() {
                return currentURL;
            }

            @Override
            public boolean matchesUrl(final String currentURL) {
                return currentURL != null && currentURL.equals(this.currentURL);
            }

            @Override
            public boolean equals(final Object that) {
                return that != null && that instanceof CurrentConfig && currentURL.equals(((CurrentConfig) that).getCurrentURL());
            }

            @Override
            public int hashCode() {
                return new HashCodeBuilder(13, 17).append(currentURL).toHashCode();
            }
        }
    }
}
