/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package com.cwuom.ouo.bridge;

import static com.cwuom.ouo.HostInfo.requireMinQQVersion;

import android.util.Log;

import com.cwuom.ouo.Initiator;
import com.cwuom.ouo.bridge.kernelcompat.ContactCompat;
import com.cwuom.ouo.bridge.kernelcompat.KernelMsgServiceCompat;
import com.cwuom.ouo.bridge.ntapi.MsgServiceHelper;
import com.cwuom.ouo.util.AppRuntimeHelper;
import com.cwuom.ouo.util.Logger;
import com.cwuom.ouo.util.QAppUtils;
import com.cwuom.ouo.util.QQVersion;
import com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService;
import com.tencent.qqnt.kernel.nativeinterface.MsgAttributeInfo;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgAvatarPendant;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgBubble;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgElement;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgFont;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgIceBreak;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgNamePlate;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Nt_kernel_bridge {

    public static void send_msg(ContactCompat contact, ArrayList<MsgElement> elements) {
        HashMap<Integer, MsgAttributeInfo> attrMap = new HashMap<>();
        MsgAttributeInfo info = getDefaultAttributeInfo();
        if (info != null) {
            attrMap.put(0, info);

            try {
                KernelMsgServiceCompat service = MsgServiceHelper.getKernelMsgService(AppRuntimeHelper.getAppRuntime());
                long msgUniqueId;
                if (requireMinQQVersion(QQVersion.QQ_9_0_30)) {
                    msgUniqueId = service.generateMsgUniqueId(contact.getChatType(), QAppUtils.getServiceTime());
                } else {
                    msgUniqueId = service.getMsgUniqueId(QAppUtils.getServiceTime());
                }
                service.sendMsg(msgUniqueId, contact, elements, attrMap, (i2, str) -> {
                });
            } catch (Exception e) {
                Logger.e("Nt_kernel_bridge.send_msg", e);
            }
        }
    }

    public static MsgAttributeInfo getDefaultAttributeInfo() {

        VASMsgNamePlate plate = new VASMsgNamePlate(258, 64, 0, 0, 0, 0, 258, 0, new ArrayList<>(), 0, 0);
        VASMsgBubble bubble = new VASMsgBubble(0, 0, 0, 0);
        VASMsgFont font = new VASMsgFont(65536, 0L, 0, 0, 0);
        VASMsgAvatarPendant pendant = new VASMsgAvatarPendant();
        VASMsgIceBreak iceBreak = new VASMsgIceBreak(null, null);
        VASMsgElement element = new VASMsgElement(plate, bubble, pendant, font, iceBreak);
        try {
            Class<?> msgAttributeInfoClazz = Initiator.load("com.tencent.qqnt.kernel.nativeinterface.MsgAttributeInfo");
            if (msgAttributeInfoClazz != null) {
                for (Constructor<?> constructor : msgAttributeInfoClazz.getDeclaredConstructors()) {
                    if (constructor.getParameterTypes().length < 1) continue;
                    List<Object> args = new ArrayList<>();
                    args.add(0);
                    args.add(0);
                    args.add(element);
                    for (int i = 0; i < constructor.getParameterTypes().length - 3; i++) {
                        args.add(null);
                    }
                    return (MsgAttributeInfo) constructor.newInstance(args.toArray());
                }
            }
        } catch (Throwable e) {
            Logger.e(e);
        }
        return null;
    }
}
