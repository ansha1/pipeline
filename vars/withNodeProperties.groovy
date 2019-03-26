/*
This method is used for configuration job parameters from the code
 */

def call(Map configuration) {
    node("master") {
        properties(generateProperties(configuration))
    }
}

List generateProperties(Map configuration) {

    /* jobTriggers can be of these types:
    [cron('H/15 * * * *'),
    upstream(threshold: hudson.model.Result.SUCCESS, upstreamProjects: "surveys-server/dev")]
    */
    def jobTriggers = configuration.get("jobTriggers", [])

    /*
    buildDiscarder options
     */
    def buildDaysToKeepStr = configuration.get("buildDaysToKeepStr", "10")
    def buildNumToKeepStr = configuration.get("buildNumToKeepStr", "10")
    def buildArtifactDaysToKeepStr = configuration.get("buildArtifactDaysToKeepStr", "10")
    def buildArtifactNumToKeepStr = configuration.get("buildArtifactNumToKeepStr", "10")

    /* The parameters can be of these types:
    https://jenkins.io/doc/book/pipeline/syntax/#parameters

    string(name: 'DEPLOY_ENV', defaultValue: 'TESTING', description: 'The target environment')
    text(name: 'DEPLOY_TEXT', defaultValue: 'One\nTwo\nThree\n', description: '')
    booleanParam(name: 'DEBUG', description: 'Enable DEBUG mode with extended output', defaultValue: false)
    choice(choices: 'a\nb', description: 'Select A or B when deploying to Production', name: 'stack')

    https://wiki.jenkins.io/display/JENKINS/Git+Parameter+Plugin
    gitParameter(branch: '', branchFilter: 'origin/(.*)',  defaultValue: 'master', description: '', name: 'BRANCH', quickFilterEnabled: false, selectedValue: 'NONE', sortMode: 'NONE', tagFilter: '*', type: 'PT_BRANCH')
     */
    List parametersList = configuration.get("paramlist", []) + [booleanParam(name: 'DEBUG', description: 'Enable DEBUG mode with extended output', defaultValue: false)]

    /* Enable Matrix-based security for the job based on branch name e.g. :
    auth : ["dev": ["user1", "user2", "user3"],
            "master": ["user2"]]
    */
    Map auth = configuration.get("auth", [:])
    List allowedUsers = auth.get(env.BRANCH_NAME, ["authenticated"])
    List securityPermissions = generateSecurityPermissions(allowedUsers)

    def propertiesList = [pipelineTriggers(jobTriggers),
                          buildDiscarder(logRotator(daysToKeepStr: buildDaysToKeepStr, numToKeepStr: buildNumToKeepStr), artifactDaysToKeepStr: buildArtifactDaysToKeepStr, artifactNumToKeepStr: buildArtifactNumToKeepStr),
                          authorizationMatrix(inheritanceStrategy: nonInheriting(), permissions: securityPermissions),
                          parameters(parametersList),
                          disableConcurrentBuilds(),]

    return propertiesList
}


List<String> generateSecurityPermissions(List<String> allowedUsers) {
    List<String> permissions = ['hudson.model.Item.Read:authenticated']
    allowedUsers.each {
        permissions.add("hudson.model.Item.Build:${it}")
        permissions.add("hudson.model.Item.Cancel:${it}")
        permissions.add("hudson.model.Item.Workspace:${it}")
    }
    return permissions
}
