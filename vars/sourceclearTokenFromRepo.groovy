#!groovy
import static com.nextiva.SharedJobsStaticVars.*
import com.nextiva.*

/*
  currently unused, this was my attempt into breaking this off into a vars class, couldn't figure out how to call it.
 */

def call(String repo)  {

        def credName = null

        switch (repo) {

            case ~/(?i).*crm.*/:
                credName = 'CRM_SRCCLR'
                break
            case ~/(?i).*analytics.*/:
                credName = 'ANALYTICS_SRCCLR'
                break;
            case ~/(?i).*dash.*/:
                credName = 'DASHBOARD_SRCCLR'
                break;
            case ~/(?i).*rengine.*/:
                credName = 'RULES_SRCCLR'
                break;
            case ~/(?i).*surveys.*/:
                credName = 'SURVEYS_SRCCLR'
                break;
            case ~/(?i).*migration.*/:
                credName = 'DM_SRCCLR'
                break;
            case ~/(?i).*realtalk.*/:
                credName = 'RT_SRCCLR'
                break;
            case ~/(?i).*platform.*/:
                credName = 'PLATFORM_SRCCLR'
                break;
        }
        return credName

}