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


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.TaskListener;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import jenkins.plugins.git.GitSCMFileSystem;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSourceOwner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IgnoreCommitterStrategy.class, GitSCMSource.class})
public class IgnoreCommitterStrategyTest {
    private SCMHead head;
    private GitSCMSource source;
    private SCMRevisionImpl currRevision;
    private SCMRevisionImpl prevRevision;
    private SCMRevisionImpl lastSeenRevision;
    private List<String> ignoredAuthors = Arrays.asList("jenkins@example.com", "jenkins-ci@example.com");
    private List<String> nonIgnoredAuthors = Arrays.asList("hello@example.com", "john.galt@whois.com");
    private SCM scm;

    @Before
    public void setUp() {
        GitSCMSource sourceMock = PowerMockito.mock(GitSCMSource.class);

        this.head = new SCMHead("test-branch");
        this.source = sourceMock;
        this.currRevision = new SCMRevisionImpl(head, "222");
        this.prevRevision = new SCMRevisionImpl(head, "111");
        this.lastSeenRevision = new SCMRevisionImpl(head, "111");
        this.scm = new GitSCM("http://example.com.au");
    }

    @Test
    public void testIsAutomaticBuildReturnsTrueIfAllAuthorsAreNotIgnored() throws Exception {

        String commits = "";
        for (String author : nonIgnoredAuthors) {
            commits += getCommit(author);
        }

        assertTrue(setupIgnoreCommitterStrategy(commits));
    }

    @Test
    public void testIsAutomaticBuildReturnsTrueIfOneAuthorIsNotIgnoredAndAllowBuildIfNotExcludedAuthor() throws Exception {

        String commits = "";
        for (String author : ignoredAuthors) {
            commits += getCommit(author);
        }
        for (String author : nonIgnoredAuthors) {
            commits += getCommit(author);
        }

        assertTrue(setupIgnoreCommitterStrategy(commits));
    }

    @Test
    public void testIsAutomaticBuildReturnsFalseIfAllAuthorsAreIgnoredAndAllowBuildIfNotExcludedAuthor() throws Exception {

        String commits = "";
        for (String author : ignoredAuthors) {
            commits += getCommit(author);
        }

        assertFalse(setupIgnoreCommitterStrategy(commits));
    }

    @Test
    public void testIsAutomaticBuildReturnsFalseIfOneAuthorIsIgnored() throws Exception {

        String commits = "";

        for (String author : nonIgnoredAuthors) {
            commits += getCommit(author);
        }

        for (String author : ignoredAuthors) {
            commits += getCommit(author);
        }

        assertFalse(setupIgnoreCommitterStrategy(commits));
    }

    @Test
    public void testIsAutomaticBuildReturnsTrueIfCommitCantbeParsed() throws Exception {

        String commits = "";

        for (String author : nonIgnoredAuthors) {
            commits += getBrokenCommit(author);
        }

        assertTrue(setupIgnoreCommitterStrategy(commits));
    }

    private boolean setupIgnoreCommitterStrategy(String commits) throws Exception {
        // prepare mock GitSCMFileSystem to be returned by builderMock
        GitSCMFileSystem fileSystemMock = Mockito.mock(GitSCMFileSystem.class);
        // mock builderMock to build a mocked GitSCMFileSystem
        GitSCMFileSystem.BuilderImpl builderMock = Mockito.mock(GitSCMFileSystem.BuilderImpl.class);
        // mock ByteArrayOutputStream to return preset response
        ByteArrayOutputStream ByteArrayOutputStreamMock = Mockito.mock(ByteArrayOutputStream.class);
        // mock ownerMock
        SCMSourceOwner ownerMock = PowerMockito.mock(WorkflowMultiBranchProject.class);

        try {
            PowerMockito.when(source.build(head, currRevision)).thenReturn(scm);
            PowerMockito.when(source.getOwner()).thenReturn(ownerMock);

            // set returns for mocked methods
            Mockito.when(ByteArrayOutputStreamMock.toByteArray()).thenReturn(commits.getBytes());
            Mockito.when(builderMock.build(source.getOwner(), scm, currRevision)).thenReturn(fileSystemMock);
            Mockito.when(fileSystemMock.changesSince(prevRevision, ByteArrayOutputStreamMock)).thenReturn(true);

            // mock classes in the tested target class to return mocked  objects when initiated
            PowerMockito.whenNew(ByteArrayOutputStream.class).withNoArguments().thenReturn(ByteArrayOutputStreamMock);
            PowerMockito.whenNew(GitSCMFileSystem.BuilderImpl.class).withNoArguments().thenReturn(builderMock);

            IgnoreCommitterStrategy IgnoreCommitterStrategy = new IgnoreCommitterStrategy(
                    String.join(",", ignoredAuthors), false
            );
            return IgnoreCommitterStrategy.isAutomaticBuild(source, head, currRevision, prevRevision, lastSeenRevision, null);
        } catch (Exception e) {
            throw e;
        }
    }

    private String getCommit(String authorEmail) {
        List<String> lines = new ArrayList<String>();
        lines.add(String.format("commit %s", "1567861636cd854f4dd6fa40bf94c0c657681dd5"));
        lines.add(String.format("author John Galt<%s> 1363879004 +0100", authorEmail));
        lines.add("");
        lines.add("    [task] Updated version.");
        lines.add("    ");
        lines.add("    Including earlier updates.");
        lines.add("    ");
        lines.add("    Changes in this version:");
        lines.add("    - Changed to take the gerrit url from gerrit query command.");
        lines.add("    - Aligned reason information with our new commit hooks");
        lines.add("    ");
        lines.add("    Change-Id: Ife96d2abed5b066d9620034bec5f04cf74b8c66d");
        lines.add("    Reviewed-on: https://gerrit.e.se/12345");
        lines.add("    Tested-by: Jenkins <jenkins@no-mail.com>");
        lines.add("    Reviewed-by: Mister Another <mister.another@ericsson.com>");


        return String.join("\n", lines);
    }

    private String getBrokenCommit(String authorEmail) {
        List<String> lines = new ArrayList<String>();
        lines.add(String.format("commit %s", "1567861636cd854f4dd6fa40bf94c0c657681dd5"));
        lines.add(String.format("Authorzzz John Galt<%s> 1363879004 +0100", authorEmail));
        lines.add("");
        lines.add("    [task] Updated version.");
        lines.add("    ");
        lines.add("    Including earlier updates.");
        lines.add("    ");
        lines.add("    Changes in this version:");
        lines.add("    - Changed to take the gerrit url from gerrit query command.");
        lines.add("    - Aligned reason information with our new commit hooks");
        lines.add("    ");
        lines.add("    Change-Id: Ife96d2abed5b066d9620034bec5f04cf74b8c66d");
        lines.add("    Reviewed-on: https://gerrit.e.se/12345");
        lines.add("    Tested-by: Jenkins <jenkins@no-mail.com>");
        lines.add("    Reviewed-by: Mister Another <mister.another@ericsson.com>");


        return String.join("\n", lines);
    }
}
