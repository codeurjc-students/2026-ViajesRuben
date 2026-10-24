package com.ruben.backend.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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
    driver.get(baseUrl + "/trips");
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(
            ExpectedConditions.textToBePresentInElementLocated(By.id("trips"), "Viaje a Lisbon"));

    String tripsText = driver.findElement(By.id("trips")).getText();
    assertThat(tripsText).contains("Viaje a Lisbon", "Viaje a Rome", "Viaje a Tokyo");
  }

  @Test
  void moreResultsAppendsTheNextPageAndDisappearsOnTheLastOne() {
    driver.get(baseUrl + "/trips");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    By cards = By.cssSelector("#trips > li");
    By moreResults = By.xpath("//button[normalize-space()='Más resultados']");
    wait.until(ExpectedConditions.numberOfElementsToBe(cards, 10));

    WebElement button = driver.findElement(moreResults);
    ((JavascriptExecutor) driver)
        .executeScript(
            "arguments[0].scrollIntoView({block: 'center', behavior: 'instant'});", button);
    wait.until(ExpectedConditions.elementToBeClickable(button)).click();

    List<WebElement> allCards =
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(cards, 10));
    Boolean buttonGone = wait.until(ExpectedConditions.invisibilityOfElementLocated(moreResults));

    assertThat(allCards).hasSizeGreaterThan(10);
    assertThat(buttonGone).isTrue();
  }
}
