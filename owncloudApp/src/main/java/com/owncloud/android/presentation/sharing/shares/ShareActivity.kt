/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2019 ownCloud GmbH.
 *
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.presentation.sharing.shares

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.owncloud.android.R
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.operations.RemoveShareOperation
import com.owncloud.android.operations.UpdateSharePermissionsOperation
import com.owncloud.android.presentation.UIResult.Status
import com.owncloud.android.presentation.capabilities.OCCapabilityViewModel
import com.owncloud.android.presentation.sharing.sharees.SearchShareesFragment
import com.owncloud.android.presentation.sharing.sharees.UsersAndGroupsSearchProvider
import com.owncloud.android.presentation.sharing.shares.fragment.EditShareFragment
import com.owncloud.android.presentation.sharing.shares.fragment.PublicShareDialogFragment
import com.owncloud.android.presentation.sharing.shares.fragment.ShareFileFragment
import com.owncloud.android.presentation.sharing.shares.fragment.ShareFragmentListener
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.dialog.RemoveShareDialogFragment
import com.owncloud.android.ui.utils.showDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * Activity for sharing files
 */
class ShareActivity : FileActivity(), ShareFragmentListener {
    /**
     * Shortcut to get access to the [ShareFileFragment] instance, if any
     *
     * @return A [ShareFileFragment] instance, or null
     */
    private val shareFileFragment: ShareFileFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_SHARE_FRAGMENT) as ShareFileFragment

    /**
     * Shortcut to get access to the [SearchShareesFragment] instance, if any
     *
     * @return A [SearchShareesFragment] instance, or null
     */
    private val searchShareesFragment: SearchShareesFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_SEARCH_FRAGMENT) as SearchShareesFragment?

    /**
     * Shortcut to get access to the [PublicShareDialogFragment] instance, if any
     *
     * @return A [PublicShareDialogFragment] instance, or null
     */
    private val publicShareFragment: PublicShareDialogFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_PUBLIC_SHARE_DIALOG_FRAGMENT) as PublicShareDialogFragment?

    /**
     * Shortcut to get access to the [EditPrivateShareFragment] instance, if any
     *
     * @return A [EditPrivateShareFragment] instance, or null
     */
    private val editPrivateShareFragment: EditPrivateShareFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_EDIT_SHARE_FRAGMENT) as EditPrivateShareFragment?

    private val ocShareViewModel: OCShareViewModel by viewModel {
        parametersOf(
            file.remotePath,
            account
        )
    }

    private val ocCapabilityViewModel: OCCapabilityViewModel by viewModel {
        parametersOf(
            account
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.share_activity)

        // Set back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val ft = supportFragmentManager.beginTransaction()

        if (savedInstanceState == null && file != null && account != null) {
            // Add Share fragment on first creation
            val fragment = ShareFileFragment.newInstance(file, account!!)
            ft.replace(
                R.id.share_fragment_container, fragment,
                TAG_SHARE_FRAGMENT
            )
            ft.commit()
        }
    }

    override fun onNewIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEARCH -> {  // Verify the action and get the query
                val query = intent.getStringExtra(SearchManager.QUERY)
                Log_OC.w(TAG, "Ignored Intent requesting to query for $query")
            }
            UsersAndGroupsSearchProvider.suggestIntentAction -> {
                val data = intent.data
                val dataString = intent.dataString
                val shareWith = dataString!!.substring(dataString.lastIndexOf('/') + 1)
                createPrivateShare(
                    shareWith,
                    data?.authority
                )
            }
            else -> Log_OC.e(TAG, "Unexpected intent $intent")
        }
    }

    public override fun onStop() {
        super.onStop()
    }

    /**
     * Updates the view, reading data from [com.owncloud.android.data.viewmodel.OCShareViewModel]
     */
    private fun refreshSharesFromStorageManager() {
        val editShareFragment = editShareFragment
        if (editShareFragment?.isAdded == true) {
            editShareFragment.refreshUiFromDB()
        }
    }

    override fun refreshAllShares() {
        refreshCapabilities()
        refreshPrivateShares()
        observePublicShares()
    }

    override fun refreshCapabilities(shouldFetchFromNetwork: Boolean) {
//        ocCapabilityViewModel.getCapabilityForAccount(shouldFetchFromNetwork).observe(
//            this,
//            Observer { resource ->
//                when (resource?.status) {
//                    SUCCESS -> {
//                        if (publicShareFragment != null) {
//                            publicShareFragment?.updateCapabilities(resource.data)
//                        } else {
//                            shareFileFragment?.updateCapabilities(resource.data)
//                        }
//                        dismissLoadingDialog()
//                    }
//                    ERROR -> {
//                        val errorMessage = ErrorMessageAdapter.getResultMessage(
//                            resource.code,
//                            resource.exception,
//                            OperationType.GET_CAPABILITIES,
//                            resources
//                        )
//                        if (publicShareFragment != null) {
//                            publicShareFragment?.showError(errorMessage)
//                        } else {
//                            Snackbar.make(findViewById(android.R.id.content), errorMessage, Snackbar.LENGTH_SHORT)
//                                .show()
//                            shareFileFragment?.updateCapabilities(resource.data)
//                        }
//                        dismissLoadingDialog()
//                    }
//                    LOADING -> {
//                        showLoadingDialog(R.string.common_loading)
//                        if (publicShareFragment != null) {
//                            publicShareFragment?.updateCapabilities(resource.data)
//                        } else {
//                            shareFileFragment?.updateCapabilities(resource.data)
//                        }
//                    }
//                    else -> {
//                        Log.d(TAG, "Unknown status when loading capabilities in account ${account?.name}")
//                    }
//                }
//            }
//        )
    }

    /**************************************************************************************************************
     *********************************************** PRIVATE SHARES ***********************************************
     **************************************************************************************************************/

    override fun refreshPrivateShares() {
        ocShareViewModel.privateShares.observe(
            this,
            Observer { uiResult ->
                when (uiResult?.status) {
                    Status.SUCCESS -> {
                        updatePrivateSharesInFileFragment(uiResult.data)
                        updatePrivateSharesInSearchShareesFragment(uiResult.data)
                        dismissLoadingDialog()
                    }
                    Status.ERROR -> {
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            uiResult.errorMessage!!,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        updatePrivateSharesInFileFragment(uiResult.data)
                        updatePrivateSharesInSearchShareesFragment(uiResult.data)
                        dismissLoadingDialog()
                    }
                    Status.LOADING -> {
                        showLoadingDialog(R.string.common_loading)
                        shareFileFragment?.updatePrivateShares(uiResult.data as ArrayList<OCShareEntity>)
                    }
                    else -> {
                        Log.d(
                            TAG, "Unknown status when loading private shares for file ${file?.fileName} in account" +
                                    "${account?.name}"
                        )
                    }
                }
            }
        )
    }

    private fun updatePrivateSharesInFileFragment(privateShares: List<OCShareEntity>?) {
        if (shareFileFragment != null && shareFileFragment!!.isAdded) {
            shareFileFragment?.updatePrivateShares(privateShares as ArrayList<OCShareEntity>)
        }
    }

    private fun updatePrivateSharesInSearchShareesFragment(privateShares: List<OCShareEntity>?) {
        if (searchShareesFragment != null && searchShareesFragment!!.isAdded) {
            searchShareesFragment?.updatePrivateShares(privateShares as ArrayList<OCShareEntity>)
        }
    }

    override fun refreshPrivateShare(remoteId: Long) {
        ocShareViewModel.getPrivateShare(remoteId).observe(
            this,
            Observer { updatedShare ->
                editPrivateShareFragment?.updateShare(updatedShare)
            }
        )
    }

    override fun showSearchUsersAndGroups() {
        val searchFragment = SearchShareesFragment.newInstance(file, account)
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(
            R.id.share_fragment_container, searchFragment,
            TAG_SEARCH_FRAGMENT
        )
        ft.addToBackStack(null)    // BACK button will recover the ShareFragment
        ft.commit()
    }

    private fun createPrivateShare(shareeName: String, dataAuthority: String?) {
        val shareType = UsersAndGroupsSearchProvider.getShareType(dataAuthority)

//        ocShareViewModel.insertPrivateShare(
//            file.remotePath,
//            shareType,
//            shareeName,
//            getAppropiatePermissions(shareType)
//        ).observe(
//            this,
//            Observer { resource ->
//                when (resource?.status) {
//                    Status.ERROR -> {
//                        val errorMessage = ErrorMessageAdapter.getResultMessage(
//                            resource.code,
//                            resource.exception,
//                            OperationType.CREATE_SHARE_WITH_SHAREES,
//                            resources
//                        )
//                        Snackbar.make(
//                            findViewById(android.R.id.content),
//                            errorMessage,
//                            Snackbar.LENGTH_SHORT
//                        ).show()
//                    }
//                    Status.LOADING -> {
//                        showLoadingDialog(R.string.common_loading)
//                    }
//                }
//            }
//        )
    }

    private fun getAppropiatePermissions(shareType: ShareType?): Int {
        // check if the Share is FERERATED
        val isFederated = ShareType.FEDERATED == shareType

        if (file.isSharedWithMe) {
            return RemoteShare.READ_PERMISSION_FLAG    // minimum permissions

        } else if (isFederated) {
            val serverVersion = com.owncloud.android.authentication.AccountUtils.getServerVersion(account)
            return if (serverVersion != null && serverVersion.isNotReshareableFederatedSupported) {
                if (file.isFolder)
                    RemoteShare.FEDERATED_PERMISSIONS_FOR_FOLDER_AFTER_OC9
                else
                    RemoteShare.FEDERATED_PERMISSIONS_FOR_FILE_AFTER_OC9
            } else {
                if (file.isFolder)
                    RemoteShare.FEDERATED_PERMISSIONS_FOR_FOLDER_UP_TO_OC9
                else
                    RemoteShare.FEDERATED_PERMISSIONS_FOR_FILE_UP_TO_OC9
            }
        } else {
            return if (file.isFolder)
                RemoteShare.MAXIMUM_PERMISSIONS_FOR_FOLDER
            else
                RemoteShare.MAXIMUM_PERMISSIONS_FOR_FILE
        }
    }

    override fun showSearchUsersAndGroups() {
        val searchFragment = SearchShareesFragment.newInstance(file, account)
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(
            R.id.share_fragment_container, searchFragment,
            TAG_SEARCH_FRAGMENT
        )
        ft.addToBackStack(null)    // BACK button will recover the ShareFragment
        ft.commit()
    }

    override fun showEditPrivateShare(share: OCShareEntity) {
        val ft = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag(TAG_EDIT_SHARE_FRAGMENT)
        if (prev != null) {
            ft.remove(prev)    // BACK button will recover the previous fragment
        }
        ft.addToBackStack(null)

        // Create and show the dialog.
        val newFragment = EditShareFragment.newInstance(share, file, account)
        newFragment.show(
            ft,
            TAG_EDIT_SHARE_FRAGMENT
        )
    }

    override fun updatePrivateShare(remoteId: Long, permissions: Int) {
        ocShareViewModel.updatePrivateShare(
            remoteId,
            permissions
        ).observe(
            this,
            Observer { resource ->
                when (resource?.status) {
                    Status.ERROR -> {
                        val errorMessage: String = resource.msg ?: ErrorMessageAdapter.getResultMessage(
                            resource.code,
                            resource.exception,
                            OperationType.UPDATE_SHARE,
                            resources
                        )
                        editPrivateShareFragment?.refreshUiFromState()
                        editPrivateShareFragment?.showError(errorMessage)
                        dismissLoadingDialog()
                    }
                    Status.LOADING -> {
                        showLoadingDialog(R.string.common_loading)
                    }
                    else -> {
                        Log.d(TAG, "Unknown status when updating private share with remote id $remoteId")
                    }
                }
            }
        )
    }

    override fun copyOrSendPrivateLink(file: OCFile) {
        fileOperationsHelper.copyOrSendPrivateLink(file)
    }

    /**************************************************************************************************************
     *********************************************** PUBLIC SHARES ************************************************
     **************************************************************************************************************/

    private fun observePublicShares() {
        ocShareViewModel.publicShares.observe(
            this,
            Observer { uiResult ->
                when (uiResult?.status) {
                    Status.SUCCESS -> {
                        shareFileFragment?.updatePublicShares(uiResult.data as ArrayList<OCShareEntity>)
                        dismissLoadingDialog()
                    }
                    Status.ERROR -> {
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            uiResult.errorMessage!!,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        shareFileFragment?.updatePublicShares(uiResult.data as ArrayList<OCShareEntity>)
                        dismissLoadingDialog()
                    }
                    Status.LOADING -> {
                        showLoadingDialog(R.string.common_loading)
                        shareFileFragment?.updatePublicShares(uiResult.data as ArrayList<OCShareEntity>)
                    }
                    else -> {
                        Log.d(
                            TAG, "Unknown status when loading public shares for file ${file?.fileName} in account" +
                                    "${account?.name}"
                        )
                    }
                }
            }
        )
    }

    override fun showAddPublicShare(defaultLinkName: String) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.

        // Create and show the dialog
        val createPublicShareFragment = PublicShareDialogFragment.newInstanceToCreate(
            file,
            defaultLinkName
        )

        showDialogFragment(
            createPublicShareFragment,
            TAG_PUBLIC_SHARE_DIALOG_FRAGMENT
        )
    }

    override fun createPublicShare(
        permissions: Int,
        name: String,
        password: String,
        expirationTimeInMillis: Long,
        publicUpload: Boolean
    ) {
//        ocShareViewModel.insertPublicShare(
//            file.remotePath,
//            permissions,
//            name,
//            password,
//            expirationTimeInMillis,
//            publicUpload
//        ).observe(
//            this,
//            Observer { resource ->
//                when (resource?.status) {
//                    Status.SUCCESS -> {
//                        publicShareFragment?.dismiss()
//                        Log_OC.d("TESTS", "Closing share creation dialog")
//                    }
//                    Status.ERROR -> {
//                        val errorMessage: String = resource.msg ?: ErrorMessageAdapter.getResultMessage(
//                            resource.code,
//                            resource.exception,
//                            OperationType.CREATE_PUBLIC_SHARE,
//                            resources
//                        );
//                        publicShareFragment?.showError(errorMessage)
//                        dismissLoadingDialog()
//                    }
//                    Status.LOADING -> {
//                        showLoadingDialog(R.string.common_loading)
//                    }
//                    else -> {
//                        Log.d(
//                            TAG, "Unknown status when creating public share with name ${name} \" +" +
//                                    "from account ${account?.name}"
//                        )
//                    }
//                }
//            }
//        )
    }

    override fun showEditPublicShare(share: OCShareEntity) {
        // Create and show the dialog.
        val editPublicShareFragment = PublicShareDialogFragment.newInstanceToUpdate(file, share)
        showDialogFragment(
            editPublicShareFragment,
            TAG_PUBLIC_SHARE_DIALOG_FRAGMENT
        )
    }

    override fun updatePublicShare(
        remoteId: Long,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean
    ) {
//        ocShareViewModel.updatePublicShareForFile(
//            remoteId,
//            name,
//            password,
//            expirationDateInMillis,
//            permissions,
//            publicUpload
//        ).observe(
//            this,
//            Observer { resource ->
//                when (resource?.status) {
//                    Status.SUCCESS -> {
//                        publicShareFragment?.dismiss()
//                    }
//                    Status.ERROR -> {
//                        val errorMessage: String = resource.msg ?: ErrorMessageAdapter.getResultMessage(
//                            resource.code,
//                            resource.exception,
//                            OperationType.UPDATE_PUBLIC_SHARE,
//                            resources
//                        );
//                        publicShareFragment?.showError(errorMessage)
//                        dismissLoadingDialog()
//                    }
//                    Status.LOADING -> {
//                        showLoadingDialog(R.string.common_loading)
//                    }
//                    else -> {
//                        Log.d(
//                            TAG, "Unknown status when updating public share with name ${name} " +
//                                    "from account ${account?.name}"
//                        )
//                    }
//                }
//            }
//        )
    }

    override fun showRemovePublicShare(share: OCShareEntity) {
        val removePublicShareFragment = RemoveShareDialogFragment.newInstance(share)
        showDialogFragment(
            removePublicShareFragment,
            TAG_REMOVE_SHARE_DIALOG_FRAGMENT
        )
    }

    override fun removePublicShare(share: OCShareEntity) {
//        ocShareViewModel.deletePublicShare(share.remoteId).observe(
//            this,
//            Observer { resource ->
//                when (resource?.status) {
//                    Status.SUCCESS -> {
//                        dismissLoadingDialog()
//                    }
//                    Status.ERROR -> {
//                        val errorMessage = ErrorMessageAdapter.getResultMessage(
//                            resource.code,
//                            resource.exception,
//                            OperationType.REMOVE_SHARE,
//                            resources
//                        )
//                        Snackbar.make(findViewById(android.R.id.content), errorMessage, Snackbar.LENGTH_SHORT).show()
//                        dismissLoadingDialog()
//                    }
//                    Status.LOADING -> {
//                        showLoadingDialog(R.string.common_loading)
//                    }
//                    else -> {
//                        Log.d(
//                            TAG, "Unknown status when removing public share with name ${share.name} " +
//                                    "from account ${account?.name}"
//                        )
//                    }
//                }
//            }
//        )
    }

    override fun copyOrSendPublicLink(share: OCShareEntity) {
        fileOperationsHelper.copyOrSendPublicLink(share)
    }

    /**
     * Updates the view associated to the activity after the finish of some operation over files
     * in the current account.
     *
     * @param operation Removal operation performed.
     * @param result    Result of the removal.
     */
    override fun onRemoteOperationFinish(operation: RemoteOperation<*>, result: RemoteOperationResult<*>) {
        super.onRemoteOperationFinish(operation, result)

        if (operation is RemoveShareOperation && result.isSuccess && editPrivateShareFragment != null) {
            supportFragmentManager.popBackStack()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval = true
        when (item.itemId) {
            android.R.id.home -> if (!supportFragmentManager.popBackStackImmediate()) {
                finish()
            }
            else -> retval = super.onOptionsItemSelected(item)
        }
        return retval
    }

    companion object {
        private val TAG = ShareActivity::class.java.simpleName

        const val TAG_SHARE_FRAGMENT = "SHARE_FRAGMENT"
        const val TAG_SEARCH_FRAGMENT = "SEARCH_USER_AND_GROUPS_FRAGMENT"
        const val TAG_EDIT_SHARE_FRAGMENT = "EDIT_SHARE_FRAGMENT"
        const val TAG_PUBLIC_SHARE_DIALOG_FRAGMENT = "PUBLIC_SHARE_DIALOG_FRAGMENT"
        const val TAG_REMOVE_SHARE_DIALOG_FRAGMENT = "REMOVE_SHARE_DIALOG_FRAGMENT"
    }
}
