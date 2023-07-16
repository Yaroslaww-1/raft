package ucu.edu.utils

import kotlinx.coroutines.runBlocking

fun repeatedTest(times: Int, body: suspend () -> Unit) {
    repeat(times) {
        runBlocking {
            body()
        }
    }
}