package ch.furthermore.poc.sfpc;


import java.util.Arrays;

import software.amazon.awscdk.core.CfnParameter;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;

public class FargateStack extends Stack {
	public FargateStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

	public FargateStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        CfnParameter repositoryArn = CfnParameter.Builder.create(this, "repositoryArn")
                .type("String")
                .description("ECR Repository ARN")
                .build();
        
        CfnParameter repositoryUri = CfnParameter.Builder.create(this, "repositoryUri")
                .type("String")
                .description("ECR Repository URI")
                .build();
        
        CfnParameter dockerImageVersion = CfnParameter.Builder.create(this, "dockerImageVersion")
                .type("String")
                .description("ECR Repository URI")
                .build();
        
        Vpc vpc = Vpc.Builder.create(this, "FargateVPC") 
            .maxAzs(3)
            .subnetConfiguration(Arrays.asList(SubnetConfiguration.builder()
        		.cidrMask(24)
        		.subnetType(SubnetType.PUBLIC)
        		.name("public")
        		.build()))
            .build();

		Cluster cluster = Cluster.Builder.create(this, "FargateCluster")
			.vpc(vpc)
			.build();
		
		ApplicationLoadBalancedFargateService service = ApplicationLoadBalancedFargateService.Builder.create(this, "FargateService") // FIXME tweak min/max percentages and "cooldown" times to speedup re-deployment
	    	.cluster(cluster)           
	        .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
	        	.image(ContainerImage.fromRegistry(repositoryUri.getValueAsString() + ":" + dockerImageVersion.getValueAsString())) 
	        	.containerPort(8080)
	            .build())
	        .taskSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
	        .desiredCount(1)
	        .cpu(256)                   
	        .memoryLimitMiB(1024)       
	        .publicLoadBalancer(true)
	        .assignPublicIp(true) 
	        .listenerPort(80)
	        .build();
		service.getService().getTaskDefinition().addToExecutionRolePolicy(PolicyStatement.Builder.create()
    	        .effect(Effect.ALLOW)
    	        .actions(Arrays.asList("ecr:GetAuthorizationToken"))
    	        .resources(Arrays.asList("*"))
    	        .build());
		service.getService().getTaskDefinition().addToExecutionRolePolicy(PolicyStatement.Builder.create()
    	        .effect(Effect.ALLOW)
    	        .actions(Arrays.asList("ecr:BatchCheckLayerAvailability", "ecr:GetDownloadUrlForLayer", "ecr:BatchGetImage"))
    	        .resources(Arrays.asList(repositoryArn.getValueAsString()))
    	        .build());
    }
}
