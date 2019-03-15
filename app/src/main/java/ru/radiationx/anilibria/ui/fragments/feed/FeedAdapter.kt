package ru.radiationx.anilibria.ui.fragments.feed

import android.os.Handler
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import ru.radiationx.anilibria.entity.app.feed.FeedItem
import ru.radiationx.anilibria.entity.app.feed.FeedScheduleItem
import ru.radiationx.anilibria.entity.app.release.ReleaseItem
import ru.radiationx.anilibria.entity.app.youtube.YoutubeItem
import ru.radiationx.anilibria.ui.adapters.*
import ru.radiationx.anilibria.ui.adapters.feed.FeedReleaseDelegate
import ru.radiationx.anilibria.ui.adapters.feed.FeedSchedulesDelegate
import ru.radiationx.anilibria.ui.adapters.feed.FeedSectionDelegate
import ru.radiationx.anilibria.ui.adapters.feed.FeedYoutubeDelegate
import ru.radiationx.anilibria.ui.adapters.global.LoadMoreDelegate
import ru.radiationx.anilibria.ui.adapters.other.DividerShadowItemDelegate
import ru.radiationx.anilibria.ui.common.adapters.OptimizeAdapter

/* Created by radiationx on 31.10.17. */

class FeedAdapter(
        val loadMoreListener: () -> Unit,
        schedulesClickListener: () -> Unit,
        releaseClickListener: (ReleaseItem, View) -> Unit,
        releaseLongClickListener: (ReleaseItem, View) -> Unit,
        youtubeClickListener: (YoutubeItem, View) -> Unit,
        scheduleClickListener: (FeedScheduleItem, View) -> Unit
) : OptimizeAdapter<MutableList<ListItem>>() {

    private val scheduleSection = FeedSectionListItem("Ожидаются сегодня", "Расписание")
    private val feedSection = FeedSectionListItem("Обновления", hasBg = true)

    private val sectionClickListener = { item: FeedSectionListItem ->
        if (item == scheduleSection) {
            schedulesClickListener.invoke()
        }
    }

    init {
        items = mutableListOf()
        addDelegate(LoadMoreDelegate(object : LoadMoreDelegate.Listener {
            override fun onLoadMore() {}
        }))
        addDelegate(FeedSectionDelegate(sectionClickListener))
        addDelegate(FeedSchedulesDelegate(scheduleClickListener))
        addDelegate(FeedReleaseDelegate(releaseClickListener, releaseLongClickListener))
        addDelegate(FeedYoutubeDelegate(youtubeClickListener))
        addDelegate(DividerShadowItemDelegate())
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any?>) {

        val time = System.currentTimeMillis()
        super.onBindViewHolder(holder, position, payloads)

        Log.d("nonono", "onBindViewHolder pos=$position type=${getItemViewType(position)} time=${System.currentTimeMillis() - time}")
        val threshold = (items.lastIndex - position)
        if (threshold <= 3) {
            Handler().post {
                loadMoreListener.invoke()
            }
        }
    }

    fun bindSchedules(newItems: List<FeedScheduleItem>) {
        val index = items.indexOf(scheduleSection)


        Log.d("kokoko", "bindSchedules before ${items.joinToString { it.javaClass.simpleName }}")

        if (newItems.isEmpty()) {
            if (index != -1) {
                items.removeAt(index)
                items.removeAt(index)
                notifyItemRangeRemoved(index, 2)
            }
        } else {
            if (index == -1) {
                items.addAll(0, listOf(
                        scheduleSection,
                        FeedSchedulesListItem(newItems)
                ))
                notifyDataSetChanged()
                /*notifyItemInserted(0)
                notifyItemInserted(1)*/
                //notifyItemRangeInserted(0, 2)
            } else {
                items[index + 1] = FeedSchedulesListItem(newItems)
                notifyItemChanged(index + 1)
            }
        }

        Log.d("kokoko", "bindSchedules after ${items.joinToString { it.javaClass.simpleName }}")
    }

    fun bindItems(newItems: List<FeedItem>) {

        val progress = isProgress()

        var startIndex = items.indexOf(feedSection)

        Log.e("ninini", "beforekek  $startIndex ")

        if (startIndex == -1) {
            startIndex = items.indexOfLast { it is FeedSchedulesListItem }
            //items.add(DividerShadowListItem())
            items.add(feedSection)
            notifyItemRangeInserted(startIndex, 1)
        }
        startIndex = items.indexOf(feedSection) + 1

        Log.e("ninini", "afterkek  $startIndex = ${items.joinToString()}")

        val currentFeedItems = (startIndex until itemCount)
                .filter { index ->
                    Log.e("ninini", "current index = $index => ${items[index]}")
                    val listItem = items[index]
                    listItem is FeedListItem || listItem is DividerShadowListItem
                }
                .map { items[it] }
        val newListItems = mutableListOf<ListItem>()

        var lastFeedItem: FeedListItem? = null
        newItems.forEach { feedItem ->
            lastFeedItem?.also {
                if (it.item.release == null && feedItem.release != null
                        || it.item.youtube == null && feedItem.youtube != null) {
                    newListItems.add(DividerShadowListItem())
                }
            }
            newListItems.add(FeedListItem(feedItem).also {
                lastFeedItem = it
            })
        }

        currentFeedItems.forEach { items.remove(it) }
        items.addAll(startIndex, newListItems)
        //notifyDataSetChanged()

        when {
            currentFeedItems.size == newListItems.size -> {
                notifyItemRangeChanged(startIndex, currentFeedItems.size, FeedUpdateDataPayload)
            }
            currentFeedItems.size < newListItems.size -> {
                val insertIndex = startIndex + currentFeedItems.size
                notifyItemRangeChanged(startIndex, currentFeedItems.size, FeedUpdateDataPayload)
                notifyItemRangeInserted(insertIndex, newListItems.size - currentFeedItems.size)
            }
            currentFeedItems.size > newListItems.size -> {
                val removeIndex = startIndex + newListItems.size
                notifyItemRangeChanged(startIndex, newListItems.size, FeedUpdateDataPayload)
                notifyItemRangeRemoved(removeIndex, currentFeedItems.size - newListItems.size)
            }
        }


        val loadMoreIndex = items.indexOfLast { it is LoadMoreListItem }
        if (loadMoreIndex != -1) {
            items.removeAt(loadMoreIndex)
            notifyItemRemoved(loadMoreIndex)
        }

        if (progress) {
            items.add(LoadMoreListItem())
            notifyItemInserted(items.size)
        }
    }

    fun showProgress(isVisible: Boolean) {
        val progress = isProgress()

        if (isVisible && !progress) {
            items.add(LoadMoreListItem())
            notifyItemInserted(items.lastIndex)
        } else if (!isVisible && progress) {
            items.remove(items.last())
            notifyItemRemoved(items.size)
        }
    }

    private fun isProgress() = items.isNotEmpty() && items.last() is LoadMoreListItem

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val time = System.currentTimeMillis()
        return super.onCreateViewHolder(parent, viewType).also {
            Log.d("nonono", "onCreateViewHolder  type=${viewType} time=${System.currentTimeMillis() - time}")
        }
    }

}
