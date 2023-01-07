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
    msgHandler: IWorkMsgHandler?,
    eventHandler: IWorkEventHandler?,
    wxBizMsgCrypt: WXBizMsgCrypt,
    private val suiteInfoHandler: ISuiteInfoHandler?
): WorkMsgHub(wxBizMsgCrypt,msgHandler,eventHandler) {

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
    override fun parseXmlThenDispatch(appId: String, agentId:String?, decryptedXmlText: String, outerMap: Map<String, String?>): ReBaseMSg?{
        //val suiteId = outerMap["ToUserName"] //收到的数据包中ToUserName为产生事件的SuiteId，以ww或wx开头应用id， corpId也会以ww开头
        val agentId_ = outerMap["AgentID"] //AgentID为空

        val reader: XMLEventReader = XMLInputFactory.newInstance().createXMLEventReader(decryptedXmlText.byteInputStream())

        //内建应用的body
        //body=<xml><ToUserName><![CDATA[wwb096af3f1c]]></ToUserName><Encrypt><![CDATA[d9...LE=]]></Encrypt><AgentID><![CDATA[1000002]]></AgentID></xml>
        //以agentId是否为空，区分是应用授权部分的回调通知，还是普通应用的通知
        val reMsg = if(agentId_ == null){
            dispatchSuiteInfo(appId, reader, SuiteInfo.fromXml(decryptedXmlText, reader))
            null
        }else{
            val base = BaseInfo.fromXml(decryptedXmlText, reader)
            when(base.msgType){
                MsgType.EVENT -> dispatchEvent(appId, null, reader,base)
                else -> dispatchMsg(appId, null, reader,base)
            }
        }

        reader.close()
        return reMsg
    }



    fun dispatchSuiteInfo(suiteId: String, reader: XMLEventReader, suiteInfo: SuiteInfo)
    {
        if(suiteInfoHandler == null) return
        when(suiteInfo.infoType){
            SuiteInfo.INFO_TYPE_TICKET -> suiteInfoHandler.onTicket(suiteId,  SuiteTicket(suiteInfo).apply { read(reader) })
            SuiteInfo.INFO_TYPE_AUTH_CREATE -> suiteInfoHandler.onAuthCode(suiteId,  AuthCode(suiteInfo))
            SuiteInfo.INFO_TYPE_AUTH_CHANGE -> suiteInfoHandler.onAuthChange(suiteId,  AuthChange(suiteInfo))
            SuiteInfo.INFO_TYPE_AUTH_CANCEL -> suiteInfoHandler.onAuthCancel(suiteId,  AuthCancel(suiteInfo))
            SuiteInfo.INFO_TYPE_AGENT_SHARE_CHANGE -> suiteInfoHandler.onAgentShareChange(suiteId,  AgentShareChange(suiteInfo).apply { read(reader) })
            SuiteInfo.INFO_TYPE_CONTACT_CHANGE -> {
                val base = ContactChangeBase(suiteInfo).apply { read(reader) }
                when(base.changeType){
                    ContactChangeBase.CHANGE_TYPE_CREATE_USER -> suiteInfoHandler.onContactAdd(suiteId,  ContactAdd(base).apply { read(reader) })
                    ContactChangeBase.CHANGE_TYPE_UPDATE_USER -> suiteInfoHandler.onContactUpdate(suiteId,  ContactUpdate(base).apply { read(reader) })
                    ContactChangeBase.CHANGE_TYPE_DEL_USER -> suiteInfoHandler.onContactDel(suiteId,  ContactDel(base).apply { read(reader) })
                    ContactChangeBase.CHANGE_TYPE_CREATE_PARTY -> suiteInfoHandler.onDepartmentAdd(suiteId,  DepartmentAdd(base).apply { read(reader) })
                    ContactChangeBase.CHANGE_TYPE_UPDATE_PARTY -> suiteInfoHandler.onDepartmentUpdate(suiteId,  DepartmentUpdate(base).apply { read(reader) })
                    ContactChangeBase.CHANGE_TYPE_DEL_PARTY -> suiteInfoHandler.onDepartmentDel(suiteId,  DepartmentDel(base).apply { read(reader) })
                    ContactChangeBase.CHANGE_TYPE_UPDATE_TAG -> suiteInfoHandler.onTagContactChange(suiteId,  TagContactChange(base).apply { read(reader) })
                }
            }
            else -> suiteInfoHandler.onDefault(suiteId,  suiteInfo)
        }
    }
}