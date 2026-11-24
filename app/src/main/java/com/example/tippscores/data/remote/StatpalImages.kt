package com.example.tippscores.data.remote

/**
 * A StatPal "Get Image" végpontja (v2/soccer/images) képet ad vissza
 * csapatokhoz, játékosokhoz és edzőkhöz. A pontos paraméterneveket a
 * docs oldal egyetlen minta-hívása alapján kell finomítani: ha a
 * képek nem töltenek be, elég az [BASE] vagy a [build] függvényt
 * átírni (vagy az [enabled] jelzőt kikapcsolni), a képernyők ilyenkor
 * automatikusan a mezszámos / emojis tartalékra esnek vissza.
 */
object StatpalImages {

    /** MainActivity állítja be: az API-kulcs az appon belül adható meg. */
    var keyProvider: () -> String = { "" }

    var enabled: Boolean = true

    private const val BASE = "https://statpal.io/api/v2/soccer/images"

    fun player(id: String?): String? = build("player", id)

    fun team(id: String?): String? = build("team", id)

    fun coach(id: String?): String? = build("coach", id)

    private fun build(type: String, id: String?): String? {
        if (!enabled) return null
        val cleanId = id?.trim().orEmpty()
        if (cleanId.isEmpty()) return null
        val key = keyProvider().trim()
        if (key.isEmpty()) return null
        return "$BASE?type=$type&id=$cleanId&access_key=$key"
    }
}
