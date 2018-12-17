package com.nextiva

import com.nextiva.SemanticVersion
import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import utils.Mocks
import utils.Validator

import static org.junit.Assert.assertEquals


class SemanticVersionTest extends BasePipelineTest implements Validator, Mocks {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    def getJavaClassObject() {
        def baseVersion = "1.1.1-alpha.x.rc1+23"
        return new SemanticVersion(baseVersion)
    }

    @Test
    void check_constructor_string_parsing_full_version() {
        def version = getJavaClassObject()
        assertEquals("23", version.getMeta())
        assertEquals("alpha.x.rc1", version.getPreRelease())
        assertEquals("1.1.1", version.getVersion().toString())
        assertEquals("1.1.1-alpha.x.rc1+23", version.toString())
    }

    @Test
    void check_constructor_string_parsing_meta_version() {
        def version = new SemanticVersion('1.1.1+23')
        assertEquals("23", version.getMeta())
        assertEquals("1.1.1", version.getVersion().toString())
        assertEquals("1.1.1+23", version.toString())
    }

    @Test
    void check_constructor_string_parsing_prerelease_version() {
        def version = new SemanticVersion('1.1.1-prerelease.1')
        assertEquals("prerelease.1", version.getPreRelease())
        assertEquals("1.1.1", version.getVersion().toString())
        assertEquals("1.1.1-prerelease.1", version.toString())
    }

    @Test
    void check_constructor_string_parts_parsing_version() {
        def version = new SemanticVersion('1.4', 'beta.11.rc2', '39')
        assertEquals("39", version.getMeta())
        assertEquals("beta.11.rc2", version.getPreRelease())
        assertEquals("1.4.0", version.getVersion().toString())
        assertEquals("1.4.0-beta.11.rc2+39", version.toString())
    }

    @Test
    void check_constructor_string_parts_no_meta() {
        def version = new SemanticVersion('7', 'rc1')
        assertEquals("rc1", version.getPreRelease())
        assertEquals("7.0.0", version.getVersion().toString())
        assertEquals("7.0.0-rc1", version.toString())
    }

    @Test
    void check_constructor_using_integer_version() {
        def version = new SemanticVersion(1, 1, 1, 'alpha.x.rc1', '23')
        assertEquals("23", version.getMeta())
        assertEquals("alpha.x.rc1", version.getPreRelease())
        assertEquals("1.1.1", version.getVersion().toString())
        assertEquals("1.1.1-alpha.x.rc1+23", version.toString())
    }

    @Test
    void check_constructor_using_integer_only() {
        def version = new SemanticVersion(3, 2)
        assertEquals("", version.getMeta())
        assertEquals("", version.getPreRelease())
        assertEquals("3.2.0", version.getVersion().toString())
        assertEquals("3.2.0", version.toString())
    }

    @Test
    void check_bump_major_method() {
        def version = getJavaClassObject()
        def nextOne = version.bumpMajor()
        def nextFive = version.bumpMajor(5)
        assertEquals("2.0.0", nextOne.getVersion().toString())
        assertEquals(2, nextOne.getVersion().getMajor())
        assertEquals(2, nextOne.getMajor())
        assertEquals("6.0.0", nextFive.getVersion().toString())
        assertEquals(6, nextFive.getVersion().getMajor())
        assertEquals(6, nextFive.getMajor())
    }

    @Test
    void check_bump_minor_method() {
        def version = getJavaClassObject()
        def nextOne = version.bumpMinor()
        def nextFive = version.bumpMinor(5)
        assertEquals("1.2.0", nextOne.getVersion().toString())
        assertEquals(2, nextOne.getVersion().getMinor())
        assertEquals(2, nextOne.getMinor())
        assertEquals("1.6.0", nextFive.getVersion().toString())
        assertEquals(6, nextFive.getVersion().getMinor())
        assertEquals(6, nextFive.getMinor())
    }

    @Test
    void check_bump_patch_method() {
        def version = getJavaClassObject()
        def nextOne = version.bumpPatch()
        def nextFive = version.bumpPatch(5)
        assertEquals("1.1.2", nextOne.getVersion().toString())
        assertEquals(2, nextOne.getVersion().getPatch())
        assertEquals(2, nextOne.getPatch())
        assertEquals("1.1.6", nextFive.getVersion().toString())
        assertEquals(6, nextFive.getVersion().getPatch())
        assertEquals(6, nextFive.getPatch())
    }

    @Test
    void check_bump_patch_level_method() {
        def version = getJavaClassObject()
        def nextOne = version.bump(PatchLevel.MAJOR)
        def nextTwo = version.bump(PatchLevel.MINOR, 2)
        def nextFive = version.bump(PatchLevel.PATCH, 5)
        assertEquals("2.0.0", nextOne.getVersion().toString())
        assertEquals(2, nextOne.getVersion().getMajor())
        assertEquals(2, nextOne.getMajor())
        assertEquals("1.3.0", nextTwo.getVersion().toString())
        assertEquals(3, nextTwo.getVersion().getMinor())
        assertEquals(3, nextTwo.getMinor())
        assertEquals("1.1.6", nextFive.getVersion().toString())
        assertEquals(6, nextFive.getVersion().getPatch())
        assertEquals(6, nextFive.getPatch())
    }

    @Test
    void check_set_prerelease_version_method() {
        def version = getJavaClassObject()
        def nextPre = version.setPreRelease("rcx.alpha.2")
        assertEquals("alpha.x.rc1", version.getPreRelease())
        assertEquals("1.1.1-alpha.x.rc1+23", version.toString())
        assertEquals("rcx.alpha.2", nextPre.getPreRelease())
        assertEquals("1.1.1-rcx.alpha.2+23", nextPre.toString())
    }

    @Test
    void check_set_build_meta_data_method() {
        def version = getJavaClassObject()
        def nextMeta = version.setMeta("24")
        assertEquals("23", version.getMeta())
        assertEquals("1.1.1-alpha.x.rc1+23", version.toString())
        assertEquals("24", nextMeta.getMeta())
        assertEquals("1.1.1-alpha.x.rc1+24", nextMeta.toString())
    }
}
