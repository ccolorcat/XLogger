package cc.colorcat.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import cc.colorcat.xlogger.XLogger

class MainActivity : AppCompatActivity() {
    private val logger by lazy(LazyThreadSafetyMode.NONE) { XLogger.getLogger(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.test).setOnClickListener {
            logger.e(RuntimeException("test runtime exception"))
            { "it is a test log." }
        }
    }
}
