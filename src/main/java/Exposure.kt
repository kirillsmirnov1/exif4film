import java.util.*

data class Exposure(
        val number: Int,
        val time: Date? = null,
        val description: String? = null,
        val aperture: String? = null,
        val shutterSpeed: String? = null
)