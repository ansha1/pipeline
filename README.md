# Description

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

