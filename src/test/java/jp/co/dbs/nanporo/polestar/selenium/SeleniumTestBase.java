package jp.co.dbs.nanporo.polestar.selenium;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/** 実ブラウザ(Chrome headless)で起動中アプリを操作するテストの共通基盤。 */
@Tag("selenium")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class SeleniumTestBase {

    protected static final String PASSWORD = "Selenium1234!";
    protected static final String CUSTOMER = "selenium-customer@example.com";
    protected static final String STAFF = "selenium-staff@example.com";
    protected static final String MANAGER = "selenium-manager@example.com";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder encoder;

    protected WebDriver driver;
    protected WebDriverWait wait;

    @BeforeEach
    void setUpBrowser() {
        seedUser(CUSTOMER, "セレニウム顧客", "顧客");
        seedUser(STAFF, "セレニウム店員", "店員");
        seedUser(MANAGER, "セレニウム店長", "店長");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--window-size=1280,1024",
                "--no-sandbox", "--disable-gpu", "--lang=ja");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDownBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void seedUser(String mail, String name, String role) {
        jdbc.update("DELETE FROM user_m WHERE mail = ?", mail);
        jdbc.update("INSERT INTO user_m(mail, name, password, role, member_rank, gender, birthday, cancel_count, alive, point, point_card_complete, icon) "
                + "VALUES (?, ?, ?, ?, '一般', '男', '1990-01-01', 0, false, 0, 0, 'sibainu1.png')",
                mail, name, encoder.encode(PASSWORD), role);
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    protected void open(String path) {
        driver.get(url(path));
    }

    protected void login(String mail, String password) {
        open("/login");
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("splash-screen")));
        driver.findElement(By.id("mail")).sendKeys(mail);
        driver.findElement(By.id("password")).sendKeys(password);
        wait.until(ExpectedConditions.elementToBeClickable(By.id("loginBtn"))).click();
        wait.until(d -> !d.getCurrentUrl().endsWith("/login-success"));
    }

    /** エラー画面(Whitelabel/5xx)でないことを確認するための本文取得。 */
    protected String bodyText() {
        return driver.findElement(By.tagName("body")).getText();
    }

    protected boolean isErrorPage() {
        String text = bodyText();
        String title = driver.getTitle() == null ? "" : driver.getTitle();
        return text.contains("Whitelabel Error Page")
                || text.contains("Internal Server Error")
                || title.startsWith("HTTP Status")
                || driver.getCurrentUrl().contains("/error");
    }
}
