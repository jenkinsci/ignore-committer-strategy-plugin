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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import jenkins.plugins.git.GitRefSCMHead;
import jenkins.plugins.git.GitRefSCMRevision;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.GitSampleRepoRule;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

public class IgnoreCommitterStrategyTest {

    @ClassRule
    public static JenkinsRule r = new JenkinsRule();

    @ClassRule
    public static GitSampleRepoRule sampleRepo = new GitSampleRepoRule();

    private IgnoreCommitterStrategy strategy;

    public IgnoreCommitterStrategyTest() {}

    private static String branchName;
    private static String commit1, commit2;

    @BeforeClass
    public static void createGitRepository() throws Exception {
        branchName = "is-automatic-build-test-branch";
        sampleRepo.init();
        commit1 = sampleRepo.head();
        sampleRepo.git("checkout", "-b", branchName);
        sampleRepo.write("file", "modified-file");
        sampleRepo.git("commit", "--all", "--message=commit-to-branch-" + branchName);
        commit2 = sampleRepo.head();
    }

    private SCMSource source;
    private SCMSourceOwner owner;
    private GitRefSCMHead head;
    private SCMRevision currRevision, prevRevision, lastSeenRevision;
    private TaskListener listener;
    private ByteArrayOutputStream baos;

    @Before
    public void createSCMSource() {
        source = new GitSCMSource(sampleRepo.toString());
        owner = Mockito.mock(FakeSCMSourceOwner.class);
        source.setOwner(owner);
        head = new GitRefSCMHead(branchName);
        currRevision = new GitRefSCMRevision(head, commit2);
        prevRevision = new GitRefSCMRevision(head, commit1);
        lastSeenRevision = prevRevision;
        baos = new ByteArrayOutputStream();
        listener = new StreamTaskListener(baos, Charset.defaultCharset());
    }

    @Test
    public void testIsAutomaticBuildEmptyIgnoredAuthors() {
        strategy = new IgnoreCommitterStrategy("", true);
        boolean result =
                strategy.isAutomaticBuild(source, head, currRevision, prevRevision, lastSeenRevision, listener);
        String msg = "Changeset contains non ignored author gits@mplereporule";
        assertThat(baos.toString(Charset.defaultCharset()), containsString(msg));
        assertTrue(result);
    }

    @Test
    public void testIsAutomaticBuildEmptyIgnoredAuthorsNoBuildIfExcluded() {
        strategy = new IgnoreCommitterStrategy("", false);
        boolean result =
                strategy.isAutomaticBuild(source, head, currRevision, prevRevision, lastSeenRevision, listener);
        String msg = "All commits in the changeset are made by Non excluded authors, build is true";
        assertThat(baos.toString(Charset.defaultCharset()), containsString(msg));
        assertTrue(result);
    }

    @Test
    public void testIsAutomaticBuildValidIgnoredAuthor() {
        strategy = new IgnoreCommitterStrategy("gits@mpleRepoRule", true); // Author from sampleRepoRule
        boolean result =
                strategy.isAutomaticBuild(source, head, currRevision, prevRevision, lastSeenRevision, listener);
        String msg = "Ignored authors: [gits@mplereporule]";
        assertThat(baos.toString(Charset.defaultCharset()), startsWith(msg));
        assertFalse(result);
    }

    @Test
    public void testIsAutomaticBuildValidIgnoredAuthors() {
        strategy = new IgnoreCommitterStrategy("gits@mpleRepoRule,ignore@example.com", true);
        boolean result =
                strategy.isAutomaticBuild(source, head, currRevision, prevRevision, lastSeenRevision, listener);
        String msg = "Ignored authors: [gits@mplereporule, ignore@example.com]";
        assertThat(baos.toString(Charset.defaultCharset()), startsWith(msg));
        assertFalse(result);
    }

    @Test
    public void testIsAutomaticBuildValidIgnoredAuthorsNoBuildIfExcluded() {
        strategy = new IgnoreCommitterStrategy("ignore@example.com,gits@mpleRepoRule", false);
        boolean result =
                strategy.isAutomaticBuild(source, head, currRevision, prevRevision, lastSeenRevision, listener);
        String msg = "Ignored authors: [ignore@example.com, gits@mplereporule]";
        String msg2 = "contains ignored author gits@mplereporule";
        assertThat(baos.toString(Charset.defaultCharset()), startsWith(msg));
        assertThat(baos.toString(Charset.defaultCharset()), containsString(msg2));
        assertFalse(result);
    }

    @Test
    public void testIsAutomaticBuildValidIgnoredAuthorNullRevision() {
        strategy = new IgnoreCommitterStrategy("gits@mpleRepoRule,other@example.com", true);
        boolean result = strategy.isAutomaticBuild(source, head, currRevision, prevRevision, null, listener);
        String msg = "Ignored authors: [gits@mplereporule, other@example.com]";
        assertThat(baos.toString(Charset.defaultCharset()), startsWith(msg));
    }

    @Test
    public void testIsAutomaticBuildValidIgnoredAuthorsNullRevision() {
        strategy = new IgnoreCommitterStrategy("gits@mpleRepoRule,ignore@example.com,other@example.com", true);
        boolean result = strategy.isAutomaticBuild(source, head, currRevision, null, lastSeenRevision, listener);
        String msg = "Ignored authors: [gits@mplereporule, ignore@example.com, other@example.com]";
        assertThat(baos.toString(Charset.defaultCharset()), startsWith(msg));
    }

    @Test
    public void testIsAutomaticBuildValidIgnoredAuthorsNullRevisions() {
        strategy = new IgnoreCommitterStrategy("gits@mpleRepoRule,ignore@example.com,other@example.com", true);
        boolean result = strategy.isAutomaticBuild(source, head, currRevision, null, null, listener);
        String msg = "Ignored authors: [gits@mplereporule, ignore@example.com, other@example.com]";
        assertThat(baos.toString(Charset.defaultCharset()), startsWith(msg));
    }

    @Test
    public void testIsAutomaticBuildValidIgnoredAuthorsNoBuildIfExcludedNullRevision() {
        strategy = new IgnoreCommitterStrategy("ignore@example.com,gits@mpleRepoRule", false);
        boolean result = strategy.isAutomaticBuild(source, head, null, prevRevision, lastSeenRevision, listener);
        String msg = "Ignored authors: [ignore@example.com, gits@mplereporule]";
        assertThat(baos.toString(Charset.defaultCharset()), startsWith(msg));
        assertThat(
                baos.toString(Charset.defaultCharset()),
                containsString("Changeset contains ignored author gits@mplereporule"));
    }

    private abstract static class FakeSCMSourceOwner implements SCMSourceOwner {}

    // Incorrect value test case - null owner
    @Test
    public void testNullOwner() {
        strategy = new IgnoreCommitterStrategy("gits@mpleRepoRule", false);
        source.setOwner(null);
        boolean result =
                strategy.isAutomaticBuild(source, head, currRevision, prevRevision, lastSeenRevision, listener);
        assertThat(baos.toString(Charset.defaultCharset()), startsWith("ERROR: Error retrieving SCMSourceOwner"));
        assertTrue(result);
    }
}
