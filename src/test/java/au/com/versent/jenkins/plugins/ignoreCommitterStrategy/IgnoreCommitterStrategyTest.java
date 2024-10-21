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

import hudson.model.TaskListener;
import jenkins.plugins.git.GitRefSCMHead;
import jenkins.plugins.git.GitRefSCMRevision;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.GitSampleRepoRule;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

    public IgnoreCommitterStrategyTest() {
    }

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

    @Before
    public void createSCMSource() {
        source = new GitSCMSource(sampleRepo.toString());
        owner = Mockito.mock(FakeSCMSourceOwner.class);
        source.setOwner(owner);
        head = new GitRefSCMHead(branchName);
        currRevision = new GitRefSCMRevision(head, commit2);
        prevRevision = new GitRefSCMRevision(head, commit1);
        lastSeenRevision = new GitRefSCMRevision(head, commit1);
        listener = TaskListener.NULL;
    }

    @Test
    public void testIsAutomaticBuildEmptyIgnoredAuthors() throws Throwable {
        strategy = new IgnoreCommitterStrategy("", true);
        assertTrue(strategy.isAutomaticBuild(source, head, currRevision, prevRevision, lastSeenRevision, listener));
    }

    @Test
    public void testIsAutomaticBuildEmptyIgnoredAuthorsNoBuildIfExcluded() throws Throwable {
        strategy = new IgnoreCommitterStrategy("", false);
        assertTrue(strategy.isAutomaticBuild(source, head, currRevision, prevRevision, lastSeenRevision, listener));
    }

    @Test
    public void testIsAutomaticBuildValidIgnoredAuthor() throws Throwable {
        strategy = new IgnoreCommitterStrategy("gits@mpleRepoRule", true); // Author from sampleRepoRule
        assertFalse(strategy.isAutomaticBuild(source, head, currRevision, prevRevision, lastSeenRevision, listener));
    }

    @Test
    public void testIsAutomaticBuildValidIgnoredAuthors() throws Throwable {
        strategy = new IgnoreCommitterStrategy("gits@mpleRepoRule,ignore@example.com", true); // Author from sampleRepoRule
        assertFalse(strategy.isAutomaticBuild(source, head, currRevision, prevRevision, lastSeenRevision, listener));
    }

    @Test
    public void testIsAutomaticBuildValidIgnoredAuthorsNoBuildIfExcluded() throws Throwable {
        strategy = new IgnoreCommitterStrategy("ignore@example.com,gits@mpleRepoRule", false); // Author from sampleRepoRule
        assertFalse(strategy.isAutomaticBuild(source, head, currRevision, prevRevision, lastSeenRevision, listener));
    }

    private static abstract class FakeSCMSourceOwner implements SCMSourceOwner {
    }

    // Incorrect value test case - null owner
    @Test
    public void testNullOwner() throws Throwable {
        strategy = new IgnoreCommitterStrategy("gits@mpleRepoRule", false);
        source.setOwner(null);
        assertTrue(strategy.isAutomaticBuild(source, head, currRevision, prevRevision, lastSeenRevision, listener));
    }
}
