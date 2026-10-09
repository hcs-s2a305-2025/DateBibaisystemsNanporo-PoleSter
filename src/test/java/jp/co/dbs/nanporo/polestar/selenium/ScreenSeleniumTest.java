package jp.co.dbs.nanporo.polestar.selenium;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

class ScreenSeleniumTest extends SeleniumTestBase {

    // ---- 未ログイン ----

    @Test
    void loginPageIsDisplayed() {
        open("/login");
        assertFalse(isErrorPage());
        assertTrue(driver.findElement(By.id("loginBtn")).isDisplayed()
                || !driver.findElements(By.id("loginBtn")).isEmpty());
    }

    @Test
    void signupPageIsDisplayed() {
        open("/signup");
        assertFalse(isErrorPage());
        assertTrue(driver.getCurrentUrl().contains("/signup"));
    }

    @Test
    void anonymousIsRedirectedToLogin() {
        open("/menu");
        assertTrue(driver.getCurrentUrl().contains("/login"));
        open("/w/dashboard");
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    void loginFailureShowsError() {
        login(CUSTOMER, "wrong-password");
        assertTrue(driver.getCurrentUrl().contains("/login"));
        assertTrue(driver.getCurrentUrl().contains("error"));
        assertTrue(bodyText().contains("認証に失敗しました"));
    }

    // ---- 顧客 ----

    @Test
    void customerLoginGoesToCustomerHome() {
        login(CUSTOMER, PASSWORD);
        assertFalse(driver.getCurrentUrl().contains("/w/"));
        assertFalse(driver.getCurrentUrl().contains("/login"));
        assertFalse(isErrorPage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "/", "/home", "/menu", "/history", "/cart", "/settings", "/settings/edit", "/stampcard" })
    void customerScreens(String path) {
        login(CUSTOMER, PASSWORD);
        open(path);
        assertFalse(driver.getCurrentUrl().contains("/login"), path + " がログインへ戻された");
        assertFalse(isErrorPage(), path + " でエラー画面");
    }

    @Test
    void customerCannotAccessStaffArea() {
        login(CUSTOMER, PASSWORD);
        open("/w/dashboard");
        assertTrue(isErrorPage() || !driver.getCurrentUrl().contains("/w/dashboard"));
    }

    @Test
    void logoutReturnsToLogin() {
        login(CUSTOMER, PASSWORD);
        open("/home");
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.btn-logout"))).click();
        wait.until(d -> d.getCurrentUrl().contains("/login"));
    }

    // ---- 店員 / 店長 ----

    @Test
    void staffLoginGoesToStaffHome() {
        login(STAFF, PASSWORD);
        assertTrue(driver.getCurrentUrl().contains("/w/home"));
        assertFalse(isErrorPage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "/w/home", "/w/dashboard", "/w/polestarpos", "/w/casherhistory", "/w/editmenu",
            "/w/innerdisplay", "/w/outerdisplay", "/w/account/customer", "/w/account/staff" })
    void managerScreens(String path) {
        login(MANAGER, PASSWORD);
        open(path);
        assertFalse(driver.getCurrentUrl().contains("/login"), path + " がログインへ戻された");
        assertFalse(isErrorPage(), path + " でエラー画面");
    }

    @ParameterizedTest
    @ValueSource(strings = { "/w/home", "/w/polestarpos", "/w/casherhistory", "/w/innerdisplay", "/w/outerdisplay" })
    void staffScreens(String path) {
        login(STAFF, PASSWORD);
        open(path);
        assertFalse(driver.getCurrentUrl().contains("/login"), path + " がログインへ戻された");
        assertFalse(isErrorPage(), path + " でエラー画面");
    }
}
