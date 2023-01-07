/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 14:38
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

package com.github.rwsbillyang.wxSDK.work

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Invoice(
    @SerialName("card_id")val cardId: String,
    @SerialName("encrypt_code")val encryptCode: String)

class InvoiceApi(corpId: String?, agentId: String?, suiteId: String?)
    : WorkBaseApi(corpId, agentId,suiteId)
{

    override val group = "card/invoice/reimburse"

    /**
     * 查询电子发票
     * */
    fun getInvoiceInfo(cardId: String, encryptCode: String) = doPostRaw(
            "getinvoiceinfo",
        mapOf("card_id" to cardId,"encrypt_code" to encryptCode))


    /**
     * 更新发票状态
     * 锁定：电子发票进入了企业的报销流程时应该执行锁定操作，执行锁定操作后的电子发票仍然会存在于用户卡包内，但无法重复提交报销。
     * 解锁：当电子发票由于各种原因，无法完成报销流程时，应执行解锁操作。执行锁定操作后的电子发票将恢复可以被提交的状态。
     * 报销：当电子发票报销完成后，应该使用本接口执行报销操作。执行报销操作后的电子发票将从用户的卡包中移除，用户可以在卡包的消息中查看到电子发票的核销信息。注意，报销为不可逆操作，请开发者慎重调用。
     * https://work.weixin.qq.com/api/doc/90000/90135/90285
     * */
    fun updateStatus(cardId: String, encryptCode: String, status: String) = doPostRaw(
            "updateinvoicestatus",
        mapOf("card_id" to cardId,"encrypt_code" to encryptCode,"reimburse_status" to status))


    /**
     * 批量查询电子发票
     *
     * */
    fun getInvoiceInfoBatch(invoiceList: List<Invoice>) = doPostRaw(
            "getinvoiceinfobatch",
        mapOf( "item_list" to invoiceList))

    /**
     * 批量更新发票状态
     *
     * openid	是	用户openid，可用“userid与openid互换接口”获取
     * reimburse_status	是	发票报销状态 INVOICE_REIMBURSE_INIT：发票初始状态，未锁定；INVOICE_REIMBURSE_LOCK：发票已锁定，无法重复提交报销;INVOICE_REIMBURSE_CLOSURE:发票已核销，从用户卡包中移除
     * invoice_list	是	发票列表，必须全部属于同一个openid
     * card_id	是	发票卡券的card_id
     * encrypt_code	是	发票卡券的加密code，和card_id共同构成一张发票卡券的唯一标识
     * */
    fun batchUpdateStatus(openId:String, invoiceList: List<Invoice>, status: String) = doPostRaw(
            "updatestatusbatch",
        mapOf("openid" to openId, "invoice_list" to invoiceList, "reimburse_status" to status))



}