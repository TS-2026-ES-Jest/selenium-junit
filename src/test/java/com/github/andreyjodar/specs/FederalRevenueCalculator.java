package com.github.andreyjodar.specs;

import com.github.andreyjodar.model.TestCaseRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FederalRevenueCalculator {

    private static WebDriver driver;
    private static WebDriverWait wait;

    @BeforeAll
    public static void setUpAll() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get("https://www27.receita.fazenda.gov.br/simulador-irpf/");

        WebElement yearDropdown = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("mat-select[formcontrolname='anoCalendario']")
        ));
        yearDropdown.click();

        WebElement selectedYear = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//mat-option//span[contains(text(), '2022')]")
        ));
        selectedYear.click();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".cdk-overlay-backdrop")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCaseStreamProvider")
    public void testInpostCalculation(TestCaseRecord caso) throws InterruptedException {

        WebElement inputValue = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input[formcontrolname='rendTributaveis']")
        ));

        // DICA DE OURO PARA ANGULAR: O método .clear() do Selenium às vezes falha
        // em componentes Material que aplicam máscaras de moeda. A forma mais
        // segura de limpar é simular o "Ctrl + A" e deletar:
        String os = System.getProperty("os.name");
        Keys ctrlOrCmd = os.contains("Mac") ? Keys.COMMAND : Keys.CONTROL;
        inputValue.sendKeys(Keys.chord(ctrlOrCmd, "a"), Keys.BACK_SPACE);

        String formattedInputValue = String.format("%.2f", caso.inputValue()).replace(".", ",");
        inputValue.sendKeys(formattedInputValue);

        // Pressionar TAB para engatilhar o cálculo automático
        inputValue.sendKeys(Keys.TAB);

        // Usado XPath para garantir que pegamos a label vizinha de "4. Imposto"
        By resultSelector = By.xpath("//label[contains(text(), '4. Imposto')]/following-sibling::label[contains(@class, 'card-result-input')]");

        // Aguarda a tela ser atualizada
        WebElement resultField = wait.until(ExpectedConditions.visibilityOfElementLocated(resultSelector));

        // Pausa para garantir que o DOM atualizou o texto antes de lermos
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        String formattedResponse = resultField.getText().trim();

        assertEquals(caso.formattedResponse(), formattedResponse,
                "Falha na validação do " + caso.testCaseId() + " (" + caso.description() + ")");
    }

    @AfterAll
    public static void tearDownAll() {
        if (driver != null) {
            driver.quit();
        }
    }

    private static Stream<TestCaseRecord> testCaseStreamProvider() {
        return Stream.of(
                new TestCaseRecord("CT01", "Limite máximo da faixa de Isenção", 1903.98, "0,00"),
                new TestCaseRecord("CT02", "Limite mínimo da 2ª faixa", 1903.99, "0,00"),
                new TestCaseRecord("CT03", "Limite máximo da 2ª faixa", 2826.65, "69,20"),
                new TestCaseRecord("CT04", "Limite mínimo da 3ª faixa", 2826.66, "69,20"),
                new TestCaseRecord("CT05", "Limite máximo da 3ª faixa", 3751.05, "207,86"),
                new TestCaseRecord("CT06", "Limite mínimo da 4ª faixa", 3751.06, "207,86"),
                new TestCaseRecord("CT07", "Limite máximo da 4ª faixa", 4664.68, "413,42"),
                new TestCaseRecord("CT08", "Limite mínimo da última faixa", 4664.69, "413,42"),
                new TestCaseRecord("CT09", "Valor bem acima do limite", 7000.00, "1.055,64")
        );
    }
}