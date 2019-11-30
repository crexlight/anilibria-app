package ru.radiationx.anilibria.model.data.holders

import io.reactivex.Observable
import ru.radiationx.anilibria.entity.app.other.LinkMenuItem
import ru.radiationx.anilibria.entity.app.release.GenreItem
import ru.radiationx.anilibria.ui.adapters.MenuListItem

interface MenuHolder {
    fun observe(): Observable<List<LinkMenuItem>>
    fun save(items: List<LinkMenuItem>)
    fun get(): List<LinkMenuItem>
}