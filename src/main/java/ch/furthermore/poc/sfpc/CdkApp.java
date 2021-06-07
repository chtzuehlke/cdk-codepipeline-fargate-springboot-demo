package ch.furthermore.poc.sfpc;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.StackProps;

public class CdkApp {
    public static void main(final String[] args) {
        App app = new App();

        BuildStack buildStack = new BuildStack(app, "BuildStack", StackProps.builder().build(), "springdemo");

        new FargateStack(app, "FargateDevStack", StackProps.builder().build(), buildStack.getRepositoryUri(), buildStack.getRepositoryArn());
        
        app.synth();
    }
}
