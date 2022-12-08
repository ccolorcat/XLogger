/*
 * Copyright 2022 cxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.colorcat.xlogger

import android.annotation.SuppressLint
import androidx.collection.LruCache
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Author: cxx
 * Date: 2022-02-14
 * GitHub: https://github.com/ccolorcat
 */
class XLogger private constructor(private val tag: String) {
    var threshold: Int = sThreshold
        set(value) {
            field = value.coerceAtLeast(ALL).coerceAtMost(NONE)
        }

    var stackTraceDepth: Int = sStackTraceDepth
        set(value) {
            field = value.coerceAtLeast(DEPTH_MIN).coerceAtMost(DEPTH_MAX)
        }

    var timeFormatter: TimeFormatter = sTimeFormatter

    var threadNameFormatter: ThreadNameFormatter = sThreadNameFormatter

    fun v(msg: String) {
        print(VERBOSE, msg)
    }

    fun v(builder: () -> CharSequence) {
        print(VERBOSE, builder)
    }

    fun d(msg: String) {
        print(DEBUG, msg)
    }

    fun d(builder: () -> CharSequence) {
        print(DEBUG, builder)
    }

    fun i(msg: String) {
        print(INFO, msg)
    }

    fun i(builder: () -> CharSequence) {
        print(INFO, builder)
    }

    fun w(msg: String) {
        print(WARN, msg)
    }

    fun w(builder: () -> CharSequence) {
        print(WARN, builder)
    }

    fun e(msg: String) {
        print(ERROR, msg)
    }

    fun e(throwable: Throwable) {
        return e(throwable) { "" }
    }

    fun e(throwable: Throwable, builder: () -> CharSequence) {
        return e {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            kotlin.runCatching {
                pw.use {
                    val msg = builder.invoke()
                    if (msg.isNotEmpty()) {
                        it.println(msg)
                    }
                    throwable.printStackTrace(pw)
                    it.flush()
                }
            }
            sw.toString()
        }
    }

    fun e(builder: () -> CharSequence) {
        print(ERROR, builder)
    }

    fun print(priority: Int, builder: () -> CharSequence) {
        if (priority >= threshold) {
            print(priority, builder())
        }
    }

    fun print(priority: Int, msg: CharSequence) {
        if (priority < threshold) return
        val builder = StringBuilder().append(" \n")
        concatRepeat(builder, SEPARATOR, sSeparatorHalfLength)
        val time = timeFormatter.format(System.currentTimeMillis())
        val threadName = threadNameFormatter.format(Thread.currentThread())
        builder.append(' ').append("[$tag][$time][$threadName]").append(' ')
        concatRepeat(builder, SEPARATOR, sSeparatorHalfLength)
        val lineLength: Int = builder.length - 2
        concatStackTrace(builder)
        builder.append("\n\n").append(msg).append('\n')
        concatRepeat(builder, SEPARATOR, lineLength / SEPARATOR.length)
        val log = builder.toString()
        val length = log.length
        if (length <= sMaxLength) {
            sPrinter.print(priority, tag, log)
            return
        }
        var start = 0
        var end: Int
        while (start < length) {
            end = friendlyEnd(log, start, (start + sMaxLength).coerceAtMost(length))
            sPrinter.print(priority, tag, log.substring(start, end))
            start = end
        }
    }

    private fun concatStackTrace(builder: StringBuilder): StringBuilder {
        if (stackTraceDepth <= 0) return builder
        var matched = false
        var count = 0
        for (e in Thread.currentThread().stackTrace) {
            if (count >= stackTraceDepth) break
            val msg = e.toString()
            if (msg.startsWith(CLASS_NAME)) {
                matched = true
            } else if (matched) {
                ++count
                builder.append('\n').append(msg)
            }
        }
        return builder
    }

    companion object {
        internal const val DEPTH_MIN = 0
        internal const val DEPTH_MAX = 100

        internal const val HALF_LENGTH_MIN = 30
        internal const val HALF_LENGTH_MAX = 200

        internal const val MAX_LENGTH_MIN = 80
        internal const val MAX_LENGTH_MAX = Int.MAX_VALUE

        const val VERBOSE = 2
        const val DEBUG = 3
        const val INFO = 4
        const val WARN = 5
        const val ERROR = 6
        const val ASSERT = 7

        const val ALL = 1
        const val NONE = 8

        private const val SEPARATOR = "="
        private const val DEFAULT_TAG = "XLogger"

        private val CLASS_NAME = XLogger::class.java.name

        private val sLogger = LruCache<String, XLogger>(6)

        private var sPrinter: LogPrinter = AndroidLogPrinter()
        private var sMaxLength = 4000

        private var sSeparatorHalfLength = 50
        private var sThreshold = ALL
        private var sStackTraceDepth = 3
        private var sTimeFormatter: TimeFormatter = object : TimeFormatter {
            @SuppressLint("SimpleDateFormat")
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
            override fun format(time: Long): String {
                return formatter.format(Date(time))
            }
        }
        private var sThreadNameFormatter: ThreadNameFormatter = ThreadNameFormatter {
            it.toString()
        }

        fun setDefaultThreshold(threshold: Int) {
            sThreshold = threshold.coerceAtLeast(ALL).coerceAtMost(NONE)
        }

        fun setDefaultTraceDepth(depth: Int) {
            sStackTraceDepth = depth.coerceAtLeast(DEPTH_MIN).coerceAtMost(DEPTH_MAX)
        }

        fun setDefaultTimeFormatter(formatter: TimeFormatter) {
            sTimeFormatter = formatter
        }

        fun setDefaultThreadNameFormatter(formatter: ThreadNameFormatter) {
            sThreadNameFormatter = formatter
        }

        fun setLogPrinter(printer: LogPrinter) {
            sPrinter = printer
        }

        fun setMaxLength(maxLength: Int) {
            sMaxLength = maxLength.coerceAtLeast(MAX_LENGTH_MIN).coerceAtMost(MAX_LENGTH_MAX)
        }

        fun setSeparatorHalfLength(halfLength: Int) {
            sSeparatorHalfLength = halfLength.coerceAtLeast(HALF_LENGTH_MIN).coerceAtMost(HALF_LENGTH_MAX)
        }

        fun getLogger(any: Any): XLogger {
            return getLogger(any.javaClass.simpleName)
        }


        fun getLogger(tag: String): XLogger {
            var logger = sLogger[tag]
            if (logger == null) {
                logger = XLogger(tag)
                sLogger.put(tag, logger)
            }
            return logger
        }


        val default: XLogger
            get() = getLogger(DEFAULT_TAG)


        private fun friendlyEnd(msg: String, start: Int, end: Int): Int {
            if (msg.length == end || msg[end] == '\n') {
                return end
            }
            for (last in end - 1 downTo start + 1) {
                if (msg[last] == '\n') {
                    return last + 1
                }
            }
            return end
        }

        private fun concatRepeat(builder: StringBuilder, str: String, count: Int): StringBuilder {
            for (i in 0 until count) {
                builder.append(str)
            }
            return builder
        }
    }
}
