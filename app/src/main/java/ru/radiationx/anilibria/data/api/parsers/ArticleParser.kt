package ru.radiationx.anilibria.data.api.parsers

import android.text.Html
import android.util.Log
import ru.radiationx.anilibria.data.api.Api
import ru.radiationx.anilibria.data.api.models.Paginated
import ru.radiationx.anilibria.data.api.models.article.ArticleFull
import ru.radiationx.anilibria.data.api.models.article.ArticleItem
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by radiationx on 18.12.17.
 */
object ArticleParser {

    /*
    * 1.    Int     Какой-то айдишник битриксовский
    * 2.    String  Относительная ссылка на статью
    * 3.    String  Заголовок статьи
    * 4.    Int     Id юзера
    * 5.    String  Ник юзера
    * 6.    String  Относительная ссылка на изображение
    * 7.    Int     Ширина изображения
    * 8.    Int     Высота изображения
    * 9.    String^ Текстовый контент
    * 10.   String? Ссылка на "ВСЕ ВЫПУСКИ"
    * 11.   Int     Просмотры
    * 12.   Int     Комментарии
    * */
    private val listPatternSource = "<div[^>]*?class=\"[^\"]*?news_block[^\"]*?\"[^>]*?id=\"bx_\\d+_(\\d+)\"[^>]*?>[\\s\\S]*?<h1[^>]*?class=\"[^\"]*?news-name[^\"]*?\"[^>]*?>[\\s\\S]*?<a[^>]*?href=\"([^\"]*?)\"[^>]*?>([\\s\\S]*?)<\\/a>[\\s\\S]*?<\\/h1>[\\s\\S]*?<span[^>]*?class=\"published\"[^>]*?>[\\s\\S]*?<a[^>]*?href=\"\\/user\\/(\\d+)\\/\"[^>]*?>([\\s\\S]*?)<\\/a>[\\s\\S]*?<\\/span>[\\s\\S]*?<div[^>]*?class=\"[^\"]*?news-content[^\"]*?\"[^>]*?>[\\s\\S]*?<a[^>]*>[^<]*?<img[^>]*?src=\"([^\"]*?)\"[^>]*?width=\"(\\d+)\"[^>]*?height=\"(\\d+)\"[^>]*?>[^<]*?<\\/a>[^<]*?<span[^>]*?class=\"news-preview-text\"[^>]*?>([\\s\\S]*?)<\\/span>[^<]*?<div[^>]*?class=\"block_fix\"[^>]*>[^<]*?<\\/div>[\\s\\S]*?<div[^>]*?class=\"news_footer\"[^>]*?>[^<]*?(?:<a[^>]*?>[\\s\\S]*?<\\/a>[^<]*?)?(?:<a[^>]*?href=\"([^\"]*?)\"[^>]*?>[\\s\\S]*?<\\/a>)?[^<]*?<span[^>]*?>[^<]*?(\\d+)[^<]*?<\\/span>[^<]*?<span[^>]*?>[^<]*?(\\d+)[^<]*?<\\/span>"

    /*
    * 1.    Int     Текущая страница
    * 2.    Int     Последняя страница (всего)
    * */
    private val paginationPatternSource = "<div[^>]*?class=\"[^\"]*?bx_pagination_page[^\"]*?\"[^>]*?>[\\s\\S]*?<li[^>]*?class=\"bx_active\"[^>]*?>(\\d+)<\\/li>[\\s\\S]*?<li><a[^>]*?>(\\d+)<\\/a><\\/li>[^<]*?<li><a[^>]*?>&#8594;<\\/a>"

    private val fullArticlePatternSource = "<div[^>]*?class=\"[^\"]*?news-detail-header[^\"]*?\"[^>]*?>[^<]*?<h1[^>]*?>([\\s\\S]*?)<\\/h1>[^<]*?<\\/div>[\\s\\S]*?<div[^>]*?class=\"[^\"]*?news-detail-content[^\"]*?\"[^>]*?>([\\s\\S]*?)(?:<a[^>]*?id=\"back-to-list\"[^>]*?>[\\s\\S]*?<\\/a>[^<]*?)?<\\/div>[^<]*?<div[^>]*?class=\"[^\"]*?news-detail-footer[^\"]*?\"[^>]*?>[^<]*?<span[^>]*?>[\\s\\S]*?<a[^>]*?href=\"[^\"]*?(\\d+)[^\"]*?\"[^>]*?>([\\s\\S]*?)<\\/a>[\\s\\S]*?<\\/span>[\\s\\S]*?<span[^>]*?>[^<]*?<i[^>]*?>([\\s\\S]*?)<\\/i>[\\s\\S]*?<\\/span>"

    private val youtubeLink = "(?:http(?:s?):)?\\/\\/(?:www\\.)?youtu(?:be\\.com\\/watch\\?v=|\\.be\\/|be.com\\/embed\\/)([\\w\\-\\_]*)(&(amp;)[\\w\\=]*)?"
    private val iframeYT = "<iframe[^>]*?src=\"(?:http(?:s?):)?\\/\\/(?:www\\.)?youtu(?:be\\.com\\/watch\\?v=|\\.be\\/|be.com\\/embed\\/)([\\w\\-\\_]*)(&(amp;)[\\w\\=]*)?[^\"]*?\"[^>]*?>[\\s\\S]*?<\\/iframe>"
    private val iframeVK = "<iframe[^>]*?src=\"(?:http(?:s?):)?\\/\\/(?:www\\.)?vk\\.com\\/video_ext\\.php\\?oid=([^&\"]*?)&id=([^&\"]*?)(&hash[^\"]*?)?\"[^>]*?>[\\s\\S]*?<\\/iframe>"
    private val alibBordLine = "<img[^>]*?src=\"[^\"]*?borderline\\.[^\"]*?\"[^>]*?>"

    private val listPattern: Pattern by lazy {
        Pattern.compile(listPatternSource, Pattern.CASE_INSENSITIVE)
    }

    private val paginationPattern: Pattern by lazy {
        Pattern.compile(paginationPatternSource, Pattern.CASE_INSENSITIVE)
    }

    private val fullPattern: Pattern by lazy {
        Pattern.compile(fullArticlePatternSource, Pattern.CASE_INSENSITIVE)
    }

    fun articles(httpResponse: String): Paginated<List<ArticleItem>> {
        val items = mutableListOf<ArticleItem>()
        val matcher: Matcher = listPattern.matcher(httpResponse)
        while (matcher.find()) {
            items.add(ArticleItem().apply {
                elementId = matcher.group(1).toInt()
                url = matcher.group(2)
                title = Html.fromHtml(matcher.group(3)).toString()
                userId = matcher.group(4).toInt()
                userNick = Html.fromHtml(matcher.group(5)).toString()
                imageUrl = Api.BASE_URL_IMAGES + matcher.group(6)
                imageWidth = matcher.group(7).toInt()
                imageHeight = matcher.group(8).toInt()
                content = matcher.group(9).trim()
                otherUrl = Api.BASE_URL + matcher.group(10)
                viewsCount = matcher.group(11).toInt()
                commentsCount = matcher.group(12).toInt()
            })
        }
        val result = Paginated(items)

        val paginationMatcher = paginationPattern.matcher(httpResponse)
        if (paginationMatcher.find()) {
            result.current = paginationMatcher.group(1).toInt()
            result.allPages = paginationMatcher.group(2).toInt()
            result.itemsPerPage = 6
        }

        return result
    }

    fun article(httpResponse: String): ArticleFull {
        val result = ArticleFull()
        val matcher: Matcher = fullPattern.matcher(httpResponse)
        if (matcher.find()) {
            result.apply {
                title = Html.fromHtml(matcher.group(1)).toString()
                content = matcher.group(2).trim()
                userId = matcher.group(3).toInt()
                userNick = Html.fromHtml(matcher.group(4)).toString()
                date = matcher.group(5)
            }
        }
        result.content = result.content.replace(Regex(iframeYT), "<div class=\"alib_button yt\"><a href=\"https://youtu.be/$1\">Смотреть на YouTube</a></div>")
        result.content = result.content.replace(Regex(iframeVK), "<div class=\"alib_button vk\"><a href=\"https://vk.com/video?z=video$1_$2$3\">Смотреть в VK</a></div>")
        result.content = result.content.replace(Regex(alibBordLine), "<div class=\"alib_borderline\">$0</div>")
        Log.e("SUKA", "PARSED :" + result.title)
        return result
    }
}