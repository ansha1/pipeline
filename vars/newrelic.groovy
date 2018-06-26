def postBuildVersion(String appId, String apiKey, String version) {
    sh "curl -X POST 'https://api.newrelic.com/v2/applications/${appId}/deployments.json' \
        -H 'X-Api-Key: ${apiKey}' -i \
        -H 'Content-Type: application/json' \
        -d '{\"deployment\": {\"revision\": \"${version}\"}}'"
}
