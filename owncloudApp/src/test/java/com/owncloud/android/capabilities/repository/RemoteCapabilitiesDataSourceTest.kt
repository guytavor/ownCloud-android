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

package com.owncloud.android.capabilities.repository

import com.owncloud.android.data.capabilities.datasources.RemoteCapabilitiesDataSource
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.GetRemoteCapabilitiesOperation
import com.owncloud.android.lib.resources.status.RemoteCapability

class RemoteCapabilitiesDataSourceTest(private val remoteOperationResult: RemoteOperationResult<RemoteCapability>) :
    RemoteCapabilitiesDataSource {

    override fun getCapabilities(getRemoteCapabilitiesOperation: GetRemoteCapabilitiesOperation):
            RemoteOperationResult<RemoteCapability> = remoteOperationResult
}
