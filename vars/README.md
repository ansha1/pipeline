Install Pipeline AWS Plugin. Go to Manage Jenkins -> Manage Plugins -> Available tab -> Filter by 'Pipeline AWS'. Install the plugin.

Add Credentials as per your environment. Example here:

Jenkins > Credentials > System > Global credentials (unrestricted) -> Add

Kind = AWS Credentials and add your AWS credentials

Note the ID

Then in your Pipeline project (Similar to the code I use)

```
node {

    stage('Upload') {

        dir('path/to/your/project/workspace'){

            pwd(); //Log current directory

            withAWS(region:'yourS3Region',credentials:'yourIDfromStep2') {

                 def identity=awsIdentity();//Log AWS credentials

                // Upload files from working directory 'dist' in your project workspace
                s3Upload(bucket:"yourBucketName", workingDir:'dist', includePathPattern:'**/*');
            }

        };
    }
}
```