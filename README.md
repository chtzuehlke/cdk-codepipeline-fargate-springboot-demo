CDK & CodePipeline & Fargate & SpringBoot Hello World (not production ready - use at your own risk)

Preconditions

- Unix-like environment (tested with macOS)
- AWS CLI, AWS CDK, JDK 11, maven and ruby/rake installed
- AWS_PROFILE and AWS_DEFAULT_REGION set

Howto

    rake cdkdeploypipeline
    rake uploadsourcespipeline

Teardown

    rake deletefargatedev
    rake deletepipeline
