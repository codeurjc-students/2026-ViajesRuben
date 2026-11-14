package com.ruben.backend.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * A user publishes a trip and an anonymous visitor finds it in the home carousel; unpublishing it
 * makes it disappear. Laura creates her own trip with a unique name and deletes it at the end, so
 * the shared sample data is never touched. Two browsers are used: Laura's, with her session, and
 * the visitor's, with none.
 */
@Tag("I7")
class PublicItineraryUiE2ETest {

  private static final String SAMPLE_PASSWORD = "Demo-2026";
  private static final By CARDS = By.cssSelector("article.public-card");

  private WebDriver driver;
  private WebDriver visitor;
  private WebDriverWait wait;
  private WebDriverWait visitorWait;
  private final String baseUrl = System.getProperty("app.url", "http://localhost:5173");

  private static WebDriver newBrowser() {
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless=new", "--window-size=1280,900");
    return new ChromeDriver(options);
  }

  @BeforeEach
  void setUp() {
    driver = newBrowser();
    visitor = newBrowser();
    wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    visitorWait = new WebDriverWait(visitor, Duration.ofSeconds(10));
  }

  @AfterEach
  void tearDown() {
    driver.quit();
    visitor.quit();
  }

  // --- helpers ---

  private static void click(WebDriver browser, WebDriverWait waiting, By locator) {
    WebElement element = waiting.until(ExpectedConditions.presenceOfElementLocated(locator));
    ((JavascriptExecutor) browser)
        .executeScript(
            "arguments[0].scrollIntoView({block: 'center', behavior: 'instant'});", element);
    waiting.until(ExpectedConditions.elementToBeClickable(element)).click();
  }

  private void click(By locator) {
    click(driver, wait, locator);
  }

  private static By button(String text) {
    return By.xpath("//button[normalize-space()='" + text + "']");
  }

  private static By link(String text) {
    return By.xpath("//a[normalize-space()='" + text + "']");
  }

  /** Selenium's clear() does not reach React's state, so the old text is selected and deleted. */
  private void replaceText(By locator, String text) {
    WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
    field.sendKeys(text);
  }

  /** A date input takes its text in the browser's locale, so the value is set as React reads it. */
  private void setDate(String id, String isoDate) {
    wait.until(ExpectedConditions.presenceOfElementLocated(By.id(id)));
    ((JavascriptExecutor) driver)
        .executeScript(
            "const input = document.getElementById(arguments[0]);"
                + " const setter = Object.getOwnPropertyDescriptor("
                + "HTMLInputElement.prototype, 'value').set;"
                + " setter.call(input, arguments[1]);"
                + " input.dispatchEvent(new Event('input', {bubbles: true}));",
            id,
            isoDate);
  }

  private void logInAsLaura() {
    driver.get(baseUrl + "/login");
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email")))
        .sendKeys("laura@tripmate.com");
    driver.findElement(By.id("login-password")).sendKeys(SAMPLE_PASSWORD);
    click(By.cssSelector("form button[type='submit']"));
    wait.until(ExpectedConditions.urlContains("/trips"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("trips")));
  }

  /** Creates the trip through the form and leaves the browser on its detail page. */
  private String createTrip(String name) {
    click(link("Nuevo viaje"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("trip-name")));
    replaceText(By.id("trip-name"), name);
    replaceText(By.id("trip-destination"), "Cadiz");
    setDate("trip-start-date", "2028-09-01");
    setDate("trip-end-date", "2028-09-03");
    click(button("Crear viaje"));
    wait.until(ExpectedConditions.urlMatches(".*/trips/\\d+$"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), name));
    return driver.getCurrentUrl();
  }

  /** Best effort: whatever the test did, the trip must not stay behind, least of all published. */
  private void deleteTrip(String detailUrl) {
    try {
      driver.get(detailUrl);
      click(button("Eliminar"));
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[role='dialog']")));
      click(button("Eliminar viaje"));
      wait.until(ExpectedConditions.urlMatches(".*/trips$"));
    } catch (RuntimeException ignored) {
      // The trip may already be gone, or the page was never reached.
    }
  }

  /** The names of the cards of the carousel, once at least one is shown. */
  private java.util.List<String> carouselNames() {
    visitor.get(baseUrl + "/");
    visitorWait.until(ExpectedConditions.presenceOfElementLocated(CARDS));
    return visitor.findElements(CARDS).stream()
        .map(card -> card.findElement(By.tagName("h3")).getText())
        .toList();
  }

  // --- tests ---

  @Test
  void aUserPublishesATripItAppearsInTheAnonymousCarouselAndUnpublishingRemovesIt() {
    String name = "Publico UI " + UUID.randomUUID().toString().substring(0, 8);
    logInAsLaura();
    String detailUrl = createTrip(name);
    try {
      // A new trip is private: no badge, and the creator is offered to publish it.
      assertThat(driver.findElements(By.cssSelector(".badge"))).isEmpty();
      assertThat(driver.findElements(button("Despublicar"))).isEmpty();
      assertThat(carouselNames()).doesNotContain(name);

      // Publishing shows the badge and turns the button into "Despublicar".
      click(button("Publicar"));
      wait.until(ExpectedConditions.presenceOfElementLocated(button("Despublicar")));
      assertThat(driver.findElement(By.cssSelector(".badge")).getText()).isEqualTo("Público");
      assertThat(driver.findElements(button("Publicar"))).isEmpty();

      // The visitor finds it first in the carousel, with its author, and opens it read-only.
      assertThat(carouselNames()).first().isEqualTo(name);
      WebElement card = visitor.findElements(CARDS).get(0);
      assertThat(card.getText()).contains("Cadiz · 3 días", "Por Laura Pérez");
      click(
          visitor,
          visitorWait,
          By.xpath("//a[@aria-label='Ver itinerario completo de " + name + "']"));
      visitorWait.until(ExpectedConditions.urlMatches(".*/public/\\d+$"));
      visitorWait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), name));
      assertThat(visitor.findElement(By.tagName("main")).getText())
          .contains("Cadiz", "Laura Pérez", "Itinerario");
      assertThat(visitor.findElements(link("Nueva actividad"))).isEmpty();
      assertThat(visitor.findElements(button("Despublicar"))).isEmpty();
      String publicUrl = visitor.getCurrentUrl();

      // The creator's list shows the badge too.
      click(link("Volver a Mis viajes"));
      wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("trips"), name));
      assertThat(driver.findElement(By.id("trips")).getText()).contains("Público");
      driver.get(detailUrl);
      wait.until(ExpectedConditions.presenceOfElementLocated(button("Despublicar")));

      // Unpublishing removes the badge and the trip from the carousel and from its public page.
      click(button("Despublicar"));
      wait.until(ExpectedConditions.presenceOfElementLocated(button("Publicar")));
      assertThat(driver.findElements(By.cssSelector(".badge"))).isEmpty();
      assertThat(carouselNames()).doesNotContain(name);
      visitor.get(publicUrl);
      visitorWait.until(
          ExpectedConditions.textToBePresentInElementLocated(
              By.tagName("h1"), "No encontramos esta página"));
    } finally {
      deleteTrip(detailUrl);
    }
  }
}
