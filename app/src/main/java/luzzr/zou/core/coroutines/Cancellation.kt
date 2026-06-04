package luzzr.zou.core.coroutines

import kotlinx.coroutines.CancellationException

fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) {
        throw this
    }
}
