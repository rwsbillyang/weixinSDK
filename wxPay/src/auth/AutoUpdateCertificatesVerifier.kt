/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-01 12:11
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

package com.github.rwsbillyang.wxSDK.wxPay.auth

import com.github.rwsbillyang.wxSDK.wxPay.WxPayApi
import com.github.rwsbillyang.wxSDK.security.AesUtil
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateFactory
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock


/**
 * 自动更新微信平台证书
 *
 * 某些情况下，微信支付会更新平台证书。这时，商户有多个微信支付平台证书可以用于加密。为了保证解密顺利，
 * 商户发起请求的HTTP头部中应包括证书序列号，以声明加密所用的密钥对和证书。
 *
 * 从2018年6月起，微信支付开始在微信支付分、营销代金券等API，使用权威CA颁发的平台证书标识微信支付的身份信息。
 * 商户也可以选择“惰性加载”的方式实现平滑切换。当业务逻辑服务上没有证书序列号所对应的证书时，调用API获取对应的平台证书，
 * 再进行签名验证或敏感信息加密。为了提高效率，商户可以在短时间内进行适当的缓存。
 *
 * https://github.com/wechatpay-apiv3/wechatpay-apache-httpclient#%E8%87%AA%E5%8A%A8%E6%9B%B4%E6%96%B0%E8%AF%81%E4%B9%A6%E5%8A%9F%E8%83%BD%E5%8F%AF%E9%80%89
 * https://wechatpay-api.gitbook.io/wechatpay-api-v3/qian-ming-zhi-nan-1/min-gan-xin-xi-jia-mi
 * https://wechatpay-api.gitbook.io/wechatpay-api-v3/qian-ming-zhi-nan-1/wei-xin-zhi-fu-ping-tai-zheng-shu-geng-xin-zhi-yin
 */
class AutoUpdateCertificatesVerifier @JvmOverloads constructor(
    //private val credentials: Credentials,
    private val apiV3Key: ByteArray,
    //证书更新间隔时间，单位为分钟
    private val minutesInterval: Int = TimeInterval.SixHours.minutes
) : Verifier {
    //时间间隔枚举，支持一小时、六小时以及十二小时
    enum class TimeInterval(val minutes: Int) {
        OneHour(60), SixHours(60 * 6), TwelveHours(60 * 12);

    }

    companion object {
        private val log = LoggerFactory.getLogger("AutoUpdateCertificatesVerifier")
    }

    //上次更新时间
    @Volatile
    private var instant: Instant? = null
    private var verifier: CertificatesVerifier? = null
    private val lock = ReentrantLock()
    override val validCertificate: X509Certificate
        get() = verifier!!.validCertificate
    private var firstlyUse = true

    init {
        //构造时更新证书
        instant = try {
            autoUpdateCert()
            Instant.now()
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: GeneralSecurityException) {
            throw RuntimeException(e)
        }
    }

    override fun verify(serialNumber: String, message: ByteArray, signature: String): Boolean {
        if(firstlyUse) return true  //首次下载平台证书时，自己验证自己的请求，总返回true
        else{
            if (instant == null
                || Duration.between(instant, Instant.now()).toMinutes() >= minutesInterval
            ) {
                if (lock.tryLock()) {
                    try {
                        autoUpdateCert()
                        instant = Instant.now()
                    } catch (e: GeneralSecurityException) {
                        log.warn("Auto update cert failed, exception = $e")
                    } catch (e: IOException) {
                        log.warn("Auto update cert failed, exception = $e")
                    } finally {
                        lock.unlock()
                    }
                }
            }
           //若首次请求下载失败，将导致此处的verifier为null
            return if(verifier == null){
                log.warn("verifier is null after autoUpdateCert")
                false
            }else{
                verifier!!.verify(serialNumber, message, signature)
            }
        }

    }
    /**
     * 风险
     * 因为不需要传入微信支付平台证书，AutoUpdateCertificatesVerifier 在首次更新证书时不会验签，
     * 也就无法确认应答身份，可能导致下载错误的证书。但下载时会通过 HTTPS、AES 对称加密来保证证书安全，
     * 所以可以认为，在使用官方 JDK、且 APIv3 密钥不泄露的情况下，AutoUpdateCertificatesVerifier 是安全的。
     *
     * 首次下载时，也是使用自己进行验证，不像GitHub上官方版本单独使用一个httpclient进行请求，
     * 第一次因verifier为空，而使用一个独立的总返回true的verifier
     * */
    private fun autoUpdateCert(){
        val bean = WxPayApi.downloadPlatformCerts()
        //以下代码将在verify通过后执行
        val decryptor = AesUtil(apiV3Key)
        val cf = CertificateFactory.getInstance("X509")
        val newCertList = bean.data.mapNotNull {
            //解密
            val cert = decryptor.decryptToString(
                it.encryptCertificate.associatedData?.toByteArray(),
                it.encryptCertificate.nonce.toByteArray(),
                it.encryptCertificate.ciphertext)

            val x509Cert = cf.generateCertificate(
                ByteArrayInputStream(cert.toByteArray(charset("utf-8")))
            ) as X509Certificate

            val cert509 = try {
                x509Cert.checkValidity()
                x509Cert
            } catch (e: CertificateExpiredException) {
                log.warn("e.message=${e.message}")
                null
            } catch (e: CertificateNotYetValidException) {
                log.warn("e.message=${e.message}")
                null
            }
            cert509
        }

        firstlyUse = false
        if(newCertList.isNullOrEmpty()){
            log.warn("got empty newCertList, it will no verifier")
        }else{
            verifier = CertificatesVerifier(newCertList)
        }

    }

}