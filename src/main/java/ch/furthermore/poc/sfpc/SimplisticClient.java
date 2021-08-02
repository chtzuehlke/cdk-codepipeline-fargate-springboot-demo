package ch.furthermore.poc.sfpc;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimplisticClient {
	private static void log(String s) {
		System.out.println(Thread.currentThread().getName() + ": " + new Date() + ": " + s);
	}
	
	public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
		ExecutorService threadPool = Executors.newFixedThreadPool(2);
		for (int i = 0; i < 2; i++) {
			threadPool.execute(new Runnable() {
				@Override
				public void run() {
					try {
						long next = System.currentTimeMillis();
						HttpClient client = HttpClient.newBuilder().version(Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build();
						for (;;) {
							long waitUntil = next;
							next += 3000;
							while (waitUntil > System.currentTimeMillis()) {
								Thread.sleep(100);
							}
							HttpRequest request = HttpRequest.newBuilder(new URI(System.getProperty("SERVICE_URI", "http://localhost:8080/"))).timeout(Duration.ofSeconds(5)).GET().build();
							log("Request");
							HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
							if (response.statusCode() != 200) {
								throw new RuntimeException("unexpected status code=" + response.statusCode()); 
							}
							log("Response: " + response.body());
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			Thread.sleep(1500);
		}
		threadPool.shutdown();
		threadPool.awaitTermination(1, TimeUnit.HOURS);
	}
}
