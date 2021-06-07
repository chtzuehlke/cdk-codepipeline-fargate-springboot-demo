package ch.furthermore.poc.sfpc;


import java.util.List;
import java.util.Map;

import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codebuild.Project;
import software.amazon.awscdk.services.codebuild.S3SourceProps;
import software.amazon.awscdk.services.codebuild.Source;
import software.amazon.awscdk.services.s3.Bucket;

public class BuildStack extends Stack {
	public BuildStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

	public BuildStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Bucket sourceBucket = Bucket.Builder.create(this, "SourceBucket")
    		.removalPolicy(RemovalPolicy.DESTROY)
	        .autoDeleteObjects(true)
        	.build();
            
        Project.Builder.create(this, "BuildProject")
    		.buildSpec(BuildSpec.fromObjectToYaml(Map.of(
					"version", "0.2",
					"phases", Map.of(
						"build", Map.of(
							"commands", List.of(
								"nohup /usr/local/bin/dockerd --host=unix:///var/run/docker.sock --host=tcp://127.0.0.1:2375 --storage-driver=overlay2 &",
								"timeout 15 sh -c \"until docker info; do echo .; sleep 1; done\"",
								"chmod +x ./mvnw && rake dockerbuild"
					))))))
            .environment(BuildEnvironment.builder()
            		.buildImage(LinuxBuildImage.STANDARD_5_0)
            		.privileged(true)
            		.build())
            .source(Source.s3(S3SourceProps.builder()
            		.bucket(sourceBucket)
            		.path("sources.zip")
            		.build()))
        	.build();
        
        CfnOutput.Builder.create(this, "SourceBucketName").value(sourceBucket.getBucketName()).build();
    }
}
