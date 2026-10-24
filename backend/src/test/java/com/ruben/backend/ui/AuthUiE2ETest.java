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

@Tag("B2")
class AuthUiE2ETest {

  private static final String PASSWORD = "secret-pass";

  private WebDriver driver;
  private WebDriverWait wait;
  private final String baseUrl = System.getProperty("app.url", "http://localhost:5173");
  private final String email = "ui-" + UUID.randomUUID() + "@example.com";

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

  private void clickSubmit() {
    WebElement button = driver.findElement(By.cssSelector("form button[type='submit']"));
    ((JavascriptExecutor) driver)
        .executeScript(
            "arguments[0].scrollIntoView({block: 'center', behavior: 'instant'});", button);
    wait.until(ExpectedConditions.elementToBeClickable(button)).click();
  }

  private void fillAndSubmitRegistration(String name, String address) {
    driver.get(baseUrl + "/register");
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("register-name")))
        .sendKeys(name);
    driver.findElement(By.id("register-email")).sendKeys(address);
    driver.findElement(By.id("register-password")).sendKeys(PASSWORD);
    driver.findElement(By.id("register-confirmation")).sendKeys(PASSWORD);
    clickSubmit();
  }

  private void fillAndSubmitLogin(String address, String password) {
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email")))
        .sendKeys(address);
    driver.findElement(By.id("login-password")).sendKeys(password);
    clickSubmit();
  }

  private void logOut() {
    wait.until(ExpectedConditions.elementToBeClickable(logoutButton())).click();
  }

  private static By logoutButton() {
    return By.xpath("//button[normalize-space()='Cerrar sesión']");
  }

  private static By loginLink() {
    return By.xpath("//a[normalize-space()='Iniciar sesión']");
  }

  @Test
  void registerLogsInThenLogoutAndLoginWorks() {
    fillAndSubmitRegistration("Lucía", email);

    wait.until(ExpectedConditions.urlContains("/trips"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(logoutButton()));
    assertThat(driver.findElement(By.className("app-header__user")).getText()).contains("Lucía");

    logOut();
    wait.until(ExpectedConditions.visibilityOfElementLocated(loginLink()));
    assertThat(driver.findElements(logoutButton())).isEmpty();

    driver.findElement(loginLink()).click();
    fillAndSubmitLogin(email, PASSWORD);

    wait.until(ExpectedConditions.urlContains("/trips"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(logoutButton()));
    assertThat(driver.findElement(By.className("app-header__user")).getText()).contains("Lucía");
  }

  @Test
  void aPrivatePageSendsAnonymousVisitorsToLoginAndBackAfterwards() {
    fillAndSubmitRegistration("Lucía", email);
    wait.until(ExpectedConditions.visibilityOfElementLocated(logoutButton()));
    logOut();
    wait.until(ExpectedConditions.visibilityOfElementLocated(loginLink()));

    driver.get(baseUrl + "/trips/new");
    wait.until(ExpectedConditions.urlContains("/login"));
    fillAndSubmitLogin(email, PASSWORD);

    wait.until(ExpectedConditions.urlContains("/trips/new"));
    assertThat(driver.getCurrentUrl()).endsWith("/trips/new");
  }

  @Test
  void aWrongPasswordShowsAnErrorAndStaysOnLogin() {
    fillAndSubmitRegistration("Lucía", email);
    wait.until(ExpectedConditions.visibilityOfElementLocated(logoutButton()));
    logOut();
    wait.until(ExpectedConditions.visibilityOfElementLocated(loginLink()));

    driver.get(baseUrl + "/login");
    fillAndSubmitLogin(email, "not-the-password");

    By alert = By.cssSelector("[role='alert']");
    wait.until(ExpectedConditions.textToBePresentInElementLocated(alert, "incorrectos"));
    assertThat(driver.getCurrentUrl()).contains("/login");
  }

  @Test
  void registeringAnExistingEmailShowsTheErrorUnderTheEmailField() {
    fillAndSubmitRegistration("Lucía", email);
    wait.until(ExpectedConditions.visibilityOfElementLocated(logoutButton()));
    logOut();
    wait.until(ExpectedConditions.visibilityOfElementLocated(loginLink()));

    fillAndSubmitRegistration("Otra", email);

    By message =
        By.xpath(
            "//input[@id='register-email']/following-sibling::div[contains(@class,"
                + " 'invalid-feedback')]");
    wait.until(ExpectedConditions.textToBePresentInElementLocated(message, "Ya existe una cuenta"));
    assertThat(driver.getCurrentUrl()).contains("/register");
  }
}
