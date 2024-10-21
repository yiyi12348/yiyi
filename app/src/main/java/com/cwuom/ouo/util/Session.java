package com.cwuom.ouo.util;

import static com.cwuom.ouo.HookInit.cPeerUID;
import static com.cwuom.ouo.HookInit.cChatType;

import com.cwuom.ouo.Initiator;
import com.cwuom.ouo.bridge.kernelcompat.ContactCompat;
import com.cwuom.ouo.reflex.XField;
import com.tencent.qqnt.kernel.nativeinterface.Contact;

public class Session {
    public static String getCurrentPeerID() {
        if (cPeerUID == null) {
            throw new IllegalStateException("cPeerUID is null");
        }
        return cPeerUID.toString();
    }
    public static int getCurrentChatType() {
        if (cChatType == -1) {
            throw new IllegalStateException("cChatType is null");
        }
        return cChatType;
    }

    public static ContactCompat getContact() {
        try {
            ContactCompat contact = new ContactCompat();
            contact.setPeerUid(getCurrentPeerID());

            int chatType = getCurrentChatType();
            contact.setChatType(chatType);

//            if (chatType == 4){
//                contact.setGuildId(getCurrentGuildIDByAIOContact(AIOContact));
//            }
            return contact;

        } catch (Exception e){
            Logger.e("Session.getContact", e);
            return null;
        }

    }
}
