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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.TaskListener;
import hudson.scm.SubversionSCM;
import hudson.util.StreamTaskListener;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Random;
import jenkins.plugins.git.GitRefSCMHead;
import jenkins.plugins.git.GitRefSCMRevision;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.GitSampleRepoRule;
import jenkins.plugins.git.junit.jupiter.WithGitSampleRepo;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.impl.SingleSCMSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mockito;

@WithJenkins
@WithGitSampleRepo
class IgnoreCommitterStrategyTest {

    private static JenkinsRule j;

    private static GitSampleRepoRule sampleRepo;

    private static String branchName;
    private static String commit1, commit2;

    private static final String KNOWN_AUTHOR = "gits@mplereporule";

    private final Random picker = new Random();

    private IgnoreCommitterStrategy strategy;
    private SCMSource source;
    private SCMSourceOwner owner;
    private GitRefSCMHead head;
    private SCMRevision current, previous, lastSeen;
    private TaskListener listener;
    private ByteArrayOutputStream baos;

    @BeforeAll
    static void setUp(JenkinsRule rule, GitSampleRepoRule repo) throws Exception {
        j = rule;
        sampleRepo = repo;

        branchName = "is-automatic-build-test-branch";
        sampleRepo.init();
        commit1 = sampleRepo.head();
        sampleRepo.git("checkout", "-b", branchName);
        sampleRepo.write("file", "modified-file");
        sampleRepo.git("commit", "--all", "--message=commit-to-branch-" + branchName);
        commit2 = sampleRepo.head();
    }

    @BeforeEach
    void setUp() {
        source = new GitSCMSource(sampleRepo.toString());
        owner = Mockito.mock(FakeSCMSourceOwner.class);
        source.setOwner(owner);
        head = new GitRefSCMHead(branchName);
        current = new GitRefSCMRevision(head, commit2);
        previous = new GitRefSCMRevision(head, commit1);
        lastSeen = previous;
        baos = new ByteArrayOutputStream();
        listener = new StreamTaskListener(baos, Charset.defaultCharset());
    }

    private String getKnownAuthor() {
        String[] knownAuthors = {
            KNOWN_AUTHOR,
            "other@example.com," + KNOWN_AUTHOR,
            KNOWN_AUTHOR + ",not-other@example.com",
            "other@example.com," + KNOWN_AUTHOR + ",not-other@example.com"
        };
        return knownAuthors[picker.nextInt(knownAuthors.length)];
    }

    private String getUnknownAuthor() {
        String unknown = "unknown@example.com";
        String[] unknownAuthors = {
            unknown,
            "other@example.com," + unknown,
            unknown + ",not-other@example.com",
            "other@example.com," + unknown + ",not-other@example.com"
        };
        return unknownAuthors[picker.nextInt(unknownAuthors.length)];
    }

    @Test
    void testIsAutomaticBuildEmptyIgnoredAuthorsTrue() {
        strategy = new IgnoreCommitterStrategy("", true);
        boolean result = strategy.isAutomaticBuild(source, head, current, previous, lastSeen, listener);
        String msg = "Changeset contains non ignored author " + KNOWN_AUTHOR;
        assertThat(baos.toString(Charset.defaultCharset()), containsString(msg));
        assertTrue(result);
    }

    @Test
    void testIsAutomaticBuildEmptyIgnoredAuthorsFalse() {
        strategy = new IgnoreCommitterStrategy("", false);
        boolean result = strategy.isAutomaticBuild(source, head, current, previous, lastSeen, listener);
        String msg = "All commits in the changeset are made by non-excluded authors, build is true";
        assertThat(baos.toString(Charset.defaultCharset()), containsString(msg));
        assertTrue(result);
    }

    @Test
    void testIsAutomaticBuildValidIgnoredAuthorTrue() {
        strategy = new IgnoreCommitterStrategy(getKnownAuthor(), true);
        boolean result = strategy.isAutomaticBuild(source, head, current, previous, lastSeen, listener);
        String msg = "All commits in the changeset are made by excluded authors, build is false";
        assertThat(baos.toString(Charset.defaultCharset()), containsString(msg));
        assertFalse(result);
    }

    @Test
    void testIsAutomaticBuildValidIgnoredAuthorFalse() {
        strategy = new IgnoreCommitterStrategy(getKnownAuthor(), false);
        boolean result = strategy.isAutomaticBuild(source, head, current, previous, lastSeen, listener);
        String msg = "Changeset contains ignored author " + KNOWN_AUTHOR;
        assertThat(baos.toString(Charset.defaultCharset()), containsString(msg));
        String msg2 = "allowBuildIfNotExcludedAuthor is false, therefore build is not required";
        assertThat(baos.toString(Charset.defaultCharset()), containsString(msg2));
        assertFalse(result);
    }

    @Test
    void testIsAutomaticBuildValidIgnoredAuthorNullRevisionTrue() {
        strategy = new IgnoreCommitterStrategy(getKnownAuthor(), true);
        boolean result = strategy.isAutomaticBuild(source, head, current, previous, null, listener);
        String msg = "All commits in the changeset are made by excluded authors, build is false";
        assertThat(baos.toString(Charset.defaultCharset()), containsString(msg));
        assertFalse(result);
    }

    @Test
    void testIsAutomaticBuildValidIgnoredAuthorNullRevisionFalse() {
        strategy = new IgnoreCommitterStrategy(getKnownAuthor(), false);
        boolean result = strategy.isAutomaticBuild(source, head, current, previous, null, listener);
        String msg = "Changeset contains ignored author " + KNOWN_AUTHOR;
        assertThat(baos.toString(Charset.defaultCharset()), containsString(msg));
        String msg2 = "allowBuildIfNotExcludedAuthor is false, therefore build is not required";
        assertThat(baos.toString(Charset.defaultCharset()), containsString(msg2));
        assertFalse(result);
    }

    @Test
    void testSCMRevisionNotGitRefSCMRevision() {
        strategy = new IgnoreCommitterStrategy(getKnownAuthor(), false);
        MySCMRevision myCurrent = new MySCMRevision(current.getHead(), commit2);
        MySCMRevision myPrevious = new MySCMRevision(current.getHead(), commit1);
        boolean result = strategy.isAutomaticBuild(source, head, myCurrent, myPrevious, myPrevious, listener);
        assertThat(
                baos.toString(Charset.defaultCharset()),
                containsString("Changeset contains ignored author " + KNOWN_AUTHOR));
        assertFalse(result);
    }

    @Test
    void testSCMRevisionNotGitRefSCMRevisionAllowBuildsIfExcludedAuthor() {
        strategy = new IgnoreCommitterStrategy(getKnownAuthor(), true);
        MySCMRevision myCurrent = new MySCMRevision(current.getHead(), commit2);
        MySCMRevision myPrevious = new MySCMRevision(current.getHead(), commit1);
        boolean result = strategy.isAutomaticBuild(source, head, myCurrent, myPrevious, myPrevious, listener);
        String msg = "All commits in the changeset are made by excluded authors, build is false";
        assertThat(baos.toString(Charset.defaultCharset()), containsString(msg));
        assertFalse(result);
    }

    @Test
    void testSCMRevisionNotGitRefSCMRevisionNoExcludingAuthorTrue() {
        strategy = new IgnoreCommitterStrategy(getUnknownAuthor(), true);
        MySCMRevision myCurrent = new MySCMRevision(current.getHead(), commit2);
        MySCMRevision myPrevious = new MySCMRevision(current.getHead(), commit1);
        boolean result = strategy.isAutomaticBuild(source, head, myCurrent, myPrevious, myPrevious, listener);
        String msg = "Changeset contains non ignored author " + KNOWN_AUTHOR;
        assertThat(baos.toString(Charset.defaultCharset()), containsString(msg));
        assertTrue(result);
    }

    @Test
    void testSCMRevisionNotGitRefSCMRevisionNoExcludingAuthorFalse() {
        strategy = new IgnoreCommitterStrategy(getUnknownAuthor(), false);
        MySCMRevision myCurrent = new MySCMRevision(current.getHead(), commit2);
        MySCMRevision myPrevious = new MySCMRevision(current.getHead(), commit1);
        boolean result = strategy.isAutomaticBuild(source, head, myCurrent, myPrevious, myPrevious, listener);
        String msg = "All commits in the changeset are made by non-excluded authors, build is true";
        assertThat(baos.toString(Charset.defaultCharset()), containsString(msg));
        assertTrue(result);
    }

    @Test
    void testSCMRevisionNotGitRefSCMRevisionAndInvalidHash() {
        strategy = new IgnoreCommitterStrategy(getKnownAuthor(), false);
        MySCMRevision myCurrent = new MySCMRevision(current.getHead(), "0000" + commit1);
        boolean result = strategy.isAutomaticBuild(source, head, myCurrent, myCurrent, myCurrent, listener);
        String msg = "All commits in the changeset are made by non-excluded authors, build is true";
        assertThat(baos.toString(Charset.defaultCharset()), containsString(msg));
        assertTrue(result);
    }

    @Test
    void testSCMRevisionNotGitRefSCMRevisionAndTooShort() {
        strategy = new IgnoreCommitterStrategy(getKnownAuthor(), false);
        MySCMRevision myCurrent = new MySCMRevision(current.getHead(), "deed"); // Valid SHA1 but too short
        boolean result = strategy.isAutomaticBuild(source, head, myCurrent, myCurrent, myCurrent, listener);
        String msg = "ERROR: Exception: java.lang.StringIndexOutOfBoundsException";
        assertThat(baos.toString(Charset.defaultCharset()), containsString(msg));
        assertTrue(result);
    }

    private static class MySCMRevision extends SCMRevision {

        private final String hash;

        public MySCMRevision(SCMHead head, String hash) {
            super(head);
            this.hash = hash;
        }

        public String getHash() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            MySCMRevision that = (MySCMRevision) o;

            return Objects.equals(hash, that.hash) && Objects.equals(getHead(), that.getHead());
        }

        @Override
        public int hashCode() {
            return Objects.hash(hash, getHead());
        }

        @Override
        public String toString() {
            return hash;
        }
    }

    private abstract static class FakeSCMSourceOwner implements SCMSourceOwner {}

    // Incorrect value test case - null owner
    @Test
    void testNullOwner() {
        strategy = new IgnoreCommitterStrategy(getKnownAuthor(), false);
        source.setOwner(null);
        boolean result = strategy.isAutomaticBuild(source, head, current, previous, lastSeen, listener);
        assertThat(baos.toString(Charset.defaultCharset()), startsWith("ERROR: Error retrieving SCMSourceOwner"));
        assertTrue(result);
    }

    // Unsupported SCMSource test case, cannot retrieve SCMFileSystem
    @Test
    void testUnsupportedSCMSource() {
        strategy = new IgnoreCommitterStrategy(getKnownAuthor(), false);
        SubversionSCM scm = new SubversionSCM("http://svn.apache.org/repos/asf/xml/trunk");
        SCMSource unsupportedSource = new SingleSCMSource("Subversion", scm);
        unsupportedSource.setOwner(source.getOwner());
        boolean result = strategy.isAutomaticBuild(unsupportedSource, head, current, previous, lastSeen, listener);
        assertThat(baos.toString(Charset.defaultCharset()), startsWith("ERROR: Error retrieving SCMFileSystem"));
        assertTrue(result);
    }
}
