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

package com.owncloud.android.domain.capabilities

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.owncloud.android.data.capabilities.datasources.LocalCapabilitiesDataSource
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.data.Resource
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.RemoteCapability
import com.owncloud.android.domain.utils.InstantExecutors
import com.owncloud.android.domain.utils.DomainTestUtil
import com.owncloud.android.domain.utils.mock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(JUnit4::class)
class OCCapabilityRepositoryTest {
    private lateinit var ocCapabilityRepository: OCCapabilityRepository

    private val localCapabilitiesDataSource = mock(LocalCapabilitiesDataSource::class.java)

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun loadCapabilityFromNetwork() {
        val dbData = MutableLiveData<OCCapabilityEntity>()

        `when`(
            localCapabilitiesDataSource.getCapabilityForAccountAsLiveData(
                "admin@server"
            )
        ).thenReturn(
            dbData
        )

        val remoteCapability = DomainTestUtil.createRemoteCapability("admin@server")
        val remoteOperationResult = DomainTestUtil.createRemoteOperationResultMock(remoteCapability, true)
        val remoteCapabilitiesDataSource =
            RemoteCapabilitiesDataSourceTest(remoteOperationResult)

        ocCapabilityRepository =
            OCCapabilityRepository.create(
                InstantExecutors(), localCapabilitiesDataSource, remoteCapabilitiesDataSource
            )

        val data = ocCapabilityRepository.getCapabilityForAccount("admin@server")

        val observer = mock<Observer<Resource<OCCapabilityEntity>>>()
        data.observeForever(observer)

        dbData.postValue(null)

        // Get capabilities from database to observe them, is called twice (one showing current db capabilities while
        // getting capabilities from server and another one with db capabilities already updated with server ones)
        verify(localCapabilitiesDataSource, times(2)).getCapabilityForAccountAsLiveData(
            "admin@server"
        )

        // Retrieving capabilities from server...

        // Capabilities are always retrieved from server and inserted in database if not empty list
        verify(localCapabilitiesDataSource).insert(
            listOf(OCCapabilityEntity.fromRemoteCapability(remoteCapability.apply { accountName = "admin@server" }))
        )

        // Observe changes in database livedata when there's a new capability
        val newCapability = DomainTestUtil.createCapability("user@server")

        dbData.postValue(
            newCapability
        )

        verify(observer).onChanged(Resource.success(newCapability))
    }

    @Test
    fun loadEmptyCapabilityForAccountFromNetwork() {
        val dbData = MutableLiveData<OCCapabilityEntity>()

        `when`(
            localCapabilitiesDataSource.getCapabilityForAccountAsLiveData(
                "user@server"
            )
        ).thenReturn(
            dbData
        )

        val remoteOperationResult = DomainTestUtil.createRemoteOperationResultMock(RemoteCapability(), true)

        val remoteCapabilitiesDataSource =
            RemoteCapabilitiesDataSourceTest(remoteOperationResult)

        ocCapabilityRepository =
            OCCapabilityRepository.create(
                InstantExecutors(),
                localCapabilitiesDataSource,
                remoteCapabilitiesDataSource
            )

        val data = ocCapabilityRepository.getCapabilityForAccount(
            "user@server"
        )

        val observer = mock<Observer<Resource<OCCapabilityEntity>>>()
        data.observeForever(observer)

        dbData.postValue(null)

        // Get capabilities from database to observe them, is called twice (one showing current db capabilities while
        // getting capabilities from server and another one with db capabilities already updated with server ones)
        verify(localCapabilitiesDataSource, times(2)).getCapabilityForAccountAsLiveData(
            "user@server"
        )

        // Retrieving capabilities from server...

        // Observe changes in database livedata when the list of capabilities is empty
        dbData.postValue(null)

        verify(observer).onChanged(Resource.success(null))
    }

    @Test
    fun loadCapabilitiesForAccountFromNetworkWithError() {
        val dbData = MutableLiveData<OCCapabilityEntity>()

        dbData.value = null // DB does not include capabilities yet

        `when`(
            localCapabilitiesDataSource.getCapabilityForAccountAsLiveData("cfo@server")
        ).thenReturn(
            dbData
        )

        val exception = Exception("Error when retrieving capabilities")

        val remoteOperationResult = DomainTestUtil.createRemoteOperationResultMock(
            RemoteCapability(),
            false,
            resultCode = RemoteOperationResult.ResultCode.FORBIDDEN,
            exception = exception
        )

        val remoteCapabilitiesDataSourceTest =
            RemoteCapabilitiesDataSourceTest(remoteOperationResult)

        ocCapabilityRepository =
            OCCapabilityRepository.create(
                InstantExecutors(),
                localCapabilitiesDataSource,
                remoteCapabilitiesDataSourceTest
            )

        val data = ocCapabilityRepository.getCapabilityForAccount(
            "cfo@server"
        )

        // Get capabilities from database to observe them
        verify(localCapabilitiesDataSource).getCapabilityForAccountAsLiveData(
            "cfo@server"
        )

        // Retrieving capabilities from server...

        // Observe changes in database livedata when there's an error from server
        val observer = mock<Observer<Resource<OCCapabilityEntity>>>()
        data.observeForever(observer)

        verify(observer).onChanged(
            Resource.error(
                RemoteOperationResult.ResultCode.FORBIDDEN, dbData.value, exception = exception
            )
        )
    }
}
