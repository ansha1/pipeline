# Nextiva Pipeline

## Description

An efficient software development process is vital for success in building
business applications we keen on to use a shared library for all builds in Nextiva.

## Confluence pages
[CI\CD page](https://confluence.nextiva.xyz/pages/viewpage.action?pageId=24871188)   
[Nextiva Pipeline FAQ](https://confluence.nextiva.xyz/display/DP/Nextiva+Pipeline+FAQ)   
[Nextiva pipeline integration instructions](https://confluence.nextiva.xyz/display/DP/Nextiva+pipeline+integration+instructions)  
[Jenkins multibranch pipeline usage](https://confluence.nextiva.xyz/display/DP/Jenkins+multibranch+pipeline+usage)

## List of all available parameters for Nextiva Pipeline - jobTemplate
https://git.nextiva.xyz/projects/REL/repos/pipelines/browse/examples/Jenkinsfile-docs  

## Examples of Jenkinsfile
Java app https://git.nextiva.xyz/projects/REL/repos/pipelines/browse/examples/Jenkinsfile-java-app  
Java lib https://git.nextiva.xyz/projects/REL/repos/pipelines/browse/examples/Jenkinsfile-java-lib  
Python app https://git.nextiva.xyz/projects/REL/repos/pipelines/browse/examples/Jenkinsfile-python-app  
Python lib https://git.nextiva.xyz/projects/REL/repos/pipelines/browse/examples/Jenkinsfile-python-lib  
JavaScript app https://git.nextiva.xyz/projects/REL/repos/pipelines/browse/examples/Jenkinsfile-js-app  

## Repository tree
`````
(root)
+- src                     # Groovy source files
|   +- org
|       +- foo
|           +- Bar.groovy  # for org.foo.Bar class
+- vars
|   +- foo.groovy          # for global 'foo' variable
|   +- foo.txt             # help for 'foo' variable
+- resources               # resource files (external libraries only)
|   +- org
|       +- foo
|           +- bar.json    # static helper data for org.foo.Bar
+- jobs                    # custom jobs for Jenkins

`````

# Nextiva Pipeline v2
## Additional features
### Closure as a build step
In some rare cases just running shell commands might be not enough and some additional logic on the Jenkins side is required.
Instead bloating pipeline's library with additional code for every situation, this can be done by using [Groovy closure](http://www.groovy-lang.org/closures.html)
instead of shell command string.

To provide access to some pipeline internals and helper methods, closure's [delegate](http://www.groovy-lang.org/closures.html#_delegate_of_a_closure)
will be set to _com.nextiva.utils.Utils_ class
and to make it more convenient to use, closure's resolveStrategy is set to Closure.DELEGATE_FIRST.
This means that you can call _com.nextiva.utils.Utils_ methods directly from your code, as it shown in example below.

#### Example
Let's assume the situation such situation:
* you have your application **foo**
* upon **foo** integration tests completion you need to trigger another Jenkins job in **bar** project
* the second job name has to be based on **foo** build's branch name, e.g. _/bar/branch_name_
* some additional parameters has to be passed to **bar**: _application name_ and _version number_
* _version number_ should be extracted from **foo's** build.properties file

Here is how this can be achieved with the use of closure and its delegate property:
```groovy
// Our custom closure. Notice that delegate points to com.nextiva.utils.Utils
def triggerBar = {
    def branch = this.env.BRANCH_NAME
    this.build job: "/bar/$branch", parameters: [
        this.string(name: "version", value: getGlobalVersion()), // will call com.nextiva.utils.Utils.getGlobalVersion()
        this.string(name: "appName", value: getGlobal().appName) // will call com.nextiva.utils.Utils.getGlobal().appName
    ]
}

nextivaPipeline {
    appName = "foo" // your application name
    channelToNotify = "testchannel"
    build = [
            "pip"   : [
                    "integrationTestCommands"    : "curl http://foo.local", // a string to be executed by Jenkins 'sh' step
                    "postIntegrationTestCommands": triggerBar // a closure that will be called as is after integration tests 
            ]
    ]
}
```
