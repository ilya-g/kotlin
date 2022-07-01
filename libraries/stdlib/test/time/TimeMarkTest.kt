/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalTime::class)
package test.time

import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTime::class)
class TimeMarkTest {

    @Test
    fun adjustment() {
        val timeSource = TestTimeSource()

        fun TimeMark.assertHasPassed(hasPassed: Boolean) {
            assertEquals(!hasPassed, this.hasNotPassedNow(), "Expected mark in the future")
            assertEquals(hasPassed, this.hasPassedNow(), "Expected mark in the past")

            assertEquals(
                !hasPassed,
                this.elapsedNow() < Duration.ZERO,
                "Mark elapsed: ${this.elapsedNow()}, expected hasPassed: $hasPassed"
            )
        }

        val mark = timeSource.markNow()
        val markFuture1 = (mark + 1.milliseconds).apply { assertHasPassed(false) }
        val markFuture2 = (mark - (-1).milliseconds).apply { assertHasPassed(false) }
        assertTrue(markFuture1 > mark)
        assertTrue(markFuture2 > mark)

        val markPast1 = (mark - 1.milliseconds).apply { assertHasPassed(true) }
        val markPast2 = (markFuture1 + (-2).milliseconds).apply { assertHasPassed(true) }
        assertTrue(markPast1 < mark)
        assertTrue(markPast2 < mark)

        timeSource += 500_000.nanoseconds

        val markElapsed = timeSource.markNow()
        val elapsedDiff = markElapsed - mark

        val elapsed = mark.elapsedNow()
        val elapsedFromFuture = elapsed - 1.milliseconds
        val elapsedFromPast = elapsed + 1.milliseconds

        assertEquals(0.5.milliseconds, elapsed)
        assertEquals(elapsedFromFuture, markFuture1.elapsedNow())
        assertEquals(elapsedFromFuture, markFuture2.elapsedNow())
        assertEquals(elapsedDiff, elapsed)

        val markToElapsed = mark + elapsedDiff
        assertEquals(markElapsed, markToElapsed)
        assertEquals(markElapsed.hashCode(), markToElapsed.hashCode())

        assertEquals(elapsedFromPast, markPast1.elapsedNow())
        assertEquals(elapsedFromPast, markPast2.elapsedNow())

        markFuture1.assertHasPassed(false)
        markPast1.assertHasPassed(true)

        timeSource += 1.milliseconds

        markFuture1.assertHasPassed(true)
        markPast1.assertHasPassed(true)

    }

    fun testAdjustmentInfinite(timeSource: TimeSource<ComparableTimeMark>) {
        val baseMark = timeSource.markNow()
        val infiniteFutureMark = baseMark + Duration.INFINITE
        val infinitePastMark = baseMark - Duration.INFINITE

        assertTrue(infinitePastMark < baseMark)
        assertTrue(baseMark < infiniteFutureMark)
        assertTrue(infinitePastMark < infiniteFutureMark)

        assertEquals(Duration.INFINITE, infiniteFutureMark - infinitePastMark)
        assertEquals(Duration.INFINITE, infiniteFutureMark - baseMark)
        assertEquals(-Duration.INFINITE, infinitePastMark - baseMark)
        assertEquals(Duration.ZERO, infiniteFutureMark - infiniteFutureMark)
        assertEquals(Duration.ZERO, infinitePastMark - infinitePastMark)

        assertEquals(-Duration.INFINITE, infiniteFutureMark.elapsedNow())
        assertTrue(infiniteFutureMark.hasNotPassedNow())

        assertEquals(Duration.INFINITE, infinitePastMark.elapsedNow())
        assertTrue(infinitePastMark.hasPassedNow())

        assertFailsWith<IllegalArgumentException> { infiniteFutureMark - Duration.INFINITE }
        assertFailsWith<IllegalArgumentException> { infinitePastMark + Duration.INFINITE }


        val longDuration = Long.MAX_VALUE.nanoseconds
        val long2Duration = longDuration + 1001.milliseconds

        val pastMark = baseMark - longDuration
        val futureMark = pastMark + long2Duration
        val sameMark = futureMark - (long2Duration - longDuration)

        val elapsedMark = timeSource.markNow()
        val elapsedDiff = (sameMark.elapsedNow() - baseMark.elapsedNow()).absoluteValue
        val elapsedDiff2 = (baseMark.elapsedNow() - sameMark.elapsedNow()).absoluteValue
        assertTrue(maxOf(elapsedDiff, elapsedDiff2) < 1.milliseconds, "$elapsedDiff, $elapsedDiff2")
        assertEquals(elapsedMark - baseMark, elapsedMark - sameMark, "$elapsedMark; $baseMark; $sameMark")
    }

    @Test
    fun adjustmentInfinite() {
        testAdjustmentInfinite(TestTimeSource())
    }

    fun testLongDisplacement(timeSource: TimeSource<ComparableTimeMark>, wait: (Duration) -> Unit) {
        val baseMark = timeSource.markNow()
        val longDuration = Long.MAX_VALUE.nanoseconds
        val waitDuration = 20.milliseconds
        val pastMark = baseMark - longDuration
        wait(waitDuration)
        val elapsedMark = timeSource.markNow()
        val elapsed = pastMark.elapsedNow()
        val elapsedDiff = elapsedMark - pastMark
        assertTrue(elapsed > longDuration)
        assertTrue(elapsed >= longDuration + waitDuration, "$elapsed, $longDuration, $waitDuration")
        assertTrue(elapsedDiff >= longDuration + waitDuration)
        assertTrue(elapsed >= elapsedDiff)
    }

    @Test
    fun longDisplacement() {
        val timeSource = TestTimeSource()
        testLongDisplacement(timeSource, { waitDuration -> timeSource += waitDuration })
    }

    @Test
    fun timeMarkDifferenceAndComparison() {
        val timeSource = TestTimeSource()
        val timeSource2 = TestTimeSource()
        val baseMark = timeSource.markNow()

        var markBefore = baseMark
        markBefore -= 100.microseconds
        markBefore -= 100.microseconds

        val markAfter = baseMark + 100.microseconds

        assertEquals(300.microseconds,markAfter - markBefore)
        assertTrue(markBefore < markAfter)
        assertFalse(markBefore > markAfter)
        assertEquals(0,baseMark compareTo baseMark)
        assertEquals(baseMark as Any, baseMark as Any)

        timeSource += 100.microseconds
        val markElapsed = timeSource.markNow()
        assertEquals(0, markAfter compareTo markElapsed)
        assertEquals(Duration.ZERO, markAfter - markElapsed)
        assertEquals(markAfter, markElapsed)
        assertEquals(markAfter.hashCode(), markElapsed.hashCode())

        val differentSourceMark = TimeSource.Monotonic.markNow()
        assertFailsWith<IllegalArgumentException> { baseMark - differentSourceMark }
        assertFailsWith<IllegalArgumentException> { baseMark < differentSourceMark }

        val differentSourceMark2 = timeSource2.markNow()
        assertFailsWith<IllegalArgumentException> { baseMark - differentSourceMark2 }
        assertFailsWith<IllegalArgumentException> { baseMark < differentSourceMark2 }
    }

    @Test
    fun defaultTimeMarkAdjustment() {
        val baseMark = TimeSource.Monotonic.markNow()

        var markBefore = baseMark
        markBefore -= 100.microseconds
        markBefore -= 100.microseconds

        val markAfter = baseMark + 100.microseconds

        MeasureTimeTest.longRunningCalc()

        val elapsedMark = TimeSource.Monotonic.markNow()
        val elapsedDiff = elapsedMark - baseMark
        assertTrue(elapsedDiff > Duration.ZERO)

        val elapsedAfter = markAfter.elapsedNow()
        val elapsedBase = baseMark.elapsedNow()
        val elapsedBefore = markBefore.elapsedNow()
        assertTrue(elapsedBefore >= elapsedBase + 200.microseconds)
        assertTrue(elapsedAfter <= elapsedBase - 100.microseconds)
        assertTrue(elapsedBase >= elapsedDiff)
    }

    @Test
    fun defaultTimeMarkAdjustmentInfinite() {
        testAdjustmentInfinite(TimeSource.Monotonic)

        // do the same with specialized methods
        val baseMark = TimeSource.Monotonic.markNow()
        val infiniteFutureMark = baseMark + Duration.INFINITE
        val infinitePastMark = baseMark - Duration.INFINITE

        assertEquals(-Duration.INFINITE, infiniteFutureMark.elapsedNow())
        assertTrue(infiniteFutureMark.hasNotPassedNow())

        assertEquals(Duration.INFINITE, infinitePastMark.elapsedNow())
        assertTrue(infinitePastMark.hasPassedNow())

        assertFailsWith<IllegalArgumentException> { infiniteFutureMark - Duration.INFINITE }
        assertFailsWith<IllegalArgumentException> { infinitePastMark + Duration.INFINITE }

        val longDuration = Long.MAX_VALUE.nanoseconds
        val long2Duration = longDuration + 1001.milliseconds

        val pastMark = baseMark - longDuration
        val futureMark = pastMark + long2Duration
        val sameMark = futureMark - (long2Duration - longDuration)

        val elapsedDiff = (sameMark.elapsedNow() - baseMark.elapsedNow()).absoluteValue
        val elapsedDiff2 = (baseMark.elapsedNow() - sameMark.elapsedNow()).absoluteValue
        assertTrue(maxOf(elapsedDiff, elapsedDiff2) < 1.milliseconds, "$elapsedDiff, $elapsedDiff2")
    }

    @Test
    fun defaultTimeMarkDifferenceAndComparison() {
        val baseMark = TimeSource.Monotonic.markNow()

        var markBefore = baseMark
        markBefore -= 100.microseconds
        markBefore -= 100.microseconds

        val markAfter = baseMark + 100.microseconds

        assertEquals(300.microseconds,markAfter - markBefore)
        assertTrue(markBefore < markAfter)
        assertFalse(markBefore > markAfter)
        assertEquals(0,baseMark compareTo baseMark)
        assertEquals(baseMark as Any, baseMark as Any)
        assertEquals(baseMark.hashCode(), baseMark.hashCode())

        val differentSourceMark = TestTimeSource().markNow()
        assertFailsWith<IllegalArgumentException> { baseMark - differentSourceMark }
        assertFailsWith<IllegalArgumentException> { baseMark < differentSourceMark }
    }
}
