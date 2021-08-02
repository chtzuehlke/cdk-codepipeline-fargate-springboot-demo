package ch.furthermore.poc.sfpc;


import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SpringFargatePipelineCdkApplication {
	private final static Logger log = LoggerFactory.getLogger(SpringFargatePipelineCdkApplication.class);
	
	@GetMapping("/") 
	public String hello(HttpServletRequest req) {
		log.info("Processing request. User-Agent={}", Arrays.asList(req.getHeader("User-Agent")));
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		log.info("End: simulate 3s task");
		return "Hello World V1";
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
