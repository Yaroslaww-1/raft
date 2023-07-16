package ucu.edu.utils

import mu.KotlinLogging
import java.util.*
import kotlin.concurrent.schedule

class RandomisedTimer(
    min: Long,
    max: Long,
    val action: () -> Unit
) {
    private val logger = KotlinLogging.logger {}

    private val interval = (min..max).random()
    private var timer = Timer()

    fun start() {
        timer.schedule(interval) {
            action()
        }
    }

    fun cancel() {
        timer.cancel()
        timer = Timer()
    }

    fun restart() {
        cancel()
        start()
    }
}