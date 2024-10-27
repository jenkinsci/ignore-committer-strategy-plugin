/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Versent
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package au.com.versent.jenkins.plugins.ignoreCommitterStrategy;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.TaskListener;
import hudson.plugins.git.GitChangeLogParser;
import hudson.plugins.git.GitChangeSet;
import hudson.scm.SCM;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import jenkins.branch.BranchBuildStrategy;
import jenkins.branch.BranchBuildStrategyDescriptor;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.plugins.git.GitSCMFileSystem;
import jenkins.scm.api.*;
import org.kohsuke.stapler.DataBoundConstructor;

public class IgnoreCommitterStrategy extends BranchBuildStrategy {
    private final String ignoredAuthors;
    private final Boolean allowBuildIfNotExcludedAuthor;

    @DataBoundConstructor
    public IgnoreCommitterStrategy(String ignoredAuthors, Boolean allowBuildIfNotExcludedAuthor) {
        this.ignoredAuthors = ignoredAuthors;
        this.allowBuildIfNotExcludedAuthor = allowBuildIfNotExcludedAuthor;
    }

    /**
     * Get comma-separated list of ignored commit authors
     *
     * @return comma separated list of ignored authors
     */
    public String getIgnoredAuthors() {
        return ignoredAuthors;
    }

    /**
     * Determine if build is allowed if at least one author in the changeset is not excluded
     * @return indicates if build should be triggered if one of the authors is not in the exclude list
     */
    public Boolean getAllowBuildIfNotExcludedAuthor() {
        return allowBuildIfNotExcludedAuthor;
    }

    /**
     * Determine if build is required by checking if any of the commit authors is in the ignore list
     * and/or if changesets with at least one non excluded author are allowed
     * <p>
     * {@inheritDoc}
     *
     * @return true if changeset does not have commits by ignored users or at least one user is not excluded and {allowBuildIfNotExcludedAuthor} is true
     */
    @Override
    public boolean isAutomaticBuild(
            @NonNull SCMSource source,
            @NonNull SCMHead head,
            @NonNull SCMRevision currRevision,
            @CheckForNull SCMRevision lastBuiltRevision,
            @CheckForNull SCMRevision lastSeenRevision,
            @NonNull TaskListener listener) {
        GitSCMFileSystem.Builder builder = new GitSCMFileSystem.BuilderImpl();

        try {
            SCM scm = source.build(head, currRevision);
            SCMSourceOwner owner = source.getOwner();

            if (owner == null) {
                listener.error("Error retrieving SCMSourceOwner");
                return true;
            }

            SCMFileSystem fileSystem;
            if (!(currRevision instanceof AbstractGitSCMSource.SCMRevisionImpl)) {
                fileSystem = builder.build(
                        source,
                        head,
                        new AbstractGitSCMSource.SCMRevisionImpl(
                                head, currRevision.toString().substring(0, 40)));
            } else {
                fileSystem = builder.build(owner, scm, currRevision);
            }

            if (fileSystem == null) {
                listener.error("Error retrieving SCMFileSystem");
                return true;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            if (lastBuiltRevision != null && !(lastBuiltRevision instanceof AbstractGitSCMSource.SCMRevisionImpl)) {
                fileSystem.changesSince(
                        new AbstractGitSCMSource.SCMRevisionImpl(
                                head, lastBuiltRevision.toString().substring(0, 40)),
                        out);
            } else {
                fileSystem.changesSince(lastBuiltRevision, out);
            }

            GitChangeLogParser parser = new GitChangeLogParser(true);

            List<GitChangeSet> logs = parser.parse(new ByteArrayInputStream(out.toByteArray()));
            List<String> ignoredAuthorsList = Arrays.stream(ignoredAuthors.split(","))
                    .map(e -> e.trim().toLowerCase())
                    .collect(Collectors.toList());

            listener.getLogger().printf("Ignored authors: %s%n", ignoredAuthorsList.toString());

            for (GitChangeSet log : logs) {
                String authorEmail = log.getAuthorEmail().trim().toLowerCase();
                Boolean isIgnoredAuthor = ignoredAuthorsList.contains(authorEmail);

                if (isIgnoredAuthor) {
                    if (!allowBuildIfNotExcludedAuthor) {
                        // if author is ignored and changesets with at least one non-excluded author are not allowed
                        listener.getLogger()
                                .printf(
                                        "Changeset contains ignored author %s (%s), and allowBuildIfNotExcludedAuthor is %s, therefore build is not required%n",
                                        authorEmail, log.getCommitId(), allowBuildIfNotExcludedAuthor);
                        return false;
                    }

                } else {
                    if (allowBuildIfNotExcludedAuthor) {
                        // if author is not ignored and changesets with at least one non-excluded author are allowed
                        listener.getLogger()
                                .printf(
                                        "Changeset contains non ignored author %s (%s) and allowIfNotExcluded is %s, build is required%n",
                                        authorEmail, log.getCommitId(), allowBuildIfNotExcludedAuthor);
                        return true;
                    }
                }
            }
            // here if commits are made by ignored authors and allowBuildIfNotExcludedAuthor is true, in this case
            // return false
            // or if all commits are made by non-ignored authors and allowBuildIfNotExcludedAuthor is false, in this
            // case return true
            listener.getLogger()
                    .printf(
                            "All commits in the changeset are made by %sexcluded authors, build is %s%n",
                            allowBuildIfNotExcludedAuthor ? "" : "non-", !allowBuildIfNotExcludedAuthor);

            return !allowBuildIfNotExcludedAuthor;
        } catch (Exception e) {
            listener.error("Exception: %s%n", e);
            return true;
        }
    }

    @Extension
    public static class DescriptorImpl extends BranchBuildStrategyDescriptor {
        public String getDisplayName() {
            return "Ignore Committer Strategy";
        }
    }
}
