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
 * The public home page as an anonymous visitor uses it. The public itineraries are the sample trips
 * the application publishes with SAMPLE_DATA (see the README): the administrator's Rome, Maldives
 * and Tokyo on the first page of three, then Ana's Lisbon and Carlos's Andalusia on the second.
 */
@Tag("B1")
@Tag("I7")
class HomeUiE2ETest {

  private static final String SAMPLE_PASSWORD = "Demo-2026";
  private static final By CARDS = By.cssSelector("article.public-card");

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
  void anAnonymousVisitorSeesThePublicItinerariesAndPagesThemOnTheHomePage() {
    driver.get(baseUrl + "/");

    // The hero invites to register or log in.
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("h1"), "Planifica tus viajes con TripMate"));
    assertThat(driver.findElements(link("Regístrate gratis"))).hasSize(1);
    assertThat(driver.findElements(link("Ya tengo cuenta"))).hasSize(1);

    // Three cards with their key facts, their author and the preview of the first days.
    List<WebElement> cards = waitForCards(3);
    // On a desktop the three cards sit side by side, not one below the other.
    assertThat(cards)
        .extracting(card -> card.getLocation().getY())
        .containsOnly(cards.get(0).getLocation().getY());
    assertThat(cards.get(1).getLocation().getX()).isGreaterThan(cards.get(0).getLocation().getX());
    assertThat(driver.findElement(By.cssSelector("section.public-trips h2")).getText())
        .isEqualTo("Itinerarios públicos");
    assertThat(cards).allSatisfy(card -> assertThat(card.getText()).contains("Por "));
    assertThat(cards.get(0).findElement(By.tagName("h3")).getText())
        .isEqualTo("Roma en cinco días");
    assertThat(cards.get(0).getText())
        .contains(
            "Roma, Italia · 5 días",
            "Día 1 · Visita guiada al Coliseo",
            "Día 2 · Museos Vaticanos");
    assertThat(cards.get(1).findElement(By.tagName("h3")).getText())
        .isEqualTo("Escapada a las Maldivas");
    assertThat(cards.get(1).getText())
        .contains("Maldivas · 7 días", "Día 1 · Traslado en hidroavión al resort");
    assertThat(cards.get(2).findElement(By.tagName("h3")).getText()).isEqualTo("Lo mejor de Tokio");
    assertThat(cards.get(2).getText()).contains("Tokio, Japón · 7 días", "Día 1 · Templo Senso-ji");
    assertThat(driver.findElements(By.cssSelector("article.public-card a.btn")))
        .extracting(WebElement::getText)
        .containsOnly("Ver itinerario completo");

    // There are five public trips, so the carousel pages: the second page has the other two
    // authors, and going back shows the first page again.
    By next = By.cssSelector("button[aria-label='Itinerarios siguientes']");
    By previous = By.cssSelector("button[aria-label='Itinerarios anteriores']");
    assertThat(driver.findElement(previous).isEnabled()).isFalse();
    click(next);
    List<WebElement> second = waitForCards(2);
    assertThat(second.get(0).findElement(By.tagName("h3")).getText()).isEqualTo("Lisbon getaway");
    assertThat(second.get(0).getText()).contains("Por Ana García");
    assertThat(second.get(1).findElement(By.tagName("h3")).getText()).isEqualTo("Andalusia route");
    assertThat(second.get(1).getText()).contains("Por Carlos Ruiz");
    assertThat(driver.findElement(next).isEnabled()).isFalse();
    click(previous);
    waitForCards(3);
  }

  @Test
  void anAnonymousVisitorOpensAPublicItineraryAndBrowsesEveryDayOfIt() {
    driver.get(baseUrl + "/");
    waitForCards(3);

    // Opening a public itinerary shows the trip and its first day, read-only.
    click(openExample("Roma en cinco días"));
    wait.until(ExpectedConditions.urlContains("/public/"));
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), "Roma en cinco días"));
    waitForItineraryText("Visita guiada al Coliseo");
    assertThat(driver.findElement(By.tagName("main")).getText())
        .contains("Roma, Italia", "Presupuesto", "Itinerario");

    // Browsing the days changes the activities, and every one of the five days has some.
    click(tab("Día 2"));
    waitForItineraryText("Museos Vaticanos y Capilla Sixtina");
    assertThat(driver.findElement(By.cssSelector("section.itinerary")).getText())
        .doesNotContain("Visita guiada al Coliseo");
    click(tab("Día 3"));
    waitForItineraryText("Galería Borghese y jardines");
    click(tab("Día 4"));
    waitForItineraryText("Clase de cocina de pasta fresca");
    click(tab("Día 5"));
    waitForItineraryText("Museos Capitolinos");
    assertThat(driver.findElement(By.cssSelector("section.itinerary")).getText())
        .doesNotContain("No hay actividades");

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
  void theSeeAllLinkOfTheCarouselOpensThePublicItinerariesPage() {
    driver.get(baseUrl + "/");
    waitForCards(3);

    click(link("Ver todos"));

    wait.until(ExpectedConditions.urlMatches(".*/public$"));
    assertThat(
            wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                    By.tagName("h1"), "Viajes públicos")))
        .isTrue();
  }

  @Test
  void theRegisterButtonOfTheHeroOpensTheRegistrationForm() {
    driver.get(baseUrl + "/");

    click(link("Regístrate gratis"));

    wait.until(ExpectedConditions.urlContains("/register"));
    assertThat(
            wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                    By.tagName("h1"), "Crear cuenta")))
        .isTrue();
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
