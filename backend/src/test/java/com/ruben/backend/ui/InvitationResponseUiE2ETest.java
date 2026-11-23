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
 * The invited person answers (I2): a user who has just registered is invited by Ana to her sample
 * trip "Lisbon getaway", sees the invitation in "Mis viajes", accepts or rejects it, and Ana sees
 * the answer in her list. The link of the email is not used here, because CI has no inbox; the page
 * it opens is covered by the frontend and API tests. Every run registers a different user, so the
 * invitations it leaves behind never clash with another run.
 */
@Tag("I2")
class InvitationResponseUiE2ETest {

  private static final String ANA_EMAIL = "ana@tripmate.com";
  private static final String SAMPLE_PASSWORD = "Demo-2026";
  private static final String PASSWORD = "secret-pass";
  private static final String TRIP = "Lisbon getaway";
  private static final String INVITATIONS = "//section[@aria-labelledby='invitations-title']";
  private static final String PENDING = "//section[@aria-labelledby='pending-invitations']";
  private static final By EMAIL_FIELD = By.id("invitation-email");

  private WebDriver driver;
  private WebDriverWait wait;
  private final String baseUrl = System.getProperty("app.url", "http://localhost:5173");
  private final String invitee = "invitee-" + UUID.randomUUID() + "@example.com";

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

  /** Registers the invitee through the form; registering also logs the new user in. */
  private void registerInvitee() {
    driver.get(baseUrl + "/register");
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("register-name")))
        .sendKeys("Invitee");
    driver.findElement(By.id("register-email")).sendKeys(invitee);
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

  private void openAnasTrip() {
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("trips")));
    click(tripLink());
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), TRIP));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath(INVITATIONS + "//h2[normalize-space()='Invitaciones']")));
  }

  /** Selenium's clear() does not reach React's state, so the old text is selected and deleted. */
  private void inviteInvitee() {
    WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD));
    field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
    field.sendKeys(invitee);
    click(By.xpath(INVITATIONS + "//button[normalize-space()='Enviar invitación']"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(row()));
  }

  /** The row of Ana's list for the invitee; it has the address and the state of the invitation. */
  private By row() {
    return By.xpath(INVITATIONS + "//li[.//span[normalize-space()='" + invitee + "']]");
  }

  /** Ana invites the invitee from her trip page and logs out; the invitee is then logged in. */
  private void anaInvitesTheInvitee() {
    registerInvitee();
    logOut();
    logIn(ANA_EMAIL, SAMPLE_PASSWORD);
    openAnasTrip();
    inviteInvitee();
    logOut();
    logIn(invitee, PASSWORD);
  }

  private static By pendingButton(String action) {
    return By.xpath(PENDING + "//button[@aria-label='" + action + " invitación a " + TRIP + "']");
  }

  /** Ana's list shows the state the invitee answered with. */
  private String statusSeenByAna() {
    logOut();
    logIn(ANA_EMAIL, SAMPLE_PASSWORD);
    openAnasTrip();
    return wait.until(ExpectedConditions.visibilityOfElementLocated(row())).getText();
  }

  // --- tests ---

  @Test
  void theInvitedUserSeesThePendingInvitationAcceptsAndTheTripAppearsReadOnly() {
    anaInvitesTheInvitee();

    // The invitation waits in "Mis viajes", and the invitee has no trip yet.
    WebElement section =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(PENDING)));
    assertThat(section.getText())
        .contains("Invitaciones pendientes")
        .contains("te invita a «" + TRIP + "»");
    assertThat(driver.findElements(tripLink())).isEmpty();

    click(pendingButton("Aceptar"));

    // The notice appears, the invitation leaves the list and the trip shows up.
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[@role='status'][contains(., 'Te has unido a «" + TRIP + "».')]")));
    wait.until(ExpectedConditions.visibilityOfElementLocated(tripLink()));
    assertThat(driver.findElements(By.xpath(PENDING))).isEmpty();

    // It was stored: the trip is still there, and the invitation is not, after reloading.
    driver.navigate().refresh();
    wait.until(ExpectedConditions.visibilityOfElementLocated(tripLink()));
    assertThat(driver.findElements(By.xpath(PENDING))).isEmpty();

    // The trip opens with no way to change the trip itself (its summary card has no buttons), but
    // the itinerary is planned together with its creator (I4): "Nueva actividad" is there and
    // every activity can be edited and deleted.
    click(tripLink());
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), TRIP));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//h2[normalize-space()='Itinerario']")));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("li.activity-card")));
    String tripSummary = "//article[contains(@class, 'trip-detail')]";
    for (String name : new String[] {"Editar", "Eliminar", "Publicar", "Despublicar"}) {
      assertThat(
              driver.findElements(
                  By.xpath(
                      tripSummary
                          + "//*[self::a or self::button][normalize-space()='"
                          + name
                          + "']")))
          .as(name)
          .isEmpty();
    }
    assertThat(driver.findElements(By.xpath("//a[normalize-space()='Nueva actividad']")))
        .hasSize(1);
    String firstActivity = "(//li[contains(@class, 'activity-card')])[1]";
    assertThat(driver.findElements(By.xpath(firstActivity + "//a[normalize-space()='Editar']")))
        .hasSize(1);
    assertThat(
            driver.findElements(By.xpath(firstActivity + "//button[normalize-space()='Eliminar']")))
        .hasSize(1);
    assertThat(driver.findElements(By.xpath(INVITATIONS))).isEmpty();

    // Ana sees the invitation as accepted.
    assertThat(statusSeenByAna()).contains(invitee).contains("Aceptada");
  }

  @Test
  void rejectingTheInvitationRemovesItAndTheTripNeverAppears() {
    anaInvitesTheInvitee();
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(PENDING)));

    click(pendingButton("Rechazar"));

    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(PENDING)));
    driver.navigate().refresh();
    wait.until(ExpectedConditions.visibilityOfElementLocated(logoutButton()));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//a[normalize-space()='Añadir tu primer viaje']")));
    assertThat(driver.findElements(By.xpath(PENDING))).isEmpty();
    assertThat(driver.findElements(tripLink())).isEmpty();

    // Ana sees the invitation as rejected.
    assertThat(statusSeenByAna()).contains(invitee).contains("Rechazada");
  }
}
