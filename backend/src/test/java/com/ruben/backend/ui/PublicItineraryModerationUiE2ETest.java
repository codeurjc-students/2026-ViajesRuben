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
 * The administrator unpublishes a public itinerary of another user (I10). A fresh user publishes a
 * uniquely named trip, so no sample trip is ever unpublished.
 */
@Tag("I10")
class PublicItineraryModerationUiE2ETest {

  private static final String PASSWORD = "secret-pass";
  // The development administrator, the same one the CI backend starts with.
  private static final String ADMIN_EMAIL = "admin@tripmate.com";
  private static final String ADMIN_PASSWORD = "admin";
  private static final By DIALOG = By.cssSelector("[role='dialog']");
  private static final By MAIN = By.tagName("main");

  private WebDriver driver;
  private WebDriverWait wait;
  private final String baseUrl = System.getProperty("app.url", "http://localhost:5173");
  private final String email = "ui-" + UUID.randomUUID() + "@example.com";
  private final String tripName = "Despublicar " + UUID.randomUUID().toString().substring(0, 8);

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

  private void register(String name, String address) {
    driver.get(baseUrl + "/register");
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("register-name")))
        .sendKeys(name);
    driver.findElement(By.id("register-email")).sendKeys(address);
    driver.findElement(By.id("register-password")).sendKeys(PASSWORD);
    driver.findElement(By.id("register-confirmation")).sendKeys(PASSWORD);
    click(By.cssSelector("form button[type='submit']"));
    wait.until(ExpectedConditions.urlContains("/trips"));
  }

  private void createAndPublishTrip(String name) {
    click(By.xpath("//a[normalize-space()='Crear viaje']"));
    replaceText(By.id("trip-name"), name);
    replaceText(By.id("trip-destination"), "Moderlandia");
    setDate("trip-start-date", "2028-08-01");
    setDate("trip-end-date", "2028-08-05");
    click(By.xpath("//button[normalize-space()='Crear viaje']"));
    wait.until(ExpectedConditions.urlMatches(".*/trips/\\d+$"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), name));
    click(By.xpath("//main//button[normalize-space()='Publicar']"));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//main//button[normalize-space()='Despublicar']")));
  }

  /**
   * The creator takes the trip off the public list again from the trip page, so a trip published by
   * this test never stays in the home carousel, where other tests expect the sample trips.
   */
  private void unpublishOwnTrip() {
    driver.get(baseUrl + "/trips");
    click(By.xpath("//main//a[normalize-space()='" + tripName + "']"));
    click(By.xpath("//main//button[normalize-space()='Despublicar']"));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//main//button[normalize-space()='Publicar']")));
  }

  private void logOut() {
    click(By.xpath("//button[normalize-space()='Cerrar sesión']"));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//a[normalize-space()='Iniciar sesión']")));
  }

  /** Opens the login page from the header link, as a visitor does. */
  private void logIn(String address, String password) {
    click(By.xpath("//a[normalize-space()='Iniciar sesión']"));
    replaceText(By.id("login-email"), address);
    replaceText(By.id("login-password"), password);
    click(By.cssSelector("form button[type='submit']"));
    wait.until(ExpectedConditions.urlContains("/trips"));
  }

  private void searchPublicTrips(String text) {
    replaceText(By.id("public-trips-search"), text);
    click(By.xpath("//form[@role='search']//button[normalize-space()='Buscar']"));
  }

  private By tripLink() {
    return By.xpath("//main//ul//a[normalize-space()='" + tripName + "']");
  }

  // --- tests ---

  @Test
  void theAdministratorUnpublishesAnItineraryAndItsCreatorKeepsTheTrip() {
    register("Lucía", email);
    createAndPublishTrip(tripName);
    logOut();
    logIn(ADMIN_EMAIL, ADMIN_PASSWORD);

    // The administrator finds the public itinerary by its name.
    driver.get(baseUrl + "/public");
    searchPublicTrips(tripName);
    wait.until(ExpectedConditions.visibilityOfElementLocated(tripLink()));

    // Unpublishing asks for confirmation and says the trip is kept.
    click(By.cssSelector("button[aria-label='Despublicar " + tripName + "']"));
    WebElement dialog = wait.until(ExpectedConditions.visibilityOfElementLocated(DIALOG));
    assertThat(dialog.getText()).contains("dejará de ser público; el viaje no se borra.");
    click(By.xpath("//*[@role='dialog']//button[normalize-space()='Despublicar']"));
    wait.until(ExpectedConditions.invisibilityOfElementLocated(DIALOG));

    // It is gone from the public list, and the page says so.
    wait.until(ExpectedConditions.invisibilityOfElementLocated(tripLink()));
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            MAIN, "«" + tripName + "» ya no es público."));

    // Its creator still has the trip, no longer public.
    logOut();
    logIn(email, PASSWORD);
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//main//a[normalize-space()='" + tripName + "']")));
    assertThat(driver.findElement(MAIN).getText()).doesNotContain("Público");
  }

  @Test
  void theAdministratorUnpublishesFromThePreviewAndGoesBackToTheList() {
    register("Lucía", email);
    createAndPublishTrip(tripName);
    logOut();
    logIn(ADMIN_EMAIL, ADMIN_PASSWORD);
    driver.get(baseUrl + "/public");
    searchPublicTrips(tripName);
    click(tripLink());
    wait.until(ExpectedConditions.urlMatches(".*/public/\\d+$"));

    click(By.xpath("//main//button[normalize-space()='Despublicar']"));
    click(By.xpath("//*[@role='dialog']//button[normalize-space()='Despublicar']"));

    wait.until(ExpectedConditions.urlMatches(".*/public$"));
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            MAIN, "«" + tripName + "» ya no es público."));
    searchPublicTrips(tripName);
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            MAIN, "Ningún viaje público coincide con la búsqueda."));
  }

  @Test
  void aRegularUserSeesNoUnpublishButtonOnThePublicPages() {
    register("Lucía", email);
    createAndPublishTrip(tripName);

    try {
      driver.get(baseUrl + "/public");
      searchPublicTrips(tripName);
      wait.until(ExpectedConditions.visibilityOfElementLocated(tripLink()));

      assertThat(driver.findElements(By.cssSelector("button[aria-label^='Despublicar ']")))
          .isEmpty();
      click(tripLink());
      wait.until(ExpectedConditions.urlMatches(".*/public/\\d+$"));
      wait.until(
          ExpectedConditions.visibilityOfElementLocated(
              By.xpath("//main//button[normalize-space()='Copiar en tu cuenta']")));
      assertThat(driver.findElements(By.xpath("//main//button[normalize-space()='Despublicar']")))
          .isEmpty();
    } finally {
      unpublishOwnTrip();
    }
  }
}
