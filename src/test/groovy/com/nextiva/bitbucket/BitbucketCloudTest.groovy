package com.nextiva.bitbucket

import org.junit.Test

import static org.junit.Assert.assertEquals

/*
 * Copyright (c) 2019 Nextiva, Inc. to Present.
 * All rights reserved.
 */

class BitbucketCloudTest {

    @Test
    void testFileDiff() {
        def diff = """diff --git a/configset/aws-prod/spring-configuration-server.conf b/configset/aws-prod/spring-configuration-server.conf
            index 00e08f6..c3b9dff 100644
            --- a/configset/aws-prod/spring-configuration-server.conf
            +++ b/configset/aws-prod/spring-configuration-server.conf
            @@ -1,4 +1,4 @@
            -SPRING_CONFIG_JAVA_OPTS=-Dvalue1
            +SPRING_CONFIG_JAVA_OPTS=-Dvalue2
             MEMORY_LIMIT=512M
             CPU_REQUEST=100m
             REPLICAS=3
            \\ No newline at end of file
        """.stripIndent()

        def list = BitbucketCloud.getFileListFromDiff(diff)
        assertEquals(1, list.size())
        assertEquals("configset/aws-prod/spring-configuration-server.conf", list[0])
    }
}
