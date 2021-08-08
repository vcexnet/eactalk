package com.eactalk.screenshots;

import android.os.Build;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingPolicies;
import androidx.test.rule.ActivityTestRule;

import com.eactalk.R;
import com.eactalk.presenter.activities.BreadActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasToString;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;
import tools.fastlane.screengrab.locale.LocaleUtil;
@RunWith(JUnit4.class)
public class JUnit4StyleTests {
    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityTestRule<BreadActivity> activityRule = new ActivityTestRule<>(BreadActivity.class);

    @BeforeClass
    public static void beforeClass() {
        IdlingPolicies.setMasterPolicyTimeout(600, TimeUnit.SECONDS);
        IdlingPolicies.setIdlingResourceTimeout(600, TimeUnit.SECONDS);
    }

    @Before
    public void setUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
        }
        activityRule.getActivity();
        LocaleUtil.changeDeviceLocaleTo(LocaleUtil.getTestLocale());
    }

    @After
    public void tearDown() {
        LocaleUtil.changeDeviceLocaleTo(LocaleUtil.getEndingLocale());
    }

    @Test
    public void testTakeScreenshot() {
        sleep(1000);
        Screengrab.screenshot("transaction_list");
        onView(withId(R.id.menu_layout)).perform(click());
        clickMenu(0);
//        sleep(1000);
        Screengrab.screenshot("security_center");
        Espresso.pressBack();
        clickMenu(1);
        sleep(1000);
        Screengrab.screenshot("support");
        Espresso.pressBack();
        clickMenu(3);
//        sleep(1000);
        Screengrab.screenshot("unlock_screen");

    }

    private void clickMenu(int pos) {
        onData(anything()).inAdapterView(withId(R.id.menu_listview)).atPosition(pos).perform(click());
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}