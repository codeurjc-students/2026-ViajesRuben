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
 * The creator of a trip invites an email from the trip page (I1): Ana, with her sample trip "Lisbon
 * getaway". Every run invites a different address, so the invitations it leaves behind never clash
 * with another run or with the sample data.
 */
@Tag("I1")
class InvitationUiE2ETest {

  private static final String ANA_EMAIL = "ana@tripmate.com";
  private static final String SAMPLE_PASSWORD = "Demo-2026";
  private static final String TRIP = "Lisbon getaway";
  private static final String SECTION = "//section[@aria-labelledby='invitations-title']";
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

  private void logInAsAna() {
    driver.get(baseUrl + "/login");
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email")))
        .sendKeys(ANA_EMAIL);
    driver.findElement(By.id("login-password")).sendKeys(SAMPLE_PASSWORD);
    click(By.cssSelector("form button[type='submit']"));
    wait.until(ExpectedConditions.urlContains("/trips"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("trips")));
  }

  private void openTripPage() {
    click(By.xpath("//ul[@id='trips']//a[normalize-space()='" + TRIP + "']"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), TRIP));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath(SECTION + "//h2[normalize-space()='Invitaciones']")));
  }

  /** Selenium's clear() does not reach React's state, so the old text is selected and deleted. */
  private void invite(String address) {
    WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD));
    field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
    field.sendKeys(address);
    click(By.xpath(SECTION + "//button[normalize-space()='Enviar invitación']"));
  }

  /** The row of the list for an address; it has the address and the state of the invitation. */
  private static By row(String address) {
    return By.xpath(SECTION + "//li[.//span[normalize-space()='" + address + "']]");
  }

  private WebElement sectionText() {
    return driver.findElement(By.xpath(SECTION));
  }

  // --- tests ---

  @Test
  void theCreatorInvitesAnEmailAndSeesItAsPendingAlsoAfterReloading() {
    logInAsAna();
    openTripPage();

    invite(invitee);

    // The form says it was sent, empties itself, and the list shows the invitation as pending.
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.xpath(SECTION), "Invitación enviada a " + invitee + "."));
    WebElement item = wait.until(ExpectedConditions.visibilityOfElementLocated(row(invitee)));
    assertThat(item.getText()).contains(invitee).contains("Pendiente");
    assertThat(driver.findElement(EMAIL_FIELD).getAttribute("value")).isEmpty();

    // It was stored: it is still there when the page is loaded again.
    driver.navigate().refresh();
    item = wait.until(ExpectedConditions.visibilityOfElementLocated(row(invitee)));
    assertThat(item.getText()).contains("Pendiente");
  }

  @Test
  void invitingTheSameEmailAgainShowsTheErrorAndKeepsOneInvitation() {
    logInAsAna();
    openTripPage();
    invite(invitee);
    wait.until(ExpectedConditions.visibilityOfElementLocated(row(invitee)));

    invite(invitee);

    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.xpath(SECTION + "//*[@role='alert']"),
            "Ya hay una invitación pendiente para ese email."));
    assertThat(driver.findElements(row(invitee))).hasSize(1);
    // The address stays in the field so that it can be corrected.
    assertThat(driver.findElement(EMAIL_FIELD).getAttribute("value")).isEqualTo(invitee);
  }

  @Test
  void theCreatorsOwnEmailAndAnInvalidOneAreRefusedBeforeSending() {
    logInAsAna();
    openTripPage();

    invite(ANA_EMAIL);

    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.xpath(SECTION), "Revisa el email: no puede ser el tuyo."));
    assertThat(driver.findElements(row(ANA_EMAIL))).isEmpty();

    invite("not-an-email");
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.xpath(SECTION), "Escribe un email válido, por ejemplo nombre@correo.com."));
    assertThat(sectionText().getText()).doesNotContain("Invitación enviada");
  }
}
