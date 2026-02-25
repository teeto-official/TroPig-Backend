package com.tropig.backend.contents.model.request

import com.tropig.backend.contents.enums.ContentsStatus

data class UpdateContentStatusRequest(val status: ContentsStatus)
