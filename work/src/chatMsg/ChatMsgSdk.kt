/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-17 16:39
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

//package com.github.rwsbillyang.wxSDK.work.chatMsg
package com.tencent.wework;

/* sdk返回数据
typedef struct Slice_t {
    char* buf;
    int len;
} Slice_t;

typedef struct MediaData {
    char* outindexbuf;
    int out_len;
    char* data;
    int data_len;
    int is_finish;
} MediaData_t;
*/

object ChatMsgSdk {
    external fun NewSdk(): Long

    /**
     * 初始化函数
     * Return值=0表示该API调用成功
     *
     * @param [in]  sdk	NewSdk返回的sdk指针
     * @param [in]  corpid  调用企业的企业id，例如：wwd08c8exxxx5ab44d，
     * 可以在企业微信管理端--我的企业--企业信息查看
     *
     * @param [in]  secret	聊天内容存档的Secret，
     * 可以在企业微信管理端--管理工具--聊天内容存档查看
     *
     *
     * @return 返回是否初始化成功
     * 0   - 成功
     * !=0 - 失败
     */
    external fun Init(sdk: Long, corpid: String, secret: String): Int

    /**
     * 拉取聊天记录函数
     * Return值=0表示该API调用成功
     *
     *
     * @param [in]  sdk	NewSdk返回的sdk指针
     * @param [in]  seq	从指定的seq开始拉取消息，注意的是返回的消息从seq+1开始返回，
     * seq为之前接口返回的最大seq值。首次使用请使用seq:0
     * 本次请求获取消息记录开始的seq值。首次访问填写0，非首次使用上次企业微信返回的最大seq。
     * 允许从任意seq重入拉取。Uint64类型，范围0-pow(2,64)-1
     *
     * @param [in]  limit 一次拉取的消息条数，最大值1000条，超过1000条会返回错误
     * @param [in]  proxy	使用代理的请求，需要传入代理的链接。如：socks5://10.0.0.1:8081
     * 或者 http://10.0.0.1:8081
     * @param [in]  passwd	代理账号密码，需要传入代理的账号密码。如 user_name:passwd_123
     * @param [in]  timeout 超时时间，单位秒
     * @param [out] chatDatas 返回本次拉取消息的数据，返回的ChatDatas内容为json格式
     * slice结构体.内容包括errcode/errmsg，
     * 以及每条消息内容。示例如下：
     * {
    "errcode": 0,
    "errmsg": "ok",
    "chatdata": [{
    "seq": 196,
    "msgid": "CAQQ2fbb4QUY0On2rYSAgAMgip/yzgs=",
    "publickey_ver": 3,
    "encrypt_random_key":"ftJ+uz3n/z1DsxlkwxNgE+mL38H42/KCvN8T60gbbtPD+Rta1hKTuQPzUzO6Hzne97MgKs7FfdDxDck/v8cDT6gUVjA2tZ/M7euSD0L66opJ/IUeBtpAtvgVSD5qhlaQjvfKJc/zPMGNK2xCLFYqwmQBZXbNT7uA69Fflm512nZKW/piK2RKdYJhRyvQnA1ISxK097sp9WlEgDg250fM5tgwMjujdzr7ehK6gtVBUFldNSJS7ndtIf6aSBfaLktZgwHZ57ONewWq8GJe7WwQf1hwcDbCh7YMG8nsweEwhDfUz+u8rz9an+0lgrYMZFRHnmzjgmLwrR7B/32Qxqd79A==",
    "encrypt_chat_msg": "898WSfGMnIeytTsea7Rc0WsOocs0bIAerF6de0v2cFwqo9uOxrW9wYe5rCjCHHH5bDrNvLxBE/xOoFfcwOTYX0HQxTJaH0ES9OHDZ61p8gcbfGdJKnq2UU4tAEgGb8H+Q9n8syRXIjaI3KuVCqGIi4QGHFmxWenPFfjF/vRuPd0EpzUNwmqfUxLBWLpGhv+dLnqiEOBW41Zdc0OO0St6E+JeIeHlRZAR+E13Isv9eS09xNbF0qQXWIyNUi+ucLr5VuZnPGXBrSfvwX8f0QebTwpy1tT2zvQiMM2MBugKH6NuMzzuvEsXeD+6+3VRqL"
    }]
    }
    errcode	0表示成功，错误返回非0错误码，需要参看errmsg。Uint32类型
    errmsg	返回信息，如非空为错误原因。String类型
    chatdata	聊天记录数据内容。数组类型。包括seq、msgid等内容
    seq	消息的seq值，标识消息的序号。再次拉取需要带上上次回包中最大的seq。Uint64类型，范围0-pow(2,64)-1
    msgid	消息id，消息的唯一标识，企业可以使用此字段进行消息去重。String类型。msgid以_external结尾的消息，表明该消息是一条外部消息。
    publickey_ver	加密此条消息使用的公钥版本号。Uint32类型
    encrypt_random_key	使用publickey_ver指定版本的公钥进行非对称加密后base64加密的内容，需要业务方先base64 decode处理后，再使用指定版本的私钥进行解密，得出内容。String类型
    encrypt_chat_msg	消息密文。需要业务方使用将encrypt_random_key解密得到的内容，与encrypt_chat_msg，传入sdk接口DecryptData,得到消息明文。String类型
     *
     * encrypt_random_key内容解密说明：
     * encrypt_random_key是使用企业在管理端填写的公钥（使用模值为2048bit的秘钥），采用RSA加密算法进
     * 行加密处理后base64 encode的内容，加密内容为企业微信产生。RSA使用PKCS1。
     *
     * 企业通过GetChatData获取到会话数据后：
    a) 需首先对每条消息的encrypt_random_key内容进行base64 decode,得到字符串str1.
    b) 使用publickey_ver指定版本的私钥，使用RSA PKCS1算法对str1进行解密，得到解密内容str2.
    c) 得到str2与对应消息的encrypt_chat_msg，调用下方描述的DecryptData接口，即可获得消息明文。
     *
     *
     * @return 返回是否调用成功
     * 0   - 成功
     * !=0 - 失败
     */
    external fun GetChatData(sdk: Long, seq: Long, limit: Int, proxy: String?, passwd: String?, timeout: Long, chatData: Long): Int

    /**
     * 拉取媒体消息函数
     * Return值=0表示该API调用成功
     *
     * @param [in]  sdk	NewSdk返回的sdk指针
     * @param [in]  sdkFileid	从GetChatData返回的聊天消息中，媒体消息包括的sdkfileid
     * @param [in]  proxy	使用代理的请求，需要传入代理的链接。如：socks5://10.0.0.1:8081 或者 http://10.0.0.1:8081
     * @param [in]  passwd	代理账号密码，需要传入代理的账号密码。如 user_name:passwd_123
     * @param [in]  indexbuf 媒体消息分片拉取，需要填入每次拉取的索引信息。首次不需要填写，默认拉取512k，后续每次调用只需要将上次调用返回的outindexbuf填入即可。
     * @param [out] media_data	返回本次拉取的媒体数据.MediaData结构体.内容包括data(数据内容)/outindexbuf(下次索引)/is_finish(拉取完成标记)
     *
     * @return 返回是否调用成功
     * 0   - 成功
     * !=0 - 失败
     */
    external fun GetMediaData(sdk: Long, indexbuf: String?, sdkField: String, proxy: String?, passwd: String?, timeout: Long, mediaData: Long): Int

    /**
     * @brief 解析密文
     * @param [in]  encrypt_key, getchatdata返回的encrypt_key 企业私钥解密encrypt_random_key后的内容
     * @param [in]  encrypt_msg, GetChatdata接口返回的加密消息encrypt_chat_msg
     * @param [out] msg, 解密的消息明文
     * @return 返回是否调用成功
     * 0   - 成功
     * !=0 - 失败
     */
    external fun DecryptData(sdk: Long, encrypt_key: String, encrypt_msg: String, msg: Long): Int

    external fun DestroySdk(sdk: Long)

    external fun NewSlice(): Long

    /**
     * @brief 释放slice，和NewSlice成对使用
     * @return
     */
    external fun FreeSlice(slice: Long)

    /**
     * @brief 获取slice内容
     * @return 内容
     */
    external fun GetContentFromSlice(slice: Long): String?

    /**
     * @brief 获取slice内容长度
     * @return 内容
     */
    external fun GetSliceLen(slice: Long): Int
    external fun NewMediaData(): Long
    external fun FreeMediaData(mediaData: Long)

    /**
     * @brief 获取mediadata outindex
     * @return outindex
     */
    external fun GetOutIndexBuf(mediaData: Long): String

    /**
     * @brief 获取mediadata data数据
     * @return data
     */
    external fun GetData(mediaData: Long): ByteArray
    external fun GetIndexLen(mediaData: Long): Int
    external fun GetDataLen(mediaData: Long): Int

    /**
     * @brief 判断mediadata是否结束
     * @return 1完成、0未完成
     */
    external fun IsMediaDataFinish(mediaData: Long): Int

    init {
        //建议将so放置在系统路径：LD_LIBRARY_PATH指定的路径内，或者将so所在的目录加入到LD_LIBRARY_PATH的路径范围内。
        System.loadLibrary("WeWorkFinanceSdk_Java")
    }
}