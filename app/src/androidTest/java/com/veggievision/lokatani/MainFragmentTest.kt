package com.veggievision.lokatani

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.veggievision.lokatani.view.HostActivity

@RunWith(AndroidJUnit4::class)
class MainFragmentTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(HostActivity::class.java)

    @Test
    fun dashboardViewTest() {
        onView(withId(R.id.tvTitle))
            .check(matches(isDisplayed()))
            .check(matches(withText("Prediksi Konsumsi Sayuran")))

        onView(withId(R.id.chartCardView)).check(matches(isDisplayed()))

        onView(withId(R.id.filterSpinner)).check(matches(isDisplayed()))

        onView(withId(R.id.lineChart)).check(matches(isDisplayed()))

        onView(withId(R.id.additionalInfoLayout)).check(matches(isDisplayed()))

        onView(withId(R.id.tvCurrentWeekLabel)).check(matches(isDisplayed()))
        onView(withId(R.id.tvCurrentWeekInfo)).check(matches(isDisplayed()))

        onView(withId(R.id.tvMostConsumedLabel)).check(matches(isDisplayed()))
        onView(withId(R.id.tvMostConsumedVegetable)).check(matches(isDisplayed()))
    }
}