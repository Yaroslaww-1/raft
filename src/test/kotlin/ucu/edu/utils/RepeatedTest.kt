package ucu.edu.utils

import kotlinx.coroutines.runBlocking

fun repeatedTest(times: Int, body: suspend () -> Unit) = runBlocking {
    repeat(times) {
        println("-------------------------------TEST STARTED-------------------------------")
        body()
        println("-------------------------------TEST ENDED-------------------------------")
    }
}
