/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.domain.sharing.shares

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.data.AppExecutors
import com.owncloud.android.data.common.NetworkBoundResource
import com.owncloud.android.data.common.Resource
import com.owncloud.android.data.sharing.shares.ShareRepository
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.data.ShareRepository
import com.owncloud.android.shares.data.datasources.LocalShareDataSource
import com.owncloud.android.shares.data.datasources.RemoteShareDataSource
import com.owncloud.android.testing.OpenForTesting
import com.owncloud.android.vo.Resource

class OCShareRepository(
    private val localSharesDataSource: com.owncloud.android.data.sharing.shares.datasources.LocalSharesDataSource,
    private val remoteSharesDataSource: com.owncloud.android.data.sharing.shares.datasources.RemoteSharesDataSource
) : ShareRepository {

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    override fun getPrivateShares(filePath: String): LiveData<Resource<List<OCShareEntity>>> {
        return getShares(
            filePath,
            listOf(
                ShareType.USER,
                ShareType.GROUP,
                ShareType.FEDERATED
            )
        )
    }

    override fun insertPrivateShare(
        filePath: String,
        shareType: ShareType?,
        shareeName: String,     // User or group name of the target sharee.
        permissions: Int        // See https://doc.owncloud.com/server/developer_manual/core/apis/ocs-share-api.html
    ): LiveData<Resource<Unit>> {

        if (shareType != ShareType.USER && shareType != ShareType.GROUP && shareType != ShareType.FEDERATED) {
            throw IllegalArgumentException("Illegal share type $shareType");
        }

        return insertShare(
            filePath = filePath,
            shareType = shareType,
            shareWith = shareeName,
            permissions = permissions
        )
    }

    override fun updatePrivateShare(remoteId: Long, permissions: Int): LiveData<Resource<Unit>> {
        return updateShare(
            remoteId = remoteId,
            permissions = permissions
        )
    }

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    override fun getPublicShares(filePath: String): LiveData<Resource<List<OCShareEntity>>> {
        return getShares(
            filePath,
            listOf(ShareType.PUBLIC_LINK)
        )
    }

    override fun insertPublicShare(
        filePath: String,
        permissions: Int,
        name: String,
        password: String,
        expirationTimeInMillis: Long,
        publicUpload: Boolean
    ): LiveData<Resource<Unit>> {
        return insertShare(
            filePath = filePath,
            shareType = ShareType.PUBLIC_LINK,
            permissions = permissions,
            name = name,
            password = password,
            expirationTimeInMillis = expirationTimeInMillis,
            publicUpload = publicUpload
        )
    }

    override fun updatePublicShare(
        remoteId: Long,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean
    ): LiveData<Resource<Unit>> {
        return updateShare(
            remoteId,
            permissions,
            name,
            password,
            expirationDateInMillis,
            publicUpload
        )
    }

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    private fun getShares(
        filePath: String,
        shareTypes: List<ShareType>
    ): MutableLiveData<Resource<List<OCShare>>> {
        return object : NetworkBoundResource<List<OCShare>, ShareParserResult>(appExecutors) {
            override fun saveCallResult(item: ShareParserResult) {
                val sharesFromServer = item.shares.map { remoteShare ->
                    OCShare.fromRemoteShare(remoteShare).also { it.accountOwner = accountName }
                }

                if (sharesFromServer.isEmpty()) {
                    localShareDataSource.deleteSharesForFile(filePath, accountName)
                }

                localShareDataSource.replaceShares(sharesFromServer)
            }

            override fun shouldFetchFromNetwork(data: List<OCShare>?) = true

            override fun loadFromDb(): LiveData<List<OCShare>> =
                localShareDataSource.getSharesAsLiveData(
                    filePath, accountName, shareTypes
                )

            override fun createCall() =
                remoteShareDataSource.getShares(filePath, reshares = true, subfiles = false)
        }.asMutableLiveData()
    }

    override fun getShare(remoteId: Long): LiveData<OCShare> {
        return localShareDataSource.getShareAsLiveData(remoteId)
    }

    private fun insertShare(
        filePath: String,
        shareType: ShareType,
        shareWith: String = "",
        permissions: Int,
        name: String = "",
        password: String = "",
        expirationTimeInMillis: Long = RemoteShare.INIT_EXPIRATION_DATE_IN_MILLIS,
        publicUpload: Boolean = false
    ): LiveData<Resource<Unit>> {
        val result = MutableLiveData<Resource<Unit>>()
        result.postValue(Resource.loading())

        appExecutors.networkIO().execute {
            // Perform network operation
            val remoteOperationResult = remoteShareDataSource.insertShare(
                filePath,
                shareType,
                shareWith,
                permissions,
                name,
                password,
                expirationTimeInMillis,
                publicUpload
            )

            if (remoteOperationResult.isSuccess) {
                val newShareFromServer = remoteOperationResult.data.shares.map { remoteShare ->
                    OCShare.fromRemoteShare(remoteShare).also { it.accountOwner = accountName }
                }
                localShareDataSource.insert(newShareFromServer)
                result.postValue(Resource.success())
            } else {
                notifyError(result, remoteOperationResult)
            }
        }
        return result
    }

    private fun updateShare(
        remoteId: Long,
        permissions: Int,
        name: String = "",
        password: String? = "",
        expirationDateInMillis: Long = RemoteShare.INIT_EXPIRATION_DATE_IN_MILLIS,
        publicUpload: Boolean = false
    ): LiveData<Resource<Unit>> {
        val result = MutableLiveData<Resource<Unit>>()
        result.postValue(Resource.loading())

        appExecutors.networkIO().execute {
            // Perform network operation
            val remoteOperationResult = remoteShareDataSource.updateShare(
                remoteId,
                name,
                password,
                expirationDateInMillis,
                permissions,
                publicUpload
            )

            if (remoteOperationResult.isSuccess) {
                val updatedShareForFileFromServer = remoteOperationResult.data.shares.map { remoteShare ->
                    OCShareEntity.fromRemoteShare(remoteShare)
                        .also { it.accountOwner = accountName }
                }
                localShareDataSource.update(updatedShareForFileFromServer.first())
                result.postValue(Resource.success())
            } else {
                notifyError(result, remoteOperationResult)
            }
        }
        return result
    }

    override fun deleteShare(
        remoteId: Long
    ): LiveData<Resource<Unit>> {
        val result = MutableLiveData<Resource<Unit>>()

        result.postValue(Resource.loading())

        // Perform network operation
        appExecutors.networkIO().execute {
            // Perform network operation
            val remoteOperationResult = remoteShareDataSource.deleteShare(remoteId)

            if (remoteOperationResult.isSuccess) {
                localShareDataSource.deleteShare(remoteId)
                result.postValue(Resource.success()) // Used to close the share edition dialog
            } else {
                result.postValue(
                    Resource.error(
                        remoteOperationResult.code,
                        msg = remoteOperationResult.httpPhrase,
                        exception = remoteOperationResult.exception
                    )
                )
            }
        }
        return result
    }

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    private fun getShares(
        filePath: String,
        shareTypes: List<ShareType>
    ): MutableLiveData<Resource<List<OCShareEntity>>> {
        return object :
            NetworkBoundResource<List<OCShareEntity>, ShareParserResult>(appExecutors) {
            override fun saveCallResult(item: ShareParserResult) {
                val sharesForFileFromServer = item.shares.map { remoteShare ->
                    OCShareEntity.fromRemoteShare(remoteShare)
                        .also { it.accountOwner = accountName }
                }

                if (sharesForFileFromServer.isEmpty()) {
                    localSharesDataSource.deleteSharesForFile(filePath, accountName)
                }

                localSharesDataSource.replaceShares(sharesForFileFromServer)
            }

            override fun shouldFetchFromNetwork(data: List<OCShareEntity>?) = true

            override fun loadFromDb(): LiveData<List<OCShareEntity>> =
                localSharesDataSource.getSharesAsLiveData(
                    filePath, accountName, shareTypes
                )

            override fun createCall() =
                remoteSharesDataSource.getShares(filePath, reshares = true, subfiles = false)
        }.asMutableLiveData()
    }

    /**
     * Notify error in the given LiveData
     *
     * @param result liveData in which notify the error
     * @param remoteOperationResult contains the information of the error
     */
    private fun notifyError(
        result: MutableLiveData<Resource<Unit>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ) {
        result.postValue(
            Resource.error(
                remoteOperationResult.code,
                msg = remoteOperationResult.httpPhrase,
                exception = remoteOperationResult.exception
            )
        )
    }
}
