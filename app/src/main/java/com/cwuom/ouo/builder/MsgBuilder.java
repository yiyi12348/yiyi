package com.cwuom.ouo.builder;

import androidx.annotation.NonNull;

import com.tencent.qqnt.kernel.nativeinterface.ArkElement;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernel.nativeinterface.TextElement;

public class MsgBuilder {
    public static MsgElement nt_build_text(String text){
        TextElement textElement = new TextElement();
        textElement.setContent(text);

        MsgElement msgElement = new MsgElement();
        msgElement.setElementType(1);
        msgElement.setTextElement(textElement);
        return msgElement;
    }

    @NonNull
    public static MsgElement nt_build_ark(@NonNull String json) {
        MsgElement msgElement = new MsgElement();
        ArkElement arkElement = new ArkElement(json,null,null);
        msgElement.setArkElement(arkElement);
        msgElement.setElementType(10);
        return msgElement;
    }
}
