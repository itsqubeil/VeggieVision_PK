package com.veggievision.lokatani

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.veggievision.lokatani.R
import com.veggievision.lokatani.view.HostActivity

@RunWith(AndroidJUnit4::class)
class NLPFragmentTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(HostActivity::class.java)


    @Before
    fun setUp() {
       onView(withId(R.id.nlpFragment)).perform(click())
    }

    @Test
    fun dataQnAViewTest() {
        // Test semua elemen UI ditampilkan dengan benar

        // RecyclerView untuk history chat
        onView(withId(R.id.rvHistory))
            .check(matches(isDisplayed()))

        // Input field untuk query
        onView(withId(R.id.etQuery))
            .check(matches(isDisplayed()))

        // Tombol Ask
        onView(withId(R.id.btnAsk))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))

        // Tombol Import Excel
        onView(withId(R.id.btnImportExcel))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))

    }

    @Test
    fun inputTextTest() {
        val testQuery = "Test input text"

        // Test apakah EditText bisa menerima input
        onView(withId(R.id.etQuery))
            .perform(typeText(testQuery), closeSoftKeyboard())

        // Verifikasi teks berhasil diinput
        onView(withId(R.id.etQuery))
            .check(matches(withText(testQuery)))
    }

}