/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-20 21:46
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

package com.github.rwsbillyang.wxWork.contacts


import com.github.rwsbillyang.ktorKit.apiBox.UmiPagination
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.ktorKit.db.MongoGenericService
import com.github.rwsbillyang.ktorKit.toObjectId
import com.github.rwsbillyang.ktorKit.util.DatetimeUtil
import com.github.rwsbillyang.wxSDK.work.DepartmentItem
import com.github.rwsbillyang.wxSDK.work.EnterSessionContext
import com.github.rwsbillyang.wxSDK.work.FollowUser
import com.github.rwsbillyang.wxWork.wxWorkModule
import kotlinx.coroutines.runBlocking
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection
import org.slf4j.LoggerFactory

class ContactService(cache: ICache) : MongoGenericService(cache) {
    private val log = LoggerFactory.getLogger("ContactService")

    private val dbSource: MongoDataSource by inject(qualifier = named(wxWorkModule.dbName!!))

    private val contactCol: CoroutineCollection<Contact> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val contactExtraCol: CoroutineCollection<ContactExtra> by lazy {
        dbSource.mongoDb.getCollection()
    }

    private val externalContactCol: CoroutineCollection<ExternalContact> by lazy {
        dbSource.mongoDb.getCollection()
    }
    private val relateChangeCol: CoroutineCollection<RelationChange> by lazy {
        dbSource.mongoDb.getCollection()
    }

    private val departmentCol: CoroutineCollection<Department> by lazy {
        dbSource.mongoDb.getCollection()
    }
    //agent允许的部门成员信息
//    private val contactsDepartmentCol: CoroutineCollection<ContactDepartment> by lazy {
//        dbSource.mongoDb.getCollection()
//    }


    fun findContact(userId: String, corpId: String) = cacheable("contact/$corpId/$userId") {
        runBlocking { contactCol.findOne( Contact::userId eq userId, Contact::corpId eq corpId) }
    }
    fun findContactByOpenId(openId: String, corpId: String) = runBlocking { contactCol.findOne( Contact::openId eq openId, Contact::corpId eq corpId) }
    fun findContactByUserId(userId: String, corpId: String) = runBlocking { contactCol.findOne( Contact::userId eq userId, Contact::corpId eq corpId) }
    fun findContact(_id: String) = runBlocking { contactCol.findOneById(_id.toObjectId()) }
    fun findContact(_id: ObjectId) = runBlocking { contactCol.findOneById(_id) }
    fun upsertContact(doc: Contact) = runBlocking {
        val old = contactCol.findOne(Contact::corpId eq doc.corpId, Contact::userId eq doc.userId)
        if(old == null){
            contactCol.insertOne(doc)
        }else{
            contactCol.replaceOneById (old._id!!, doc.apply { _id = old._id })
        }
    }
    fun updateContactOpenId(userId: String, corpId: String, openId: String)= runBlocking {
        contactCol.updateOne(and(Contact::userId eq userId, Contact::corpId eq corpId), setValue(Contact::openId, openId))
    }
    fun insertContact(doc: Contact) = runBlocking {
        contactCol.insertOne(doc)
    }
    fun updateContact(newContact: Contact, newUserId: String?) = runBlocking {
        if(newContact.userId == null){
            log.warn("newContact.userId is null, ignore updateContact")
            0L
        }
        val old = findContact(newContact.userId!!, newContact.corpId)
        newContact._id = old?._id
        if(!newUserId.isNullOrBlank()){
            newContact.userId = newUserId
        }
        contactCol.save(newContact)?.modifiedCount
    }
    fun delContact(userId: String, corpId: String) = runBlocking{
        contactCol.deleteOne(Contact::userId eq userId, Contact:: corpId eq corpId)
    }
    fun findAllContactList(filter: Bson) = runBlocking {
        contactCol.find(filter).toList()
    }
    fun findPaginationContactList(params: ContactListParams) = findPage(contactCol, params)

    fun findContactExtra(_id: String) = runBlocking { contactExtraCol.findOneById(_id.toObjectId()) }
    fun findContactExtra(_id: ObjectId) = runBlocking { contactExtraCol.findOneById(_id) }
    fun findContactArchiveList(filter: Bson) = runBlocking {
        contactExtraCol.find(filter).toList()
    }
    /**
     * 更新内部联系人（客服）的外部好友（客户）
     * */
    fun upsertContactCustomerIds(_id: String, ids: List<String>?) = runBlocking {
        contactExtraCol.updateOneById(_id.toObjectId(), setValue(ContactExtra::ids, ids), upsert())
    }
    fun upsertContactCustomerIds(corpId: String,userId: String, ids: List<String>?) = runBlocking {
        contactExtraCol.updateOne(and(ContactExtra::corpId eq corpId, ContactExtra::userId eq userId), setValue(ContactExtra::ids, ids), upsert())
    }
    fun upsertContactCustomerId(corpId: String,userId: String, externalId: String) = runBlocking {
        contactExtraCol.updateOne(and(ContactExtra::corpId eq corpId, ContactExtra::userId eq userId), addToSet(ContactExtra::ids, externalId), upsert())
    }
    fun removeContactCustomerId(corpId: String,userId: String, externalId: String) = runBlocking {
        contactExtraCol.updateOne(and(ContactExtra::corpId eq corpId, ContactExtra::userId eq userId), pull(ContactExtra::ids, externalId))
    }

    fun upsertContactCustomerArchive(id: String, archive: Int) =  runBlocking {
        contactExtraCol.updateOneById(id.toObjectId(), setValue(ContactExtra::archive, archive), upsert())
    }


    fun findExternalContact(_id: ObjectId) = runBlocking {
        externalContactCol.findOneById(_id)
    }
    fun findExternalContactByExternalId(externalId: String, corpId: String) = runBlocking {
        externalContactCol.findOne(
            and(
                ExternalContact::externalId eq externalId,
                ExternalContact::corpId eq corpId)
        )
    }
    fun findExternalContact(externalId: String, corpId: String)
    = runBlocking { externalContactCol.findOne(ExternalContact::externalId eq externalId, ExternalContact::corpId eq corpId) }
    fun upsertExternalContact(doc: ExternalContact) = runBlocking {
        //externalContactCol.replaceOne(and(ExternalContact::externalId eq doc.externalId, ExternalContact::corpId eq doc.corpId), doc, ReplaceOptions().upsert(true))
        val old = externalContactCol.findOne(ExternalContact::externalId eq doc.externalId, ExternalContact::corpId eq doc.corpId)
        if(old == null){
            externalContactCol.insertOne(doc)
        }else{
            externalContactCol.replaceOneById (old._id!!, doc.apply { _id = old._id })
        }
    }
    fun upsertExternalContactByWxkf(
        corpId: String, externalId: String,
        name: String, avatar: String?, gender: Int, unionId: String?,
    enterSessionInfo: List<EnterSessionContext>) = runBlocking {
        externalContactCol.updateOne(and(ExternalContact::externalId eq externalId, ExternalContact::corpId eq corpId),
            combine(
                set(
                    SetTo(ExternalContact::name, name),
                    SetTo(ExternalContact::avatar, avatar),
                    SetTo(ExternalContact::gender, gender),
                    SetTo(ExternalContact::unionId, unionId),
                    SetTo(ExternalContact::wxkf, true)
                ),
                pushEach(ExternalContact::enterSessions, enterSessionInfo),
               )
        , upsert())
    }
    fun findExternalContactByOpenId(openId: String, corpId: String) = runBlocking { externalContactCol.findOne(ExternalContact::openId eq openId, ExternalContact::corpId eq corpId) }
    fun updateExternalContactOpenId(userId: String, corpId: String, openId: String)= runBlocking {
        externalContactCol.updateOne(and(ExternalContact::externalId eq userId, ExternalContact::corpId eq corpId), setValue(ExternalContact::openId, openId))
    }
    //客户删除成员，即删除follow
    fun removeExternalContactFollow(corpId: String, externalUserId: String, userId: String) = runBlocking {
        externalContactCol.updateOne(and(ExternalContact::corpId eq corpId, ExternalContact::externalId eq externalUserId),
            pullByFilter(FollowUser::userid eq userId) //FollowUser::userid eq userId
        )
    }
    /**
     * 获取某个企业corpId的某个用户的userId的渠道channelId的外部客户列表
     * @param state channelId 渠道id
     * @param userId 某个用户 可能存在userId更改
     * @param corpId 用户所在企业
     * */
    fun findExternalOfState(state: String, userId: String, corpId: String) = runBlocking {
        externalContactCol.find(ExternalContact::corpId eq corpId,
            ExternalContact::followers.elemMatch(and(FollowUser::state eq state, FollowUser::userid eq userId))  ).toList()
    }
    fun findExternalListAll(filter: Bson) = runBlocking {
        externalContactCol.find(filter).toList()
    }

    fun insertExternalContacts(list: List<ExternalContact>) = runBlocking{
        externalContactCol.insertMany(list)
    }
    fun insertEnterSessionContext(externalId: String, corpId: String,enterSessionInfo: List<EnterSessionContext>) = runBlocking {
        externalContactCol.updateOne(and(ExternalContact::externalId eq externalId, ExternalContact::corpId eq corpId),
            pushEach(ExternalContact::enterSessions, enterSessionInfo))
    }

    fun findExternalListByPage(param: ExternalListParams) = findPage(externalContactCol, param)
    fun addRelationChange(createTime: Long?, corpId: String, userId: String, contactId: ObjectId?, externalId: String, type:Int,  state: String?) = runBlocking{
        val time = DatetimeUtil.format(System.currentTimeMillis() )

        //很有可能重复收到信息，使用corpId和ctime去重
        relateChangeCol.updateOne(and(RelationChange::ctime eq createTime, RelationChange::corpId eq corpId),
            set(
                SetTo(RelationChange::userId, userId),
                SetTo(RelationChange::contactId, contactId),
                SetTo(RelationChange::externalUserId, externalId),
                SetTo(RelationChange::type, type),
                SetTo(RelationChange::time, time),
                SetTo(RelationChange::ctime, createTime),
                SetTo(RelationChange::state, state),
            ), upsert() )
    }

    fun findRelationChanges(userId: String, contactId: ObjectId?, externalId: String) = runBlocking{
        if(contactId == null)
            relateChangeCol.find(RelationChange::userId eq userId, RelationChange::externalUserId eq externalId).toList()
        else
            relateChangeCol.find(RelationChange::contactId eq contactId, RelationChange::externalUserId eq externalId).toList()
    }



    fun findDepartment(corpId: String, id: Int) = runBlocking {
        departmentCol.findOne(Department::corpId eq corpId, Department::id eq id)
    }
    fun insertDepartment(doc: Department) = runBlocking {
        departmentCol.insertOne(doc)
    }
    fun upsertDepartment(corpId: String,id: Int, name: String?, parentId: Int?) = runBlocking {
        val list = mutableListOf<Bson>()
        if(!name.isNullOrBlank()) list.add(setValue(Department::name, name))
        list.add(setValue(Department::parentId, parentId))


        departmentCol.updateOne(and(Department::corpId eq corpId, Department::id eq id),
            combine(list),upsert()
        )
    }
    fun upsertDepartment(corpId: String,item: DepartmentItem) = runBlocking {
        val list = mutableListOf<Bson>()
        if(!item.name.isNullOrBlank()) list.add(setValue(Department::name, item.name))
        list.add(setValue(Department::parentId, item.parentId))
        list.add(setValue(Department::order, item.order))
        list.add(setValue(Department::nameEn, item.nameEn))
        list.add(setValue(Department::leader, item.leader))

        departmentCol.updateOne(and(Department::corpId eq corpId, Department::id eq item.id),
            combine(list),upsert()
        )
    }
    fun delDepartment(corpId: String, id: Int) = runBlocking {
        departmentCol.deleteOne(Department::corpId eq corpId, Department::id eq id)
    }



}