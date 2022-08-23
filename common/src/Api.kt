/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 15:09
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

package com.github.rwsbillyang.wxSDK

import com.github.rwsbillyang.ktorKit.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


abstract class Api  {

    abstract fun url(name: String, requestParams: Map<String, String?>? = null, needAccessToken: Boolean = true): String

    /**
     * 返回R泛型类型结果
     * */
    inline fun <reified R> doGet(name: String, parameters: Map<String, String?>? = null, headerPair: Pair<String,String>? = null): R
        = doGetByUrl(url(name, parameters),headerPair)


    inline fun <reified R> doGetByUrl(url: String, headerPair: Pair<String,String>? = null): R = runBlocking{
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            DefaultClient.get(url){ headerPair?.let { header(headerPair.first, headerPair.second) }}.body()
        }
    }

    /**
     * 返回HttpResponse，需自行解析
     * */
    fun doGetRaw(name: String, parameters: Map<String, String?>? = null, headerPair: Pair<String,String>? = null)
        = doGetRawByUrl(url(name, parameters),headerPair)
    fun doGetRawByUrl(url: String, headerPair: Pair<String,String>? = null) = runBlocking {
        DefaultClient.get(url){ headerPair?.let { header(headerPair.first, headerPair.second) }}
    }



    /**
     * 返回R泛型类型结果
     * */
    inline fun <reified T,  reified R> doPost(name: String, data: T? = null, parameters: Map<String, String?>? = null, headerPair: Pair<String,String>? = null):R = runBlocking{
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            doPostRaw(name, data, parameters).body()
        }
    }
    /**
     * 返回HttpResponse，需自行解析
     * */
    inline fun <reified T> doPostRaw(name: String, data: T? = null, parameters: Map<String, String?>? = null, headerPair: Pair<String,String>? = null) = runBlocking {
        DefaultClient.post(url(name, parameters)) {
            headerPair?.let { header(headerPair.first, headerPair.second)}
            setBody(data)
        }
    }

}

