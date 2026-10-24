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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

@Tag("B3")
class ProfileUiE2ETest {

  private static final String PASSWORD = "secret-pass";
  private static final String NEW_PASSWORD = "brand-new-pass";

  @TempDir Path tempDir;

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

  private void openProfileFromHeader() {
    click(By.cssSelector("a[aria-label='Mi perfil']"));
    wait.until(ExpectedConditions.urlContains("/profile"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("profile-details-title")));
  }

  private void logOut() {
    click(button("Cerrar sesión"));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//a[normalize-space()='Iniciar sesión']")));
  }

  /**
   * Opens the login page from the header link, as a visitor does. Reloading /login instead would
   * keep the history state that a redirect from a private page leaves behind.
   */
  private void logIn(String address, String password) {
    click(By.xpath("//a[normalize-space()='Iniciar sesión']"));
    replaceText(By.id("login-email"), address);
    replaceText(By.id("login-password"), password);
    click(By.cssSelector("form button[type='submit']"));
  }

  /** A real PNG: the browser must be able to decode it, or the avatar falls back to the initial. */
  private String newPicture() throws IOException {
    Path file = tempDir.resolve("avatar.png");
    ImageIO.write(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB), "png", file.toFile());
    return file.toString();
  }

  private void uploadAvatar() throws IOException {
    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='file']")))
        .sendKeys(newPicture());
  }

  private static final By HEADER_AVATAR_IMAGE = By.cssSelector(".app-header__profile img");
  private static final By HEADER_USER = By.className("app-header__user");

  /** Waits until the browser has decoded the picture, so a broken image does not pass. */
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

  // --- tests ---

  @Test
  void registerEditNameChangeAvatarChangePasswordLogoutAndLoginWithTheNewPassword()
      throws IOException {
    register("Lucía", email);
    openProfileFromHeader();

    // Edit the name: the header follows without reloading the page.
    click(button("Editar datos"));
    replaceText(By.id("profile-name"), "Lucía Pérez");
    click(button("Guardar cambios"));
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("main"), "Perfil actualizado."));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(HEADER_USER, "Lucía Pérez"));

    // Change the avatar: the picture appears in the header and in the profile.
    assertThat(driver.findElements(HEADER_AVATAR_IMAGE)).isEmpty();
    uploadAvatar();
    wait.until(ExpectedConditions.presenceOfElementLocated(HEADER_AVATAR_IMAGE));
    waitForLoadedPicture(".app-header__profile img");
    waitForLoadedPicture(".user-avatar--lg img");
    assertThat(driver.findElement(HEADER_AVATAR_IMAGE).getAttribute("src"))
        .contains("/api/v1/images/");

    // Change the password.
    driver.findElement(By.id("profile-current-password")).sendKeys(PASSWORD);
    driver.findElement(By.id("profile-new-password")).sendKeys(NEW_PASSWORD);
    driver.findElement(By.id("profile-confirm-password")).sendKeys(NEW_PASSWORD);
    click(button("Guardar contraseña"));
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("main"), "Contraseña cambiada"));

    // The session survives, then log out and in again with the new password.
    logOut();
    logIn(email, PASSWORD);
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.cssSelector("[role='alert']"), "incorrectos"));
    logIn(email, NEW_PASSWORD);
    wait.until(ExpectedConditions.urlContains("/trips"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(HEADER_USER, "Lucía Pérez"));
    wait.until(ExpectedConditions.presenceOfElementLocated(HEADER_AVATAR_IMAGE));
    waitForLoadedPicture(".app-header__profile img");
  }

  @Test
  void aWrongCurrentPasswordIsExplainedUnderTheField() {
    register("Lucía", email);
    openProfileFromHeader();

    driver.findElement(By.id("profile-current-password")).sendKeys("not-the-password");
    driver.findElement(By.id("profile-new-password")).sendKeys(NEW_PASSWORD);
    driver.findElement(By.id("profile-confirm-password")).sendKeys(NEW_PASSWORD);
    click(button("Guardar contraseña"));

    By message =
        By.xpath(
            "//input[@id='profile-current-password']/following-sibling::div[contains(@class,"
                + " 'invalid-feedback')]");
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            message, "La contraseña actual no es correcta."));
    assertThat(driver.getCurrentUrl()).contains("/profile");
  }

  @Test
  void anEmailOfAnotherAccountIsExplainedUnderTheEmailField() {
    String otherEmail = "ui-" + UUID.randomUUID() + "@example.com";
    register("Bob", otherEmail);
    logOut();
    register("Lucía", email);
    openProfileFromHeader();

    click(button("Editar datos"));
    replaceText(By.id("profile-email"), otherEmail);
    click(button("Guardar cambios"));

    By message =
        By.xpath(
            "//input[@id='profile-email']/following-sibling::div[contains(@class,"
                + " 'invalid-feedback')]");
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            message, "Ya existe una cuenta con ese email."));
    assertThat(driver.findElement(By.id("profile-email")).isDisplayed()).isTrue();
  }

  @Test
  void deletingTheAvatarBringsBackTheInitial() throws IOException {
    register("Lucía", email);
    openProfileFromHeader();
    uploadAvatar();
    wait.until(ExpectedConditions.presenceOfElementLocated(HEADER_AVATAR_IMAGE));

    click(button("Eliminar foto"));

    wait.until(ExpectedConditions.invisibilityOfElementLocated(HEADER_AVATAR_IMAGE));
    assertThat(driver.findElement(By.cssSelector(".app-header__profile .user-avatar")).getText())
        .isEqualTo("L");
    assertThat(driver.findElements(button("Eliminar foto"))).isEmpty();
  }

  @Test
  void aFileThatIsNotAnImageIsRejectedBeforeSendingIt() throws IOException {
    register("Lucía", email);
    openProfileFromHeader();
    Path text = tempDir.resolve("notes.txt");
    java.nio.file.Files.writeString(text, "not an image");

    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='file']")))
        .sendKeys(text.toString());

    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.cssSelector("[role='alert']"), "no es una imagen válida"));
    assertThat(driver.findElements(HEADER_AVATAR_IMAGE)).isEmpty();
  }
}
