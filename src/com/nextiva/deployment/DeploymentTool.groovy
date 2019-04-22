package com.nextiva.deployment

interface DeploymentTool {
    Boolean deploy(Map<String, String> playbookContext)
}