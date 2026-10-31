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

@Tag("B7")
class AdminUserUiE2ETest {

  private static final String PASSWORD = "secret-pass";
  // The development administrator, the same one the CI backend starts with.
  private static final String ADMIN_EMAIL = "admin@tripmate.com";
  private static final String ADMIN_PASSWORD = "admin";
  private static final By DIALOG = By.cssSelector("[role='dialog']");

  private WebDriver driver;
  private WebDriverWait wait;
  private final String baseUrl = System.getProperty("app.url", "http://localhost:5173");
  // A fresh user, so the sample users that other tests rely on are never edited or blocked.
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
    replaceText(By.id("admin-users-search"), text);
    click(By.xpath("//button[normalize-space()='Buscar']"));
  }

  /** The row of the only user the search found. */
  private By userRow() {
    return By.cssSelector(".admin-user");
  }

  private void confirmInDialog(String label) {
    click(By.xpath("//*[@role='dialog']//button[normalize-space()='" + label + "']"));
    wait.until(ExpectedConditions.invisibilityOfElementLocated(DIALOG));
  }

  // --- tests ---

  @Test
  void adminSearchesAUserEditsTheNameBlocksAndUnblocks() {
    register("Lucía", email);
    logOut();
    logIn(ADMIN_EMAIL, ADMIN_PASSWORD);
    wait.until(ExpectedConditions.urlContains("/trips"));

    click(By.xpath("//a[normalize-space()='Usuarios']"));
    wait.until(ExpectedConditions.urlContains("/admin/users"));
    searchFor(email);
    wait.until(ExpectedConditions.textToBePresentInElementLocated(userRow(), "Lucía"));
    assertThat(driver.findElements(userRow())).hasSize(1);
    assertThat(driver.findElement(userRow()).getText()).contains(email).contains("Activo");

    // Edit the name.
    click(By.cssSelector("button[aria-label='Editar Lucía']"));
    replaceText(By.id("edit-user-name"), "Lucía Pérez");
    click(By.xpath("//*[@role='dialog']//button[normalize-space()='Guardar cambios']"));
    wait.until(ExpectedConditions.invisibilityOfElementLocated(DIALOG));
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("main"), "Datos de Lucía Pérez actualizados."));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(userRow(), "Lucía Pérez"));

    // Block, with confirmation.
    click(By.cssSelector("button[aria-label='Bloquear Lucía Pérez']"));
    confirmInDialog("Bloquear");
    wait.until(ExpectedConditions.textToBePresentInElementLocated(userRow(), "Bloqueado"));

    // Unblock, with confirmation.
    click(By.cssSelector("button[aria-label='Desbloquear Lucía Pérez']"));
    confirmInDialog("Desbloquear");
    wait.until(ExpectedConditions.textToBePresentInElementLocated(userRow(), "Activo"));
  }

  @Test
  void aRegularUserOpeningTheUsersPageSeesTheNoAccessPage() {
    register("Lucía", email);

    driver.get(baseUrl + "/admin/users");

    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("main"), "No tienes acceso a esta página"));
    assertThat(driver.findElements(By.id("admin-users-search"))).isEmpty();
    assertThat(driver.findElements(By.xpath("//a[normalize-space()='Usuarios']"))).isEmpty();
  }
}
