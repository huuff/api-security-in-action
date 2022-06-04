package xyz.haff.apisecurity.util

infix fun Boolean.implies(f: () -> Unit) {
    if (this)
        f()
}