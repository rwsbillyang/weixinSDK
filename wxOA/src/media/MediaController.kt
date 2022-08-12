/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-21 21:07
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

package com.github.rwsbillyang.wxOA.media


import com.github.rwsbillyang.ktorKit.apiJson.DataBox


import com.github.rwsbillyang.wxSDK.officialAccount.MaterialApi
import com.github.rwsbillyang.wxSDK.officialAccount.MediaType
import org.apache.commons.codec.digest.DigestUtils
import org.bson.types.ObjectId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

import org.slf4j.LoggerFactory

class MediaController : KoinComponent {
    private val log = LoggerFactory.getLogger("MediaController")
    private val service: MediaService by inject()

    private val pageSize = 20



    /**
     * 同步素材： 从官方服务器中获取，保存到本地
     * */
    fun syncMaterials(appId: String?, type: String): DataBox<Int>{
        if(appId == null) return DataBox.ko("invalida parameter: no appId")
        val materialApi = MaterialApi(appId)
        val res = materialApi.getCount()
        if(res.isOK()){
            val newsPages = res.articleCount?:0 / pageSize +  if (res.articleCount?:0 % pageSize > 0) 1 else 0
            val imgPages = res.imageCount?:0 / pageSize +  if (res.imageCount?:0 % pageSize > 0) 1 else 0
            val voicePages = res.voiceCount?:0 / pageSize +  if (res.voiceCount?:0 % pageSize > 0) 1 else 0
            val videoPages = res.videoCount?:0 / pageSize +  if (res.videoCount?:0 % pageSize > 0) 1 else 0

            when(type){
                MediaType.NEWS.value -> {
                    if(newsPages > 0){
                        service.dropNewsCol()
                        getAllNewsMedia(materialApi, appId, newsPages)
                    }
                }
                MediaType.VIDEO.value -> {
                    if(videoPages > 0){
                        service.dropVideoCol()
                        getAllMaterial(materialApi, appId, MediaType.VIDEO, videoPages)
                    }
                }
                else -> {
                    var dropped = false
                    if(imgPages > 0){
                        service.dropMediaCol()
                        dropped = true
                        getAllMaterial(materialApi, appId, MediaType.IMAGE, imgPages)
                    }
                    if(voicePages > 0){
                        if(!dropped){
                            service.dropMediaCol()
                            dropped = true
                        }
                        getAllMaterial(materialApi, appId, MediaType.VOICE, voicePages)
                    }
                }
            }

            return DataBox.ok(newsPages+imgPages+voicePages+videoPages)
        }else{
            log.warn("syncMaterials : ${res.errCode}: ${res.errMsg}")
            return DataBox.ko(res.errMsg?:"fail to get count")
        }
    }

    private fun getAllMaterial(materialApi: MaterialApi, appId:String, type: MediaType, totalPages: Int)
    {
        for(i in 0 until totalPages){
            val res = materialApi.getMediaList(type,i*pageSize, pageSize)
            if(res.isOK() && !res.item.isNullOrEmpty()){
                if(type == MediaType.VIDEO){
                    service.insertVideoList(res.item!!.map {
                        val ret = materialApi.getVideo(it.mediaId)
                        if(ret.isOK()){
                            MaterialVideo(appId, it.mediaId, it.name, it.updateTime, ret.videoUrl?:it.url, ret.title, ret.description)
                        }else{
                            MaterialVideo(appId, it.mediaId, it.name, it.updateTime, it.url, type.value)
                        }
                    })
                }else{
                    service.insertMediaList(res.item!!.map { MaterialMedia(appId,  it.mediaId, type.value,it.name, it.updateTime, it.url) })
                }
            }else{
                log.warn("getAllMaterial type=$type at page $i: ${res.errCode}: ${res.errMsg}")
            }
        }
    }

    private fun getAllNewsMedia(materialApi: MaterialApi,appId: String, totalPages: Int){
        for(i in 0 until totalPages){
            val res = materialApi.getNewsList(i*pageSize, pageSize)
            if(res.isOK() && !res.item.isNullOrEmpty()){
                res.item?.forEach {
                    it.content.list.forEach {
                       // DigestU.content
                    }
                }
                service.insertNewsMediaList(res.item!!.map {
                    val list =  it.content.list.map {
                        val sha1 = DigestUtils.sha1Hex(it.content)
                        val doc = service.findArticleDetail(sha1)
                        val id = if(doc==null){
                            val contentId = ObjectId()
                            service.insertArticleDetail(ArticleDetail(appId, it.content, sha1, contentId))
                            contentId
                        }else{
                            doc._id
                        }
                        ArticleOutline(it.title,it.thumbMediaId, id, it.srcUrl, it.showCover,
                                it.canComment, it.onlyFansCanComment, it.author, it.digest,it.url)
                    }
                    MaterialNews(appId,it.mediaId, it.updateTime, ArticleOutlineList(list))
                })
            }else{
                log.warn("getAllNewsMedia at page $i::${res.errCode}: ${res.errMsg}")
            }
        }
    }


    fun findAllNews() = DataBox.ok(service.findAllNews())
    fun findAllVideo() = DataBox.ok(service.findAllVideo())
    fun findAllByType(type: String) = DataBox.ok(service.findAllByType(type))

    fun findMaterialNewsList(listParam: MaterialNewsListParams): MaterialNewsListBox {
        val filter = listParam.toFilter()
        val total = service.countNews(filter)
        val list = service.findNewsPage(filter, listParam.pagination)
        return MaterialNewsListBox(total,list)
    }
    fun findMaterialVideoList(listParam: MaterialVideoListParams): MaterialVideoListBox {
        val filter = listParam.toFilter()
        val total = service.countVideo(filter)
        val list = service.findVideoPage(filter, listParam.pagination)
        return MaterialVideoListBox(total,list)
    }
    fun findMaterialMediaList(listParam: MaterialMediaListParams): MaterialMediaListBox {
        val filter = listParam.toFilter()
        val total = service.countMedia(filter)
        val list = service.findMediaPage(filter, listParam.pagination)
        return MaterialMediaListBox(total,list)
    }

    fun del(appId: String?, type: String, id: String, delType: Int): DataBox<Int> {
        if(appId == null) return DataBox.ko("invalida parameter: no appId")
        return if(delType != 0){
            val materialApi = MaterialApi(appId)
            val ret = materialApi.del(id)
            if(ret.isOK()){
                del(type, id)
            }else{
                DataBox.ko("${ret.errCode}: ${ret.errMsg}")
            }
        }else{
            del(type, id)
        }
    }
    private fun del(type: String, id: String): DataBox<Int>{
        val deleteResult = when(type){
            MediaType.NEWS.value -> {
                service.delNewsById(id)
            }
            MediaType.VIDEO.value -> {
                service.delVideoById(id)
            }
            else -> {
                service.delMediaById(id)
            }
        }
        return DataBox.ok(deleteResult.deletedCount.toInt())
    }


}
