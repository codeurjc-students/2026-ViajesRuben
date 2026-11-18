package com.ruben.backend.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * The public itineraries page as an anonymous visitor uses it. The public trips are the sample
 * trips the application publishes with SAMPLE_DATA: Ana's "Lisbon getaway" is on the second page of
 * the home carousel, so finding it needs the search of the page.
 */
@Tag("I8")
class PublicItinerariesUiE2ETest {

  private WebDriver driver;
  private WebDriverWait wait;
  private final String baseUrl = System.getProperty("app.url", "http://localhost:5173");

  @BeforeEach
  void setUp() {
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless=new", "--window-size=1280,900");
    driver = new ChromeDriver(options);
    wait = new WebDriverWait(driver, Duration.ofSeconds(10));
  }

  @AfterEach
  void tearDown() {
    driver.quit();
  }

  private void click(By locator) {
    WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    ((JavascriptExecutor) driver)
        .executeScript(
            "arguments[0].scrollIntoView({block: 'center', behavior: 'instant'});", element);
    wait.until(ExpectedConditions.elementToBeClickable(element)).click();
  }

  private void search(String text) {
    WebElement box =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("public-trips-search")));
    box.clear();
    box.sendKeys(text);
    click(By.xpath("//form[@role='search']//button[normalize-space()='Buscar']"));
  }

  private static By tripLink(String name) {
    return By.xpath("//main//ul//a[normalize-space()='" + name + "']");
  }

  @Test
  void anAnonymousVisitorGoesFromTheHomeToAllTheTripsSearchesOneAndOpensIt() {
    driver.get(baseUrl + "/");
    wait.until(ExpectedConditions.numberOfElementsToBe(By.cssSelector("article.public-card"), 3));

    click(By.xpath("//a[normalize-space()='Ver todos']"));

    wait.until(ExpectedConditions.urlMatches(".*/public$"));
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), "Viajes públicos"));
    // Every public trip, newest first: the sample trips of the three authors.
    wait.until(ExpectedConditions.visibilityOfElementLocated(tripLink("Roma en cinco días")));
    assertThat(driver.findElement(By.tagName("main")).getText()).contains("Por Ana García");

    // A search by name, in other case, leaves only that trip.
    search("LISBON getaway");
    wait.until(ExpectedConditions.invisibilityOfElementLocated(tripLink("Roma en cinco días")));
    wait.until(ExpectedConditions.visibilityOfElementLocated(tripLink("Lisbon getaway")));
    assertThat(driver.findElements(By.xpath("//main//ul//li"))).hasSize(1);

    // A search with no results says so.
    search("zzz-no-trip");
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("main"), "Ningún viaje público coincide con la búsqueda"));

    // Searching again and opening the trip shows its read-only preview.
    search("lisbon");
    click(tripLink("Lisbon getaway"));
    wait.until(ExpectedConditions.urlContains("/public/"));
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), "Lisbon getaway"));
  }

  @Test
  void theHeaderLinkOpensThePageForAnAnonymousVisitor() {
    driver.get(baseUrl + "/");

    click(By.xpath("//nav//a[normalize-space()='Viajes públicos']"));

    wait.until(ExpectedConditions.urlMatches(".*/public$"));
    assertThat(
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(tripLink("Roma en cinco días")))
                .isDisplayed())
        .isTrue();
  }

  @Test
  void theListFitsAPhoneWithoutHorizontalScroll() {
    driver.manage().window().setSize(new Dimension(375, 800));
    driver.get(baseUrl + "/public");
    wait.until(ExpectedConditions.visibilityOfElementLocated(tripLink("Roma en cinco días")));

    Boolean overflows =
        (Boolean)
            ((JavascriptExecutor) driver)
                .executeScript(
                    "return document.documentElement.scrollWidth >"
                        + " document.documentElement.clientWidth;");

    assertThat(overflows).isFalse();
  }
}
