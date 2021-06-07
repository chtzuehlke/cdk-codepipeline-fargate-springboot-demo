package ch.furthermore.poc.sfpc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SpringFargatePipelineCdkApplication {
	@GetMapping("/")
	public String hello() {
		return "world";
	}
	
	public static void main(String[] args) {
		if (System.getenv("RUN_CDK") != null) {
			CdkApp.main(args);
		}
		else {
			SpringApplication.run(SpringFargatePipelineCdkApplication.class, args);
		}
	}
}
