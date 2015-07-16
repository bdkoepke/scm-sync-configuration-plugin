package hudson.plugins.scm_sync_configuration.strategies.model;

import java.util.regex.Pattern;

public class PageMatcher {
    private final Pattern urlRegex;

    public PageMatcher(final String urlRegexStr) {
        this.urlRegex = Pattern.compile(urlRegexStr);
    }

    public Pattern getUrlRegex() {
        return urlRegex;
    }
}
