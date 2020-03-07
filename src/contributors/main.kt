package contributors

fun main() {
    setDefaultFontSize(18f)
    ContributorsView().apply {
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }
}