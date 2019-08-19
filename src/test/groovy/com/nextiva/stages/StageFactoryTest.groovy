package com.nextiva.stages

import org.junit.Test
import utils.BranchNames

import java.util.regex.Pattern

import static com.nextiva.stages.StageFactory.branchingModelRegexps
import static org.assertj.core.api.Assertions.assertThat

class StageFactoryTest extends GroovyTestCase {

    private void regexpTester(String regexp, Set<String> branchNamesMatch, Set<String> branchNamesNotMatch) {
        def pattern = Pattern.compile(regexp)

        branchNamesMatch.forEach { branchName ->
            assertThat(branchName).describedAs("Checking $pattern").matches(pattern)
        }

        branchNamesNotMatch.forEach { branchName ->
            assertThat(branchName).describedAs("Checking $pattern").doesNotMatch(pattern)
        }
    }

    @Test
    void testRegexpsAny() {
        def regexp = branchingModelRegexps.any
        def branchNamesMatch = BranchNames.feature + BranchNames.release + BranchNames.develop + BranchNames.master
        Set<String> branchNamesNotMatch = []
        regexpTester(regexp, branchNamesMatch, branchNamesNotMatch)
    }

    @Test
    void testRegexpsNotMaster() {
        def regexp = branchingModelRegexps.notMaster
        def branchNamesMatch = BranchNames.feature + BranchNames.release + BranchNames.develop
        def branchNamesNotMatch = BranchNames.master
        regexpTester(regexp, branchNamesMatch, branchNamesNotMatch)
    }

    @Test
    void testRegexpsReleaseOrHotfix() {
        def regexp = branchingModelRegexps.releaseOrHotfix
        def branchNamesMatch = BranchNames.release
        def branchNamesNotMatch = BranchNames.feature + BranchNames.develop + BranchNames.master
        regexpTester(regexp, branchNamesMatch, branchNamesNotMatch)
    }

    @Test
    void testRegexpsMaster() {
        def regexp = branchingModelRegexps.master
        def branchNamesMatch = BranchNames.master
        def branchNamesNotMatch = BranchNames.feature + BranchNames.release + BranchNames.develop
        regexpTester(regexp, branchNamesMatch, branchNamesNotMatch)
    }

    @Test
    void testRegexpsMainline() {
        def regexp = branchingModelRegexps.mainline
        def branchNamesMatch = BranchNames.release + BranchNames.develop
        def branchNamesNotMatch = BranchNames.feature + BranchNames.master
        regexpTester(regexp, branchNamesMatch, branchNamesNotMatch)
    }

    @Test
    void testRegexpsMainlineWithMaster() {
        def regexp = branchingModelRegexps.mainlineWithMaster
        def branchNamesMatch = BranchNames.release + BranchNames.develop + BranchNames.master
        def branchNamesNotMatch = BranchNames.feature
        regexpTester(regexp, branchNamesMatch, branchNamesNotMatch)
    }

    @Test
    void testRegexpsNotMainline() {
        def regexp = branchingModelRegexps.notMainline
        def branchNamesMatch = BranchNames.feature
        def branchNamesNotMatch = BranchNames.release + BranchNames.develop + BranchNames.master
        regexpTester(regexp, branchNamesMatch, branchNamesNotMatch)
    }

    @Test
    void testRegexpsDevelop() {
        def regexp = branchingModelRegexps.develop
        def branchNamesMatch = BranchNames.develop
        def branchNamesNotMatch = BranchNames.feature + BranchNames.release + BranchNames.master
        regexpTester(regexp, branchNamesMatch, branchNamesNotMatch)
    }
}
