package cc.colorcat.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import cc.colorcat.xlogger.XLogger

class MainActivity : AppCompatActivity() {
    private val logger by lazy(LazyThreadSafetyMode.NONE) { XLogger.getLogger(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.test).setOnClickListener {
            logger.d { "this is a test log." }
        }
    }
}
