package com.example.tippscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tippscores.data.local.FollowStore
import com.example.tippscores.data.model.LeaguePlayerStat
import com.example.tippscores.data.model.LeagueStats
import com.example.tippscores.data.model.PlayerFull
import com.example.tippscores.data.model.PlayerStatRow
import com.example.tippscores.data.remote.StatpalImages
import com.example.tippscores.data.repository.PlayerRepository
import com.example.tippscores.data.repository.TeamProfileRepository
import com.example.tippscores.data.util.PlayerBenchmarks
import com.example.tippscores.ui.components.FractionBar
import com.example.tippscores.ui.components.RadarChart
import com.example.tippscores.ui.components.RemoteImage
import com.example.tippscores.ui.components.rememberHu
import com.example.tippscores.ui.theme.AppColorScheme
import com.example.tippscores.ui.theme.LocalAppColors
import java.util.Locale

private val Good = Color(0xFF16A34A)
private val Warn = Color(0xFFD97706)
private val Bad = Color(0xFFDC2626)
private val Blue = Color(0xFF2563EB)

/** Teljes képernyős játékosprofil: áttekintés, radar a bajnoksághoz képest, statisztika, karrier. */
@Composable
fun PlayerProfileScreen(
    playerId: String,
    playerName: String,
    playerRepository: PlayerRepository,
    teamRepository: TeamProfileRepository,
    followStore: FollowStore,
    onBack: () -> Unit,
    onTeamClick: (String, String) -> Unit,
    onPlayerClick: (String, String) -> Unit = { _, _ -> }
) {
    val colors = LocalAppColors.current

    var player by remember(playerId) { mutableStateOf<PlayerFull?>(null) }
    var loading by remember(playerId) { mutableStateOf(true) }
    var leagueStats by remember(playerId) { mutableStateOf<LeagueStats?>(null) }
    var tab by remember(playerId) { mutableIntStateOf(0) }
    var following by remember(playerId) { mutableStateOf(followStore.isFollowing(playerId)) }

    LaunchedEffect(playerId) {
        loading = true
        player = try {
            playerRepository.fetch(playerId)
        } catch (e: Exception) {
            null
        }
        loading = false
    }

    val leagueId = player?.currentSeason?.leagueId

    LaunchedEffect(leagueId) {
        leagueStats = if (leagueId.isNullOrBlank()) {
            null
        } else {
            try {
                teamRepository.fetchLeagueStats(leagueId)
            } catch (e: Exception) {
                null
            }
        }
    }

    val ownLeagueStat = leagueStats?.players?.firstOrNull { it.playerId == playerId }
    val metrics = remember(ownLeagueStat, leagueStats) {
        if (ownLeagueStat != null && leagueStats != null) {
            PlayerBenchmarks.metrics(ownLeagueStat, leagueStats?.players.orEmpty())
        } else {
            emptyList()
        }
    }

    val similarPlayers = remember(ownLeagueStat, leagueStats) {
        if (ownLeagueStat != null && leagueStats != null) {
            PlayerBenchmarks.similar(ownLeagueStat, leagueStats?.players.orEmpty(), 5)
        } else {
            emptyList()
        }
    }

    Scaffold(
        containerColor = colors.screenBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.topBar)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "←",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(end = 14.dp)
                        .clickable(onClick = onBack)
                )
                Text(
                    "Játékosprofil",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (following) "★" else "☆",
                    color = if (following) Color(0xFFFBBF24) else Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier.clickable {
                        following = followStore.toggle(playerId)
                    }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val p = player

            item {
                PlayerHero(
                    player = p,
                    fallbackName = playerName,
                    playerId = playerId,
                    onTeamClick = onTeamClick
                )
            }

            if (loading) {
                item { PlainCard(colors) { LoadingRow(colors) } }
            } else if (p == null) {
                item {
                    PlainCard(colors) {
                        Text(
                            "A játékos profiladatai jelenleg nem érhetők el.",
                            color = colors.secondaryText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                item {
                    ProfileTabs(
                        titles = listOf("Áttekintés", "Statisztika", "Karrier"),
                        selected = tab,
                        onSelect = { tab = it },
                        colors = colors
                    )
                }

                when (tab) {
                    0 -> overviewItems(p, metrics, ownLeagueStat, leagueStats != null, similarPlayers, onPlayerClick, colors)
                    1 -> statisticsItems(p, colors)
                    else -> careerItems(p, colors, onTeamClick)
                }
            }
        }
    }
}

// ================================================================
// FEJLÉC
// ================================================================

@Composable
private fun PlayerHero(
    player: PlayerFull?,
    fallbackName: String,
    playerId: String,
    onTeamClick: (String, String) -> Unit
) {
    val name = player?.name ?: fallbackName
    val position = rememberHu(player?.position)
    val nationality = rememberHu(player?.nationality)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0B1B3B), Color(0xFF1D4ED8)))
            )
            .padding(horizontal = 16.dp, vertical = 22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RemoteImage(
                url = StatpalImages.player(playerId),
                contentDescription = "$name fotója",
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                fallback = {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            initials(name),
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            Text(
                name,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            val teamName = player?.team
            val teamId = player?.teamId
            if (!teamName.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    if (teamId != null) "$teamName ›" else teamName,
                    color = Color(0xFFBFDBFE),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = if (teamId != null) {
                        Modifier.clickable { onTeamClick(teamId, teamName) }
                    } else {
                        Modifier
                    }
                )
            }

            val chips = listOfNotNull(
                position?.takeIf { it.isNotBlank() },
                player?.age?.takeIf { it.isNotBlank() }?.let { "$it év" },
                nationality?.takeIf { it.isNotBlank() }
            )
            if (chips.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.forEach { chip ->
                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0x33FFFFFF)) {
                            Text(
                                chip,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            player?.marketValueEur?.let { value ->
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFBBF24)) {
                    Text(
                        "Piaci érték: ${formatEuro(value)}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = Color(0xFF422006),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileTabs(
    titles: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    colors: AppColorScheme
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.cardBackground
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            titles.forEachIndexed { index, title ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        title,
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = if (selected == index) Blue else colors.secondaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(if (selected == index) Blue else Color.Transparent)
                    )
                }
            }
        }
    }
}

// ================================================================
// ÁTTEKINTÉS
// ================================================================

private fun LazyListScope.overviewItems(
    player: PlayerFull,
    metrics: List<PlayerBenchmarks.Metric>,
    ownLeagueStat: LeaguePlayerStat?,
    leagueLoaded: Boolean,
    similar: List<LeaguePlayerStat>,
    onPlayerClick: (String, String) -> Unit,
    colors: AppColorScheme
) {
    val season = player.currentSeason

    item {
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                season?.let { "${it.title} • ${it.subtitle}" } ?: "Aktuális szezon",
                color = colors.tertiaryText,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Mérkőzés", season?.int("appearances", "appearences")?.toString() ?: "–", colors, Modifier.weight(1f))
                StatTile("Gól", season?.int("goals")?.toString() ?: "–", colors, Modifier.weight(1f))
                StatTile("Gólpassz", season?.int("assists")?.toString() ?: "–", colors, Modifier.weight(1f))
                StatTile(
                    "Értékelés",
                    season?.double("rating")?.let { String.format(Locale.US, "%.2f", it) } ?: "–",
                    colors,
                    Modifier.weight(1f),
                    valueColor = season?.double("rating")?.let { ratingColor(it) }
                )
            }
        }
    }

    // Radar a bajnokság azonos posztú játékosaihoz képest.
    item {
        PlainCard(colors, horizontalPadding = 12) {
            Text(
                "Teljesítmény a bajnokságban",
                color = colors.primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
            Text(
                "Az azonos posztú, legalább 270 percet játszó játékosokhoz képest (százalékos helyezés).",
                color = colors.tertiaryText,
                fontSize = 10.sp
            )

            if (metrics.size >= 3) {
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    RadarChart(
                        labels = metrics.map { "${it.label}\n${it.percentile}." },
                        values = metrics.map { it.percentile / 100f },
                        accent = roleColor(PlayerBenchmarks.roleOf(player.position)),
                        gridColor = colors.divider,
                        labelColor = colors.secondaryText
                    )
                }
                Spacer(Modifier.height(4.dp))
                metrics.forEach { metric ->
                    PercentileRow(metric, colors)
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (leagueLoaded && ownLeagueStat == null) {
                        "A játékos nem szerepel a bajnoki statisztikában."
                    } else if (leagueLoaded) {
                        "Még kevés a játszott perc vagy az összehasonlítható adat a diagramhoz."
                    } else {
                        "A bajnoki összehasonlítás betöltése…"
                    },
                    color = colors.secondaryText,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (similar.isNotEmpty()) {
        item {
            PlainCard(colors, horizontalPadding = 12) {
                Text(
                    "Hasonló játékosok a bajnokságban",
                    color = colors.primaryText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Text(
                    "Azonos poszt, hasonló mutatók (gól, gólpassz, védekezés, passzok).",
                    color = colors.tertiaryText,
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(6.dp))
                similar.forEach { other ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                other.playerId?.let { id -> onPlayerClick(id, other.name) }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${other.name} ›",
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                "${other.teamName} • ${other.appearances} mérk. • ${other.goals} gól • ${other.assists} assz.",
                                color = colors.tertiaryText,
                                fontSize = 10.sp
                            )
                        }
                        other.rating?.let {
                            Text(
                                String.format(Locale.US, "%.2f", it),
                                color = ratingColor(it),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    item {
        PlainCard(colors, horizontalPadding = 12) {
            Text("Adatlap", color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            InfoLine("Teljes név", listOfNotNull(player.firstName, player.lastName).joinToString(" ").takeIf { it.isNotBlank() }, colors)
            InfoLine("Születési dátum", player.birthdate, colors)
            InfoLine("Születési hely", player.birthplace, colors)
            InfoLine("Nemzetiség", rememberHu(player.nationality), colors)
            InfoLine("Magasság", player.height?.let { "$it cm" }, colors)
            InfoLine("Testsúly", player.weight?.let { "$it kg" }, colors)
            InfoLine("Erősebb láb", footHu(player.preferredFoot), colors)
            InfoLine("Piaci érték", player.marketValueEur?.let { formatEuro(it) }, colors)
        }
    }
}

@Composable
private fun PercentileRow(metric: PlayerBenchmarks.Metric, colors: AppColorScheme) {
    val color = percentileColor(metric.percentile)

    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                metric.label,
                modifier = Modifier.weight(1f),
                color = colors.primaryText,
                fontSize = 12.sp
            )
            Text(
                metric.display,
                color = colors.secondaryText,
                fontSize = 11.sp,
                modifier = Modifier.width(54.dp),
                textAlign = TextAlign.End
            )
            Surface(shape = RoundedCornerShape(8.dp), color = color, modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    "${metric.percentile}.",
                    modifier = Modifier
                        .width(34.dp)
                        .padding(vertical = 2.dp),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        FractionBar(metric.percentile / 100f, color, colors.divider)
    }
}

// ================================================================
// STATISZTIKA
// ================================================================

private fun LazyListScope.statisticsItems(player: PlayerFull, colors: AppColorScheme) {
    val season = player.currentSeason

    if (season == null) {
        item {
            PlainCard(colors, horizontalPadding = 12) {
                Text(
                    "Ehhez a játékoshoz nincs szezonstatisztika.",
                    color = colors.secondaryText,
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    fun v(vararg keys: String): String? = season.text(*keys)

    fun pct(part: Int?, total: Int?): String? =
        if (part != null && total != null && total > 0 && part <= total) "${part * 100 / total}%" else null

    fun ratio(part: String?, total: String?): String? {
        val a = part?.toIntOrNull()
        val b = total?.toIntOrNull()
        return when {
            a == null && b == null -> null
            b == null -> part
            a == null -> null
            else -> "$a / $b" + (pct(a, b)?.let { " ($it)" } ?: "")
        }
    }

    val minutes = season.int("minutes_played") ?: 0
    fun per90(key: String): String? {
        val count = season.int(key) ?: return null
        if (minutes < 90) return null
        return String.format(Locale.US, "%.2f", count * 90.0 / minutes)
    }

    item {
        StatGroup(
            "Játékidő", colors,
            listOf(
                "Mérkőzés" to v("appearances", "appearences"),
                "Kezdőként" to v("starting_lineups", "lineups"),
                "Csereként beállt" to v("substitute_in"),
                "Lecserélték" to v("substitute_out"),
                "Padon" to v("on_bench", "substitutes_on_bench"),
                "Percek" to v("minutes_played")
            )
        )
    }
    item {
        StatGroup(
            "Támadás", colors,
            listOf(
                "Gól" to v("goals"),
                "Gól / 90 perc" to per90("goals"),
                "Gólpassz" to v("assists"),
                "Gólpassz / 90 perc" to per90("assists"),
                "Lövés (kapura)" to ratio(v("shots_on_target", "shots_on"), v("shots_total"))
                    ?.let { "${v("shots_on_target", "shots_on") ?: "–"} / ${v("shots_total") ?: "–"}" },
                "Kapura lövés aránya" to pct(season.int("shots_on_target", "shots_on"), season.int("shots_total")),
                "Kapufa" to v("shots_woodwork"),
                "Tizenegyes (gól / kihagyott)" to ratio(v("pen_scored", "penalties_scored"), v("pen_missed", "penalties_missed"))
            )
        )
    }
    item {
        StatGroup(
            "Passzok és játékszervezés", colors,
            listOf(
                "Kulcspassz" to v("key_passes"),
                "Kulcspassz / 90 perc" to per90("key_passes"),
                "Passzkísérlet" to v("pass_attempts"),
                "Beadás (pontos / összes)" to ratio(v("crosses_accurate"), v("crosses_total"))
            )
        )
    }
    item {
        StatGroup(
            "Védekezés", colors,
            listOf(
                "Szerelés" to v("tackles"),
                "Szerelés / 90 perc" to per90("tackles"),
                "Labdaszerzés" to v("interceptions"),
                "Felszabadítás" to v("clearances"),
                "Blokk" to v("blocks")
            )
        )
    }
    item {
        StatGroup(
            "Párharcok és cselek", colors,
            listOf(
                "Párharc (nyert / összes)" to ratio(v("duels_won"), v("duels_total")),
                "Csel (sikeres / kísérlet)" to ratio(v("dribble_success"), v("dribble_attempts")),
                "Labdavesztés" to v("dispossesed", "dispossessed"),
                "Szabálytalanság (elkövetett / kiharcolt)" to ratio(v("fouls_committed"), v("fouls_drawn"))
            )
        )
    }
    item {
        StatGroup(
            "Kapusstatisztika", colors,
            listOf(
                "Védés" to v("saves"),
                "Kapott gól" to v("goals_conceded").takeIf { v("saves") != null },
                "Tizenegyes-védés" to v("pen_saved", "penalties_saved"),
                "Tizenhatoson belüli védés" to v("inside_box_saves")
            )
        )
    }
    item {
        StatGroup(
            "Fegyelem", colors,
            listOf(
                "Sárga lap" to v("yellowcards"),
                "Sárga-piros lap" to v("yellowred"),
                "Piros lap" to v("redcards")
            )
        )
    }

    if (player.cups.isNotEmpty()) {
        item {
            PlainCard(colors, horizontalPadding = 12) {
                Text("Kupák", color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                player.cups.forEach { SeasonLine(it, colors) }
            }
        }
    }
}

// ================================================================
// KARRIER
// ================================================================

private fun LazyListScope.careerItems(
    player: PlayerFull,
    colors: AppColorScheme,
    onTeamClick: (String, String) -> Unit
) {
    player.overall?.let { overall ->
        item {
            StatGroup(
                "Klubkarrier összesen", colors,
                listOf(
                    "Mérkőzés" to overall.text("appearances", "appearences"),
                    "Kezdőként" to overall.text("starting_lineups"),
                    "Percek" to overall.text("minutes_played"),
                    "Gólpassz" to overall.text("assists"),
                    "Lövés (összes / kapura)" to overall.text("shots_total")?.let {
                        "$it / ${overall.text("shots_on_target", "shots_on") ?: "–"}"
                    },
                    "Tizenegyes-gól" to overall.text("pen_scored", "penalties_scored"),
                    "Átlagos értékelés" to overall.double("rating")?.let { String.format(Locale.US, "%.2f", it) },
                    "Sárga lap" to overall.text("yellowcards"),
                    "Piros lap" to overall.text("redcards")
                )
            )
        }
    }

    if (player.seasons.isNotEmpty()) {
        item {
            PlainCard(colors, horizontalPadding = 12) {
                Text("Bajnoki szezonok", color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                player.seasons.forEach { SeasonLine(it, colors) }
            }
        }
    }

    if (player.national.isNotEmpty()) {
        item {
            PlainCard(colors, horizontalPadding = 12) {
                Text("Válogatott", color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                player.national.forEach { SeasonLine(it, colors) }
            }
        }
    }

    if (player.transfers.isNotEmpty()) {
        item {
            PlainCard(colors, horizontalPadding = 12) {
                Text("Átigazolások", color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                player.transfers.forEach { transfer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                listOfNotNull(transfer.from, transfer.to).joinToString(" → "),
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            val detail = listOfNotNull(rememberHu(transfer.type), transfer.price?.takeIf { it.isNotBlank() })
                                .joinToString(" • ")
                            if (detail.isNotBlank()) {
                                Text(detail, color = colors.tertiaryText, fontSize = 10.sp)
                            }
                        }
                        Text(transfer.date, color = colors.secondaryText, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (player.trophies.isNotEmpty()) {
        item {
            PlainCard(colors, horizontalPadding = 12) {
                Text("Trófeák", color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                player.trophies.forEach { trophy ->
                    Column(modifier = Modifier.padding(vertical = 5.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "🏆 ${trophy.league}",
                                modifier = Modifier.weight(1f),
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                listOfNotNull(rememberHu(trophy.status), trophy.count?.let { "×$it" }).joinToString(" "),
                                color = colors.secondaryText,
                                fontSize = 11.sp
                            )
                        }
                        if (!trophy.seasons.isNullOrBlank()) {
                            Text(trophy.seasons, color = colors.tertiaryText, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    if (player.sidelined.isNotEmpty()) {
        item {
            PlainCard(colors, horizontalPadding = 12) {
                Text("Sérülések és kihagyások", color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                player.sidelined.take(15).forEach {
                    Text("• $it", color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }

    if (player.overall == null && player.seasons.isEmpty() && player.transfers.isEmpty() && player.trophies.isEmpty()) {
        item {
            PlainCard(colors, horizontalPadding = 12) {
                Text("Ehhez a játékoshoz nincs karrieradat.", color = colors.secondaryText, fontSize = 12.sp)
            }
        }
    }
}

// ================================================================
// KISEBB ELEMEK
// ================================================================

@Composable
private fun SeasonLine(row: PlayerStatRow, colors: AppColorScheme) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(row.title, color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        if (row.subtitle.isNotBlank()) {
            Text(row.subtitle, color = colors.tertiaryText, fontSize = 10.sp)
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MiniStat("Mérk.", row.int("appearances", "appearences"), colors)
            MiniStat("Gól", row.int("goals"), colors)
            MiniStat("Assz.", row.int("assists"), colors)
            MiniStat("Perc", row.int("minutes_played"), colors)
            row.double("rating")?.let {
                MiniStat("Ért.", null, colors, String.format(Locale.US, "%.2f", it))
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: Int?, colors: AppColorScheme, text: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text ?: value?.toString() ?: "–",
            color = colors.primaryText,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp
        )
        Text(label, color = colors.tertiaryText, fontSize = 9.sp)
    }
}

@Composable
private fun StatGroup(title: String, colors: AppColorScheme, rows: List<Pair<String, String?>>) {
    val visible = rows.filter { !it.second.isNullOrBlank() }
    if (visible.isEmpty()) return

    PlainCard(colors, horizontalPadding = 12) {
        Text(title, color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        Spacer(Modifier.height(6.dp))
        visible.forEach { (label, value) ->
            InfoLine(label, value, colors)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String?, colors: AppColorScheme) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = colors.primaryText,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    colors: AppColorScheme,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = colors.cardBackground) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                color = valueColor ?: colors.primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
            Text(label, color = colors.tertiaryText, fontSize = 10.sp)
        }
    }
}

@Composable
private fun PlainCard(
    colors: AppColorScheme,
    horizontalPadding: Int = 12,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding.dp),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            content()
        }
    }
}

@Composable
private fun LoadingRow(colors: AppColorScheme) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        Spacer(Modifier.height(8.dp))
        Text("Játékosadatok betöltése…", color = colors.secondaryText, fontSize = 12.sp)
    }
}

private fun initials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

private fun ratingColor(rating: Double): Color =
    when {
        rating >= 7.5 -> Good
        rating >= 6.8 -> Warn
        else -> Bad
    }

private fun percentileColor(percentile: Int): Color =
    when {
        percentile >= 75 -> Good
        percentile >= 50 -> Blue
        percentile >= 25 -> Warn
        else -> Bad
    }

private fun roleColor(role: PlayerBenchmarks.Role): Color =
    when (role) {
        PlayerBenchmarks.Role.GOALKEEPER -> Warn
        PlayerBenchmarks.Role.DEFENDER -> Blue
        PlayerBenchmarks.Role.MIDFIELDER -> Good
        PlayerBenchmarks.Role.FORWARD -> Bad
    }

private fun footHu(foot: String?): String? =
    when (foot?.trim()?.lowercase(Locale.ROOT)) {
        "left" -> "Bal"
        "right" -> "Jobb"
        "both" -> "Mindkettő"
        else -> foot
    }

private fun formatEuro(value: Long): String =
    when {
        value >= 1_000_000_000L -> String.format(Locale.US, "€%.2f Mrd", value / 1_000_000_000.0)
        value >= 10_000_000L -> String.format(Locale.US, "€%.0f M", value / 1_000_000.0)
        value >= 1_000_000L -> String.format(Locale.US, "€%.1f M", value / 1_000_000.0)
        value >= 1_000L -> String.format(Locale.US, "€%.0f E", value / 1_000.0)
        else -> "€$value"
    }
