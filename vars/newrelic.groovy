import groovy.json.JsonSlurper
import groovy.transform.Field

@Field static final NEW_RELIC_API = "https://api.newrelic.com/v2"
@Field static final API_KEYS = [
    'production': '83904bdc782deef7783afde4a50348538b758577a208889',
    'demo'      : '8ce1df1dcff3f8db0f31209bea93346b371721108be58e6',
    'rc'        : '2ead29f8fc8a62095547db197dfa1e9fdbb66e065c9e94a',
    'dev'       : 'e86fd6976f3de3f0de81be785186401a047c14a63ad6d9f'
]

/**
 * Post the deployment version for the current job
 *
 * @param jobConfig the job configuration object
 */
def postDeployment(jobConfig) {

    Integer appId = 0
    String environment = jobConfig.deployToSalesDemo ? 'demo' : jobConfig.ANSIBLE_ENV

    if (jobConfig.NEWRELIC_APP_ID_MAP) {
        log.warning("[DEPRECATION] Use NEW_RELIC_APP_ID instead of NEWRELIC_APP_ID_MAP")

        if (jobConfig.NEWRELIC_APP_ID_MAP.containsKey(environment)) {
            appId = jobConfig.NEWRELIC_APP_ID_MAP[environment]
        }
    }

    if (appId == 0 && jobConfig.NEW_RELIC_APP_ID && jobConfig.NEW_RELIC_APP_ID.containsKey(environment)) {
        appId = jobConfig.NEW_RELIC_APP_ID[environment] as Integer
    }

    if (appId == 0 && jobConfig.NEW_RELIC_APP_NAME) {
        appId = getAppIdByAppName(environment, jobConfig.NEW_RELIC_APP_NAME)
    }

    if (appId != 0 && jobConfig.BUILD_VERSION) {
        postDeploymentByAppId(environment, appId, jobConfig.BUILD_VERSION)
    }
}

/**
 * Post a deployment for a given app id, if the given app ID is zero (0), this
 * method will not attempt to add the deployment.
 *
 * @param environment The deployment environment
 * @param appId the app id for the application to add a new deployment version
 * @param appVersion the version for the new deployment
 */
def postDeploymentByAppId(String environment, Integer appId, String appVersion) {
    if (appId == 0) {
        log.info("Invalid New Relic App Id ${appId}")
        return
    }

    def apiKey = getApiKey(environment)
    if (!apiKey?.trim()) {
        log.info("No New Relic API Key found for environment ${environment}")
        return
    }

    log.info("Posting App ID ${appId} v${appVersion} to ${environment} environment")

    def body = "{\"deployment\": {\"revision\": \"${appVersion}\"}}"

    httpRequest(
        httpMode: 'POST',
        url: "${NEW_RELIC_API}/applications/${appId}/deployments.json",
        acceptType: 'APPLICATION_JSON',
        contentType: 'APPLICATION_JSON',
        customHeaders: [
            [name: 'X-Api-Key', value: apiKey]
        ],
        requestBody: body,
        consoleLogResponseBody: log.isDebug(),
    )

    log.info("Deployment added to New Relic")
}

/**
 * @param environment The deployment environment
 * @param appName the literal name of the application used in New Relic
 * @return the app id for the first application returned from New Relic
 * with the given name. Returns 0 if one is not found.
 */
Integer getAppIdByAppName(String environment, String appName) {
    def apiKey = getApiKey(environment)

    if (!apiKey?.trim()) {
        log.info("No New Relic API Key found for environment ${environment}")
        return
    }

    Integer appId = 0
    def param = URLEncoder.encode(appName, "UTF-8")
    def responseTxt = httpRequest(
        httpMode: 'GET',
        url: "${NEW_RELIC_API}/applications.json?filter[name]=${param}",
        acceptType: 'APPLICATION_JSON',
        contentType: 'APPLICATION_JSON',
        customHeaders: [
            [name: 'X-Api-Key', value: apiKey]
        ],
        consoleLogResponseBody: log.isDebug(),
    )

    def response = new JsonSlurper().parseText(responseTxt.getContent())

    if (response.applications.size() > 0) {
        appId = response.applications[0].id
        log.info("Found ${response.applications.size()} applications, using id ${appId}")
    } else {
        log.info("No applications found in New Relic named ${appName}")
    }

    return appId
}

/**
 * @param environment the deployment environment
 * @return New Relic API key for environment, or empty string if unknown environment
 */
static String getApiKey(String environment) {
    return API_KEYS.containsKey(environment) ? API_KEYS[environment] : ''
}
