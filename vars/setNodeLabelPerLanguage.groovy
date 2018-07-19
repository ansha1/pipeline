#!groovy
import static com.nextiva.SharedJobsStaticVars.*
import com.nextiva.*

def call(){

        if (jobConfig.nodeLabel != null) {
            agent { label jobConfig.nodeLabel }
        }

        if (jobConfig.nodeLabel = null & jobConfig.projectFlow.language.equals('python')){
           stage('Set agent node per python language'){
           agent python
        }

        if (jobConfig.nodeLabel = null & jobConfig.projectFlow.language.equals('js')){
           stage('Set agent node per js language'){
           agent nodejs
        }

        if (jobConfig.nodeLabel = null & jobConfig.projectFlow.language.equals('java')){
           stage('Set agent node per java language'){
           agent java
        }
}