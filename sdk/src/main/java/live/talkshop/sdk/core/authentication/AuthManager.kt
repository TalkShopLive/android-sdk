package live.talkshop.sdk.core.authentication

import live.talkshop.sdk.core.show.models.ProductModel
import live.talkshop.sdk.core.show.models.ShowModel

internal var isAuthenticated: Boolean = false
internal var globalShowKey: String = ""
internal var globalShowId: String? = null
internal var currentShow: ShowModel? = null
internal var currentShowProducts: ProductModel? = null
internal var storedClientKey: String = ""
internal var isDebugMode: Boolean = false
internal var isTestMode: Boolean = false
internal var isDNT: Boolean = false