/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.link

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.utils.toast
import im.vector.app.features.login.LoginActivity
import im.vector.app.features.login.LoginConfig
import im.vector.app.features.permalink.PermalinkHandler
import io.reactivex.android.schedulers.AndroidSchedulers
import org.matrix.android.sdk.api.MatrixCallback
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Dummy activity used to dispatch the vector URL links.
 */
class LinkHandlerActivity : VectorBaseActivity() {

    @Inject lateinit var sessionHolder: ActiveSessionHolder
    @Inject lateinit var errorFormatter: ErrorFormatter
    @Inject lateinit var permalinkHandler: PermalinkHandler

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getLayoutRes() = R.layout.activity_progress

    override fun initUiAndData() {
        val uri = intent.data

        if (uri == null) {
            // Should not happen
            Timber.w("Uri is null")
            finish()
            return
        }

        if (uri.getQueryParameter(CONFIG_PATH_HS_PARAMETER) != null) {
            handleConfigUrl(uri)
        } else if (SUPPORTED_HOSTS.contains(uri.host)) {
            handleSupportedHostUrl(uri)
        }
    }

    private fun handleConfigUrl(uri: Uri) {
        if (sessionHolder.hasActiveSession()) {
            displayAlreadyLoginPopup(uri)
        } else {
            // user is not yet logged in, this is the nominal case
            startLoginActivity(uri)
        }
    }

    private fun handleSupportedHostUrl(uri: Uri) {
        if (!sessionHolder.hasActiveSession()) {
            startLoginActivity(uri)
            finish()
        } else {
            convertUriToPermalink(uri)?.let { permalink ->
                startPermalinkHandler(permalink)
            } ?: run {
                // Host is correct but we do not recognize path
                Timber.w("Unable to handle this uri: $uri")
                finish()
            }
        }
    }

    private fun convertUriToPermalink(uri: Uri): String? {
        val path = SUPPORTED_PATHS.find { it in uri.toString() } ?: return null
        return uri
                .toString()
                .replace(uri.host + path, "$MATRIX_TO_HOST/#")
    }

    private fun startPermalinkHandler(permalink: String) {
        permalinkHandler.launch(this, permalink, buildTask = true)
                .delay(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { isHandled ->
                    if (!isHandled) {
                        toast(R.string.permalink_malformed)
                    }
                    finish()
                }
                .disposeOnDestroy()
    }

    /**
     * Start the login screen with identity server and home server pre-filled
     */
    private fun startLoginActivity(uri: Uri) {
        val intent = LoginActivity.newIntent(this, LoginConfig.parse(uri))
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    /**
     * Propose to disconnect from a previous HS, when clicking on an auto config link
     */
    private fun displayAlreadyLoginPopup(uri: Uri) {
        AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_warning)
                .setMessage(R.string.error_user_already_logged_in)
                .setCancelable(false)
                .setPositiveButton(R.string.logout) { _, _ ->
                    sessionHolder.getSafeActiveSession()?.signOut(true, object : MatrixCallback<Unit> {
                        override fun onFailure(failure: Throwable) {
                            displayError(failure)
                        }

                        override fun onSuccess(data: Unit) {
                            Timber.d("## displayAlreadyLoginPopup(): logout succeeded")
                            sessionHolder.clearActiveSession()
                            startLoginActivity(uri)
                        }
                    }) ?: finish()
                }
                .setNegativeButton(R.string.cancel) { _, _ -> finish() }
                .show()
    }

    private fun displayError(failure: Throwable) {
        AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(failure))
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { _, _ -> finish() }
                .show()
    }

    companion object {
        private const val CONFIG_PATH_HS_PARAMETER = "hs_url"

        private val SUPPORTED_HOSTS = arrayOf("app.element.io", "riot.im")
        private val SUPPORTED_PATHS = arrayOf("/#/room", "/#/user", "/#/group")
        private const val MATRIX_TO_HOST = "matrix.to"
    }
}
