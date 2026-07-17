package ir.lbo.locationsms

object NewMessageNotifier {
    private val listeners = mutableListOf<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun notifyListeners() {
        listeners.toList().forEach { it() }
    }
}
