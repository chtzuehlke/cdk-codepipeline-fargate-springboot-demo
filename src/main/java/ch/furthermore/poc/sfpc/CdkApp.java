package ch.furthermore.poc.sfpc;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.StackProps;

public class CdkApp {
    public static void main(final String[] args) {
        App app = new App();

        new BuildStack(app, "BuildStack", StackProps.builder().build());

        app.synth();
    }
}
