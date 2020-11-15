/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 14:37
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

package com.github.rwsbillyang.wxSDK.officialAccount

import com.github.rwsbillyang.wxSDK.IBase
import com.github.rwsbillyang.wxSDK.Response
import com.github.rwsbillyang.wxSDK.bean.Menu
import com.github.rwsbillyang.wxSDK.bean.Menus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * 自定义菜单最多包括3个一级菜单，每个一级菜单最多包含5个二级菜单。
 * 一级菜单最多4个汉字，二级菜单最多7个汉字，多出来的部分将会以“...”代替。
 * 创建自定义菜单后，菜单的刷新策略是，在用户进入公众号会话页或公众号profile页时，
 * 如果发现上一次拉取菜单的请求在5分钟以前，就会拉取一下菜单，如果菜单有更新，就会刷新客户端的菜单。
 * 测试时可以尝试取消关注公众账号后再次关注，则可以看到创建后的效果.
 * */
object MenuApi: OABaseApi() {
    override val group = "menu"

    /**
     * 创建普通菜单
     *
     * https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Creating_Custom-Defined_Menu.html
     * */
    fun create(menus: List<Menu>): Response = doPost("create", Menus(menus))

    /**
     * 获取自定义菜单配置
     * https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Getting_Custom_Menu_Configurations.html
     *
     * TODO：另一个查询接口get_current_selfmenu_info 未实现, 可以获取默认菜单和全部个性化菜单信息 sub_button中有一个list字段
     *
     * https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Creating_Custom-Defined_Menu.html
     * */
    fun detail(): ResponseMenusDetail = doGet("get")

    /**
     *
     * 删除所有菜单, 包括默认菜单和全部个性化菜单
     *
     * https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Creating_Custom-Defined_Menu.html
     * */
    fun delete(): Response = doGet("delete")

    /**
     * 创建个性化菜单
     * 让公众号的不同用户群体看到不一样的自定义菜单。该接口开放给已认证订阅号和已认证服务号
     *
     * 用户标签（开发者的业务需求可以借助用户标签来完成）
     * 性别
     * 手机操作系统
     * 地区（用户在微信客户端设置的地区）
     * 语言（用户在微信客户端设置的语言）
     *
     * 个性化菜单的更新是会被覆盖的。 例如公众号先后发布了默认菜单，个性化菜单1，个性化菜单2，个性化菜单3。
     * 那么当用户进入公众号页面时，将从个性化菜单3开始匹配，如果个性化菜单3匹配成功，则直接返回个性化菜单3，
     * 否则继续尝试匹配个性化菜单2，直到成功匹配到一个菜单。 根据上述匹配规则，为了避免菜单生效时间的混淆，
     * 决定不予提供个性化菜单编辑API，开发者需要更新菜单时，需将完整配置重新发布一轮。
     *
     * https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Personalized_menu_interface.html
     * */
    fun createConditional(menus: List<Menu>, matchRule: MatchRule): ResponseMenuId = doPost("addconditional",
        ConditionalMenus(menus, matchRule) )

    /**
     * 删除个性化菜单
     * @param menuId 来自createConditional的返回值
     *
     * https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Personalized_menu_interface.html
     * */
    fun deleteConditional(menuId: String): Response = doPost("delconditional", MenuId(menuId))

    /**
     * 测试个性化菜单匹配结果
     * @param  user_id 可以是粉丝的OpenID，也可以是粉丝的微信号。
     * */
    fun tryMatch(userId: String): Response = doPost("trymatch",  mapOf("user_id" to userId))
}


@Serializable
class ConditionalMenus(
        @SerialName("button")
        val menus: List<Menu>,
        @SerialName("matchrule")
        val matchRule: MatchRule
)

@Serializable
class MenuId(@SerialName("menuid") val menuId: String)

@Serializable
class ResponseMenuId(
        @SerialName("errcode")
        override val errCode: Int = 0,
        @SerialName("errmsg")
        override val errMsg: String? = null,
        @SerialName("menuid")
        val menuId: String): IBase

@Serializable
class ResponseMenusDetail(
    @SerialName("errcode")
        override val errCode: Int = 0,
    @SerialName("errmsg")
        override val errMsg: String? = null,
    @SerialName("menu")
        val menus: MenuItem? = null,
    @SerialName("conditionalmenu")
        val conditionalMenu: MenuItem?= null
): IBase
/**
 * @param matchRule 为空则表示默认菜单，否则未个性菜单
 * */
@Serializable
class MenuItem(
        @SerialName("button")
        val menus: List<Menu>,
        @SerialName("menuid")
        val menuId: String? = null,
        @SerialName("matchrule")
        val matchRule: MatchRule? = null
)


/**
 * matchrule共七个字段，均可为空，但不能全部为空，至少要有一个匹配信息是不为空的。
 * country、province、city组成地区信息，将按照country、province、city的顺序进行验证，要符合地区信息表的内容。
 * 地区信息从大到小验证，小的可以不填，即若填写了省份信息，则国家信息也必填并且匹配，城市信息可以不填。
 * 例如 “中国 广东省 广州市”、“中国 广东省”都是合法的地域信息，而“中国 广州市”则不合法，因为填写了城市信息但没有填写
 * 省份信息。 地区信息表 http://wximg.gtimg.com/shake_tv/mpwiki/areainfo.zip。
 *
 * @param tagId    否	用户标签的id，可通过用户标签管理接口获取
 * @param sex    否	性别：男（1）女（2），不填则不做匹配
 * @param platform    否	客户端版本，当前只具体到系统型号：IOS(1), Android(2),Others(3)，不填则不做匹配
 * @param country    否	国家信息，是用户在微信中设置的地区，具体请参考地区信息表
 * @param province    否	省份信息，是用户在微信中设置的地区，具体请参考地区信息表
 * @param city    否	城市信息，是用户在微信中设置的地区，具体请参考地区信息表
 * @param language    否	语言信息，是用户在微信中设置的语言，具体请参考语言表：
 * 1、简体中文 "zh_CN" 2、繁体中文TW "zh_TW" 3、繁体中文HK "zh_HK" 4、英文 "en" 5、印尼 "id" 6、马来 "ms"
 * 7、西班牙 "es" 8、韩国 "ko" 9、意大利 "it" 10、日本 "ja" 11、波兰 "pl" 12、葡萄牙 "pt" 13、俄国 "ru"
 * 14、泰文 "th" 15、越南 "vi" 16、阿拉伯语 "ar" 17、北印度 "hi" 18、希伯来 "he" 19、土耳其 "tr" 20、德语 "de"
 * 21、法语 "fr"
 *
    "matchrule": {
    "tag_id": "2",
    "sex": "1",
    "country": "中国",
    "province": "广东",
    "city": "广州",
    "client_platform_type": "2",
    "language": "zh_CN"
    }
 * */
@Serializable
class MatchRule(
        @SerialName("tag_id") val tagId: String? = null,
        val sex: String? = null,
        val country: String? = null,
        val province: String? = null,
        val city: String? = null,
        @SerialName("client_platform_type")
        private val platform: String? = null,
        val language: String? = null
) {
    companion object {
        const val SEX_MALE = "1"
        const val SEX_FEMALE = "2"

        const val PLATFORM_IOS = "1"
        const val PLATFORM_ANDROID = "2"
        const val PLATFORM_OTHERS = 3
    }

    init {
        require(tagId != null || sex != null || country != null || province != null ||
                city != null || platform != null || language != null) { "七个字段均可为空，但不能全部为空" }
    }
}
