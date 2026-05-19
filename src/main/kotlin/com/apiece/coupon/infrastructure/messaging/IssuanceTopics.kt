package com.apiece.coupon.infrastructure.messaging

object IssuanceTopics {
    const val REQUESTED = "issuance.requested"
    const val REQUESTED_DLT = "issuance.requested.DLT"
    const val CONSUMER_GROUP = "issuance-worker"
}
