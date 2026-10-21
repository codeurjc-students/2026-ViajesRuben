package com.ruben.backend.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

class TripUiE2ETest {

  private WebDriver driver;
  private final String baseUrl = System.getProperty("app.url", "http://localhost:5173");

  @BeforeEach
  void setUp() {
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless=new");
    driver = new ChromeDriver(options);
  }

  @AfterEach
  void tearDown() {
    driver.quit();
  }

  @Test
  void homePageShowsSampleTrips() {
    driver.get(baseUrl);
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.textToBePresentInElementLocated(By.id("trips"), "Viaje a Lisbon"));

    String tripsText = driver.findElement(By.id("trips")).getText();
    assertThat(tripsText).contains("Viaje a Lisbon", "Viaje a Rome", "Viaje a Tokyo");
  }
}
