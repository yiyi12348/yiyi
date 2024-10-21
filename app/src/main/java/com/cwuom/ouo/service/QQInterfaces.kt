package com.cwuom.ouo.service

import com.tencent.common.app.AppInterface
import com.tencent.qphone.base.remote.ToServiceMsg
import mqq.app.MobileQQ

abstract class QQInterfaces {

    companion object {
        val app = (if (PlatformUtils.isMqqPackage())
            MobileQQ.getMobileQQ().waitAppRuntime()
        else
            MobileQQ.getMobileQQ().waitAppRuntime(null)) as AppInterface

        private fun sendToServiceMsg(to: ToServiceMsg) {
            app.sendToService(to)
        }

        private fun createToServiceMsg(cmd: String): ToServiceMsg {
            return ToServiceMsg("mobileqq.service", app.currentAccountUin, cmd)
        }

        fun sendBuffer(
            cmd: String,
            isProto: Boolean,
            data: ByteArray,
        ) {
            val toServiceMsg = createToServiceMsg(cmd)
            toServiceMsg.putWupBuffer(data)
            toServiceMsg.addAttribute("req_pb_protocol_flag", isProto)
            sendToServiceMsg(toServiceMsg)
        }
    }
}