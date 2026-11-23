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
 * Managing the participants (I3): a user who has just registered is invited by Ana to her sample
 * trip "Lisbon getaway" and accepts through "Invitaciones pendientes". Ana then sees them in
 * "Participantes" and removes them, after which they no longer see the trip; in the other test the
 * participant leaves the trip by themselves. Every run registers a different user with a different
 * name, so the participations it leaves behind never clash with another run.
 */
@Tag("I3")
class ParticipantsUiE2ETest {

  private static final String ANA_EMAIL = "ana@tripmate.com";
  private static final String SAMPLE_PASSWORD = "Demo-2026";
  private static final String PASSWORD = "secret-pass";
  private static final String TRIP = "Lisbon getaway";
  private static final String INVITATIONS = "//section[@aria-labelledby='invitations-title']";
  private static final String PENDING = "//section[@aria-labelledby='pending-invitations']";
  private static final String PARTICIPANTS = "//section[@aria-labelledby='participants-title']";
  private static final String DIALOG = "//div[@role='dialog']";
  private static final By EMAIL_FIELD = By.id("invitation-email");

  private WebDriver driver;
  private WebDriverWait wait;
  private final String baseUrl = System.getProperty("app.url", "http://localhost:5173");
  private final String unique = UUID.randomUUID().toString();
  private final String guestName = "Guest " + unique.substring(0, 8);
  private final String guest = "guest-" + unique + "@example.com";
  private int participantsBefore;

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

  private static By logoutButton() {
    return By.xpath("//button[normalize-space()='Cerrar sesión']");
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

  private void openTrip() {
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("trips")));
    click(tripLink());
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), TRIP));
  }

  private void openAnasTrip() {
    openTrip();
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath(INVITATIONS + "//h2[normalize-space()='Invitaciones']")));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath(PARTICIPANTS + "//h2[normalize-space()='Participantes']")));
  }

  /** Selenium's clear() does not reach React's state, so the old text is selected and deleted. */
  private void inviteGuest() {
    WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD));
    field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
    field.sendKeys(guest);
    click(By.xpath(INVITATIONS + "//button[normalize-space()='Enviar invitación']"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(invitationRow()));
  }

  /** The row of Ana's invitations list for the guest; it has the address and the state. */
  private By invitationRow() {
    return By.xpath(INVITATIONS + "//li[.//span[normalize-space()='" + guest + "']]");
  }

  /** The row of the participants list for the guest; only the creator sees the address. */
  private By participantRow() {
    return By.xpath(PARTICIPANTS + "//li[.//span[normalize-space()='" + guest + "']]");
  }

  /** The guest accepts from "Invitaciones pendientes" and the trip shows up in "Mis viajes". */
  private void guestAccepts() {
    click(By.xpath(PENDING + "//button[@aria-label='Aceptar invitación a " + TRIP + "']"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(tripLink()));
  }

  /** Ana invites the guest, who registers beforehand and accepts; the guest is logged in. */
  private void guestJoinsAnasTrip() {
    registerGuest();
    logOut();
    logIn(ANA_EMAIL, SAMPLE_PASSWORD);
    // The sample trip may already have participants, so every count is relative to this one.
    participantsBefore = participantsOnTheCard();
    openAnasTrip();
    inviteGuest();
    logOut();
    logIn(guest, PASSWORD);
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(PENDING)));
    guestAccepts();
    // The guest's own card counts them too.
    assertThat(participantsOnTheCard()).isEqualTo(participantsBefore + 1);
  }

  /** The number that the card of the trip, in "Mis viajes", shows after "Participantes:". */
  private int participantsOnTheCard() {
    By line =
        By.xpath(
            "//ul[@id='trips']//li[.//a[normalize-space()='"
                + TRIP
                + "']]//p[starts-with(normalize-space(), 'Participantes:')]");
    String text = wait.until(ExpectedConditions.visibilityOfElementLocated(line)).getText();
    return Integer.parseInt(text.substring(text.indexOf(':') + 1).trim());
  }

  private void assertGuestHasNoTrip() {
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//a[normalize-space()='Añadir tu primer viaje']")));
    assertThat(driver.findElements(tripLink())).isEmpty();
  }

  // --- tests ---

  @Test
  void theCreatorSeesTheNewParticipantAndRemovesThemAndTheyLoseTheTrip() {
    guestJoinsAnasTrip();
    logOut();
    logIn(ANA_EMAIL, SAMPLE_PASSWORD);
    assertThat(participantsOnTheCard()).isEqualTo(participantsBefore + 1);
    openAnasTrip();

    // Ana sees the guest, with their address, name and the button to remove them.
    WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(participantRow()));
    assertThat(row.getText()).contains(guestName).contains(guest).contains("Se unió el");

    click(By.xpath(PARTICIPANTS + "//li[.//span[normalize-space()='" + guest + "']]//button"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(DIALOG)));
    assertThat(driver.findElement(By.xpath(DIALOG)).getText())
        .contains("Quitar participante")
        .contains("Se quitará a " + guestName + " del viaje.");

    // Cancelling keeps the guest in the trip.
    click(By.xpath(DIALOG + "//button[normalize-space()='Cancelar']"));
    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(DIALOG)));
    assertThat(driver.findElements(participantRow())).hasSize(1);

    // Confirming removes them from the list, and it stays removed after reloading.
    click(By.xpath(PARTICIPANTS + "//li[.//span[normalize-space()='" + guest + "']]//button"));
    click(By.xpath(DIALOG + "//button[normalize-space()='Quitar del viaje']"));
    wait.until(ExpectedConditions.invisibilityOfElementLocated(participantRow()));
    driver.navigate().refresh();
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath(PARTICIPANTS + "//h2[normalize-space()='Participantes']")));
    // The sample trip has other participants, so the list is not empty: wait until it has loaded.
    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(By.xpath(PARTICIPANTS + "//output")));
    assertThat(driver.findElements(participantRow())).isEmpty();

    // The invitation stays as history: Ana still sees it as accepted.
    assertThat(driver.findElement(invitationRow()).getText()).contains("Aceptada");

    // The card of the list counts one participant less.
    driver.get(baseUrl + "/trips");
    assertThat(participantsOnTheCard()).isEqualTo(participantsBefore);

    // The guest no longer has the trip.
    logOut();
    logIn(guest, PASSWORD);
    assertGuestHasNoTrip();
  }

  @Test
  void aParticipantLeavesTheTripAndGoesBackToMyTripsWithANotice() {
    guestJoinsAnasTrip();
    openTrip();

    // The participant sees who is in the trip but cannot remove anyone.
    WebElement section =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(PARTICIPANTS)));
    assertThat(section.getText()).contains(guestName).doesNotContain(guest);
    assertThat(
            driver.findElements(
                By.xpath(PARTICIPANTS + "//button[starts-with(normalize-space(), 'Quitar')]")))
        .isEmpty();

    click(By.xpath(PARTICIPANTS + "//button[normalize-space()='Salir del viaje']"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(DIALOG)));
    assertThat(driver.findElement(By.xpath(DIALOG)).getText())
        .contains("Dejarás de ver «" + TRIP + "».");

    // Cancelling keeps the participant on the trip.
    click(By.xpath(DIALOG + "//button[normalize-space()='Cancelar']"));
    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(DIALOG)));
    assertThat(driver.getCurrentUrl()).contains("/trips/");

    // Confirming goes back to "Mis viajes" with a notice, and the trip is gone.
    click(By.xpath(PARTICIPANTS + "//button[normalize-space()='Salir del viaje']"));
    click(By.xpath(DIALOG + "//button[normalize-space()='Salir del viaje']"));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[@role='alert'][contains(., 'Has salido de «" + TRIP + "».')]")));
    assertThat(driver.getCurrentUrl()).endsWith("/trips");
    assertGuestHasNoTrip();

    // Ana no longer sees the guest among the participants.
    logOut();
    logIn(ANA_EMAIL, SAMPLE_PASSWORD);
    assertThat(participantsOnTheCard()).isEqualTo(participantsBefore);
    openAnasTrip();
    assertThat(driver.findElements(participantRow())).isEmpty();
  }
}
