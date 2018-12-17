package com.nextiva

/**
 * Enumeration of patch levels
 */
enum PatchLevel {
  MAJOR, MINOR, PATCH
}

/**
 * Version implements core versioning functionality
 *
 * TODO: Make this a base for SemanticVersion
 * @class Version
 * @implements Serializable
 */
class Version implements Serializable {

  private int major, minor, patch

  Version(String version) {
    def versionParts = version.tokenize('.')
    if (versionParts.size > 3) {
      throw new IllegalArgumentException("Wrong version format - expected MAJOR.MINOR.PATCH - got ${version}")
    }
    this.major = versionParts[0].toInteger()
    this.minor = versionParts[1] ? versionParts[1].toInteger() : 0
    this.patch = versionParts[2] ? versionParts[2].toInteger() : 0
  }

  Version(int major, int minor, int patch) {
    this.major = major ?: 0
    this.minor = minor ?: 0
    this.patch = patch ?: 0
  }

  int getMajor() {
    return major
  }

  int getMinor() {
    return minor
  }

  int getPatch() {
    return patch
  }

  Version bump(PatchLevel patchLevel) {
    switch (patchLevel) {
      case PatchLevel.MAJOR:
        return new Version(major + 1, 0, 0)
        break
      case PatchLevel.MINOR:
        return new Version(major, minor + 1, 0)
        break
      case PatchLevel.PATCH:
        return new Version(major, minor, patch + 1)
        break
    }
    return new Version(major, minor, patch)
  }

  String toString() {
    return "${major}.${minor}.${patch}"
  }

}

/**
 * Manages attaching pre-release and metadata to version
 *
 * TODO: use Version as a base class
 *
 * @class SemanticVersion
 * @implements Serializable
 */
class SemanticVersion implements Serializable {

  private Version semVersion
  private String prerelease, meta

  SemanticVersion(String version) {
    def splitMeta = version.tokenize('+')
    def splitPreRelease = splitMeta[0].tokenize('-')
    this.semVersion = new Version(splitPreRelease[0])
    this.prerelease = splitPreRelease[1] ?: ""
    this.meta = splitMeta[1] ?: ""
  }

  SemanticVersion(String version, String prerelease, String meta = "") {
    this.semVersion = new Version(version)
    this.prerelease = prerelease ?: ""
    this.meta = meta ?: ""
  }

  SemanticVersion(Version semVersion, String prerelease = "", String meta = "") {
    this.semVersion = semVersion
    this.prerelease = prerelease ?: ""
    this.meta = meta ?: ""
  }

  SemanticVersion(int major, int minor = 0, int patch = 0, String prerelease = "", String meta = "") {
    this.semVersion = new Version(major, minor, patch)
    this.prerelease = prerelease ?: ""
    this.meta = meta ?: ""
  }

  SemanticVersion bumpMajor(int value) {
    def nextVersion = new Version(this.semVersion.getMajor() + value, 0, 0)
    return new SemanticVersion(nextVersion, this.prerelease, this.meta)
  }

  SemanticVersion bumpMajor() {
    return this.bumpMajor(1)
  }

  SemanticVersion bumpMinor(int value) {
    def major = this.semVersion.getMajor()
    def nextVersion = new Version(major, this.semVersion.getMinor() + value, 0)
    return new SemanticVersion(nextVersion, this.prerelease, this.meta)
  }

  SemanticVersion bumpMinor() {
    return this.bumpMinor(1)
  }

  SemanticVersion bumpPatch(int value) {
    def major = this.semVersion.getMajor()
    def minor = this.semVersion.getMinor()
    def nextVersion = new Version(major, minor, this.semVersion.getPatch() + value)
    return new SemanticVersion(nextVersion, this.prerelease, this.meta)
  }

  SemanticVersion bumpPatch() {
    return this.bumpPatch(1)
  }

  SemanticVersion bump(PatchLevel level, int value) {
    if (value == 1) {
      return new SemanticVersion(this.semVersion.bump(level), this.prerelease, this.meta)
    } else {
      switch (level) {
        case PatchLevel.MAJOR:
          return this.bumpMajor(value)
          break
        case PatchLevel.MINOR:
          return this.bumpMinor(value)
          break
        case PatchLevel.PATCH:
          return this.bumpPatch(value)
          break
      }
    }
    return this
  }

  SemanticVersion bump(PatchLevel level) {
    return this.bump(level, 1)
  }

  SemanticVersion setPreRelease(String prerelease) {
    return new SemanticVersion(new Version(this.semVersion.toString()), prerelease, this.meta)
  }

  SemanticVersion setMeta(String meta) {
    return new SemanticVersion(new Version(this.semVersion.toString()), this.prerelease, meta)
  }

  String getPreRelease() {
    return this.prerelease
  }

  String getMeta() {
    return this.meta
  }

  Version getVersion() {
    return this.semVersion
  }

  int getMajor() {
    return this.semVersion.getMajor()
  }

  int getMinor() {
    return this.semVersion.getMinor()
  }

  int getPatch() {
    return this.semVersion.getPatch()
  }

  String toString() {
    String version = this.semVersion.toString()

    if (this.prerelease != "") {
      version = version + "-${this.prerelease}"
    }

    if (this.meta != "") {
      version = version + "+${this.meta}"
    }

    return version
  }

}
