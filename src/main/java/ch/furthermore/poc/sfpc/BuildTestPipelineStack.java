package ch.furthermore.poc.sfpc;


import java.util.Arrays;
import java.util.List;
import java.util.Map;

import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.Cache;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.S3SourceAction;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.s3.Bucket;

public class BuildTestPipelineStack extends Stack { 
	public BuildTestPipelineStack(final Construct scope, final String id, final String ecrRepositoryName) {
        this(scope, id, null, ecrRepositoryName);
    }

	public BuildTestPipelineStack(final Construct scope, final String id, final StackProps props, final String ecrRepositoryName) {
        super(scope, id, props);

        Repository dockerRepo = Repository.Builder.create(this, "DockerRepository") // FIXME policy to remove <untagged> images
            	.repositoryName(ecrRepositoryName)
            	.build();
        CfnOutput.Builder.create(this, "DockerRepositoryURI").value(dockerRepo.getRepositoryUri()).build();
        CfnOutput.Builder.create(this, "DockerRepositoryARN").value(dockerRepo.getRepositoryArn()).build();
        
        Bucket sourceBucket = Bucket.Builder.create(this, "SourceBucket")
    		.removalPolicy(RemovalPolicy.DESTROY)
	        .autoDeleteObjects(true)
	        .versioned(true)
        	.build();
        CfnOutput.Builder.create(this, "SourceBucketName").value(sourceBucket.getBucketName()).build();
        
        Bucket cacheBucket = Bucket.Builder.create(this, "CacheBucket")
    		.removalPolicy(RemovalPolicy.DESTROY)
	        .autoDeleteObjects(true)
        	.build();
            
        PipelineProject dockerBuildProject = PipelineProject.Builder.create(this, "DockerBuild")
    		.buildSpec(BuildSpec.fromObjectToYaml(Map.of(
				"version", "0.2",
				"cache", Map.of(
					"paths", List.of(
						"/root/.m2/**/*"
				)),
				"phases", Map.of(
					"build", Map.of(
						"commands", List.of(
							"nohup /usr/local/bin/dockerd --host=unix:///var/run/docker.sock --host=tcp://127.0.0.1:2375 --storage-driver=overlay2 &",
							"timeout 15 sh -c \"until docker info; do echo .; sleep 1; done\"",
							"chmod +x ./mvnw && DOCKER_REPOSITORY_URI=" + dockerRepo.getRepositoryUri() + " rake dockerpush"
				))))))
            .environment(BuildEnvironment.builder()
        		.buildImage(LinuxBuildImage.STANDARD_5_0)
        		.privileged(true)
        		.build())
            .cache(Cache.bucket(cacheBucket))
        	.build();
        dockerBuildProject.addToRolePolicy(PolicyStatement.Builder.create()
    	        .effect(Effect.ALLOW)
    	        .actions(Arrays.asList("ecr:GetAuthorizationToken"))
    	        .resources(Arrays.asList("*"))
    	        .build());
        dockerBuildProject.addToRolePolicy(PolicyStatement.Builder.create()
    	        .effect(Effect.ALLOW)
    	        .actions(Arrays.asList("ecr:*")) // FIXME least privilege
    	        .resources(Arrays.asList(dockerRepo.getRepositoryArn()))
    	        .build());
        
        PipelineProject cdkDeployBuildProject = PipelineProject.Builder.create(this, "CdkDeploy") // FIXME "Maximum builds allowed in batch" = 1
    		.buildSpec(BuildSpec.fromObjectToYaml(Map.of(
				"version", "0.2",
				"cache", Map.of(
					"paths", List.of(
						"/root/.m2/**/*"
				)),				
				"phases", Map.of(
					"build", Map.of(
						"commands", List.of(
							"npm install -g aws-cdk",
							"chmod +x ./mvnw && DOCKER_REPOSITORY_URI=" + dockerRepo.getRepositoryUri() + " DOCKER_REPOSITORY_ARN=" + dockerRepo.getRepositoryArn() + " rake cdkdeployfargatedev" 
				))))))
            .environment(BuildEnvironment.builder()
        		.buildImage(LinuxBuildImage.STANDARD_5_0)
        		.build())
            .cache(Cache.bucket(cacheBucket))
        	.build();
        cdkDeployBuildProject.addToRolePolicy(PolicyStatement.Builder.create() //FIXME least privilege
	        .effect(Effect.ALLOW)
	        .actions(Arrays.asList("*"))
	        .resources(Arrays.asList("*"))
	        .build());
        
        Bucket artifactBucket = Bucket.Builder.create(this, "PipelineArtifacts")
			.removalPolicy(RemovalPolicy.DESTROY)
	        .autoDeleteObjects(true)
        	.build();
		Artifact sourceOutput = new Artifact();
        Pipeline pipeline = Pipeline.Builder.create(this, "BuildDeployTest")
        	.artifactBucket(artifactBucket)
            .stages(Arrays.asList(
                StageProps.builder()
                    .stageName("Source")
                    .actions(Arrays.asList(
                    	S3SourceAction.Builder.create()
                    		.actionName("Source")
                    		.bucket(sourceBucket)
                    		.bucketKey("sources.zip")
                    		.output(sourceOutput)
                    		.build()))
                    .build(),
                StageProps.builder()
                    .stageName("Build")
                    .actions(Arrays.asList(
                        CodeBuildAction.Builder.create()
                            .actionName("Build")
                            .project(dockerBuildProject)
                            .input(sourceOutput)
                            .build()))
                    .build(),
                StageProps.builder()
                    .stageName("Deploy")
                    .actions(Arrays.asList(
                        CodeBuildAction.Builder.create()
                            .actionName("Deploy")
                            .project(cdkDeployBuildProject)
                            .input(sourceOutput)
                            .build()))
                    .build()                    
            ))
            .build();
       pipeline.addToRolePolicy(PolicyStatement.Builder.create() //FIXME least privilege
   	        .effect(Effect.ALLOW)
   	        .actions(Arrays.asList("s3:*"))
   	        .resources(Arrays.asList(sourceBucket.getBucketArn()))
   	        .build());
       pipeline.addToRolePolicy(PolicyStatement.Builder.create() //FIXME least privilege
  	        .effect(Effect.ALLOW)
  	        .actions(Arrays.asList("s3:*"))
  	        .resources(Arrays.asList(sourceBucket.getBucketArn() + "/*"))
  	        .build());
    }
}
