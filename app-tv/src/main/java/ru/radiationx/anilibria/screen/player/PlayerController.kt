package ru.radiationx.anilibria.screen.player

import kotlinx.coroutines.flow.MutableSharedFlow
import ru.radiationx.data.entity.domain.types.EpisodeId
import ru.radiationx.shared.ktx.EventFlow
import toothpick.InjectConstructor

@InjectConstructor
class PlayerController {

    val selectEpisodeRelay = EventFlow<EpisodeId>()
}