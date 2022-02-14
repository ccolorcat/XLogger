package cc.colorcat.sample

import cc.colorcat.xlogger.XLogger
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Before
    fun init() {
        XLogger.setLogPrinter() { _, _, msg ->
            println(msg)
        }
    }

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testLogger() {
        XLogger.getLogger("Test").d { "this is a test log." }
    }
}