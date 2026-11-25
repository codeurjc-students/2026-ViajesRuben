package com.ruben.backend.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

@Tag("I6")
class AdminMetricsUiE2ETest {

  private static final String PASSWORD = "secret-pass";
  // The development administrator, the same one the CI backend starts with.
  private static final String ADMIN_EMAIL = "admin@tripmate.com";
  private static final String ADMIN_PASSWORD = "admin";
  private static final By MAIN = By.tagName("main");
  private static final By CARD = By.cssSelector(".admin-dashboard__card");

  private WebDriver driver;
  private WebDriverWait wait;
  private final String baseUrl = System.getProperty("app.url", "http://localhost:5173");
  // A fresh user with a fresh trip, so the totals are known to be at least one without relying on
  // the sample data of the database the test runs against.
  private final String email = "ui-" + UUID.randomUUID() + "@example.com";
  private final String tripName = "Cifras " + UUID.randomUUID().toString().substring(0, 8);

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
    replaceText(By.id("trip-destination"), "Cifralandia");
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

  /** The number of a card, written with the Spanish thousands separator ("12.345"). */
  private static long numberOf(WebElement card) {
    String text = card.findElement(By.cssSelector(".admin-dashboard__value")).getText();
    return Long.parseLong(text.replace(".", ""));
  }

  // --- tests ---

  @Test
  void adminOpensThePanelFromTheHeaderAndSeesTheThreeTotals() {
    // The totals before: whatever the database holds.
    logInAsAdmin();
    click(By.xpath("//a[normalize-space()='Panel']"));
    wait.until(ExpectedConditions.urlMatches(".*/admin$"));
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("h1"), "Panel de administración"));
    wait.until(ExpectedConditions.numberOfElementsToBe(CARD, 3));
    List<WebElement> before = driver.findElements(CARD);
    assertThat(before.get(0).getText()).contains("Usuarios");
    assertThat(before.get(1).getText()).contains("Viajes");
    assertThat(before.get(2).getText()).contains("Actividades");
    long usersBefore = numberOf(before.get(0));
    long tripsBefore = numberOf(before.get(1));
    long activitiesBefore = numberOf(before.get(2));
    logOut();

    // One new user with one new trip and no activity.
    register("Lucía", email);
    createTrip(tripName);
    logOut();

    // The panel counts exactly those two, and the administrator is not a user.
    logIn(ADMIN_EMAIL, ADMIN_PASSWORD);
    wait.until(ExpectedConditions.urlContains("/trips"));
    click(By.xpath("//a[normalize-space()='Panel']"));
    wait.until(ExpectedConditions.numberOfElementsToBe(CARD, 3));
    List<WebElement> after = driver.findElements(CARD);
    assertThat(numberOf(after.get(0))).isEqualTo(usersBefore + 1);
    assertThat(numberOf(after.get(1))).isEqualTo(tripsBefore + 1);
    assertThat(numberOf(after.get(2))).isEqualTo(activitiesBefore);
    assertThat(driver.findElement(MAIN).getText()).doesNotContain("Cargando");
  }

  @Test
  void theLinksOfThePanelOpenTheUserAndTripManagementPages() {
    logInAsAdmin();
    click(By.xpath("//a[normalize-space()='Panel']"));
    wait.until(ExpectedConditions.numberOfElementsToBe(CARD, 3));

    click(By.xpath("//main//a[normalize-space()='Gestionar usuarios']"));
    wait.until(ExpectedConditions.urlContains("/admin/users"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("admin-users-search")));

    click(By.xpath("//a[normalize-space()='Panel']"));
    wait.until(ExpectedConditions.numberOfElementsToBe(CARD, 3));
    click(By.xpath("//main//a[normalize-space()='Moderar viajes']"));
    wait.until(ExpectedConditions.urlContains("/admin/trips"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("admin-trips-search")));
  }

  @Test
  void aRegularUserOpeningThePanelSeesTheNoAccessPage() {
    register("Lucía", email);

    driver.get(baseUrl + "/admin");

    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(MAIN, "No tienes acceso a esta página"));
    assertThat(driver.findElements(CARD)).isEmpty();
    assertThat(driver.findElement(MAIN).getText()).doesNotContain("Panel de administración");
    assertThat(driver.findElements(By.xpath("//a[normalize-space()='Panel']"))).isEmpty();
  }

  @Test
  void theCardsStackAndFitTheWidthOfAPhone() {
    logInAsAdmin();
    driver.manage().window().setSize(new Dimension(375, 812));
    driver.get(baseUrl + "/admin");
    wait.until(ExpectedConditions.numberOfElementsToBe(CARD, 3));

    List<WebElement> cards = driver.findElements(CARD);
    // One column: the same left edge, each card below the previous one.
    for (int i = 1; i < cards.size(); i++) {
      assertThat(cards.get(i).getRect().getX()).isEqualTo(cards.get(0).getRect().getX());
      assertThat(cards.get(i).getRect().getY())
          .isGreaterThanOrEqualTo(
              cards.get(i - 1).getRect().getY() + cards.get(i - 1).getRect().getHeight());
    }
    // Nothing makes the page scroll sideways.
    assertThat(
            (Boolean)
                ((JavascriptExecutor) driver)
                    .executeScript(
                        "return document.documentElement.scrollWidth <= window.innerWidth;"))
        .isTrue();
  }

  private void logInAsAdmin() {
    driver.get(baseUrl + "/login");
    replaceText(By.id("login-email"), ADMIN_EMAIL);
    replaceText(By.id("login-password"), ADMIN_PASSWORD);
    click(By.cssSelector("form button[type='submit']"));
    wait.until(ExpectedConditions.urlContains("/trips"));
  }
}
