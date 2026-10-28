package com.ruben.backend.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * The public home page as an anonymous visitor uses it. The example trips are the ones the
 * application loads for the administrator with SAMPLE_DATA (see the README): Rome, Paris and the
 * Maldives on the first page of three, Tokyo on the second.
 */
@Tag("B1")
class HomeUiE2ETest {

  private static final String SAMPLE_PASSWORD = "Demo-2026";
  private static final By CARDS = By.cssSelector("article.example-card");

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

  private static By link(String text) {
    return By.xpath("//a[normalize-space()='" + text + "']");
  }

  private static By tab(String text) {
    return By.xpath("//button[@role='tab'][contains(normalize-space(), '" + text + "')]");
  }

  private static By openExample(String tripName) {
    return By.xpath("//a[@aria-label='Ver itinerario completo de " + tripName + "']");
  }

  private List<WebElement> waitForCards(int count) {
    wait.until(ExpectedConditions.numberOfElementsToBe(CARDS, count));
    return driver.findElements(CARDS);
  }

  private void waitForItineraryText(String text) {
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.cssSelector("section.itinerary"), text));
  }

  // --- tests ---

  @Test
  void anAnonymousVisitorSeesTheExampleTripsPagesThroughThemAndBrowsesOneItinerary() {
    driver.get(baseUrl + "/");

    // The hero invites to register or log in.
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("h1"), "Planifica tus viajes con TripMate"));
    assertThat(driver.findElements(link("Regístrate gratis"))).hasSize(1);
    assertThat(driver.findElements(link("Ya tengo cuenta"))).hasSize(1);

    // The first page has three cards with their key facts and the preview of the first days.
    List<WebElement> cards = waitForCards(3);
    assertThat(cards.get(0).findElement(By.tagName("h3")).getText()).isEqualTo("Rome in five days");
    assertThat(cards.get(0).getText())
        .contains("Rome, Italy · 5 días", "Día 1 · Colosseum guided tour", "Día 2 · Vatican");
    assertThat(cards.get(1).findElement(By.tagName("h3")).getText())
        .isEqualTo("Paris in four days");
    assertThat(cards.get(2).findElement(By.tagName("h3")).getText()).isEqualTo("Maldives escape");
    assertThat(driver.findElements(By.cssSelector("article.example-card a.btn")))
        .extracting(WebElement::getText)
        .containsOnly("Ver itinerario completo");

    // Next shows the fourth example alone, previous brings the first page back.
    click(By.cssSelector("button[aria-label='Ejemplos siguientes']"));
    cards = waitForCards(1);
    assertThat(cards.get(0).findElement(By.tagName("h3")).getText()).isEqualTo("Tokyo highlights");
    assertThat(
            driver
                .findElement(By.cssSelector("button[aria-label='Ejemplos siguientes']"))
                .isEnabled())
        .isFalse();
    click(By.cssSelector("button[aria-label='Ejemplos anteriores']"));
    waitForCards(3);

    // Opening an example shows the trip and its first day, read-only.
    click(openExample("Rome in five days"));
    wait.until(ExpectedConditions.urlContains("/examples/"));
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), "Rome in five days"));
    waitForItineraryText("Colosseum guided tour");
    assertThat(driver.findElement(By.tagName("main")).getText())
        .contains("Rome, Italy", "Presupuesto", "Itinerario");

    // Browsing the days changes the activities.
    click(tab("Día 2"));
    waitForItineraryText("Vatican Museums and Sistine Chapel");
    assertThat(driver.findElement(By.cssSelector("section.itinerary")).getText())
        .doesNotContain("Colosseum guided tour");
    click(tab("Día 3"));
    waitForItineraryText("Borghese Gallery and Gardens");

    // Nothing to change: no new activity, edit, delete or conflicts.
    assertThat(driver.findElements(link("Nueva actividad"))).isEmpty();
    assertThat(driver.findElements(By.xpath("//a[starts-with(@aria-label, 'Editar')]"))).isEmpty();
    assertThat(driver.findElements(By.xpath("//button[starts-with(@aria-label, 'Eliminar')]")))
        .isEmpty();
    assertThat(driver.findElement(By.tagName("main")).getText())
        .doesNotContainIgnoringCase("conflicto");

    // The way back leads to the home page.
    click(link("Volver al inicio"));
    waitForCards(3);
  }

  @Test
  void theRegisterButtonOfTheHeroOpensTheRegistrationForm() {
    driver.get(baseUrl + "/");

    click(link("Regístrate gratis"));

    wait.until(ExpectedConditions.urlContains("/register"));
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), "Crear cuenta"));
  }

  @Test
  void aLoggedInUserSeesTheSameHomePageWithALinkToTheirTrips() {
    driver.get(baseUrl + "/login");
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email")))
        .sendKeys("laura@tripmate.com");
    driver.findElement(By.id("login-password")).sendKeys(SAMPLE_PASSWORD);
    click(By.cssSelector("form button[type='submit']"));
    wait.until(ExpectedConditions.urlContains("/trips"));

    click(By.cssSelector("a.navbar-brand"));

    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("h1"), "Planifica tus viajes con TripMate"));
    waitForCards(3);
    assertThat(driver.findElements(link("Ir a Mis viajes"))).hasSize(1);
    assertThat(driver.findElements(link("Regístrate gratis"))).isEmpty();
  }

  @Test
  void onAPhoneTheCardsStackOneAbovetheOtherWithoutWideningThePage() {
    driver.manage().window().setSize(new Dimension(375, 800));
    driver.get(baseUrl + "/");

    List<WebElement> cards = waitForCards(3);

    assertThat(cards.get(1).getLocation().getY()).isGreaterThan(cards.get(0).getLocation().getY());
    assertThat(cards.get(1).getLocation().getX()).isEqualTo(cards.get(0).getLocation().getX());
    // The browser may keep the window wider than asked, so the cards are compared with the real
    // one.
    Object viewport = ((JavascriptExecutor) driver).executeScript("return window.innerWidth;");
    assertThat(cards.get(0).getSize().getWidth())
        .isLessThanOrEqualTo(((Number) viewport).intValue());
    Object pageOverflows =
        ((JavascriptExecutor) driver)
            .executeScript("return document.documentElement.scrollWidth > window.innerWidth;");
    assertThat(pageOverflows).isEqualTo(false);
  }
}
