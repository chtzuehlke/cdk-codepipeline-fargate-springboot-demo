package ch.furthermore.poc.sfpc;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.StackProps;

public class CdkApp {
    public static void main(final String[] args) {
        App app = new App();

        new BuildTestPipelineStack(app, "Pipeline", StackProps.builder().build(), "springdemo");
        
        new FargateStack(app, "FargateDev", StackProps.builder().build());
        
        app.synth();
    }
}
