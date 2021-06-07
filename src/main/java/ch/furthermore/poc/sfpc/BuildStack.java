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
import software.amazon.awscdk.services.codebuild.Project;
import software.amazon.awscdk.services.codebuild.S3SourceProps;
import software.amazon.awscdk.services.codebuild.Source;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.s3.Bucket;

public class BuildStack extends Stack {
	public BuildStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

	public BuildStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Repository dockerRepo = Repository.Builder.create(this, "DockerRepository") // FIXME policy to remove <untagged> images
        	.repositoryName("springdemo")
        	.build();
        
        CfnOutput.Builder.create(this, "DockerRepositoryURI").value(dockerRepo.getRepositoryUri()).build();
        
        Bucket sourceBucket = Bucket.Builder.create(this, "SourceBucket")
    		.removalPolicy(RemovalPolicy.DESTROY)
	        .autoDeleteObjects(true)
        	.build();
        
        Bucket cacheBucket = Bucket.Builder.create(this, "CacheBucket")
        		.removalPolicy(RemovalPolicy.DESTROY)
    	        .autoDeleteObjects(true)
            	.build();
            
        Project dockerBuildProject = Project.Builder.create(this, "SpringdemoDockerBuild") // FIXME caching (.m2 repo, downloaded docker image deps)
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
								"echo '{\"BuildStack\":{\"DockerRepositoryURI\":\"" + dockerRepo.getRepositoryUri() + "\"}}' > buildstack.json", // FIXME
								"chmod +x ./mvnw && rake dockerpush"
					))))))
            .environment(BuildEnvironment.builder()
            		.buildImage(LinuxBuildImage.STANDARD_5_0)
            		.privileged(true)
            		.build())
            .source(Source.s3(S3SourceProps.builder()
            		.bucket(sourceBucket)
            		.path("sources.zip")
            		.build()))
            .cache(Cache.bucket(cacheBucket))
        	.build();
        dockerBuildProject.addToRolePolicy(PolicyStatement.Builder.create()
    	        .effect(Effect.ALLOW)
    	        .actions(Arrays.asList("ecr:GetAuthorizationToken"))
    	        .resources(Arrays.asList("*"))
    	        .build());
        dockerBuildProject.addToRolePolicy(PolicyStatement.Builder.create()
    	        .effect(Effect.ALLOW)
    	        .actions(Arrays.asList("ecr:*"))
    	        .resources(Arrays.asList(dockerRepo.getRepositoryArn()))
    	        .build());
        
        CfnOutput.Builder.create(this, "SourceBucketName").value(sourceBucket.getBucketName()).build();
        CfnOutput.Builder.create(this, "DockerBuildProjectName").value(dockerBuildProject.getProjectName()).build();
    }
}
