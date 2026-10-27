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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * The conflict warning as the creator of a trip sees it. Laura logs in as the sample user that the
 * application loads with SAMPLE_DATA (see the README) and works on her "Alps in winter" trip, which
 * starts without activities and which no other UI test changes.
 */
@Tag("B6")
class ConflictUiE2ETest {

  private static final String SAMPLE_PASSWORD = "Demo-2026";
  private static final By WARNING = By.cssSelector(".alert-warning");

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

  private static By link(String text) {
    return By.xpath("//a[normalize-space()='" + text + "']");
  }

  /**
   * Date and time inputs take their text in the browser's locale, so the value is set as React
   * reads it.
   */
  private void setValue(String id, String value) {
    wait.until(ExpectedConditions.presenceOfElementLocated(By.id(id)));
    ((JavascriptExecutor) driver)
        .executeScript(
            "const input = document.getElementById(arguments[0]);"
                + " const setter = Object.getOwnPropertyDescriptor("
                + "HTMLInputElement.prototype, 'value').set;"
                + " setter.call(input, arguments[1]);"
                + " input.dispatchEvent(new Event('input', {bubbles: true}));",
            id,
            value);
  }

  private void logInAs(String email) {
    driver.get(baseUrl + "/login");
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email"))).sendKeys(email);
    driver.findElement(By.id("login-password")).sendKeys(SAMPLE_PASSWORD);
    click(By.cssSelector("form button[type='submit']"));
    wait.until(ExpectedConditions.urlContains("/trips"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("trips")));
  }

  private void openTrip(String name) {
    click(By.xpath("//ul[@id='trips']//a[normalize-space()='" + name + "']"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), name));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//h2[normalize-space()='Itinerario']")));
  }

  private void waitForItineraryText(String text) {
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.cssSelector("section.itinerary"), text));
  }

  /** Creates an activity on the first day of the trip, the one the form starts on. */
  private void createActivity(String name, String start, String end) {
    click(link("Nueva actividad"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("activity-name"))).sendKeys(name);
    setValue("activity-start-time", start);
    setValue("activity-end-time", end);
    click(button("Crear actividad"));
    waitForItineraryText(name);
  }

  private void changeTimes(String name, String start, String end) {
    click(By.cssSelector("a[aria-label='Editar " + name + "']"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("activity-name")));
    setValue("activity-start-time", start);
    setValue("activity-end-time", end);
    click(button("Guardar cambios"));
    wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/edit")));
    waitForItineraryText(name);
  }

  private void deleteActivity(String name) {
    click(By.cssSelector("button[aria-label='Eliminar " + name + "']"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[role='dialog']")));
    click(button("Eliminar actividad"));
    wait.until(
        ExpectedConditions.not(
            ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("section.itinerary"), name)));
  }

  // --- test ---

  @Test
  void twoOverlappingActivitiesShowTheWarningAndEditingOneRemovesIt() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String tour = "Tour " + suffix;
    String lunch = "Lunch " + suffix;
    logInAs("laura@tripmate.com");
    openTrip("Alps in winter");
    waitForItineraryText("No hay actividades este día");
    assertThat(driver.findElements(WARNING)).isEmpty();

    // The first activity alone is no conflict; the second one overlaps it.
    createActivity(tour, "10:00", "12:00");
    createActivity(lunch, "11:00", "13:00");

    String warning = wait.until(ExpectedConditions.visibilityOfElementLocated(WARNING)).getText();
    assertThat(warning)
        .contains("Conflictos en el itinerario", tour, lunch, "10:00–12:00", "11:00–13:00")
        .contains("se solapan el");

    // Moving the second one right after the first only touches it: the warning goes away.
    changeTimes(lunch, "12:00", "13:00");

    wait.until(ExpectedConditions.invisibilityOfElementLocated(WARNING));
    assertThat(driver.findElements(WARNING)).isEmpty();

    // Leave the trip as it was found.
    deleteActivity(lunch);
    deleteActivity(tour);
    waitForItineraryText("No hay actividades este día");
  }
}
