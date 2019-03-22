# Description

An efficient software development process is vital for success in building
business applications we keen on to use shared library for the all builds in Nextiva.

## Confluence pages
[CI\CD page](https://confluence.nextiva.xyz/pages/viewpage.action?pageId=24871188)   
[Nextiva Pipeline FAQ](https://confluence.nextiva.xyz/display/DP/Nextiva+Pipeline+FAQ)   
[Nextiva pipeline integration instructions](https://confluence.nextiva.xyz/display/DP/Nextiva+pipeline+integration+instructions)
##Repository tree
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