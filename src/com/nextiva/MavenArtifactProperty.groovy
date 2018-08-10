package com.nextiva

//import groovy.transform.TupleConstructor
//import groovy.transform.ToString
//
//
//@ToString
//@TupleConstructor()
class MavenArtifactProperty implements Serializable {
    String groupId
    String artifactVersion
    String artifactId
    String packaging

    @Override
    String toString() {
        return "class MavenArtifactProperty { groupId='$groupId' }"
    }
}
