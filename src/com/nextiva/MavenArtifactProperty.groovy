package com.nextiva

import groovy.transform.TupleConstructor
import groovy.transform.ToString


@ToString
@TupleConstructor()
class MavenArtifactProperty implement Serializable {
    String groupId
    String version
    String artifactId
    String packaging
}
