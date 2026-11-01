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

@Tag("B8")
class AdminTripUiE2ETest {

  private static final String PASSWORD = "secret-pass";
  // The development administrator, the same one the CI backend starts with.
  private static final String ADMIN_EMAIL = "admin@tripmate.com";
  private static final String ADMIN_PASSWORD = "admin";
  private static final By DIALOG = By.cssSelector("[role='dialog']");
  private static final By MAIN = By.tagName("main");

  private WebDriver driver;
  private WebDriverWait wait;
  private final String baseUrl = System.getProperty("app.url", "http://localhost:5173");
  // A fresh user with a fresh trip, so no trip that other tests rely on is ever deleted.
  private final String email = "ui-" + UUID.randomUUID() + "@example.com";
  private final String tripName = "Moderar " + UUID.randomUUID().toString().substring(0, 8);

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

  private void createTrip(String name) {
    click(By.xpath("//a[normalize-space()='Crear viaje']"));
    replaceText(By.id("trip-name"), name);
    replaceText(By.id("trip-destination"), "Moderlandia");
    setDate("trip-start-date", "2028-08-01");
    setDate("trip-end-date", "2028-08-05");
    click(By.xpath("//button[normalize-space()='Crear viaje']"));
    wait.until(ExpectedConditions.urlMatches(".*/trips/\\d+$"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), name));
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
  }

  private void searchFor(String text) {
    replaceText(By.id("admin-trips-search"), text);
    click(By.xpath("//button[normalize-space()='Buscar']"));
  }

  // --- tests ---

  @Test
  void adminSearchesATripOpensItReadOnlyAndDeletesIt() {
    register("Lucía", email);
    createTrip(tripName);
    logOut();
    logIn(ADMIN_EMAIL, ADMIN_PASSWORD);
    wait.until(ExpectedConditions.urlContains("/trips"));

    // Find the trip among all the trips, by its name.
    click(By.xpath("//a[normalize-space()='Viajes']"));
    wait.until(ExpectedConditions.urlContains("/admin/trips"));
    searchFor(tripName);
    By row = By.cssSelector(".admin-trip");
    wait.until(ExpectedConditions.textToBePresentInElementLocated(row, tripName));
    assertThat(driver.findElements(row)).hasSize(1);
    assertThat(driver.findElement(row).getText()).contains("Moderlandia").contains(email);

    // Open it: everything is shown, nothing can be edited.
    click(By.cssSelector("a[aria-label='Ver viaje " + tripName + "']"));
    wait.until(ExpectedConditions.urlMatches(".*/trips/\\d+$"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), tripName));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(MAIN, "Itinerario"));
    String detail = driver.findElement(MAIN).getText();
    assertThat(detail).contains("Moderlandia", "Volver a Viajes", "Eliminar viaje");
    assertThat(detail).doesNotContain("Nueva actividad");
    assertThat(driver.findElements(By.xpath("//a[normalize-space()='Editar']"))).isEmpty();
    assertThat(driver.findElements(By.xpath("//button[normalize-space()='Eliminar']"))).isEmpty();

    // Delete it, with confirmation, and land back on the list.
    click(By.xpath("//main//button[normalize-space()='Eliminar viaje']"));
    click(By.xpath("//*[@role='dialog']//button[normalize-space()='Eliminar viaje']"));
    wait.until(ExpectedConditions.invisibilityOfElementLocated(DIALOG));
    wait.until(ExpectedConditions.urlContains("/admin/trips"));

    // It is gone from the list.
    searchFor(tripName);
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            MAIN, "Ningún viaje coincide con la búsqueda."));
    assertThat(driver.findElements(By.cssSelector(".admin-trip"))).isEmpty();
  }

  @Test
  void aRegularUserOpeningTheTripModerationPageSeesTheNoAccessPage() {
    register("Lucía", email);

    driver.get(baseUrl + "/admin/trips");

    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(MAIN, "No tienes acceso a esta página"));
    assertThat(driver.findElements(By.id("admin-trips-search"))).isEmpty();
    assertThat(driver.findElements(By.xpath("//a[normalize-space()='Viajes']"))).isEmpty();
  }
}
