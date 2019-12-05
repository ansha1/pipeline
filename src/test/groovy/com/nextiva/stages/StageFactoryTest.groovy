package com.nextiva.stages

import com.nextiva.config.BranchingModelRegexps
import org.junit.Test
import utils.BranchNames

import java.util.regex.Pattern

import static org.assertj.core.api.Assertions.assertThat

class StageFactoryTest extends GroovyTestCase {

    private void regexpTester(Pattern pattern, Set<String> branchNamesMatch, Set<String> branchNamesNotMatch) {
        branchNamesMatch.forEach { branchName ->
            assertThat(branchName).describedAs("Checking $pattern").matches(pattern)
        }

        branchNamesNotMatch.forEach { branchName ->
            assertThat(branchName).describedAs("Checking $pattern").doesNotMatch(pattern)
        }
    }

    private void regexpTester(String regexp, Set<String> branchNamesMatch, Set<String> branchNamesNotMatch) {
        regexpTester(Pattern.compile(regexp), branchNamesMatch, branchNamesNotMatch)
    }

    @Test
    void testRegexpsAny() {
        def regexp = BranchingModelRegexps.any
        def branchNamesMatch = BranchNames.feature + BranchNames.release + BranchNames.develop + BranchNames.master
        Set<String> branchNamesNotMatch = []
        regexpTester(regexp, branchNamesMatch, branchNamesNotMatch)
    }

    @Test
    void testRegexpsNotMaster() {
        def regexp = BranchingModelRegexps.notMaster
        def branchNamesMatch = BranchNames.feature + BranchNames.release + BranchNames.develop
        def branchNamesNotMatch = BranchNames.master
        regexpTester(regexp, branchNamesMatch, branchNamesNotMatch)
    }

    @Test
    void testRegexpsReleaseOrHotfix() {
        def regexp = BranchingModelRegexps.releaseOrHotfix
        def branchNamesMatch = BranchNames.release
        def branchNamesNotMatch = BranchNames.feature + BranchNames.develop + BranchNames.master
        regexpTester(regexp, branchNamesMatch, branchNamesNotMatch)
    }

    @Test
    void testRegexpsMaster() {
        def regexp = BranchingModelRegexps.master
        def branchNamesMatch = BranchNames.master
        def branchNamesNotMatch = BranchNames.feature + BranchNames.release + BranchNames.develop
        regexpTester(regexp, branchNamesMatch, branchNamesNotMatch)
    }

    @Test
    void testRegexpsMainline() {
        def regexp = BranchingModelRegexps.mainline
        def branchNamesMatch = BranchNames.release + BranchNames.develop
        def branchNamesNotMatch = BranchNames.feature + BranchNames.master
        regexpTester(regexp, branchNamesMatch, branchNamesNotMatch)
    }

    @Test
    void testRegexpsMainlineWithMaster() {
        def regexp = BranchingModelRegexps.mainlineWithMaster
        def branchNamesMatch = BranchNames.release + BranchNames.develop + BranchNames.master
        def branchNamesNotMatch = BranchNames.feature
        regexpTester(regexp, branchNamesMatch, branchNamesNotMatch)
    }

    @Test
    void testRegexpsNotMainline() {
        def regexp = BranchingModelRegexps.notMainline
        def branchNamesMatch = BranchNames.feature
        def branchNamesNotMatch = BranchNames.release + BranchNames.develop + BranchNames.master
        regexpTester(regexp, branchNamesMatch, branchNamesNotMatch)
    }

    @Test
    void testRegexpsDevelop() {
        def regexp = BranchingModelRegexps.develop
        def branchNamesMatch = BranchNames.develop
        def branchNamesNotMatch = BranchNames.feature + BranchNames.release + BranchNames.master
        regexpTester(regexp, branchNamesMatch, branchNamesNotMatch)
    }
}
