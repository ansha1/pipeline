package com.nextiva.config

// For unknown reason @Singleton annotation did not work well when running on Jenkins
class Global implements Serializable {
    String appName
    String globalVersion
    String branchName
    String branchingModel

    private static Global single_instance = null

    private Global() {}

    public static Global getInstance() {
        if (single_instance == null) {
            single_instance = new Global()
        }
        return single_instance
    }
}

