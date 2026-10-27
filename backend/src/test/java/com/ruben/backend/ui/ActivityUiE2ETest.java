package com.ruben.backend.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * The itinerary as the creator of a trip uses it. They log in as the sample users that the
 * application loads with SAMPLE_DATA (see the README), whose trips Ana and Laura own come with an
 * itinerary: Ana's "Lisbon getaway" and Laura's "Weekend in Porto" (whose third day is empty).
 */
@Tag("B5")
class ActivityUiE2ETest {

  private static final String SAMPLE_PASSWORD = "Demo-2026";

  @TempDir Path tempDir;

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

  private static By tab(String text) {
    return By.xpath("//button[@role='tab'][contains(normalize-space(), '" + text + "')]");
  }

  /** Selenium's clear() does not reach React's state, so the old text is selected and deleted. */
  private void replaceText(By locator, String text) {
    WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
    field.sendKeys(text);
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

  /** Opens a trip of the logged-in user from the list and waits for its itinerary. */
  private void openTrip(String name) {
    click(By.xpath("//ul[@id='trips']//a[normalize-space()='" + name + "']"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), name));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//h2[normalize-space()='Itinerario']")));
  }

  private String itinerary() {
    return driver.findElement(By.cssSelector("section.itinerary")).getText();
  }

  private void waitForItineraryText(String text) {
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.cssSelector("section.itinerary"), text));
  }

  /** A real PNG: the browser must be able to decode it, or the image never loads. */
  private String newPicture() throws IOException {
    Path file = tempDir.resolve("activity.png");
    ImageIO.write(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB), "png", file.toFile());
    return file.toString();
  }

  private void waitForLoadedPicture(String cssSelector) {
    wait.until(
        driver ->
            Boolean.TRUE.equals(
                ((JavascriptExecutor) driver)
                    .executeScript(
                        "const img = document.querySelector(arguments[0]);"
                            + " return !!img && img.complete && img.naturalWidth > 0;",
                        cssSelector)));
  }

  private void openNewActivityForm() {
    click(link("Nueva actividad"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("activity-name")));
  }

  // --- tests ---

  @Test
  void lauraBrowsesThePortoDaysThenCreatesEditsAndDeletesAnActivityWithAnImageOnTheEmptyDay()
      throws IOException {
    String name = "UI activity " + UUID.randomUUID().toString().substring(0, 8);
    String renamed = name + " edited";
    logInAs("laura@tripmate.com");
    openTrip("Weekend in Porto");

    // Day 1 is the first one, and its activities come in time order.
    assertThat(driver.findElements(By.cssSelector("button[role='tab']"))).hasSize(3);
    wait.until(ExpectedConditions.attributeToBe(tab("Día 1"), "aria-selected", "true"));
    waitForItineraryText("Francesinha lunch");
    assertThat(itinerary().indexOf("Ribeira walk"))
        .isLessThan(itinerary().indexOf("Francesinha lunch"));
    assertThat(itinerary()).contains("2 actividades", "10:00 – 12:00", "12,50");

    // The next button moves to day 2, which has a picture.
    click(By.cssSelector("button[aria-label='Día siguiente']"));
    waitForItineraryText("Port wine cellar visit");
    assertThat(itinerary()).contains("Livraria Lello").doesNotContain("Ribeira walk");
    assertThat(driver.getCurrentUrl()).contains("day=2027-03-13");
    waitForLoadedPicture("img[alt='Imagen de Port wine cellar visit']");

    // Day 3 is empty and invites the first activity.
    click(tab("Día 3"));
    waitForItineraryText("No hay actividades este día");
    assertThat(driver.getCurrentUrl()).contains("day=2027-03-14");

    // Create one there, with an image.
    openNewActivityForm();
    assertThat(driver.findElement(By.id("activity-date")).getAttribute("value"))
        .isEqualTo("2027-03-14");
    replaceText(By.id("activity-name"), name);
    new Select(driver.findElement(By.id("activity-category"))).selectByValue("LEISURE");
    setValue("activity-start-time", "11:00");
    setValue("activity-end-time", "12:30");
    replaceText(By.id("activity-cost"), "7,50");
    replaceText(By.id("activity-location"), "Foz do Douro");
    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='file']")))
        .sendKeys(newPicture());
    wait.until(
        ExpectedConditions.presenceOfElementLocated(By.cssSelector("img[alt='Vista previa']")));
    click(button("Crear actividad"));

    // It lands on day 3 of the trip, with the data and the image.
    wait.until(ExpectedConditions.urlContains("day=2027-03-14"));
    waitForItineraryText(name);
    assertThat(itinerary()).contains("11:00 – 12:30", "1 h 30 min", "Ocio", "Foz do Douro", "7,50");
    waitForLoadedPicture("img[alt='Imagen de " + name + "']");

    // Edit it from its card.
    click(By.cssSelector("a[aria-label='Editar " + name + "']"));
    replaceText(By.id("activity-name"), renamed);
    click(button("Guardar cambios"));
    wait.until(ExpectedConditions.urlContains("day=2027-03-14"));
    waitForItineraryText(renamed);

    // Cancelling the confirmation keeps it.
    click(By.cssSelector("button[aria-label='Eliminar " + renamed + "']"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[role='dialog']")));
    click(button("Cancelar"));
    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("[role='dialog']")));
    assertThat(itinerary()).contains(renamed);

    // Confirming deletes it and the day is empty again.
    click(By.cssSelector("button[aria-label='Eliminar " + renamed + "']"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[role='dialog']")));
    click(button("Eliminar actividad"));
    waitForItineraryText("No hay actividades este día");
    assertThat(itinerary()).doesNotContain(renamed);
  }

  @Test
  void aDayOutsideTheTripIsExplainedAndNothingIsCreated() {
    String name = "Invalid UI activity " + UUID.randomUUID().toString().substring(0, 8);
    logInAs("laura@tripmate.com");
    openTrip("Weekend in Porto");
    openNewActivityForm();
    replaceText(By.id("activity-name"), name);
    setValue("activity-date", "2027-03-20");

    click(button("Crear actividad"));

    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("main"), "El día debe estar dentro de las fechas del viaje."));
    assertThat(driver.getCurrentUrl()).contains("/activities/new");
    click(button("Cancelar"));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//h2[normalize-space()='Itinerario']")));
    assertThat(itinerary()).doesNotContain(name);
  }

  @Test
  void anEndBeforeTheStartIsExplainedInTheForm() {
    logInAs("laura@tripmate.com");
    openTrip("Weekend in Porto");
    openNewActivityForm();
    replaceText(By.id("activity-name"), "Never saved");
    setValue("activity-start-time", "12:00");
    setValue("activity-end-time", "10:00");

    click(button("Crear actividad"));

    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("main"), "La hora de fin debe ser posterior a la de inicio."));
    assertThat(driver.getCurrentUrl()).contains("/activities/new");
  }

  @Test
  void anaSeesTheLisbonItineraryDayByDayWithItsImagesAndAllDayActivities() {
    logInAs("ana@tripmate.com");
    openTrip("Lisbon getaway");

    assertThat(driver.findElements(By.cssSelector("button[role='tab']"))).hasSize(5);
    waitForItineraryText("Alfama walking tour");
    waitForLoadedPicture("img[alt='Imagen de Alfama walking tour']");
    assertThat(itinerary()).contains("Lunch at Time Out Market", "18,50");

    click(tab("Día 2"));
    waitForItineraryText("Belém Tower");
    assertThat(itinerary()).contains("Pastéis de Belém tasting", "Tram 28 ride", "3 actividades");
    waitForLoadedPicture("img[alt='Imagen de Belém Tower']");

    click(tab("Día 3"));
    waitForItineraryText("Day trip to Sintra");
    assertThat(itinerary()).contains("Todo el día", "Fado dinner");

    click(tab("Día 5"));
    waitForItineraryText("No hay actividades este día");
    assertThat(driver.findElement(By.cssSelector("button[aria-label='Día siguiente']")).isEnabled())
        .isFalse();
  }

  @Test
  void theSelectedDayIsInTheUrlSoBackAndReloadKeepIt() {
    logInAs("ana@tripmate.com");
    openTrip("Lisbon getaway");
    click(tab("Día 2"));
    waitForItineraryText("Belém Tower");
    String url = driver.getCurrentUrl();

    driver.navigate().refresh();
    wait.until(ExpectedConditions.attributeToBe(tab("Día 2"), "aria-selected", "true"));
    waitForItineraryText("Belém Tower");
    driver.navigate().back();

    wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(url)));
    wait.until(ExpectedConditions.attributeToBe(tab("Día 1"), "aria-selected", "true"));
    waitForItineraryText("Alfama walking tour");
  }

  @Test
  void onAPhoneTheDayStripScrollsInsteadOfWideningThePage() {
    driver.manage().window().setSize(new Dimension(375, 800));
    logInAs("ana@tripmate.com");
    openTrip("Lisbon getaway");
    waitForItineraryText("Alfama walking tour");

    Object stripScrolls =
        ((JavascriptExecutor) driver)
            .executeScript(
                "const strip = document.querySelector('.itinerary-days__tabs');"
                    + " return strip.scrollWidth > strip.clientWidth;");
    Object pageOverflows =
        ((JavascriptExecutor) driver)
            .executeScript("return document.documentElement.scrollWidth > window.innerWidth;");

    assertThat(stripScrolls).isEqualTo(true);
    assertThat(pageOverflows).isEqualTo(false);
  }

  @Test
  void aTripOfAnotherUserHasNoItineraryForAVisitorWhoIsNotItsCreator() {
    logInAs("ana@tripmate.com");
    String anaTrip =
        driver.findElement(By.cssSelector("#trips > li a[href^='/trips/']")).getAttribute("href");
    click(button("Cerrar sesión"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(link("Iniciar sesión")));

    logInAs("carlos@tripmate.com");
    driver.get(anaTrip);

    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("h1"), "No tienes acceso a esta página"));
    assertThat(driver.findElements(By.xpath("//h2[normalize-space()='Itinerario']"))).isEmpty();
  }
}
