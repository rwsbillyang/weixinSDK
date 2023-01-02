# wxSDK
wechat(weixin) SDK for Ktor in Kotlin

## common
微信SDK中的公共库部分，主要包括：
- 微信开发中的security安全加解密包
- 对动态刷新的accessToken和jsTicket的支持
- 消息分发处理机制
- 基于ktor-cio的Http client的http请求封装

引入依赖
repositories中添加：`maven { url 'https://jitpack.io' }`:
```groovy
repositories {
			maven { url 'https://jitpack.io' }
		}
```

```groovy
implementation ("com.github.rwsbillyang.wxSDK:common:$wxSdkVersion")
```

# 用户中心（wxUser）
一个通用的Account账户体系，支持公众号、企业微信，期限权限管理等、推荐关系记录

主要用于微信开发中的用户账户中心，支持:
- 账户注册功能
- 账户微信oauth2认证登录功能（JWT Token）
- 登录统计功能
- 密码或手机号登录功能
- 密码重置功能，通过Email发送邮件
- 账户过期微信自动通知（需实现IWechatNotifier）
- 虚拟产品及其订单（服务版本，日期期限）创建
- 微信支付(支持配置)购买
- 推荐奖励
- 用户反馈

引入依赖
```groovy
implementation ("com.github.rwsbillyang.wxSDK:common:$wxSdkVersion")
implementation ("com.github.rwsbillyang.wxSDK:wxUSer:$wxSdkVersion")
```

# 公众号（officialAccount）

基本API提供了
- 支持一个运行时系统中配置多个微信公众号
- 部分公众号中的API
- 针对公众号消息分发

引入依赖
```groovy
implementation ("com.github.rwsbillyang.wxSDK:common:$wxSdkVersion")
implementation ("com.github.rwsbillyang.wxSDK:officialAccount:$wxSdkVersion")
```

## 基于ktor+MongoDB的公众号通用解决方案
- 需MongoDB
- 公众号配置
- 用户消息和事件的统计数据收录
- 对消息和事件的响应的配置
- 粉丝Fan的记录和管理
- 一个支持消息和事件记录的简单处理的handler
- 基于ktor的Entrypoint：消息接入、oauth认证、jsSDK签名等

引入依赖
```groovy
implementation ("com.github.rwsbillyang.wxSDK:common:$wxSdkVersion")
implementation ("com.github.rwsbillyang.wxSDK:officialAccount:$wxSdkVersion")
implementation ("com.github.rwsbillyang.wxSDK:wxUser:$wxSdkVersion")
implementation ("com.github.rwsbillyang.wxSDK:wxOA:$wxSdkVersion")
```

### 公众号后台配置
操作：
登录PC公众号官方后台：
1. 公众号设置 -> 功能设置 -> JS安全域名
2. 开发 -> 基本配置 -> 公众号开发信息： AppId, AppSecret， IP白名单，
服务器配置：服务器地址(URL) 域名+`/api/wx/oa/app/{appId}`  如： `http://yourdomain.com/api/wx/oa/app/wx0f92cbee09e231f9`
3. “开发 - 接口权限 - 网页服务 - 网页帐号 - 网页授权获取用户基本信息”的配置选项中，修改授权回调域名。请注意，这里填写的是域名（是一个字符串），而不是URL，因此请勿加 http:// 等协议头；


### 接收消息和事件

用于接收消息和事件，填写到微信公众号官方管理后台

**接口路径**

/api/wx/oa/app/{appId}

**请求方法**

GET POST

**请求参数**
变量名 | 字段 |	必填 | 类型 | 示例值 | 说明
---|---|---|---|----|---|---|---|---|---|---
appId |  | 是 | String |  | 公众号appId

appId经由MsgHub最终传递到MsgHandler和EventHandler的消息和事件处理函数中

get请求用于微信接入验证
post请求用于接收微信通知消息或事件

**返回结果**

结果返回给微信服务器
```
success 或 xml格式消息
```



### Oauth

用于前端请求oauth身份验证


**接口路径**

/api/wx/oa/oauth/info

**请求方法**

GET

**请求参数**
变量名 | 字段 |	必填 | 类型 | 示例值 | 说明
---|---|---|---|----|---|---|---|---|---|---
appId |  | 否 | String |  | 公众号appId，多应用需提供
owner |  | 否 | String |  | 系统注册用户的id，ObjectId字符串
openId |  | 否 | String |  | 微信用户openId
host |  | 否 | String |  | 如`www.example.com`

 前端webapp根据本地缓存信息，判断用户是否已有登录信息；若未登录，前端请求`/api/wx/oauth/info`，获取appId、state，redirect_uri等信息


**返回结果**


```kotlin
/**
 * @param appId	appid 是 公众号appId or corpId or suiteId(ISV)
 * @param redirectUri	redirect_uri 是	授权后重定向的回调链接地址， 使用 urlEncode 链接进行处理过
 * @param scope	是	应用授权作用域，snsapi_base （不弹出授权页面，直接跳转，只能获取用户openid），snsapi_userinfo （弹出授权页面，可通过openid拿到昵称、性别、所在地。并且， 即使在未关注的情况下，只要用户授权，也能获取其信息 ）
 * @param state	否	重定向后会带上state参数，开发者可以填写a-zA-Z0-9的参数值，最多128字节
 * @param agentId 企业自建应用且需要获取用户信息时需要，其它情况为null
 * */
@Serializable
class OAuthInfo(
        val appId: String,
        val redirectUri: String,
        val scope: String,
        val state: String,
        val agentId: Int?,
        var authorizeUrl: String? = null
)
```



用户授权后，微信将通知后端`/api/wx/oa/oauth/notify1/{appId}`，并附带上code和state参数，

当不能明确需要是否获取用户信息时，调用此API，上传owner参数(来自于待访问的资源如news路径中的属主信息)，用于与notify1中获取的openId决定是否需要获取访客信息。owner用于获取用户设置，openId用于查询是否已经有访客信息

后端根据owner和openId判断是否需要获取访问者用户信息（可由系统用户配置），并以重定向跳转的方式通知到前端。是否需要访问者信息，需由sdk使用者提供一个lambda函数。

若需获取用户信息，前端还需再跳转到用户授权链接。用户允许授权后，将通知后端`/api/wx/oa/oauth/notify2/{appId}`，后端获取的用户信息由sdk使用者提供的lambda函数处理



前端获取信息后，可以自行拼接url，也可直接使用authorizeUrl：
     `https://open.weixin.qq.com/connect/oauth2/authorize?appid=APPID&redirect_uri=REDIRECT_URI&response_type=code&scope=SCOPE&state=STATE#wechat_redirect`
 

通知前端oauth结果的前端路径默认是：`/wxoa/authNotify`，若前端是SPA，则可能需前面添加`#!`

前端应在`/wxoa/authNotify`中解析url，获取后端给予的登录信息。

当失败时：`/wxoa/authNotify?state=STATE&code=KO&msg=nullCodeOrState`
code为KO
msg为错误信息
state为“前端准备认证信息”中获取的state，只有一致时才表示此通知是可信任的。

当成功时：
`/wxoa/authNotify?state=STATE&code=OK&openId=OPENID&unionId=UNIONID`
code为OK
state为“前端准备认证信息”中获取的state，只有一致时才表示此通知是可信任的。
openId 用户的openID
unionId 用户的unionID，可能无此信息

前端将这些信息保存到sessionStorage或localStorage中，若是admin权限登录，前端还需进一步通过openId 或unionId进行admin用户登录获取用户uId和role等信息。


注意：前端应保存好state值，后面会用到。



### js-sdk签名

**接口路径**

/api/wx/oa/jssdk/signature

**请求方法**

GET

**请求参数**
变量名 | 字段 |	必填 | 类型 | 示例值 | 说明
---|---|---|---|----|---|---|---|---|---|---
appId |  | 否 | String |  | 公众号appId，多应用需提供
url |  | 否 | String |  |当前的页面路径，不提供则使用Referer信息

url需注意前端SPA单应用路径，以及ios的兼容性


**返回结果**


```kotlin
/**
 *
 * @param agentId 当企业微信中需要调用agentConfig进行注入时才提供，否则为空
 * */
@Serializable
class JsApiSignature(
    val appId: String,
    val nonceStr: String,
    val timestamp: Long,
    val signature: String,
    val agentId: Int?
)
```


### 自定义路径

```kotlin
object OfficialAccount {
    /**
     * 微信消息接入点"/api/wx/oa/app/{appId}"
     * */
    var msgUri = "/api/wx/oa/app"
    /**
     * 前端获取api签名信息，重定向到请求腾讯授权页面
     * */
    var oauthInfoPath: String = "/api/wx/oa/oauth/info"
    /**
     * 用户授权后的通知路径
     * */
    var oauthNotifyPath: String = "/api/wx/oa/oauth/notify"
    /**
     * 授权后通知前端的授权结果路径
     * */
    var oauthNotifyWebAppUrl: String = "/wxoa/authNotify"
}
```


### 消息处理
通过对事件的解析处理，然后分发，当不支持的类型时，将调用onDispatch进行处理，若其返回null，将继续调用onDefault进行处理。

### 被动回复消息
参见：https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Passive_user_reply_message.html
在收到微信推送过来的消息和事件后，可以回复消息，这些消息以名称形如：Rexxx

- ReTextMsg 文本消息 文本内容支持超链接
- ReImgMsg 图片消息
- ReVoiceMsg 语音消息
- ReVideoMsg 视频消息
- ReNewsMsg 图文消息
- ReMusicMsg 音乐消息
- ReTransferMsg 转到客服的消息



### 群发消息/客服消息

以下消息适合于群发和客服
- TextMsg 群发和客服 之 文本消息
- VoiceMsg 群发和客服 之 语音消息
- VideoMsg 群发和客服 之 视频消息
- CardMsg 群发和客服 之 卡券消息
- MpNewsMsg 群发和客服 之 图文消息

以下适合于客服：
- CustomerImgMsg 客服 之 图片消息
- CustomerNewsMsg 客服图文消息（点击跳转到外链）
- CustomerMusicMsg 客服music消息
- CustomerMenuMsg 客服 之 菜单消息 通常用于问卷调查
- CustomerMiniProgramMsg  客服小程序消息





### api调用

包括如下API
- CustomerServiceApi 客服APi
- JsAPI 获取js-sdk所需的签名JsApiSignature
- MaterialApi 永久素材管理
- MediaApi 临时素材管理
- MenuApi 菜单管理
- MsgApi 群发消息及模板消息
- OAuthApi 微信认证管理
- QrCodeApi 二维码管理
- TemplateApi 管理模板消息
- UserApi 用户管理 获取用户信息，支持批量获取

### 内部实现
#### token的自动刷新

- ITimelyRefreshValue

不管是accessToken还是js ticket等，在配置时，可以提供一个具备ITimelyRefreshValue的接口实例，该接口只需一个get函数，返回其值即可。
```kotlin
/**
 * 动态刷新获取的值
 * */
interface ITimelyRefreshValue {
    fun get(): String?
}
```
因此，测试环境中，提供一个该接口的实现，直接返回恒定的字符串即可。

在默认实现中，会检查值是否有效，有效的话（或正在刷新）则直接返回当前值，否则刷新后返回。
```kotlin

/**
 * 通过提供一个refresher并在一定得间隔时间内进行刷新获取某个值
 *
 * 值更新后会发出更新通知
 * */
open class TimelyRefreshValue @JvmOverloads constructor(
    val appId: String,
    private val refresher: IRefresher,
    private var refreshIntervalTime: Long
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


    /**
     *
     * 判断优先顺序：
     * 1.官方给出的超时时间是7200秒，这里用refreshIntervalTime秒来做，防止出现已经过期的情况
     * 2.刷新标识判断，如果正在刷新，则也直接跳过，避免多次重复刷新；如果没有正在刷新且已过期，则开始刷新
     */
    fun tryRefreshValue(updateType: UpdateType): String? {
        val now = System.currentTimeMillis()
        val delta: Long = now - timelyValue.time
        try {
            if (delta > refreshIntervalTime
                && refreshingFlag.compareAndSet(false, true)
            ) {
                timelyValue.value = refresher.refresh()
                timelyValue.time = System.currentTimeMillis()

                refreshingFlag.set(false)
                setChanged()
                notifyObservers(
                    UpdateMsg(
                        appId,
                        updateType,
                        timelyValue.value,
                        timelyValue.time
                    )
                )
            }
        } catch (e: Exception) {
            log.error("fail to refresh: ${e.message}")
            refreshingFlag.set(false)
        }
        return timelyValue.value
    }
}


/**
 * 动态提供accessToken的值
 *
 * @param appId       公众号或企业微信等申请的app id
 * @param refresher      刷新器
 * @param refreshIntervalTime 刷新时间间隔，默认7100s
 *
 */
class TimelyRefreshAccessToken @JvmOverloads constructor(
    appId: String,
    refresher: IRefresher,
    refreshIntervalTime: Long = 7100000
) : TimelyRefreshValue(appId, refresher, refreshIntervalTime), ITimelyRefreshValue {
    init {
        get() //第一次加载
    }
    /**
     * 获取accessToken，若过期则会自动刷新
     */
    override fun get() = tryRefreshValue(UpdateType.ACCESS_TOKEN)
}

/**
 * 动态提供Ticket的值
 *
 * @param appId       公众号或企业微信等申请的app id
 * @param refresher      刷新器
 * @param refreshIntervalTime 刷新时间间隔，默认7100s
 *
 */
class TimelyRefreshTicket(
    appId: String,
    refresher: IRefresher,
    refreshIntervalTime: Long = 7100000
) : TimelyRefreshValue(appId, refresher, refreshIntervalTime), ITimelyRefreshValue {

    init {
        get() //第一次加载
    }
    /**
     * 获取ticket，若过期则会自动刷新
     */
    override fun get() = tryRefreshValue(UpdateType.TICKET)

}

```



- Refresher

另一个是刷新器IRefresher
```kotlin
/**
 * 刷新器，刷新后得到某个值
 * */
interface IRefresher{
    /**
     * 刷新后返回新值
     * */
    fun refresh(): String
}
```

其一个默认实现是通过提供一个远程url地址，以及一个key字符串（用于从远程请求返回的Response中提取值）。下面的实现专门针对weixin的api接口：
```kotlin
/**
 *  默认的的请求刷新器，如刷新获取 accessToken，ticket等
 *
 *  https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Get_access_token.html
 *  https://work.weixin.qq.com/api/doc/90000/90135/91039
 *
 *  @param urlProvider 提供刷新的url
 *  @param key 提取结果的key 如： access_token，ticket 等
 * */
open class Refresher(private val urlProvider: IUrlProvider, private val key: String): IRefresher
{
    companion object {
        private val log: Logger = LoggerFactory.getLogger("Refresher")
    }

    /**
     * 向远程发出请求，获取最新值然后返回
     */
    override  fun refresh(): String {
        val url = urlProvider.url()
        log.debug("to refresh for key=$key...,url=$url")

        return runBlocking {
            val text: String = client.get<HttpResponse>(url).readText()
            log.debug("got text: $text")
            val jsonElement = apiBox.parseToJsonElement(text)
            if(jsonElement is JsonObject){
                val valueElement = jsonElement[key]
                if(valueElement == null || valueElement is JsonNull)
                {
                    log.error("fail refresh key=$key, because: $text")
                    throw WxException("fail refresh key=$key")
                }else{
                    val value = valueElement.jsonPrimitive.content
                    log.debug("got value=$value for key=$key")
                    value
                }
            }else{
                throw WxException("fail refresh key=$key, not a jsonObject: $text")
            }
        }
    }
}



/**
 *  请求刷新accessToken，如用于公众号、企业微信等获取accessToken
 * */
class AccessTokenRefresher(urlProvider: IUrlProvider): Refresher(urlProvider,"access_token")

/**
 * 请求刷新Ticket，如用于公众号获取jsTicket
 * */
class TicketRefresher(urlProvider: IUrlProvider): Refresher(urlProvider,"ticket")
```


- accessToken等的实现

具有时效性的accessToken和jsTicket具有有效期时效性，过期后需要动态刷新。有两种方案：1.动态周期性地主动刷新；2. 当有API调用时检查是否过期，若过期则刷新。 本sdk中采用第2种方案。

不管是accessToken还是js ticket，都被抽象成一个具有时间戳的值：
```kotlin
@Serializable
open class TimelyValue(
    var value: String? = null,
    var time: Long = 0L
)
```
这个值可以被序列化，然后可以跨webapp示例发送出去，故添加了注解@Serializable


#### API的实现

- 抽象类Api

定义了get和post请求，方便使用。这些get和post请求，传参和返回值，即支持map类型，又支持泛型T。因大量接口，需大量定义自己的POJO类，故还保留了map类型的支持，在全部使用POJO类作为参数和返回类型后，可以不再使用map类型盛装参数和返回结果数据。

- 抽象类WxApi

定义了抽象字段base、group和accessToken()函数。base和group用于拼接api的url，base代表了基本的主机域名，比如公众号和企业微信的base分别为：“https://api.weixin.qq.com/cgi-bin”和“https://qyapi.weixin.qq.com/cgi-bin”；group则代表了api分组，是api url的一部分。这样只有在每个get或post请求中指定api的名称，就可以自动拼接成api的url。 accessToken()函数则用于返回qpi请求所需的acessToken值。


- api基类

比如公众号api的基类如下：
```kotlin
abstract class OABaseApi : WxApi(){
    override val base = "https://api.weixin.qq.com/cgi-bin"
    override fun accessToken() = _OA.accessToken.get()
}
```
提供了base。各api以group组织在一起，每个api类提供group值。token则使用全局的_OA中的配置。

TODO： 若需支持多公众号，则需要改造SDK，某些地方需要一个map实现，map的key为appId


#### 消息事件的接收处理

解析接收的消息或事件，然后进行分发

- xml消息

腾讯提供的某些接口，是xml格式，格式庞杂不规范。这些消息POJO类通过toXml和fromXml手工方式进行解析和生成。

在weixin API中有部分接口采用xml数据，并且某些数据时在CDATA之中，如：
```xml
<xml>
  <ToUserName><![CDATA[toUser]]></ToUserName>
  <FromUserName><![CDATA[fromUser]]></FromUserName>
  <CreateTime>1348831860</CreateTime>
  <MsgType><![CDATA[text]]></MsgType>
  <Content><![CDATA[this is a test]]></Content>
  <MsgId>1234567890123456</MsgId>
</xml>
```
A CDATA section is "a section of element content that is marked for the parser to interpret as only character data, not markup."

亦即CDATA之中的数据不被解释转译。



# 企业微信

基本API提供了：
- 支持一个运行时系统中配置多个企业微信
- 部分企业微信中的API
- 针对企业微信的消息分发
- 部分企业微信中的API（包括会话文档、第三方开发ISV）
- 基于ktor的Entrypoint：消息接入、oauth认证、jsSDK签名等

```groovy
implementation ("com.github.rwsbillyang.wxSDK:common:$wxSdkVersion")
implementation ("com.github.rwsbillyang.wxSDK:work:$wxSdkVersion")
```

## 基于ktor+MongoDB的通用解决方案

- 需MongoDB
- 企业微信可见范围成员的登录（可见性判断），登录时同步登陆者个人及其客户信息（需在后台设置将agent设置到可调用列表）
- 首次启动时agent信息自动同步：其可见成员及客户信息同步，支持管理员手工同步
- 内部成员Contact和外部联系人ExternalContact管理
- ISV配置

引入依赖
```groovy
implementation ("com.github.rwsbillyang.wxSDK:common:$wxSdkVersion")
implementation ("com.github.rwsbillyang.wxSDK:work:$wxSdkVersion")
implementation ("com.github.rwsbillyang.wxSDK:wxUser:$wxSdkVersion")
implementation ("com.github.rwsbillyang.wxSDK:wxWork:$wxSdkVersion")
```

### 关于secret

- 不同的api可能属于不同的agent，需要维护着自己的secret和accessToken。access_token是企业后台去企业微信的后台获取信息时的重要票据，由corpid和secret产生。所有接口在通信时都需要携带此信息用于验证接口的访问权限
- 基础应用secret。某些基础应用（如“审批”“打卡”应用）通讯录管理、外部联系人管理、审批、打卡等基础应用是企业微信默认集成的应用，可以直接开启并拿到secret。支持通过API进行操作。在管理后台->“应用与小程序”->“应用->”“基础”，点进某个应用，点开“API”小按钮，即可看到。
- 如果企业需要开发自定义的应用，进入“企业应用”页面，在“自建应用”栏点击“创建应用”，完成应用的添加和配置，详细步骤请参见应用概述。自建应用secret，在管理后台->“应用与小程序”->“应用”->“自建”，点进某个应用，即可看到。
- 通讯录管理secret。通讯录同步相关接口，可以对部门、成员、标签等通讯录信息进行查询、添加、修改、删除等操作。使用通讯录管理接口，原则上需要使用 通讯录管理secret，也可以使用 应用secret。但是使用应用secret只能进行“查询”、“邀请”等非写操作，而且只能操作应用可见范围内的通讯录。在“管理工具”-“通讯录同步”里面查看（需开启“API接口同步”）；
- 客户联系管理secret。企业内的员工可以添加外部企业的联系人进行工作沟通，外部联系人分为企业微信联系人和微信联系人两种类型。
配置了客户联系功能的成员所添加的外部联系人为企业客户。在“客户联系”栏，点开“API”小按钮，此为外部联系人secret。如需使用自建应用调用外部联系人相关的接口，需要在“可调用应用”中进行配置。


对于无agentId的系统级别的secret的配置，使用WorkBaseApi.KeyXXX作为key来标识其secret和accessToken，对于拥有agentId的自建应用，则使用AgentId.toString作为key。



### 配置
最先需要指定的是哪种模式：
```
fun Application.module(testing: Boolean = false) {
    
    //需预置好数据库中的企业微信配置
    Work.isIsv = false
    Work.isMulti = true
    OAuthInfo.schema = "https" //与数据库中的配置中的各host保持一致
    
    ...
}
 

```

- 企业微信基础应用accessToken
对于一些基础应用的api调用，如通讯录，需要配置额外的secret才能使用更高权限

可以如下配置：
```
//高权限accessToken对应的字符串key，可任意指定，同时需要将其赋值给对应的api的sysAccessTokenKey字段
fun config(key: String, secret: String)

//预置的key，也可以使用其它任意key。自定义key，需要给对应的api的赋值，如contactsApi.sysAccessTokenKey = key
object SysAccessTokenKey{
    const val Contact = "Contact"
    const val ExternalContact = "ExternalContact"
    const val ChatArchive = "ChatArchive"
}
```
上述内置的key，无需再给对应的api赋值
 
 ```
 //如从数据库中读取key-secret的map后：
 systemAccessTokenKeyMap?.forEach { (k, v) ->  WorkSingle.config(k, v)}

//multi
 systemAccessTokenKeyMap?.forEach { (k, v) ->  WorkMulti.config(corpId, k, v)}
 ```



### 接收消息和事件

内建应用接收消息的路径：`/api/wx/work/msg/${corpId}/${agentId}`
第三方应用接收消息的路径：`/api/wx/work/isv/msg/{suiteId}`

需要填写到企业微信官方管理后台中


### OAuth

内建和第三方应用请求oauthInfo路径：


**接口路径**

- 内建单应用：`/api/wx/work/oauth/info`
- 内建多应用：`/api/wx/work/oauth/info?corpId=${corpId}&agentId=${agentId}`
- 第三方单应用：`/api/wx/work/oauth/info`
- 第三方多应用：`/api/wx/work/oauth/info?suiteId=${suiteId}`

**请求方法**

GET

**请求参数**
变量名 | 字段 |	必填 | 类型 | 示例值 | 说明
---|---|---|---|----|---|---|---|---|---|---
corpId |  | 否 | String |  | 多应用需提供，前端可从应用主页url路径参数中获取，然后传递给后端
agentId |  | 否 | Int |  | 多应用需提供，前端可从应用主页url路径参数中获取，然后传递给后端
suiteId |  | 否 | String |  | 第三方多应用需提供，前端可从应用主页url路径参数中获取，然后传递给后端
scope |  | 否 | Int |  |  0， 1， 2 分别对应：snsapi_base, snsapi_userinfo, snsapi_privateinfo 默认2
host |  | 否 | String |  | 如"www.example.com"

corpId和agentId经由WorkMsgHub最终传递到MsgHandler和EventHandler的消息和事件处理函数中
suiteId经由WorkMsgHub最终传递到SuiteHandler的消息和事件处理函数中

返回结果：
```kotlin
/**
 * @param appId	appid 是 公众号appId or corpId or suiteId(ISV)
 * @param redirectUri	redirect_uri 是	授权后重定向的回调链接地址， 使用 urlEncode 链接进行处理过
 * @param scope	是	应用授权作用域，snsapi_base （不弹出授权页面，直接跳转，只能获取用户openid），snsapi_userinfo （弹出授权页面，可通过openid拿到昵称、性别、所在地。并且， 即使在未关注的情况下，只要用户授权，也能获取其信息 ）
 * @param state	否	重定向后会带上state参数，开发者可以填写a-zA-Z0-9的参数值，最多128字节
 * @param agentId 企业自建应用且需要获取用户信息时需要，其它情况为null
 * */
class OAuthInfo(
        val appId: String,
        val redirectUri: String,
        val scope: String,
        val state: String,
        val agentId: Int?,
        var authorizeUrl: String? = null
)
```
其中回调地址redirectUri为，用户授权后腾讯通知到回调地址，并附带上code和state.

内建应用：`/api/wx/work/oauth/notify/{corpId?}/{agentId?}`
第三方应用： `/api/wx/work/isv/oauth/notify/{suiteId?}`
其中单应用没有corpId、agentId、suiteId等值。


2. 通知到前端
上面的腾讯通知结果被重定向到前端，将oauth结果通知到前端。
前端必须定义一个路径为：`/wxwork/authNotify`  or `/#!/wxwork/authNotify`
当前端为SPA单页应用时，分隔符通常为"#!"，可以通过Work.browserHistorySeparator进行配置，默认"#!"
如前端不需要这种分隔符，可以设置为空：`Work.browserHistorySeparator=""`

通知结果形式：
```
/#!/wxwork/authNotify?state=STATE&code=OK&UserId=USERID&corpId=CORPID&agentId=AGENTID&suiteId=SUITEID
/#!/wxwork/authNotify?state=STATE&code=KO&msg=ERRORMSG
```
原始代码：
```kotlin
val params = listOf(
                    Pair("code", "OK"),
                    Pair("state", state),
                    Pair("corpId", res.corpId?:corpId),
                    Pair("corpId", res.corpId?:corpId),
                    Pair("userId", res.userId),
                    Pair("externalUserId", res.externalUserId),
                    Pair("openId", res.openId),
                    Pair("deviceId", res.deviceId),
                    Pair("agentId", agentId?.toString()),
                    Pair("suiteId", suiteId),
                )
                    .filter{ !it.second.isNullOrBlank() }
                    .joinToString("&"){
                        "${it.first}=${it.second}"
                    }

                url = "$url?$params"
```

### jsSignature
用于内建和第三方应用使用js-SDK时的注入签名

**接口路径**

 第三方应用：`/api/wx/work/jssdk/signature?suiteId=XXX&type=agent_config`
 内建应用：`/api/wx/work/jssdk/signature?corpId=XXX&agentId=AgentId&type=agent_config`

 
 **请求方法**

GET

**请求参数**
变量名 | 字段 |	必填 | 类型 | 示例值 | 说明
---|---|---|---|----|---|---|---|---|---|---
corpId |  | 否 | String |  | 多应用需提供
agentId |  | 否 | Int |  | 多应用需提供
suiteId |  | 否 | String |  | 第三方多应用需提供
type |  | 否 | String | agent_config  |  

其中type参数表示前端需要agentConfig调用，suiteId、corpId和agentId参数为多应用时需要提供；
 
 正常的返回结果：

```
DataBox("OK", null, JsApiSignature)

class JsApiSignature(
    val appId: String,
    val nonceStr: String,
    val timestamp: Long,
    val signature: String,
    val agentId: Int?
)
```
 
### 外部发起应用授权

**接口路径**

/api/wx/work/isv/oauth/outside/{suiteId} 

 
 **请求方法**

GET

**请求参数**
变量名 | 字段 |	必填 | 类型 | 示例值 | 说明
---|---|---|---|----|---|---|---|---|---|---
suiteId |  | 否 | String |  | 第三方多应用需提供

 



### 消息和事件的分发处理
TODO

###  渠道小助手
渠道小助手用于生成特定的二维码或小程序联系方式，识别出添加我们联系方式的客户是通过哪个渠道带来的。

首页路径：/#!/admin/channel/list?corpId=${corpId}&agentId=${agentId}`

#### 主要功能
- 我的渠道管理： 渠道列表、添加渠道、修改渠道信息、删除渠道
- 可为每个渠道生成独特的二维码（或小程序联系方式）

#### 功能需求
- 用户OAuth登录，识别出coprId和userId，以及用户个人信息；权限外用户引导用户开通
- 渠道管理：新增渠道、渠道列表、渠道信息修改，渠道删除
- 生成渠道二维码（及小程序联系方式），支持保存到相册（名称区分）、分享出去
- 我的客户管理：渠道带来的客户清单(TODO)
- 支持将渠道绑定到特定联系人(TODO)

#### 企业微信后台配置
内建应用，管理员进入企业微信管理后台：
1. corpId:"我的企业"->"企业信息"->"企业ID"
2. 应用信息: "应用管理" -> "应用"->"创建应用"，创建完成后，进入该应用：
- 记录下AgentId, secret；
- 设置可见范围（即哪些人可使用）；
- 应用主页设置为：`http://yourDomain.com/#!/admin/channel/list?corpId=${corpId}&agentId=${agentId}`
- 接收消息-> API接收消息中：URL设置为：`http://yourDomain.com/api/wx/work/msg/{YourCorpId}/{YourAgentId}`, 同时随机生成Token和EncodingAESKey，记录下它们。
- 网页授权及JS-SDK：可信域名都添加为：yourDomain.com

3. 配置可使用"客户联系"功能的成员: “客户联系”->“权限配置”->“使用范围”页面，管理员设置哪些人可使用"联系客户"功能，如未配置，则无法调用后文提到的相关接口。
4. 配置可使用"客户联系"接口的应用: “客户联系”->“客户”->点开“API”小按钮-> （“可调用应用”中添加新创建的应用 and 应用级别"设置了接收API消息"） =
   （ 点开“API”小按钮后：“接收事件服务器”配置，进入配置页面，要求填写URL、Token 当服务人员添加或者删除了外部联系人时，将以事件的形式推送到指定url: `http://yourdomain.com/api/wx/work/msg/{YourCorpId}/{YourAgentId}`）



### 会话内容存档
首先调用“获取会话内容存档开启成员列表（https://work.weixin.qq.com/api/doc/90000/90135/91614）”获取客服列表，
再针对每个客服调用“获取客户列表（https://work.weixin.qq.com/api/doc/90000/90135/92113）”去获取其客户，
最后可以调用“获取会话内容（https://work.weixin.qq.com/api/doc/90000/90135/91774）”获取会话记录。

中间可以通过相关API获取客服和客户的详情信息。


调用check_single_agree获取客服与客户会话授权同意情况

chat msg archive
采用RSA加密算法进行加密处理后base64 encode的内容，加密内容为企业微信产生。RSA使用PKCS1。

配置消息解密的公钥和私钥，管理后台中配置公钥，自己的server端使用私钥，其中私钥公钥生成过程如下：
> openssl genrsa -out app_private_key.pem 2048 # 私钥的生成 pkcs＃1格式而不是pkcs＃8
> openssl rsa -in app_private_key.pem -pubout -out app_public_key.pem #导出公钥

Java本身不支持读取pkcs＃1密钥，需要使用bouncycastle库读取，或直接使用pkcs＃8，转换成pkcs＃8如下：
> openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in app_private_key.pem -out app_private_key_pkcs8.pem




# 微信支付

支持APIv3
```groovy
implementation ("com.github.rwsbillyang.wxSDK:common:$wxSdkVersion")
implementation ("com.github.rwsbillyang.wxSDK:wxPay:$wxSdkVersion")
```


# 小程序

待完善

```groovy
implementation ("com.github.rwsbillyang.wxSDK:common:$wxSdkVersion")
implementation ("com.github.rwsbillyang.wxSDK:wxUser:$wxSdkVersion")
implementation ("com.github.rwsbillyang.wxSDK:wxMini:$wxSdkVersion")
```

# 前端wxlogin

基于Framework7 react+ typeScript的微信web app样板程序，参见：https://github.com/rwsbillyang/wxWebAppBoilerPlate

