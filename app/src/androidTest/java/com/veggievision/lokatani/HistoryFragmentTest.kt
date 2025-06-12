package com.veggievision.lokatani

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veggievision.lokatani.view.HistoryAdapter
import com.veggievision.lokatani.view.HostActivity
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryFragmentTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(HostActivity::class.java)

    @Before
    fun navigateToHistoryFragment() {
        onView(withId(R.id.historyFragment)).perform(click())
    }

    @Test
    fun historyFragmentViewTest() {
        onView(withId(R.id.buttonSelectAll)).check(matches(isDisplayed()))
        onView(withId(R.id.buttonDeleteSelected)).check(matches(isDisplayed()))
        onView(withId(R.id.buttonExport)).check(matches(isDisplayed()))

       onView(withId(R.id.buttonClearSelection)).check(matches(not(isDisplayed())))

        onView(withId(R.id.textInputLayout))
            .check(matches(hasDescendant(withHint("Pilih Kategori Sayur"))))

       onView(withId(R.id.rvHistory)).check(matches(isDisplayed()))
    }

    @Test
    fun exportTest() {

        onView(withId(R.id.buttonExport)).perform(click())
        onView(withId(R.id.buttonExport)).check(matches(isClickable()))
    }
}