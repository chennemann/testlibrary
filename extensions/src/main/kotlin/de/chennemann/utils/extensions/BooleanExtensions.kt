package de.chennemann.utils.extensions

fun Boolean?.ifTrue(execution: () -> Unit): Boolean? {
    if (checkTrue(this)) {
        execution()
    }
    return this
}

fun Boolean?.ifFalse(execution: () -> Unit): Boolean? {
    if (checkFalse(this)) {
        execution()
    }
    return this
}

fun <R> Boolean?.runIfTrue(execution: () -> R): R? {
    if (checkTrue(this)) {
        return execution()
    }

    return null
}

fun <R> Boolean?.runIfFalse(execution: () -> R): R? {
    if (checkFalse(this)) {
        return execution()
    }

    return null
}

private fun checkTrue(boolean: Boolean?): Boolean {
    return boolean != null && boolean
}

private fun checkFalse(boolean: Boolean?): Boolean {
    return boolean != null && boolean.not()
}