package com.alflabs.milepostlib.utils

import com.google.common.truth.Subject
import org.junit.ComparisonFailure

fun Any.toTestString() =
    this.toString().replace("\r\n", "\n")


fun Subject.diffIsEqualTo(expected: Any) {
    try {
        this.isEqualTo(expected)
    } catch (e: ComparisonFailure) {
        if (e.javaClass.simpleName.endsWith("ComparisonFailureWithFacts")) {
            // Truth Platform$ComparisonFailureWithFacts is a ComparisonFailure but that's
            // not detected by IJ/AS and thus doesn't give us the diff link we need.
            throw ComparisonFailure(e.message, e.expected, e.actual)
        } else {
            throw e
        }
    } catch (e : Throwable) {
        throw e
    }
}
