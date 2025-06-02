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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class IgnoreCommitterStrategySimpleTest {

    private String ignoredAuthors = "";
    private boolean allowBuildIfNotExcludedAuthor = false;
    private IgnoreCommitterStrategy strategy =
            new IgnoreCommitterStrategy(ignoredAuthors, allowBuildIfNotExcludedAuthor);

    @Test
    void testGetIgnoredAuthors() {
        assertThat(strategy.getIgnoredAuthors(), is(ignoredAuthors));
    }

    @Test
    void testGetAllowBuildIfNotExcludedAuthor() {
        assertThat(strategy.getAllowBuildIfNotExcludedAuthor(), is(allowBuildIfNotExcludedAuthor));
    }

    @Test
    void testGetIgnoredAuthorsNonEmpty() {
        ignoredAuthors = "ignored@example.com";
        strategy = new IgnoreCommitterStrategy(ignoredAuthors, allowBuildIfNotExcludedAuthor);
        assertThat(strategy.getIgnoredAuthors(), is(ignoredAuthors));
    }

    @Test
    void testGetAllowBuildIfNotExcludedAuthorNegation() {
        allowBuildIfNotExcludedAuthor = !allowBuildIfNotExcludedAuthor;
        strategy = new IgnoreCommitterStrategy(ignoredAuthors, allowBuildIfNotExcludedAuthor);
        assertThat(strategy.getAllowBuildIfNotExcludedAuthor(), is(allowBuildIfNotExcludedAuthor));
    }
}
