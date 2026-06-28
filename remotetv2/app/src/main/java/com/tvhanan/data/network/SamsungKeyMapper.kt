package com.tvhanan.data.network

import com.tvhanan.domain.model.RemoteKey

object SamsungKeyMapper {

    private const val PAYLOAD_TEMPLATE = """{"method":"ms.remote.control","params":{"Cmd":"Click","DataOfCmd":"%s","Option":"false","TypeOfRemote":"SendRemoteKey"}}"""

    fun createKeyPressPayload(key: RemoteKey): String {
        return PAYLOAD_TEMPLATE.format(key.keyCode)
    }
}
