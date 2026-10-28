package com.ruben.backend.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
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

/**
 * Trip screens as a visitor uses them. They log in as the sample users that the application loads
 * with SAMPLE_DATA (see the README), so the application under test must run with it enabled.
 */
@Tag("B4")
class TripUiE2ETest {

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

  private void logInAs(String email) {
    driver.get(baseUrl + "/login");
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email"))).sendKeys(email);
    driver.findElement(By.id("login-password")).sendKeys(SAMPLE_PASSWORD);
    click(By.cssSelector("form button[type='submit']"));
    wait.until(ExpectedConditions.urlContains("/trips"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("trips")));
  }

  /** A real PNG: the browser must be able to decode it, or the cover falls back to the default. */
  private String newPicture() throws IOException {
    Path file = tempDir.resolve("cover.png");
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

  private void fillTripForm(String name, String destination, String start, String end) {
    replaceText(By.id("trip-name"), name);
    replaceText(By.id("trip-destination"), destination);
    setDate("trip-start-date", start);
    setDate("trip-end-date", end);
  }

  // --- tests ---

  @Test
  void anAnonymousVisitorOpensTheHomePageAndSeesNoErrorPage() {
    driver.get(baseUrl + "/");

    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("h1"), "Planifica tus viajes con TripMate"));
    assertThat(driver.findElement(By.tagName("body")).getText())
        .doesNotContain("Algo ha fallado")
        .doesNotContain("No hay conexión");
    assertThat(driver.findElements(link("Iniciar sesión"))).isNotEmpty();
  }

  @Test
  void aUserWithoutTripsSeesTheFirstTripInvitationThatOpensTheForm() {
    driver.get(baseUrl + "/register");
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("register-name")))
        .sendKeys("Nuevo");
    driver
        .findElement(By.id("register-email"))
        .sendKeys("ui-" + UUID.randomUUID() + "@example.com");
    driver.findElement(By.id("register-password")).sendKeys("secret-pass");
    driver.findElement(By.id("register-confirmation")).sendKeys("secret-pass");
    click(By.cssSelector("form button[type='submit']"));
    wait.until(ExpectedConditions.urlContains("/trips"));

    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//h2[normalize-space()='Tu primer viaje te está esperando']")));
    assertThat(driver.findElements(By.cssSelector("#trips > li"))).isEmpty();
    click(link("Añadir tu primer viaje"));

    wait.until(ExpectedConditions.urlContains("/trips/new"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("trip-name")));
  }

  @Test
  void aVisitorOpeningTheTripListIsSentToLogin() {
    driver.get(baseUrl + "/trips");

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email")));
    assertThat(driver.getCurrentUrl()).contains("/login");
  }

  @Test
  void anaSeesHerOwnTripsAndMoreResultsLoadsTheRestAndDisappears() {
    logInAs("ana@tripmate.com");
    By cards = By.cssSelector("#trips > li");
    By moreResults = button("Más resultados");
    wait.until(ExpectedConditions.numberOfElementsToBe(cards, 10));
    assertThat(driver.findElement(By.id("trips")).getText())
        .contains("Lisbon getaway", "Summer in Rome", "Trip to Japan");

    click(moreResults);

    List<WebElement> allCards =
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(cards, 10));
    assertThat(allCards).hasSize(12);
    assertThat(wait.until(ExpectedConditions.invisibilityOfElementLocated(moreResults))).isTrue();
  }

  @Test
  void lauraSeesOnlyHerTrips() {
    logInAs("laura@tripmate.com");

    String trips = driver.findElement(By.id("trips")).getText();

    assertThat(trips).contains("Weekend in Porto", "Alps in winter");
    assertThat(trips).doesNotContain("Lisbon getaway");
  }

  @Test
  void lauraCreatesATripWithACoverSeesItInTheListEditsItAndDeletesIt() throws IOException {
    String name = "UI trip " + UUID.randomUUID().toString().substring(0, 8);
    String renamed = name + " edited";
    logInAs("laura@tripmate.com");

    // Create it, with a cover chosen from a file.
    click(link("Nuevo viaje"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("trip-name")));
    fillTripForm(name, "Cadiz", "2028-08-01", "2028-08-05");
    replaceText(By.id("trip-budget"), "300,50");
    replaceText(By.id("trip-description"), "Beach and tapas");
    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='file']")))
        .sendKeys(newPicture());
    wait.until(
        ExpectedConditions.presenceOfElementLocated(By.cssSelector("img[alt='Vista previa']")));
    click(button("Crear viaje"));

    // The detail page shows the data and the cover.
    wait.until(ExpectedConditions.urlMatches(".*/trips/\\d+$"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), name));
    String detail = driver.findElement(By.tagName("main")).getText();
    assertThat(detail).contains("Cadiz", "300,50", "Beach and tapas", "Editar", "Eliminar");
    waitForLoadedPicture("img[alt^='Portada de']");
    String detailUrl = driver.getCurrentUrl();

    // Without activities nothing has been spent, so the whole budget is left.
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.cssSelector("dl.trip-detail__facts"), "Presupuesto restante"));
    String facts = driver.findElement(By.cssSelector("dl.trip-detail__facts")).getText();
    assertThat(facts.split("300,50", -1)).hasSize(3);

    // It is in the list.
    click(link("Volver a Mis viajes"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("trips"), name));
    waitForLoadedPicture("#trips img");

    // Edit it from its card.
    click(By.cssSelector("a[aria-label='Editar " + name + "']"));
    replaceText(By.id("trip-name"), renamed);
    click(button("Guardar cambios"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), renamed));
    assertThat(driver.getCurrentUrl()).isEqualTo(detailUrl);

    // Cancelling the confirmation keeps the trip.
    click(button("Eliminar"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[role='dialog']")));
    click(button("Cancelar"));
    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("[role='dialog']")));
    assertThat(driver.getCurrentUrl()).isEqualTo(detailUrl);

    // Confirming deletes it and the list no longer shows it.
    click(button("Eliminar"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[role='dialog']")));
    click(button("Eliminar viaje"));
    wait.until(ExpectedConditions.urlMatches(".*/trips$"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("trips")));
    wait.until(
        ExpectedConditions.not(
            ExpectedConditions.textToBePresentInElementLocated(By.id("trips"), renamed)));
    driver.get(detailUrl);
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("h1"), "No encontramos esta página"));
  }

  @Test
  void anEndDateBeforeTheStartDateIsExplainedAndNothingIsCreated() {
    String name = "Invalid UI trip " + UUID.randomUUID().toString().substring(0, 8);
    logInAs("laura@tripmate.com");
    click(link("Nuevo viaje"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("trip-name")));
    fillTripForm(name, "Cadiz", "2028-08-05", "2028-08-01");

    click(button("Crear viaje"));

    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("main"), "La fecha de fin no puede ser anterior a la de inicio."));
    assertThat(driver.getCurrentUrl()).endsWith("/trips/new");
    click(button("Cancelar"));
    wait.until(ExpectedConditions.urlMatches(".*/trips$"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("trips")));
    assertThat(driver.findElement(By.id("trips")).getText()).doesNotContain(name);
  }

  @Test
  void aCoverThatIsNotAnImageOrIsTooLargeIsRejectedInTheForm() throws IOException {
    logInAs("laura@tripmate.com");
    click(link("Nuevo viaje"));
    By picker = By.cssSelector("input[type='file']");
    wait.until(ExpectedConditions.presenceOfElementLocated(picker));

    Path text = Files.writeString(tempDir.resolve("notes.txt"), "not an image");
    driver.findElement(picker).sendKeys(text.toString());
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.cssSelector("[role='alert']"), "El archivo no es una imagen válida"));

    byte[] big = new byte[5 * 1024 * 1024 + 1];
    big[0] = (byte) 0x89;
    Path huge = Files.write(tempDir.resolve("huge.png"), big);
    driver.findElement(picker).sendKeys(huge.toString());
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.cssSelector("[role='alert']"), "La imagen supera los 5 MB"));
    assertThat(driver.findElements(By.cssSelector("img[alt='Vista previa']"))).isEmpty();
  }

  @Test
  void aTripOfAnotherUserShowsTheNoAccessPage() {
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
  }
}
