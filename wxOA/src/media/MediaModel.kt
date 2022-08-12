/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-21 21:06
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:UseContextualSerialization(ObjectId::class)

package com.github.rwsbillyang.wxOA.media

import com.github.rwsbillyang.ktorKit.apiJson.Box
import com.github.rwsbillyang.ktorKit.apiJson.IUmiListParams
import io.ktor.resources.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.litote.kmongo.*


@Serializable
data class ArticleDetail(
        val appId: String? = null,//为null，表示向前兼容没该字段的老系统
        val content: String,
        val sha1: String, // content的sha1值，避免重复
        val _id: ObjectId
)
@Serializable
class ArticleOutline(
        val title: String,
        val thumbMediaId: String,
        val contentId: ObjectId, //指向ArticleDetail._id
        val srcUrl: String,
        val showCover: Int = 0,
        val canComment: Int = 0,
        val onlyFansCanComment: Int = 0,
        val author: String? = null,
        val digest: String? = null,
        val url: String? = null
)

@Serializable
class ArticleOutlineList(
        @SerialName("news_item")
        val list: List<ArticleOutline>
)


/**
 *
 * @param updateTime 这篇图文消息素材的最后更新时间
 * */
@Serializable
data class MaterialNews(
        val appId: String? = null,//为null，表示向前兼容没该字段的老系统
        val _id: String, //mediaId
        @SerialName("update_time")
        val updateTime: Long? = null,
        val content: ArticleOutlineList
)

@Serializable
data class MaterialVideo(
        val appId: String? = null,//为null，表示向前兼容没该字段的老系统
        val _id: String, //mediaId
        val name: String,
        @SerialName("update_time")
        val updateTime: Long? = null,
        @SerialName("down_url")
        val downUrl: String? = null,
        val title: String? = null,
        val description: String? = null
)

/**
 * 图片和音频
 * @param updateTime 最后更新时间
 * @param url 当获取的列表是图片素材列表时，该字段是图片的URL
 * @param name	文件名称
 * */
@Serializable
data class MaterialMedia(
        val appId: String? = null,//为null，表示向前兼容没该字段的老系统
        val _id: String, //mediaId
        val type: String,
        val name: String,
        @SerialName("update_time")
        val updateTime: Long? = null,
        val url: String? = null
)


/**
 * 返回的列表数据
 * */
@Serializable
class MaterialNewsListBox(
        val total: Long,
        val data: List<MaterialNews>? = null
) : Box()
@Serializable
class MaterialVideoListBox(
        val total: Long,
        val data: List<MaterialVideo>? = null
) : Box()
@Serializable
class MaterialMediaListBox(
        val total: Long,
        val data: List<MaterialMedia>? = null
) : Box()

@Serializable
@Resource("/news/list")
data class MaterialNewsListParams(
        override val umi: String? = null,
        val _id: String? = null,
        val title: String? = null,
        val appId: String? = null
) : IUmiListParams {
        fun toFilter(): Bson {
                val idFilter = _id?.let{ MaterialNews::_id eq it }
                val nameFilter = title?.let {  " { \"content.news_item.0.title\": { \$regex: /.*$it.*/ } }".bson  }
                //MaterialNews::content / ArticleContent::list / 0 / Article::title regex ".*$it.*"
                val appIdFilter = appId?.let{ MaterialNews::appId eq appId}
                return and(idFilter,nameFilter, appIdFilter)
        }
}
@Serializable
@Resource("/video/list")
data class MaterialVideoListParams(
        override val umi: String? = null,
        val _id: String? = null,
        val name: String? = null,
        val appId: String? = null
) : IUmiListParams {
        fun toFilter(): Bson {
                val idFilter = _id?.let { MaterialVideo::_id eq it }
                val nameFilter = name?.let { MaterialVideo::name regex ".*$it.*" }
                val appIdFilter = appId?.let{ MaterialVideo::appId eq appId}
                return and(idFilter,nameFilter, appIdFilter)
        }
}

@Serializable
@Resource("/media/list")
data class MaterialMediaListParams(
        override val umi: String? = null,
        val _id: String? = null,
        val name: String? = null,
        val type: String? = null,
        val appId: String? = null
) : IUmiListParams {
        fun toFilter(): Bson {
                val idFilter = _id?.let { MaterialMedia::_id eq it }
                val nameFilter = name?.let { MaterialMedia::name regex ".*$it.*" }
                val typeFilter = type?.let { MaterialMedia::type eq it }
                val appIdFilter = appId?.let{ MaterialMedia::appId eq appId}
                return and(idFilter, typeFilter, nameFilter, appIdFilter)
        }
}
