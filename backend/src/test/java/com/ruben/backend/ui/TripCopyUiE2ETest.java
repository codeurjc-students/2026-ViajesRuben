package com.ruben.backend.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Copying a public itinerary (I9). Laura copies the sample trip "Roma en cinco días" with a new
 * start date, lands on her new trip with its activities and deletes it at the end, so the shared
 * sample data is never changed: the copy is the only thing the test creates.
 */
@Tag("I9")
class TripCopyUiE2ETest {

  private static final String SAMPLE_PASSWORD = "Demo-2026";
  private static final String TRIP = "Roma en cinco días";

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

  // --- helpers ---

  private void click(By locator) {
    WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    ((JavascriptExecutor) driver)
        .executeScript(
            "arguments[0].scrollIntoView({block: 'center', behavior: 'instant'});", element);
    wait.until(ExpectedConditions.elementToBeClickable(element)).click();
  }

  private static By button(String text) {
    return By.xpath("//button[normalize-space()='" + text + "']");
  }

  private static By tripLink(String name) {
    return By.xpath("//main//ul//a[normalize-space()='" + name + "']");
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

  private void fillLoginForm() {
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email")))
        .sendKeys("laura@tripmate.com");
    driver.findElement(By.id("login-password")).sendKeys(SAMPLE_PASSWORD);
    click(By.cssSelector("form button[type='submit']"));
  }

  /** Opens the sample trip from the public list and leaves the browser on its preview page. */
  private String openPublicTrip() {
    driver.get(baseUrl + "/public");
    click(tripLink(TRIP));
    wait.until(ExpectedConditions.urlMatches(".*/public/\\d+$"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), TRIP));
    return driver.getCurrentUrl();
  }

  /** Best effort: whatever the test did, the copy must not stay behind. */
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

  // --- tests ---

  @Test
  void aUserCopiesAPublicItineraryWithANewStartDateAndDeletesTheCopy() {
    driver.get(baseUrl + "/login");
    fillLoginForm();
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("trips")));
    String publicUrl = openPublicTrip();
    String copyUrl = null;
    try {
      // The modal asks for an optional start date and nothing is copied until it is confirmed.
      click(button("Copiar en tu cuenta"));
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[role='dialog']")));
      assertThat(driver.findElement(By.cssSelector("[role='dialog']")).getText())
          .contains("Nueva fecha de inicio (opcional)", "se mantienen las fechas originales");
      setDate("copy-start-date", "2028-03-06");
      click(button("Copiar viaje"));

      // She lands on the new trip, with the success message, the moved dates and the activities.
      wait.until(ExpectedConditions.urlMatches(".*/trips/\\d+$"));
      copyUrl = driver.getCurrentUrl();
      wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), TRIP));
      assertThat(driver.findElement(By.cssSelector(".alert-success")).getText())
          .contains("Itinerario copiado en tus viajes. Ya puedes editarlo.");
      String page = driver.findElement(By.tagName("main")).getText();
      assertThat(page).contains("6 mar 2028", "10 mar 2028");
      wait.until(
          ExpectedConditions.visibilityOfElementLocated(
              By.xpath("//h4[normalize-space()='Visita guiada al Coliseo']")));
      // The copy is hers and private: she can edit and delete it, and it is not public.
      assertThat(driver.findElements(By.cssSelector(".badge"))).isEmpty();
      assertThat(driver.findElements(button("Publicar"))).hasSize(1);
      By tripEdit =
          By.xpath("//a[normalize-space()='Editar' and not(contains(@href, '/activities/'))]");
      By activityEdit =
          By.xpath("//a[normalize-space()='Editar' and contains(@href, '/activities/')]");
      assertThat(driver.findElements(tripEdit)).hasSize(1);
      assertThat(driver.findElements(activityEdit)).isNotEmpty();

      // Deleting the copy leaves the public trip as it was.
      deleteTrip(copyUrl);
      driver.get(copyUrl);
      wait.until(
          ExpectedConditions.textToBePresentInElementLocated(
              By.tagName("h1"), "No encontramos esta página"));
      copyUrl = null;
      driver.get(publicUrl);
      wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), TRIP));
      wait.until(
          ExpectedConditions.visibilityOfElementLocated(
              By.xpath("//h4[normalize-space()='Visita guiada al Coliseo']")));
    } finally {
      if (copyUrl != null) {
        deleteTrip(copyUrl);
      }
    }
  }

  @Test
  void aVisitorIsSentToTheLoginAndComesBackToTheSamePublicTrip() {
    String publicUrl = openPublicTrip();

    click(button("Copiar en tu cuenta"));

    wait.until(ExpectedConditions.urlMatches(".*/login$"));
    fillLoginForm();
    wait.until(ExpectedConditions.urlToBe(publicUrl));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), TRIP));
    // Nothing was copied: the modal is closed and the button is offered again.
    assertThat(driver.findElements(By.cssSelector("[role='dialog']"))).isEmpty();
    assertThat(driver.findElements(button("Copiar en tu cuenta"))).hasSize(1);
  }
}
