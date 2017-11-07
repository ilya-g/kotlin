/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package samples

import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

typealias Sample = org.junit.Test
typealias RunWith = org.junit.runner.RunWith
typealias Enclosed = org.junit.experimental.runners.Enclosed

fun assertPrints(expression: Any?, expectedOutput: String) = assertEquals(expectedOutput, expression.toString())

class CheckedConsole {
    val writer = StringWriter()
    val out = PrintWriter(writer)

    fun assertPrintsLines(vararg lines: String) {
        val actualLines = writer.toString().lines()
        assertTrue(actualLines.lastOrNull().isNullOrEmpty(), "Output expected to end with newline")
        assertEquals(lines.asList(), actualLines.dropLast(1))
    }
}
