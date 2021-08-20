/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-08-10 17:24
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

package com.github.rwsbillyang.wxSDK.work.isv


import com.github.rwsbillyang.wxSDK.msg.BaseInfo
import com.github.rwsbillyang.wxSDK.msg.MsgType
import com.github.rwsbillyang.wxSDK.msg.ReBaseMSg
import com.github.rwsbillyang.wxSDK.security.WXBizMsgCrypt
import com.github.rwsbillyang.wxSDK.work.inMsg.*
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

/**
 * xml消息事件通知的解包、解析、分发处理
 * */
class WorkMsgHub3rd(
    msgHandler: IWorkMsgHandler,
    eventHandler: IWorkEventHandler,
    wxBizMsgCrypt: WXBizMsgCrypt,
    private val suiteInfoHandler: ISuiteInfoHandler
): WorkMsgHub(msgHandler,eventHandler, wxBizMsgCrypt) {

    /**
     * @param decryptedXmlText 由外层xml中的Encrypt字段解析出来的新xml字段
     * @param outerMap 外层xml得到的map数据
     * 外层xml：
     * <xml>
     *     <ToUserName><![CDATA[toUser]]></ToUserName>
     *     <AgentID><![CDATA[toAgentID]]></AgentID>
     *     <Encrypt><![CDATA[msg_encrypt]]></Encrypt>
     * </xml>
     *
     * ToUserName：企业微信的CorpID，当为第三方套件回调事件时，CorpID的内容为suiteid
     * AgentID：接收的应用id，可在应用的设置页面获取。对于应用授权部分，此字段为空
     * 参见：https://work.weixin.qq.com/api/doc/90000/90135/90238#%E4%BD%BF%E7%94%A8%E6%8E%A5%E6%94%B6%E6%B6%88%E6%81%AF
     * */
    override fun parseXml(decryptedXmlText: String, outerMap: Map<String, String?>): ReBaseMSg?{
        //val suiteId = outerMap["ToUserName"] //收到的数据包中ToUserName为产生事件的SuiteId，
        val agentId = outerMap["AgentID"] //AgentID为空

        val reader: XMLEventReader = XMLInputFactory.newInstance().createXMLEventReader(decryptedXmlText.byteInputStream())

        //以agentId是否为空，区分是应用授权部分的回调通知，还是普通应用的通知
        val reMsg = if(agentId.isNullOrBlank()){
            dispatchSuiteInfo(reader, SuiteInfo.fromXml(decryptedXmlText, reader))
            null
        }else{
            val base = BaseInfo.fromXml(decryptedXmlText, reader)
            when(base.msgType){
                MsgType.EVENT -> dispatchEvent(reader,base)
                else -> dispatchMsg(reader,base)
            }
        }

        reader.close()
        return reMsg
    }



    fun dispatchSuiteInfo(reader: XMLEventReader, suiteInfo: SuiteInfo)
    {
        when(suiteInfo.infoType){
            SuiteInfo.INFO_TYPE_TICKET -> suiteInfoHandler.onTicket(SuiteTicket(suiteInfo).apply { read(reader) })
            SuiteInfo.INFO_TYPE_AUTH_CREATE -> suiteInfoHandler.onAuthCode(AuthCode(suiteInfo))
            SuiteInfo.INFO_TYPE_AUTH_CHANGE -> suiteInfoHandler.onAuthChange(AuthChange(suiteInfo))
            SuiteInfo.INFO_TYPE_AUTH_CANCEL -> suiteInfoHandler.onAuthCancel(AuthCancel(suiteInfo))
            SuiteInfo.INFO_TYPE_AGENT_SHARE_CHANGE -> suiteInfoHandler.onAgentShareChange(AgentShareChange(suiteInfo).apply { read(reader) })
            SuiteInfo.INFO_TYPE_CONTACT_CHANGE -> {
                val base = ContactChangeBase(suiteInfo).apply { read(reader) }
                when(base.changeType){
                    ContactChangeBase.CHANGE_TYPE_CREATE_USER -> suiteInfoHandler.onContactAdd(ContactAdd(base).apply { read(reader) })
                    ContactChangeBase.CHANGE_TYPE_UPDATE_USER -> suiteInfoHandler.onContactUpdate(ContactUpdate(base).apply { read(reader) })
                    ContactChangeBase.CHANGE_TYPE_DEL_USER -> suiteInfoHandler.onContactDel(ContactDel(base).apply { read(reader) })
                    ContactChangeBase.CHANGE_TYPE_CREATE_PARTY -> suiteInfoHandler.onDepartmentAdd(DepartmentAdd(base).apply { read(reader) })
                    ContactChangeBase.CHANGE_TYPE_UPDATE_PARTY -> suiteInfoHandler.onDepartmentUpdate(DepartmentUpdate(base).apply { read(reader) })
                    ContactChangeBase.CHANGE_TYPE_DEL_PARTY -> suiteInfoHandler.onDepartmentDel(DepartmentDel(base).apply { read(reader) })
                    ContactChangeBase.CHANGE_TYPE_UPDATE_TAG -> suiteInfoHandler.onTagContactChange(TagContactChange(base).apply { read(reader) })
                }
            }
            else -> suiteInfoHandler.onDefault(suiteInfo)
        }
    }
}