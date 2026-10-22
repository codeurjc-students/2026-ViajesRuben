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

class ErrorPageUiE2ETest {

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
  void anUnknownUrlShowsTheErrorPageInsideTheLayoutAndTheButtonGoesHome() {
    driver.get(baseUrl + "/esta-ruta-no-existe");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("h1"), "No encontramos esta página"));

    assertThat(driver.findElement(By.cssSelector("nav.app-header"))).isNotNull();
    assertThat(driver.findElement(By.cssSelector("footer.app-footer"))).isNotNull();

    driver.findElement(By.linkText("Volver al inicio")).click();
    wait.until(ExpectedConditions.urlToBe(baseUrl + "/"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), "Inicio"));
  }
}
