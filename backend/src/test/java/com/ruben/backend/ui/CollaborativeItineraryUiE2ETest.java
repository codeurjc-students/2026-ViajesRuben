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
 * The collaborative itinerary (I4). A user who has just registered is invited by Ana to her sample
 * trip "Lisbon getaway" and accepts. The guest adds an activity and edits one of Ana's, the
 * activity that Ana added before, and both see the changes in "Historial de cambios". The guest
 * then deletes Ana's activity and their own and leaves the trip, and Ana still sees every line with
 * the guest's name. The activities are created on the last day of the trip and deleted again, and
 * the guest leaves, so the sample data is left as it was. Every run uses a different user and
 * different names.
 */
@Tag("I4")
class CollaborativeItineraryUiE2ETest {

  private static final String ANA_EMAIL = "ana@tripmate.com";
  private static final String SAMPLE_PASSWORD = "Demo-2026";
  private static final String PASSWORD = "secret-pass";
  private static final String TRIP = "Lisbon getaway";
  private static final String INVITATIONS = "//section[@aria-labelledby='invitations-title']";
  private static final String PENDING = "//section[@aria-labelledby='pending-invitations']";
  private static final String PARTICIPANTS = "//section[@aria-labelledby='participants-title']";
  private static final String LOG = "//section[@aria-labelledby='activity-log-title']";
  private static final String DIALOG = "//div[@role='dialog']";
  private static final By EMAIL_FIELD = By.id("invitation-email");

  private WebDriver driver;
  private WebDriverWait wait;
  private final String baseUrl = System.getProperty("app.url", "http://localhost:5173");
  private final String unique = UUID.randomUUID().toString();
  private final String guestName = "Guest " + unique.substring(0, 8);
  private final String guest = "guest-" + unique + "@example.com";
  private final String anasActivity = "Ana plan " + unique.substring(0, 8);
  private final String anasDraft = anasActivity + " borrador";
  private final String anasEdited = anasActivity + " revisada";
  private final String guestsActivity = "Guest plan " + unique.substring(0, 8);

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

  private static By logoutButton() {
    return button("Cerrar sesión");
  }

  /** Selenium's clear() does not reach React's state, so the old text is selected and deleted. */
  private void replaceText(By locator, String text) {
    WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
    field.sendKeys(text);
  }

  /** Registers the guest through the form; registering also logs the new user in. */
  private void registerGuest() {
    driver.get(baseUrl + "/register");
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("register-name")))
        .sendKeys(guestName);
    driver.findElement(By.id("register-email")).sendKeys(guest);
    driver.findElement(By.id("register-password")).sendKeys(PASSWORD);
    driver.findElement(By.id("register-confirmation")).sendKeys(PASSWORD);
    click(By.cssSelector("form button[type='submit']"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(logoutButton()));
  }

  private void logOut() {
    click(logoutButton());
    wait.until(ExpectedConditions.invisibilityOfElementLocated(logoutButton()));
  }

  private void logIn(String address, String password) {
    driver.get(baseUrl + "/login");
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email")))
        .sendKeys(address);
    driver.findElement(By.id("login-password")).sendKeys(password);
    click(By.cssSelector("form button[type='submit']"));
    wait.until(ExpectedConditions.urlContains("/trips"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(logoutButton()));
  }

  private static By tripLink() {
    return By.xpath("//ul[@id='trips']//a[normalize-space()='" + TRIP + "']");
  }

  /** Opens the trip from "Mis viajes" and waits for its itinerary and its change log. */
  private void openTrip() {
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("trips")));
    click(tripLink());
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), TRIP));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//h2[normalize-space()='Itinerario']")));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath(LOG + "//h2[normalize-space()='Historial de cambios']")));
  }

  /** Ana invites the guest from her trip page. */
  private void inviteGuest() {
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath(INVITATIONS + "//h2[normalize-space()='Invitaciones']")));
    WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD));
    field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
    field.sendKeys(guest);
    click(By.xpath(INVITATIONS + "//button[normalize-space()='Enviar invitación']"));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath(INVITATIONS + "//li[.//span[normalize-space()='" + guest + "']]")));
  }

  private void guestAccepts() {
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(PENDING)));
    click(By.xpath(PENDING + "//button[@aria-label='Aceptar invitación a " + TRIP + "']"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(tripLink()));
  }

  private void goToTheLastDay() {
    click(By.xpath("(//button[@role='tab'])[last()]"));
    wait.until(
        ExpectedConditions.attributeToBe(
            By.xpath("(//button[@role='tab'])[last()]"), "aria-selected", "true"));
  }

  private void waitForItineraryText(String text) {
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.cssSelector("section.itinerary"), text));
  }

  /** Adds an activity on the last day of the trip, with the form of the itinerary. */
  private void addActivity(String name) {
    goToTheLastDay();
    click(By.xpath("//a[normalize-space()='Nueva actividad']"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("activity-name")));
    replaceText(By.id("activity-name"), name);
    click(button("Crear actividad"));
    waitForItineraryText(name);
  }

  private void renameActivity(String name, String newName) {
    click(By.cssSelector("a[aria-label='Editar " + name + "']"));
    replaceText(By.id("activity-name"), newName);
    click(button("Guardar cambios"));
    waitForItineraryText(newName);
  }

  private void deleteActivity(String name) {
    click(By.cssSelector("button[aria-label='Eliminar " + name + "']"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(DIALOG)));
    click(By.xpath(DIALOG + "//button[normalize-space()='Eliminar actividad']"));
    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(
            By.cssSelector("button[aria-label='Eliminar " + name + "']")));
  }

  private void waitForLogText(String text) {
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.xpath(LOG), text));
  }

  private String log() {
    return driver.findElement(By.xpath(LOG)).getText();
  }

  // --- test ---

  @Test
  void aGuestPlansTheTripWithAnaAndEveryChangeIsInTheChangeLogForBoth() {
    // Ana invites the guest, who has just registered, and adds an activity of her own.
    registerGuest();
    logOut();
    logIn(ANA_EMAIL, SAMPLE_PASSWORD);
    openTrip();
    inviteGuest();
    addActivity(anasActivity);
    waitForLogText("añadió «" + anasActivity + "»");
    logOut();

    // The guest accepts, sees who added Ana's activity, adds one and edits Ana's.
    logIn(guest, PASSWORD);
    guestAccepts();
    openTrip();
    goToTheLastDay();
    waitForItineraryText(anasActivity);
    assertThat(driver.findElement(By.cssSelector("section.itinerary")).getText())
        .contains("Añadida por");
    addActivity(guestsActivity);
    // Two edits, so that this run writes six lines in all and the log has more than the five it
    // shows at first.
    renameActivity(anasActivity, anasDraft);
    renameActivity(anasDraft, anasEdited);

    // The guest sees both changes in the log: one short line each, the edit with what changed.
    waitForLogText(guestName + " añadió «" + guestsActivity + "»");
    waitForLogText(guestName + " editó «" + anasEdited + "»");
    assertThat(log()).contains("Cambios: nombre");
    assertThat(driver.findElements(By.xpath(LOG + "//li"))).hasSizeLessThanOrEqualTo(5);
    logOut();

    // Ana sees the same changes, and her own addition under the name it had.
    logIn(ANA_EMAIL, SAMPLE_PASSWORD);
    openTrip();
    goToTheLastDay();
    waitForItineraryText(guestsActivity);
    waitForLogText(guestName + " añadió «" + guestsActivity + "»");
    waitForLogText(guestName + " editó «" + anasEdited + "»");
    assertThat(log()).contains("añadió «" + anasActivity + "»");
    logOut();

    // The guest deletes Ana's activity, and their own, and leaves the trip.
    logIn(guest, PASSWORD);
    openTrip();
    goToTheLastDay();
    waitForItineraryText(anasEdited);
    deleteActivity(anasEdited);
    waitForLogText(guestName + " eliminó «" + anasEdited + "»");
    deleteActivity(guestsActivity);
    waitForLogText(guestName + " eliminó «" + guestsActivity + "»");
    click(By.xpath(PARTICIPANTS + "//button[normalize-space()='Salir del viaje']"));
    click(By.xpath(DIALOG + "//button[normalize-space()='Salir del viaje']"));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[@role='alert'][contains(., 'Has salido de «" + TRIP + "».')]")));
    logOut();

    // Ana still sees every change of the guest, deletions included, with the guest's name.
    logIn(ANA_EMAIL, SAMPLE_PASSWORD);
    openTrip();
    goToTheLastDay();
    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(
            By.cssSelector("section.itinerary output")));
    assertThat(driver.findElement(By.cssSelector("section.itinerary")).getText())
        .doesNotContain(anasEdited)
        .doesNotContain(guestsActivity);
    waitForLogText(guestName + " eliminó «" + anasEdited + "»");
    assertThat(log())
        .contains(guestName + " eliminó «" + guestsActivity + "»")
        .contains(guestName + " editó «" + anasEdited + "»")
        .contains(guestName + " añadió «" + guestsActivity + "»");
    assertThat(driver.findElement(By.xpath(PARTICIPANTS)).getText()).doesNotContain(guestName);

    // The log shows only the five latest lines, although this run wrote six; "Ver más" shows the
    // rest, including Ana's own addition, and "Ver menos" goes back to five.
    By lines = By.xpath(LOG + "//li");
    assertThat(driver.findElements(lines)).hasSize(5);
    assertThat(log()).doesNotContain("añadió «" + anasActivity + "»");
    click(By.xpath(LOG + "//button[normalize-space()='Ver más']"));
    wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(lines, 5));
    waitForLogText("añadió «" + anasActivity + "»");
    click(By.xpath(LOG + "//button[normalize-space()='Ver menos']"));
    wait.until(ExpectedConditions.numberOfElementsToBe(lines, 5));
    assertThat(log()).doesNotContain("añadió «" + anasActivity + "»");
  }
}
