# Hello World: AWS CDK & CodePipeline & Fargate & SpringBoot

*(not production ready - use at your own risk)*

Preconditions

- Unix-like environment (tested with macOS)
- AWS CLI, AWS CDK, JDK 11, maven, docker, ruby/rake and curl installed

Setup

    export AWS_PROFILE=...
    export AWS_DEFAULT_REGION=...

    # Bootstrap CDK (once)
    rake cdkbootstrap

    # Create build & deploy pipeline (once)
    rake cdkdeploypipeline

    # Initial trigger of the build & deploy pipeline (deploy initial app version)
    rake uploadsourcespipeline

Test

    # Send test requests (~ensure one request is in flight all the time)
    rake continuouscurl

Test 2 (2nd console)

    export AWS_PROFILE=...
    export AWS_DEFAULT_REGION=...

    # [TODO] Make change to app (e.g. return different string)

    # 2nd trigger of the build & deploy pipeline (deploy new app version)
    rake uploadsourcespipeline

Teardown

    # Remove the application
    rake deletefargatedev

    # Remove the pipeline
    rake deletepipeline

    # [TODO] Manually remove the docker images and the ECR registry 

