package com.nextiva.deployment.tool

interface DeploymentTool {
    Boolean deploy(Map<String, String> playbookContext)
}