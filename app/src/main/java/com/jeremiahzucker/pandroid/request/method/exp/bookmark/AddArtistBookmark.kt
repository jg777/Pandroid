package com.jeremiahzucker.pandroid.request.method.exp.bookmark

import com.jeremiahzucker.pandroid.request.method.Method
import com.jeremiahzucker.pandroid.request.model.SyncTokenRequestBody

/**
 * Created by Jeremiah Zucker on 8/22/2017.
 * https://6xq.net/pandora-apidoc/json/bookmarks/#bookmark-addartistbookmark
 */
object AddArtistBookmark: Method() {
    data class RequestBody(
            val trackToken: String
    ) : SyncTokenRequestBody(TokenType.USER)
}