package com.github.rwsbillyang.wxSDK.accessToken

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 动态刷新获取的值
 * */
interface ITimelyRefreshValue {
    fun get(): String?
}


/**
 * 通过提供一个refresher并在一定得间隔时间内进行刷新获取某个值
 *
 * 值更新后会发出更新通知
 *
 * @param appId appID or corpId
 * @param extra extra info such as agentName in wxwork
 * */
open class TimelyRefreshValue @JvmOverloads constructor(
    val appId: String,
    private val refresher: IRefresher,
    private var refreshIntervalTime: Long = 7100000,
    protected val extra: String? = null
) : Observable() {
    init {
        if (refreshIntervalTime > 7200000 && refreshIntervalTime <= 0) refreshIntervalTime = 7100000
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger("TimelyRefreshValue")
    }

    /**
     * 用于back fields的序列化保存
     * */
    private val timelyValue = TimelyValue()

    /**
     * accessToken正在刷新标志
     * 当请求accessToken()时候，检查token是否已经过期（7100秒）以及标识是否已经是true（表示正在刷新），
     */
    private val refreshingFlag: AtomicBoolean = AtomicBoolean(false)

    private fun doRefresh(now: Long, updateType: UpdateType){
        try {
            timelyValue.value = refresher.refresh()
            timelyValue.time = now

            refreshingFlag.set(false)
            setChanged()
            notifyObservers(
                    UpdateMsg(
                            appId,
                            updateType,
                            timelyValue.value,
                            timelyValue.time, extra
                    )
            )
        } catch (e: Exception) {
            refreshingFlag.set(false)
            log.error("fail to refresh: ${e.message}")
        }
    }
    /**
     *
     * 判断优先顺序：
     * 1.官方给出的超时时间是7200秒，这里用refreshIntervalTime秒来做，防止出现已经过期的情况
     * 2.刷新标识判断，如果正在刷新，则也直接跳过，避免多次重复刷新；如果没有正在刷新且已过期，则开始刷新
     */
    fun getRefreshedValue(updateType: UpdateType): String? {

        val now = System.currentTimeMillis()
        val delta: Long = now - timelyValue.time
        if (timelyValue.value != null){
            if(delta <= refreshIntervalTime)
            {
                return timelyValue.value
            }else if(delta < refreshIntervalTime+90)
            {
                //临近有效期，第一个请求者触发刷新
                if(refreshingFlag.compareAndSet(false, true))
                    doRefresh(now, updateType)
                return timelyValue.value
            }
        }
        //空值，或失效期过长，都触发刷新并且阻塞
        synchronized(timelyValue){
            refreshingFlag.set(true)
            doRefresh(now, updateType)
        }
        return timelyValue.value
    }

    /**
     * 用于slave server中接收到更新通知后，更新自己的back fields
     * */
    fun updateTokenInfo(value: String, time: Long) {
        timelyValue.value = value
        timelyValue.time = time
    }

}

/**
 * 动态提供accessToken的值
 *
 * @param appId       公众号或企业微信等申请的app id
 * @param refresher      刷新器
 * @param refreshIntervalTime 刷新时间间隔，默认7100s
 * @param extra extra info such as agentName in wxwork
 */
class TimelyRefreshAccessToken @JvmOverloads constructor(
    appId: String,
    refresher: IRefresher,
    refreshIntervalTime: Long = 7100000,
    extra: String? = null
) : TimelyRefreshValue(appId, refresher, refreshIntervalTime, extra), ITimelyRefreshValue {
    init {
        get() //第一次加载
    }
    /**
     * 获取accessToken，若过期则会自动刷新
     */
    override fun get() = getRefreshedValue(UpdateType.ACCESS_TOKEN)
}

/**
 * 动态提供Ticket的值
 *
 * @param appId       公众号或企业微信等申请的app id
 * @param refresher      刷新器
 * @param refreshIntervalTime 刷新时间间隔，默认7100s
 * @param extra extra info such as agentName in wxwork
 */
class TimelyRefreshTicket(
    appId: String,
    refresher: IRefresher,
    refreshIntervalTime: Long = 7100000,
    extra: String? = null
) : TimelyRefreshValue(appId, refresher, refreshIntervalTime, extra), ITimelyRefreshValue {

    init {
        get() //第一次加载
    }
    /**
     * 获取ticket，若过期则会自动刷新
     */
    override fun get() = getRefreshedValue(UpdateType.TICKET)

}
