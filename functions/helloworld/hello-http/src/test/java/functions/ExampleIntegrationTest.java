/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package functions;

// [START functions_http_integration_test]

import static com.google.common.truth.Truth.assertThat;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ExampleIntegrationTest {
  // Root URL pointing to the locally hosted function
  // The Functions Framework Maven plugin lets us run a function locally
  private static final String BASE_URL = "http://localhost:8080";

  private static Process emulatorProcess = null;
  private static HttpClient client = HttpClient.newHttpClient();

  @BeforeClass
  public static void setUp() throws IOException {
    // Emulate the function locally by running the Functions Framework Maven plugin
    emulatorProcess = (new ProcessBuilder()).command("mvn", "function:run").start();
  }

  @AfterClass
  public static void tearDown() {
    // Terminate the running Functions Framework Maven plugin process
    emulatorProcess.destroy();
  }

  @Test
  public void helloHttp_shouldRunWithFunctionsFramework() throws Throwable {
    String functionUrl = BASE_URL + "/helloHttp";

    java.net.http.HttpRequest getRequest =
        java.net.http.HttpRequest.newBuilder().uri(URI.create(functionUrl)).GET().build();

    RetryRegistry registry = RetryRegistry.of(RetryConfig.custom()
        .maxAttempts(10)
        .intervalFunction(IntervalFunction.ofExponentialBackoff(100))
        .retryExceptions(IOException.class)
        .build());
    Retry retry = registry.retry("my");
    String body = Retry.decorateCheckedFunction(retry, (HttpRequest r) -> {
      return client.send(r, HttpResponse.BodyHandlers.ofString()).body();
    }).apply(getRequest);

    assertThat(body).isEqualTo("Hello world!");
  }
}
// [END functions_http_integration_test]
