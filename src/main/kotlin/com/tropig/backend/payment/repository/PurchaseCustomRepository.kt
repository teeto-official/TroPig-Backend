package com.tropig.backend.payment.repository

import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.payment.model.request.PurchasedContentListRequest
import com.tropig.backend.payment.model.result.PurchasedContentProjection

interface PurchaseCustomRepository {

    fun findPurchasedContents(
        memberId: Long,
        request: PurchasedContentListRequest,
    ): CursorSlice<PurchasedContentProjection>
}
