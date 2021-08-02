# Hello World: AWS CDK & CodePipeline & Fargate & SpringBoot

*(not production ready - use at your own risk)*

Preconditions

- Unix-like environment (tested with macOS)
- AWS CLI, AWS CDK, JDK 11, maven, docker, ruby/rake and curl installed

AWS CLI config

    export AWS_PROFILE=...
    export AWS_DEFAULT_REGION=...

Setup

    # Bootstrap CDK (once)
    rake cdkbootstrap

    # Create build & deploy pipeline (once)
    rake cdkdeploypipeline

    # Trigger build & deploy pipeline by zipping and uploading the sources - would be a "git push" (or PR merge) in real world
    rake uploadsourcespipeline

Test

    # Send test requests (~ensure one request is in flight all the time)
    rake continuouscurl

Test II

    export AWS_PROFILE=...
    export AWS_DEFAULT_REGION=...

    # Make app change and deploy new version in 2nd terminal
    rake uploadsourcespipeline

Teardown

    # Remove the application
    rake deletefargatedev

    # Remove the pipeline
    rake deletepipeline

    # [TODO] Manually remove the docker images and the ECR registry 

