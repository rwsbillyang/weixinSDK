/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-09-03 22:06
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

package com.github.rwsbillyang.wxUser

/**
 * 前后端共同协调完成下列策略，后端已在ktorKit中实现
 * 获取欲望强调从0到100，越来越高
 * 后端没有发现fanInfo时将通知前端进行step2操作，获取用户授权信息
 * */
object NeedUserInfoType {
    const val Force_Not_Need = 0 // 明确不需要
    const val NeedIfNo = 1 //尽量不获取，有fan记录（不管有没有头像或昵称）就不获取，没有fan记录时则获取
    const val NeedIfNoNameOrImg = 2 //尽量不获取，有fan记录、且有头像和昵称则不获取，但没有头像或名称获取
    const val NeedByUserSettings = 3 //由后端用户配置是否获取，后端用户 由参数owner指定
    const val ForceNeed = 4 //直接进入step2，用户授权获取信息操作
}