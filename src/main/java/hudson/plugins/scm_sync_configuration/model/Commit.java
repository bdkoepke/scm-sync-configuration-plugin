package hudson.plugins.scm_sync_configuration.model;

import hudson.model.User;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author fcamblor
 *         Commit is an aggregation of a changeset with a commit message
 *         Note that commit message won't _always_ be the same as changeset.message since some additionnal contextual
 *         informations will be provided.
 */
public class Commit {
    private final String message;
    private final ChangeSet changeset;
    private final ScmContext scmContext;
    private final User author;

    public Commit(final ChangeSet changeset, final User author, final String userMessage, final ScmContext scmContext) {
        this.message = createCommitMessage(scmContext, changeset.getMessage(), author, userMessage);
        this.changeset = changeset;
        this.scmContext = scmContext;
        this.author = author;
    }

    private static String createCommitMessage(final ScmContext context, final String messagePrefix, final User user, final String userComment) {
        final StringBuilder commitMessage = new StringBuilder().append(messagePrefix);
        if (user != null)
            commitMessage.append(" by ").append(user.getId());
        if (userComment != null && !"".equals(userComment))
            commitMessage.append(" with following comment : ").append(userComment);
        final String message = commitMessage.toString();
        return context.getCommitMessagePattern() == null || "".equals(context.getCommitMessagePattern()) ?
                message :
                context.getCommitMessagePattern().replaceAll("\\[message\\]", message.replaceAll("\\$", "\\\\\\$"));
    }

    public String getMessage() {
        return message;
    }

    public ChangeSet getChangeset() {
        return changeset;
    }

    public ScmContext getScmContext() {
        return scmContext;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("Commit %s\n", super.toString())
                .append("  Author : %s\n", author)
                .append("  Comment : %s\n", message)
                .append("  Changeset : %s\n", changeset)
                .toString();
    }
}
