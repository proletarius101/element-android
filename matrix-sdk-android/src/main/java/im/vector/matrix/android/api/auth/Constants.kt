/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.api.auth

/**
 * Path to use when the client does not supported any or all login flows
 * Ref: https://matrix.org/docs/spec/client_server/latest#login-fallback
 * */
const val LOGIN_FALLBACK_PATH = "/_matrix/static/client/login/"

/**
 * Path to use when the client does not supported any or all registration flows
 * Not documented
 */
const val REGISTER_FALLBACK_PATH = "/_matrix/static/client/register/"
