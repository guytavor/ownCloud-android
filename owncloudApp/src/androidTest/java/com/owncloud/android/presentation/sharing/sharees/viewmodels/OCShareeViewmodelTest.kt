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

package com.owncloud.android.presentation.sharing.sharees.viewmodels

import android.accounts.Account
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.data.Resource
import com.owncloud.android.domain.sharing.sharees.OCShareeRepository
import com.owncloud.android.lib.resources.shares.GetRemoteShareesOperation
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.presentation.sharing.sharees.OCShareeViewModel
import com.owncloud.android.utils.AppTestUtil
import io.mockk.every
import io.mockk.mockkClass
import junit.framework.Assert.assertEquals
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OCShareeViewmodelTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private var testAccount: Account = AppTestUtil.createAccount("admin@server", "test")
    private var ocShareeRepository: OCShareeRepository = mockkClass(OCShareeRepository::class)

    @Test
    fun loadSharees() {
        val sharees = arrayListOf(
            AppTestUtil.createSharee("User", ShareType.USER.value.toString(), "user", "user@mail.com"),
            AppTestUtil.createSharee("Group", ShareType.GROUP.value.toString(), "user2", "user2@mail.com")
        )

        every {
            ocShareeRepository.getSharees(
                "User", 1, 10
            )
        } returns Resource.success(sharees)

        val ocShareeViewModel = createOCShareeViewModel(ocShareeRepository)

        val data: ArrayList<JSONObject>? = ocShareeViewModel.getSharees("User", 1, 10).data

        assertEquals(2, data?.size)

        val sharee1 = data?.get(0)
        assertEquals(sharee1?.getString(GetRemoteShareesOperation.PROPERTY_LABEL), "User")
        val value = sharee1?.getJSONObject(GetRemoteShareesOperation.NODE_VALUE)
        assertEquals(value?.getString(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE), ShareType.USER.value.toString())
        assertEquals(value?.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH), "user")
        assertEquals(value?.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH_ADDITIONAL_INFO), "user@mail.com")

        val sharee2 = data?.get(1)
        assertEquals(sharee2?.getString(GetRemoteShareesOperation.PROPERTY_LABEL), "Group")
        val value2 = sharee2?.getJSONObject(GetRemoteShareesOperation.NODE_VALUE)
        assertEquals(value2?.getString(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE), ShareType.GROUP.value.toString())
        assertEquals(value2?.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH), "user2")
        assertEquals(value2?.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH_ADDITIONAL_INFO), "user2@mail.com")
    }

    private fun createOCShareeViewModel(ocShareeRepository: OCShareeRepository): OCShareeViewModel {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return OCShareeViewModel(
            context,
            testAccount,
            ocShareeRepository
        )
    }
}
