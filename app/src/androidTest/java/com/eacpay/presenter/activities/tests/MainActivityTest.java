//package com.eacpay.presenter.activities.tests;
//
//import android.support.test.rule.ActivityTestRule;
//import android.support.test.runner.AndroidJUnit4;
//
//import com.eacpay.R;
//import com.eacpay.eactalk.MainActivity;
//import com.eacpay.tools.adapter.CurrencyListAdapter;
//
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import java.util.Random;
//
//import static android.support.test.espresso.Espresso.onData;
//import static android.support.test.espresso.Espresso.onView;
//import static android.support.test.espresso.Espresso.pressBack;
//import static android.support.test.espresso.action.ViewActions.clearText;
//import static android.support.test.espresso.action.ViewActions.click;
//import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
//import static android.support.test.espresso.action.ViewActions.swipeLeft;
//import static android.support.test.espresso.action.ViewActions.swipeRight;
//import static android.support.test.espresso.action.ViewActions.typeText;
//import static android.support.test.espresso.matcher.ViewMatchers.withId;
//import static org.hamcrest.Matchers.hasToString;
//import static org.hamcrest.Matchers.startsWith;
//

//
//@RunWith(AndroidJUnit4.class)
//public class MainActivityTest {
//
//    @Rule
//    public ActivityTestRule<MainActivity> mActivityRule =
//            new ActivityTestRule<>(MainActivity.class);
//
//    @Test
//    public void testCurrencyList() {
//        onView(withId(R.id.main_button_burger)).perform(click());
//        onView(withId(R.id.settings)).perform(click());
//        onView(withId(R.id.local_currency)).perform(click());
//        if (!CurrencyListAdapter.currencyListAdapter.isEmpty()) {
//            Random r = new Random();
//
//            //click on random list item;
//            int rand;
//            rand = r.nextInt(CurrencyListAdapter.currencyListAdapter.getCount());
//            onData(hasToString(startsWith("")))
//                    .inAdapterView(withId(R.id.currency_list_view)).atPosition(rand)
//                    .perform(click());
//            rand = r.nextInt(CurrencyListAdapter.currencyListAdapter.getCount());
//            onData(hasToString(startsWith("")))
//                    .inAdapterView(withId(R.id.currency_list_view)).atPosition(rand)
//                    .perform(click());
//            rand = r.nextInt(CurrencyListAdapter.currencyListAdapter.getCount());
//            onData(hasToString(startsWith("")))
//                    .inAdapterView(withId(R.id.currency_list_view)).atPosition(rand)
//                    .perform(click());
//            rand = r.nextInt(CurrencyListAdapter.currencyListAdapter.getCount());
//            onData(hasToString(startsWith("")))
//                    .inAdapterView(withId(R.id.currency_list_view)).atPosition(rand)
//                    .perform(click());
//            rand = r.nextInt(CurrencyListAdapter.currencyListAdapter.getCount());
//            onData(hasToString(startsWith("")))
//                    .inAdapterView(withId(R.id.currency_list_view)).atPosition(rand)
//                    .perform(click());
//        }
//        pressBack();
//        pressBack();
//        pressBack();
//
//    }
//
////    @Test
////    public void testFragments() {
////        onView(withId(R.id.main_button_burger)).perform(click());
////        try {
////            Thread.sleep(1000);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
////        onView(withId(R.id.settings)).perform(click());
////        onView(withId(R.id.start_recovery_wallet)).perform(click());
////        pressBack();
////        pressBack();
////        onView(withId(R.id.about)).perform(click());
////        onView(withId(R.id.main_button_burger)).perform(click());
////        onView(withId(R.id.main_button_burger)).perform(click());
////        onView(withId(R.id.main_button_burger)).perform(click());
////        onView(withId(R.id.main_layout)).perform(swipeLeft());
////        onView(withId(R.id.theAddressLayout)).perform(click());
////        onView(withId(R.id.copy_address)).perform(click());
////        onView(withId(R.id.main_layout)).perform(swipeRight());
////        try {
////            Thread.sleep(500);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
////    }
////
////
////    @Test
////    public void testChangeText_sameActivity() {
////        onView(withId(R.id.address_edit_text))
////                .perform(clearText(), typeText("some testing text"), closeSoftKeyboard(), clearText());
////
////    }
//}
