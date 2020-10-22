package com.github.rwsbillyang.wxSDK.officialAccount

import com.github.rwsbillyang.wxSDK.common.IBase
import com.github.rwsbillyang.wxSDK.common.Response
import com.github.rwsbillyang.wxSDK.common.apiJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * 永久素材管理
 * */
object MaterialApi: OABaseApi() {
    override val group: String = "material"

    /**
     * 新增永久图文素材
     *
     * */
    fun addNews(list: List<Article>):ResponseAddMaterial = doPost2("add_news",list)

    /**
     * 新增其他类型永久素材
     * 类似于MeidaApi中的uploadTmp上传临时素材，此处为永久素材
     *
     * 媒体文件类型，分别有图片（image）、语音（voice）和缩略图（thumb）
     * */
    fun addMedia(file: String, type: MediaType):ResponseAddMaterial = doUpload2("add_material",file, mapOf("type" to type.value))

    /**
     * @param title	是	视频素材的标题
     * @param introduction	是	视频素材的描述
     * */
    fun addVideo(file: String, title: String, introduction: String):ResponseAddMaterial = doUpload2("add_material",file,null,
            mapOf("description" to apiJson.encodeToString(VideoDescription(title, introduction))))


    /**
     * 删除不再需要的永久素材，节省空间
     * */
    fun del(mediaId: String): Response = doPost2("del_material", mapOf("media_id" to mediaId))

    fun updateArticle(mediaId: String, index: Int, article: Article): Response = doPost2("update_news", mapOf("media_id" to mediaId, "index" to index, "articles" to article))


    /**
     * 1.永久素材的总数，也会计算公众平台官网素材管理中的素材
     * 2.图片和图文消息素材（包括单图文和多图文）的总数上限为5000，其他素材的总数上限为1000
     * */
    fun getCount():ResponseMaterialCount = doGet2("get_materialcount")


    fun getNews(mediaId: String): ResponseNews = doPost2("get_material", mapOf("media_id" to mediaId))

    fun getVideo(mediaId: String):ResponseDownVideoMaterial = doPost2("get_material", mapOf("media_id" to mediaId))

    /**
     * 返回一个网址自行在浏览器中下载
     * FIXME: 需要使用post方法？
     * */
    fun getMaterialUrl(mediaId: String) = url("get_material",mapOf("media_id" to mediaId))

    /**
     * @param type	是	素材的类型，图片（image）、视频（video）、语音 （voice）、图文（news）
     * @param offset	是	从全部素材的该偏移位置开始返回，0表示从第一个素材 返回
     * @param count	是	返回素材的数量，取值在1到20之间
     * */
    fun getNewsList(offset: Int, count: Int):ResponseNewsList = doPost2("batchget_material",
            mapOf("type" to "news", "offset" to offset.toString(), "count" to count.toString()))

    /**
     * 除了图文（news）之外的：图片（image）、视频（video）、语音 （voice）
     * */
    fun getMediaList(type: MediaType, offset: Int, count: Int):ResponseMediaList = doPost2("batchget_material",
            mapOf("type" to type.value, "offset" to offset.toString(), "count" to count.toString()))
}




@Serializable
class VideoDescription(
        val title: String,
        val introduction: String
)


@Serializable
class ResponseDownVideoMaterial(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,

        val title: String? = null,
        val description: String? = null,
        @SerialName("down_url")
        val videoUrl: String? = null
):IBase

/**
 * @param mediaId media_id	新增的永久素材的media_id
 * @param url	新增的图片素材的图片URL（仅新增图片素材时会返回该字段）
 * */
@Serializable
class ResponseAddMaterial(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        @SerialName("media_id")
        val mediaId: String? = null,
        val url: String? = null
): IBase


/**
 * @param voiceCount voice_count	语音总数量
 * @param videoCount video_count	视频总数量
 * @param imageCount image_count	图片总数量
 * @param articleCount news_count	图文总数量
 * */
@Serializable
class ResponseMaterialCount(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,

        @SerialName("voice_count")
        val voiceCount: Int? = null,
        @SerialName("video_count")
        val videoCount: Int? = null,
        @SerialName("image_count")
        val imageCount: Int? = null,
        @SerialName("news_count")
        val articleCount: Int? = null,
): IBase

/**
 * @param totalCount total_count	该类型的素材的总数
 * @param itemCount item_count	本次调用获取的素材的数量
 * @param item 内容列表
 * */
@Serializable
class ResponseNewsList(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,

        @SerialName("total_count")
        val totalCount: Int? = null,
        @SerialName("item_count")
        val itemCount: Int? = null,
        val item: List<NewsItem>? = null
): IBase

/**
 *
 * @param updateTime 这篇图文消息素材的最后更新时间
 * */
@Serializable
class NewsItem(
        @SerialName("media_id")
        val mediaId: String,
        @SerialName("update_time")
        val updateTime: String? = null,
        val content: ArticleContent
)
@Serializable
class ArticleContent(
        @SerialName("news_item")
        val list: List<Article>
)
/**
 * 图文消息
 * @param title	是	标题
 * @param thumbMediaId thumb_media_id	是	图文消息的封面图片素材id（必须是永久mediaID）
 * @param author	否	作者
 * @param digest	否	图文消息的摘要，仅有单图文消息才有摘要，多图文此处为空。如果本字段为没有填写，则默认抓取正文前64个字。
 * @param showCover show_cover_pic	是	是否显示封面，0为false，即不显示，1为true，即显示
 * @param content	是	图文消息的具体内容，支持HTML标签，必须少于2万字符，小于1M，且此处会去除JS,涉及图片url必须来源 "上传图文消息内的图片获取URL"接口获取。外部图片url将被过滤。
 * @param srcUrl content_source_url	是	图文消息的原文地址，即点击“阅读原文”后的URL
 * @param canComment need_open_comment	否	Uint32 是否打开评论，0不打开，1打开
 * @param onlyFansCanComment only_fans_can_comment	否	Uint32 是否粉丝才可评论，0所有人可评论，1粉丝才可评论
 * @param url	图文页的URL
 * */
@Serializable
class Article(
        val title: String,
        @SerialName("thumb_media_id")
        val thumbMediaId: String,
        val content: String,
        @SerialName("content_source_url")
        val srcUrl: String,
        @SerialName("show_cover_pic")
        val showCover: Int = 0,
        @SerialName("need_open_comment")
        val canComment: Int = 0,
        @SerialName("only_fans_can_comment")
        val onlyFansCanComment: Int = 0,
        val author: String? = null,
        val digest: String? = null,
        val url: String? = null
)

@Serializable
class ResponseNews(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,

        @SerialName("news_item")
        val list: List<Article>? = null
): IBase
/**
 * @param updateTime 最后更新时间
 * @param url 当获取的列表是图片素材列表时，该字段是图片的URL
 * @param name	文件名称
 * */
@Serializable
class MediaItem(
        @SerialName("media_id")
        val mediaId: String,
        @SerialName("update_time")
        val updateTime: String? = null,
        val url: String? = null,
        val name: String
)

/**
 * @param totalCount total_count	该类型的素材的总数
 * @param itemCount item_count	本次调用获取的素材的数量
 * @param item 内容列表
 * */
@Serializable
class ResponseMediaList(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,

        @SerialName("total_count")
        val totalCount: Int? = null,
        @SerialName("item_count")
        val itemCount: Int? = null,
        val item: List<MediaItem>? = null
): IBase