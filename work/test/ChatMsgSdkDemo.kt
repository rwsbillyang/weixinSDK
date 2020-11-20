/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-19 21:12
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

package com.github.rwsbillyang.wxSDK.work.test


import com.github.rwsbillyang.wxSDK.work.chatMsg.ChatMsgSdk
import java.io.File
import java.io.FileOutputStream

/**
 * 1 seq limit proxy passwd timeout
 * 2 sdkField proxy passwd timeout
 * 3 encrypt_key encrypt_msg
 *    if (argc < 2)
{
printf("./sdktools 1 seq limit proxy passwd timeout\n");
printf("./sdktools 2 fileid savefile proxy passwd timeout\n");
printf("./sdktools 3 encrypt_key encrypt_msg\n");
return -1;
}
 * */
object SdkDemo {
    @JvmStatic
    fun main(args: Array<String>) {
        val sdk: Long = ChatMsgSdk.NewSdk()
        println(ChatMsgSdk.Init(sdk, "wwd08c8e7c775ab44d", "zJ6k0naVVQ--gt9PUSSEvs03zW_nlDVmjLCTOTAfrew"))
        val priKey = """
            -----BEGIN RSA PRIVATE KEY-----
            MIIEowIBAAKCAQEAjzJfnYgy8gI/UdR9OMzMh2/Svtz6ynPJdgqWX0qCm7361g04
            Sz/g+aJGtqu8jRJNg3rxQhezoy5mc+//QDyL6sM0auASS+eWx2igxVahLhJEWef3
            2woMrb+OHs6nJf2jBcfA494KvgkxSYXU84vl8UCHwUI839SLBSLWofb8ccFEmy9W
            VSWY7EIUVV8pO8sLZ9uVWK1IrHHSrcGkx4SF913RDIxCTExj7bLqxNRklzWZgi3m
            bYoIsTG6dmsGkmxagGPEtuVOI2UjpjQw1WyrN+6o+2+qpOIuoHKF5vGrPbQBL1jU
            nshGs3MnwTWdxVOe+bw8a+VAqxpD5DlfdKamgQIDAQABAoIBAGNRbe3mPGeMVXyd
            I6kUqrs5PPNyc2OdwVpk53z6QfJhZyu1iZjvmkuqWN9z59f0nNyXlePgapDAqwC4
            sdJM7EKM17tU5HvPCc4O7ItSlYJN2yh8cnVy1+5ekOUfMeFwtPRaYpfpNowt9ghn
            kZbGLlsRBddt6KjaUv3h9vnpQ5hlhU53slh+Zsdrsselpy0sTCF0ulVQgioZ6lzg
            /Y61xCGxRWqq71UT/7EOZwIoD3NmLRZLE6vxm8uBrFvZZ5/jw9Z//8S+vgtRUgGj
            /5v55ftTG+EOsaz2Zvt+jghkaEAchcyy5LyUv09Ir9eOYfjNAgeCk9dQv6E+2BiR
            SvcUJEECgYEA5CvcoSpV66qC1T99/JqmqyTjORZ4ZKBJY9vos4uxNYfCbbhNJLDY
            QAbCC234nBuKQ+3cVKkWtjBwfNp90mEmIoN6sYv+SdtVe2ofhJEqLMTiGyZPi2Iv
            +vvRpFmiAUfWXqFzBy+3HSoTkRqbCytUIT7NXZZ3YwKbbIhyzABTzSkCgYEAoKlf
            Gsn4rQEfmitAGJpuiSahih6KV3K+S9olnPbwd04YOw4u5UmouhyC7N0z2K8AO6zo
            IkzCQtWQS1B1uaPE/stG7I/iy0CI6q46nSly5pajBBGQc+Y5ixRf23hEjGgca556
            1pDyMs0Nb0J2AGDcr8olrbT1KwBvg8oWHOlA4ZkCgYAmC+pONXD+SwBl7qBjbqY8
            A3qgGk8Y+GFEdXbn+XMjKfARu5mhdJuakYXpwfyiizUS/qaut0NCPfGD4Cr62Zgy
            SRo8YMuWJSyr15ZJ1KrjrDDHtiutYkH959+dOBT7ga8NOH6lxB8Ujd+VYopX4nG0
            2XQFFwHxUI36GwaJXcSbgQKBgEe4VERZNTHF9p2UASD6j62aGTLXP1qaVmj2ESRo
            +B/KNPbn9fdVUoUChU/Hz4VDWg9JuLbXHUFIpQl5+ZPNj/tOM3MXKF8jh/t7m57d
            CfX1+P+v95RFihqUFdabcb5cG5PPQ3bVbclP0FeCi7rPgrTWwMsypN91alKivAxb
            9CLBAoGBALfb5SEupOoyIUetiWhOc1vlmP+71rJEXUEYwm0CMmfKZRW+I1A8qTE+
            DVT6iUMVt6jj8L/YiMzjUh8y8HLcp4mlldzdfyjssZg6v2hABJcnID6o3DcTXA8k
            8uMmaZZ0qY4oUzwgZa7bf/C//3mKlRUMoBYOT/LAksZlySIBXoST
            -----END RSA PRIVATE KEY-----
            
            """.trimIndent()

        var ret = 0
        if (args[0] == "1") {
            //1 seq limit proxy passwd timeout
            val seq = args[1].toLong()
            val limit = args[2].toInt()
            val slice: Long = ChatMsgSdk.NewSlice()
            ret = ChatMsgSdk.GetChatData(sdk, seq, limit, args[3], args[4], args[5].toLong(), slice)
            if (ret != 0) {
                println("getchatdata ret $ret")
                return
            }
            println("getchatdata :" + ChatMsgSdk.GetContentFromSlice(slice))
            ChatMsgSdk.FreeSlice(slice)
        } else if (args[0] == "2") {
            //2 sdkField proxy passwd timeout file
            var indexbuf = ""
            while (true) {
                val media_data: Long = ChatMsgSdk.NewMediaData()
                ret = ChatMsgSdk.GetMediaData(sdk, indexbuf, args[1], args[2], args[3], args[4].toLong(), media_data)
                println("getmediadata ret:$ret")
                if (ret != 0) {
                    return
                }
                System.out.printf("getmediadata outindex len:%d, data_len:%d, is_finis:%d\n", ChatMsgSdk.GetIndexLen(media_data),
                        ChatMsgSdk.GetDataLen(media_data), ChatMsgSdk.IsMediaDataFinish(media_data))
                try {
                    val outputStream = FileOutputStream(File(args[5]))
                    outputStream.write(ChatMsgSdk.GetData(media_data))
                    outputStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (ChatMsgSdk.IsMediaDataFinish(media_data) === 1) {
                    ChatMsgSdk.FreeMediaData(media_data)
                    break
                } else {
                    indexbuf = ChatMsgSdk.GetOutIndexBuf(media_data)
                    ChatMsgSdk.FreeMediaData(media_data)
                }
            }
        } else if (args[0] == "3") {
            //3 encrypt_key encrypt_msg
            // notice!  use prikey to decrpyt get args[1]
            val msg: Long = ChatMsgSdk.NewSlice()
            ret = ChatMsgSdk.DecryptData(sdk, args[1], args[2], msg)
            if (ret != 0) {
                println("getchatdata ret $ret")
                return
            }
            println("decrypt ret:" + ret + " msg:" + ChatMsgSdk.GetContentFromSlice(msg))
            ChatMsgSdk.FreeSlice(msg)
        } else {
            println("wrong args " + args[0])
        }
        ChatMsgSdk.DestroySdk(sdk)
    }
}
