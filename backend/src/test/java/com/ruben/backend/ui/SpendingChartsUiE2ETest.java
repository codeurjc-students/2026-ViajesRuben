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
 * The spending charts (I5). Ana opens her sample trip "Summer in Rome", which has costs on three of
 * its eight days and no budget: the "Gastos" section shows both charts, the total spent, the
 * figures of each category and the running sum that ends at the total. The same page is then opened
 * at the width of a phone, where nothing may spill out sideways. The test only reads, so the sample
 * data is left as it was.
 */
@Tag("I5")
class SpendingChartsUiE2ETest {

  private static final String ANA_EMAIL = "ana@tripmate.com";
  private static final String SAMPLE_PASSWORD = "Demo-2026";
  private static final String TRIP = "Summer in Rome";
  private static final String SPENDING = "//section[@aria-labelledby='spending-title']";
  private static final By FIGURES = By.xpath(SPENDING + "//p[contains(@class,'figures')]");

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

  private static By logoutButton() {
    return By.xpath("//button[normalize-space()='Cerrar sesión']");
  }

  private void logInAsAna() {
    driver.get(baseUrl + "/login");
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email")))
        .sendKeys(ANA_EMAIL);
    driver.findElement(By.id("login-password")).sendKeys(SAMPLE_PASSWORD);
    click(By.cssSelector("form button[type='submit']"));
    wait.until(ExpectedConditions.urlContains("/trips"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(logoutButton()));
  }

  /** Opens the trip from "Mis viajes" and waits until both charts are drawn. */
  private void openTripWithItsCharts() {
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("trips")));
    click(By.xpath("//ul[@id='trips']//a[normalize-space()='" + TRIP + "']"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("h1"), TRIP));
    waitForCharts();
  }

  private void waitForCharts() {
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath(SPENDING + "//h2[normalize-space()='Gastos']")));
    wait.until(ExpectedConditions.numberOfElementsToBe(By.xpath(SPENDING + "//figure"), 2));
    wait.until(
        ExpectedConditions.numberOfElementsToBeMoreThan(
            By.xpath(SPENDING + "//figure//*[local-name()='svg' and contains(@class,'surface')]"),
            1));
  }

  /** The text of an element, with the non-breaking spaces of the euro amounts as plain spaces. */
  private static String plain(String text) {
    return text.replace(' ', ' ').trim();
  }

  /** The text of the cells of every row of a chart's hidden table, as a screen reader gets it. */
  private List<String> rowsOf(String figureTitle) {
    return driver
        .findElements(
            By.xpath(
                SPENDING
                    + "//figure[.//figcaption[normalize-space()='"
                    + figureTitle
                    + "']]//tbody/tr"))
        .stream()
        .map(row -> plain(row.getAttribute("textContent")))
        .toList();
  }

  // --- tests ---

  @Test
  void anaSeesBothChartsOfASampleTripWithItsCostsAndTheExpectedTotal() {
    logInAsAna();
    openTripWithItsCharts();

    // Both figures are named, and each one draws an SVG chart.
    assertThat(driver.findElements(By.xpath(SPENDING + "//figcaption")))
        .extracting(WebElement::getText)
        .containsExactly("Gasto por categoría", "Gasto acumulado por día");
    assertThat(
            driver.findElements(
                By.cssSelector("section[aria-labelledby='spending-title'] svg.recharts-surface")))
        .hasSize(2);
    assertThat(driver.findElements(By.cssSelector(".recharts-bar-rectangle"))).hasSize(3);
    assertThat(driver.findElements(By.cssSelector(".recharts-line-curve"))).hasSize(1);

    // The total, with no budget to compare it to and so no budget line either.
    assertThat(plain(driver.findElement(FIGURES).getText())).isEqualTo("Total gastado: 72,00 €");
    assertThat(driver.findElements(By.cssSelector(".recharts-reference-line"))).isEmpty();
    assertThat(driver.findElements(By.xpath(SPENDING + "//*[@role='progressbar']"))).isEmpty();

    // The same figures, in the hidden tables: per category, and per day with the running sum.
    assertThat(rowsOf("Gasto por categoría"))
        .containsExactly("Visita36,00 €", "Comida22,00 €", "Transporte14,00 €");
    List<String> days = rowsOf("Gasto acumulado por día");
    assertThat(days).hasSize(8);
    assertThat(days.get(0)).isEqualTo("10 jul0,00 €0,00 €");
    assertThat(days.get(1)).isEqualTo("11 jul38,00 €38,00 €");
    assertThat(days.get(7)).isEqualTo("17 jul0,00 €72,00 €");
  }

  @Test
  void theChartsFitTheWidthOfAPhone() {
    logInAsAna();
    driver.manage().window().setSize(new Dimension(375, 812));
    openTripWithItsCharts();

    JavascriptExecutor script = (JavascriptExecutor) driver;
    long viewport = (Long) script.executeScript("return window.innerWidth;");
    assertThat(viewport).isLessThanOrEqualTo(500);
    // Nothing makes the page scroll sideways, and no chart is wider than the screen.
    assertThat(
            (Boolean)
                script.executeScript(
                    "return document.documentElement.scrollWidth <= window.innerWidth;"))
        .isTrue();
    for (WebElement chart :
        driver.findElements(
            By.cssSelector("section[aria-labelledby='spending-title'] svg.recharts-surface"))) {
      assertThat(chart.getRect().getWidth()).isLessThanOrEqualTo((int) viewport);
    }
    assertThat(plain(driver.findElement(FIGURES).getText())).isEqualTo("Total gastado: 72,00 €");
  }
}
