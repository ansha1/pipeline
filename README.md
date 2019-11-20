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

<a name="features"></a>
# Features

  - Dynamically provisioned build agents - you will get a fresh new
    build agent at every build run

  - Integration tests capable

  - New isolated environment for each build

  - Better logging

  - Better error handling

  - Even smaller Jenkinsfile

  - More maintainable

<a name="samples"></a>
# Quick start and sample Jenkinsfiles

<a name="samples-docker-image-builds"></a>
## Docker image builds

    #!groovy
    @Library('pipeline') _
    
    nextivaPipeline {
        appName = "nextiva-openjdk"
        channelToNotify = "cloud-engineering"
        branchingModel = "gitflow"
        isSonarAnalysisEnabled = false
        isDeployEnabled = false
    
        build = [
            ["name":"docker",
             publishArtifact": true,
             "buildCommands": "docker build ."]
        ]
    }

<a name="samples-python-application-builds"></a>
## Python application builds

Minimally viable python build

    #!groovy
    @Library('pipeline') _
    
    nextivaPipeline {
        appName = "myapp"
        channelToNotify = "mychannel"
    
        build = [["name": "python", image": "python:3.6"],
                 ["name": "docker", "publishArtifact": true]]
    }

All available build options in a single file

    #!groovy
    @Library('pipeline') _
    
    nextivaPipeline {
        appName = "myapp"
        channelToNotify = "testchannel"
    
        build = [
            [
              "name"                       : "python",
              "buildCommands"              : "build commands",
              "postBuildCommands"          : "post Build command",
              "unitTestCommands"           : "unit test commands",
              "postUnitTestCommands"       : "post unit test command",
              "integrationTestCommands"    : "integration test command",
              "postIntegrationTestCommands": "post integration test commands",
              "postDeployCommands"         : "post deploy commands",
              "image"                      : "python:3.6",
              "resourceRequestCpu"         : "1",
              "resourceLimitCpu"           : "1",
              "buildDocker"                : true,
              "resourceRequestMemory"      : "1Gi",
              "resourceLimitMemory"        : "1Gi",
            ],
            ["name": "docker", publishArtifact": true]
        ]
    
        deployTool = "kubeup"
    
        dependencies = ["postgres"                  : "latest",
                        "rabbitmq-ha"               : "latest",
                        "redis-ha"                  : "latest",
                        "rules-engine-core"         : "latest",
                        "rules-engine-orchestration": "latest",]
    
        environment = [
            ["name": "sales-demo", "branchPattern": /^master$/]
        ]
    }

<a name="build-tools"></a>
# Supported Build Tools

Build tools are defined in `build` parameter. You can have more that one
build tool used in your Jenkinsfile.

<a name="python"></a>
## Python

  - Python build tool.

  - Uses twine to upload build artifact.

  - [Registry details](https://confluence.nextiva.xyz/display/DP/Nexus+PyPI+Repositories)

Default buildCommands

    pip install -r requirements.txt

Default unitTestCommands

    pip install -r requirements.txt
    pip install -r requirements-test.txt
    python setup.py test

<a name="npm"></a>
## NPM

  - Nodejs build tool.
  
  - Deployed as a static asset by Ansible playbook into Nginx

  - [Registry details](https://confluence.nextiva.xyz/display/DP/Nexus+NPM+Repositories)

Default buildCommands

    npm ci

Default unitTestCommands

    npm run test
    npm run lint

<a name="docker"></a>
## Docker

  - [Registry details](https://confluence.nextiva.xyz/display/DP/Nexus+Docker+Repositories)

Default buildCommands

    docker build

## Maven

  - Java build tool

  - [Registry details](https://confluence.nextiva.xyz/display/DP/Nexus+Maven+Repositories)

Default buildCommands

    mvn clean package -U --batch-mode

Because unit testing is part of maven build, Jenkins Unit Test stage is skipped.

## Other Tools

Even nextivaPipeline does not have direct build tool implementation for
this language, you can use any other build tool and customize its steps.
Below is an example of Erlang application build.

Building Erlang application (Ejabberd) by overriding python tool commands

    #!groovy
    @Library('pipeline@feature/dockerTemplate') _
    
    nextivaPipeline {
      appName = "chat-ejabberd"
      channelToNotify = "chat_alerts"
      isUnitTestEnabled = true
    
      build = [
        [
          "name"                 : "python",
          "buildCommands"        : '''mix local.hex-- force
                                      modulesToCompile = "ejabberd_auth_nextiva mod_provisioning_nextiva"
                                      for module in ${modulesToCompile};
                                      do
                                          cd ${module}
                                          rebar3 compile
                                          cd ..
                                      done
                                  ''',
          "image"                : "docker.nextiva.xyz/chat-ejabberd-ci:1.0.0",
          "resourceRequestCpu"   : "1",
          "resourceLimitCpu"     : "1",
          "publishArtifact"      : false,
          "resourceRequestMemory": "1Gi",
          "resourceLimitMemory"  : "1Gi",
        ],
        ["name": "docker", "publishArtifact": true]
      ]
      deployTool = "kubeup"
    }

<a name="how-does-it-work"></a>
# How does it work

Once you have triggered jenkins job, it will create a new unique namespace inside tooling.nextiva.io Kubernetes cluster.

Then it would spin up a fresh new Jenkins agent that would take your
source code and push it through one of predefined build stages set.
Actual set is based on your branching model and git branch combination.

<a name="branching-models"></a>
## Supported Branching Models

Selected branching model directly affects job behaviour.

Currently there are two branching models supported - *GitFlow* and *Trunk Based Development*.

To specify branching model, use `branchingModel` parameter. If it is omitted, GitFlow will be used by default.

<a name="gitflow"></a>
### GitFlow

By default all pipelines are using GitFlow model.

<a name="gitflow-stages-map"></a>
#### GitFlow stages map

| Stage                                                                                     | master | develop | release | feature |
| ------------------------------------------------------------------------------------------| ------ | ------- | ------- | ------- |
| [Source code checkout](#stages-checkout)                                                  | √      | √       | √       | √       |
| [Infrastructure dependencies provisioning](#stages-startbuilddependencies)                | √      | √       | √       | √       |
| [Verify if artifact was already published to Nexus](#stages-verifyartifactversioninnexus) |        |         | √       |         |
| [Project version calculation](#stages-configureprojectversion)                            | √      | √       | √       | √       |
| [Build](#stages-build)                                                                    |        | √       | √       | √       |
| [Unit tests](#stages-unittest)                                                            |        | √       | √       | √       |
| [Sonar scan](#stages-sonarscan)                                                           |        | √       |         |         |
| [Integration tests](#stages-integrationtest)                                              | √      | √       | √       | √       |
| [Build artifacts publish](#stages-publish)                                                |        | √       | √       |         |
| [Veracode security scan](#stages-securityscan)                                            |        |         | √       |         |
| [Deployment to environment](#stages-deploy)                                               | √      | √       | √       |         |
| [QA Core Team Tests](#stages-qacoreteamtest)                                              | √      | √       | √       |         |
| [Build results collection](#stages-collectbuildresults)                                   | √      | √       | √       | √       |
| [Send Notification](#stages-sendnotifications)                                            | √      | √       | √       | √       |

[More info about GitFlow model](https://datasift.github.io/gitflow/IntroducingGitFlow.html)

<a name="trunk-based"></a>
### Trunk based

To use this model, add `branchingModel = "gitflow"` into
`nextivaPipeline` section in your Jenkinsfile.

<a name="trunk-based-model-stages-map"></a>
#### Trunk based model stages map

| Stage                                                                                     | master | non-master |
| ------------------------------------------------------------------------------------------| ------ | ---------- |
| [Source code checkout](#stages-checkout)                                                  | √      | √          |
| [Infrastructure dependencies provisioning](#stages-startbuilddependencies)                | √      | √          |
| [Verify if artifact was already published to Nexus](#stages-verifyartifactversioninnexus) | √      |            |
| [Project version calculation](#stages-configureprojectversion)                            | √      | √          |
| [Build](#stages-build)                                                                    | √      | √          |
| [Unit tests](#stages-unittest)                                                            | √      | √          |
| [Sonar scan](#stages-sonarscan)                                                           | √      |            |
| [Integration tests](#stages-integrationtest)                                              | √      | √          |
| [Build artifacts publish](#stages-publish)                                                | √      |            |
| [Veracode security scan](#stages-securityscan)                                            | √      |            |
| [Deployment to environment](#stages-deploy)                                               | √      |            |
| [QA Core Team Tests](#stages-qacoreteamtest)                                              | √      | √          |
| [Build results collection](#stages-collectbuildresults)                                   | √      | √          |
| [Send Notification](#stages-sendnotifications)                                            | √      | √          |

[More info about Trunk based development model](https://trunkbaseddevelopment.com/)

## Examples

GitFlow model will be used because *branchingModel* parameter is not defined

    nextivaPipeline {
      // ...
      build = [
        // ...
      ]
    }

Explicitly show that you are using GitFlow model

    nextivaPipeline {
      // ...
      branchingModel = "gitflow"
      build = [
        // ...
      ]
    }

Use trunk based development model

    nextivaPipeline {
      // ...
      branchingModel = "trunkbased"
      build = [
        // ...
      ]
    }

<a name="stages"></a>
## Stages

<a name="stages-checkout"></a>
### Source code checkout

This stage just collects source code from the remote git repository

<a name="stages-startbuilddependencies"></a>
### Infrastructure dependencies provisioning

Most services are interacting with each other, that is why we need to
have these dependencies available online along with main application for
integration tests.

Using `dependencies` configuration block you can bootstrap
infrastructure required by your application. These dependencies are
created within the same namespace as the jenkins agent executing your
job.

`nextivaPipeline` would automatically check whether this dependency
comes from cloud-apps or cloud-platform repository and then invoke
kubeup utility to deploy it using `configset/test`.

#### Example

    nextivaPipeline {
        // ...
        dependencies = [
            "postgres"                  : "latest",
            "rabbitmq-ha"               : "latest",
            "redis-ha"                  : "latest",
            "rules-engine-core"         : "latest",
            "rules-engine-orchestration": "latest"
        ]
    }

<a name="stages-verifyartifactversioninnexus"></a>
### Verify if artifact was already published to Nexus

This stage would determine whether or not build artifact with such
version was already published into Nexus. If that is the case, build
will be aborted.

This is useful during release builds, where you don’t want to waste time
to wait for job to fail on [Build artifacts publish](#stages-publish) stage.

<a name="stages-configureprojectversion"></a>
### Project version calculation

Extracts application version from build tool configuration files
(`build.properties`, `pom.xml`, `package.json`, etc.) for later use.

<a name="stages-build"></a>
### Build

Executes `buildCommands` followed by `postBuildCommands` for each tool
in `build` block.

  - If `buildCommands` is omitted, a default value for the [build tool](#document-BuildTools) will be used.

  - `postBuildCommands` is optional.

#### Examples

Two build tools

    nextivaPipeline {
      // ...
      build = [
        [
          "name"         : "python", 
          // install gcc and build-essential system packages before
          // installing python dependencies.
          "buildCommands": """
              apt-get update \
                && apt-get install gcc build-essential \
                && pip install -r requirements.txt""",
          "image"        : "python:3.6",
        ],
        [
          "name"             : "npm"
          // send email message after default build commands for npm
          // would be completed
          "postBuildCommands": """
              echo 'Build stage succeeded' | \
              mail -s 'An email from Jenkins' foobar@example.com""",
          "image"            : "node:10-alpine",
        ]
      // ...
    }

<a name="stages-unittest"></a>
### Unit tests

Executes `unitTestCommands` followed by `postUnitTestCommands` for each
tool in `build` block.

  - If `unitTestCommands` is omitted, a default value for build tool
    will be used.

  - To skip Unit Test stage for all build tools, add `isUnitTestEnabled = false` inside `nextivaPipeline` block.

  - To skip Unit Test stage for a single build tool, set `unitTestCommands` to empty string.

  - `postUnitTestCommands` is optional.

#### Examples

Two build tools

    nextivaPipeline {
        // ...
        build = [
            [
              "name"                : "python"
              // Skip unit tests stage for python
              "unitTestCommands"    : "",
              // This step would be skipped too
              "postUnitTestCommands": "rm -rf *",
              "image"               : "python:3.6",
            ],
            [
              "name" : "npm"
              // All steps are executed with their default npm
              // implementations
              "image": "node:10-alpine",
            ],
        // ...
    }
    
Disable Unit Tests for all build tools

    nextivaPipeline {
        // ...
        isUnitTestEnabled = false
        
        build = [
            [
              "name": "python",
              // ...
            ],
            [
              "name": "npm",
              // ...
            ],
        // ...
    }

<a name="stages-sonarscan"></a>
### Sonar scan

During this stage sonar scan would be performed.

This step can be disabled by adding `isSonarAnalysisEnabled = false`
into pipeline definition.

#### Examples

Disable Sonar scanning

    nextivaPipeline {
        isSonarAnalysisEnabled = false
    
        // ...
        build = [
           // ...
    }

<a name="stages-integrationtest"></a>
### Integration tests

Executes `integrationTestCommands` followed by
`postIntegrationTestCommands` for each tool in `build` block.

  - If `integrationTestCommands` is omitted or set to an empty string,
    whole IntegrationTest stage for that tool will be skipped.

  - Both `integrationTestCommands` and `postIntegrationTestCommands` are
    optional.

#### Examples

Two build tools, integration testing is enabled for python, but disabled for npm

    nextivaPipeline {
      // ...
      build = [
        [
          "name"                   :"python"
          "integrationTestCommands": "./run_integration_tests.sh",
          "image"                  : "python:3.6",
        ],
        [
          "name" :"npm"
          "image": "node:10-alpine",
        ],
      // ...
    }

Two build tools, integration testing stage is skipped

    nextivaPipeline {
      // ...
      build = [
        [
          "name"                      :"python"
          // Skip integration tests stage for python because
          // "integrationTestCommands" is not set
          // "postIntegrationTestCommands" would be skipped for the
          // same reason too
          "postIntegrationTestCommands": "rm -rf *",
          "image"                      : "python:3.6",
        ],
        [
          "name"                   : "npm"
          // Alternative way to disable integrations steps for the
          // build tool
          "integrationTestCommands": "",
          "image"                  : "python:3.6",
        ],
      // ...
    }

<a name="stages-publish"></a>
### Build artifacts publish

Publishes build artifacts into nexus.

  - Each build tool uploads build artifact into its [own dedicated repository](https://confluence.nextiva.xyz/display/DP/Sonatype+Nexus).

  - You can skip this stage for a build tool by adding
    `"publishArtifact": true` into build tool configuration.

#### Examples

Publish python package; assemble docker image but do not publish it

    nextivaPipeline {
        // ...
        build = [
            ["name": "python",    "image": "python:3.6"],
            ["name": "docker", "publishArtifact": false]
        ]
    }

<a name="stages-securityscan"></a>
### Veracode security scan

Perform Veracode security scan.

This step can be disabled by adding `isSecurityScanEnabled = false` into
pipeline definition.

#### Examples

Disable Veracode scanning

    nextivaPipeline {
        // ...
        isSecurityScanEnabled = false
        build = [
           // ...
        ]
    }

<a name="stages-deploy"></a>
### Deployment to environment

Deploys application to SLDC environment (dev, qa, prod) based on
[branching model](#document-branching-models) and current branch.
By default [GitFlow branching model](#gitflow) is used.

It is possible to add custom environment definition, and map it to some
branch using regular expression.

#### Examples

Deploy master branch to sandbox

    nextivaPipeline {
      // branchingModel is not defined explicitly, therefore
      // gitflow model will be used
    
      // ...
      build = [
        // ...
      ]
    
      environment = [
        // Because this build uses gitlow model, master branch would
        // be always deployed to prod. However, because
        // "branchPattern" for sales-demo matches master
        // branch too, resulting build would be deployed to
        // both prod and sales-demo
        [
          "name"         :"sales-demo"
          "branchPattern": /^master$/
        ]
      ]
    }

Deploy feature branch to sandbox

    // According to trunkbased model only master branch builds are
    // deployed automatically.
    nextivaPipeline {
      branchingModel = "trunkbased"
    
      // ...
      build = [
        // ...
      ]
    
      // Here we are forcing feature/my-precious branch builds to
      // be deployed to sandbox environment at
      // "nextiva-pipeline-sandbox.nextiva.io" Kubernetes cluster
      environment = [
        [
          "name"             :"sandbox"   
          "branchPattern"    : '^feature/my-precious$',
          "kubernetesCluster": "nextiva-pipeline-sandbox.nextiva.io"
        ]
      ]
    }

<a name="stages-qacoreteamtest"></a>
### QA Core Team Tests

Perform QA Core team tests

This step can be disabled by adding `isQACoreTeamTestEnabled = false`
into pipeline definition.

#### Example

Disable Qa Core Team Tests

    nextivaPipeline {
        // ...
        isQACoreTeamTestEnabled = false
    
        build = [
           // ...
        ]
    }

<a name="stages-collectbuildresults"></a>
### Build results collection

Archives build logs, metrics, and other build related information.

<a name="stages-sendnotifications"></a>
### Send Notification

This stage sends slack notification when build finishes to channel
defined in `channelToNotify`

#### Example

    nextivaPipeline {
        channelToNotify = "example_channel"
        // ...
    }

<a name="using-closures-as-command"></a>
## Using closures as command

In case if simple shell command in build stage is not enough, you can
use a Groovy closure.

For example, triggering another jenkins job after integration tests can
be implemented this way:

    #!groovy
    @Library('pipeline') _
    
    
    def sample_closure = {
        stage("foobar") {
            echo("Hello world: " + GroovySystem.version)
            return "Returned from foobar"
        }
    }
    
    //noinspection GroovyAssignabilityCheck
    nextivaPipeline {
        appName = "myapp"
        channelToNotify = "testchannel"
    
        build = [
          [
            "name"                       : "python"   
            "buildCommands"              : sample_closure,
            "postBuildCommands"          : """pwd""",
            "unitTestCommands"           : """cat file.txt""",
            "postUnitTestCommands"       : """pwd""",
            "integrationTestCommands"    : """pwd""",
            "postIntegrationTestCommands": {
                build job: "/bar/${getGlobal().branchName}", parameters: [
                        string(name: "version", value: getGlobalVersion()),
                        string(name: "appName", value: getGlobal().appName)]
            },
            "postDeployCommands"         : """pwd""",
            "image"                      : "maven:3.6.1-jdk-8",
            "resourceRequestCpu"         : "1",
            "resourceLimitCpu"           : "1",
            "buildDocker"                : true,
            "resourceRequestMemory"      : "1Gi",
            "resourceLimitMemory"        : "1Gi",
          ],
          [
            "name"           : "docker",
            "publishArtifact": true
          ]
        ]
    
        deployTool = "kubeup"
    
        environment = [
          [ "name": "sales-demo", "branchPattern": /^master$/ ]
        ]
    }

## Configuring Jenkins slave in Kubernetes

Each build and deploy tool specified will be created inside Kubernetes tooling cluster in its own namespace.
The namespace name is automatically generated based on jenkins job name and build number, therefore it is unique for
 each build.

Normally there is no need to change default values, however you can do so the same way as you would override default
 build commands:

    build = [
        [
          "name"                 : "python"
          "image"                : "python:3.7-alpine",
          "resourceRequestCpu"   : "100m",
          "resourceLimitCpu"     : "300m",
          "resourceRequestMemory": "256Mi",
          "resourceLimitMemory"  : "512Mi",
          "rawYaml"              : """\
                                       spec:
                                         securityContext:
                                           runAsUser: 1000
                                           runAsGroup: 1000
                                           fsGroup: 1000
                                         tolerations:
                                         - key: tooling.nextiva.io
                                           operator: Equal
                                           value: jenkins
                                           effect: NoSchedule
                                   """.stripIndent())
         ]
    ]

You can check for default values for each supported tool in
`DEFAULT_TOOL_CONFIGURATION` map at `src/com/nextiva/SharedJobsStaticVars.groovy`
