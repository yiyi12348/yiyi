package com.cwuom.ouo.creator;

import static com.cwuom.ouo.builder.MsgBuilder.nt_build_text;
import static com.cwuom.ouo.util.Session.getContact;
import static com.cwuom.ouo.util.Session.getCurrentChatType;
import static com.cwuom.ouo.util.Session.getCurrentPeerID;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cwuom.ouo.R;
import com.cwuom.ouo.bridge.Nt_kernel_bridge;
import com.cwuom.ouo.bridge.kernelcompat.ContactCompat;
import com.cwuom.ouo.helper.ElemHelperKt;
import com.cwuom.ouo.util.CommonContextWrapper;
import com.cwuom.ouo.util.Logger;
import com.cwuom.ouo.hooks.Toasts;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.core.BottomPopupView;
import com.lxj.xpopup.util.XPopupUtils;
import com.tencent.qqnt.kernel.nativeinterface.ArkElement;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressLint("ResourceType")
public class ElementSender extends BottomPopupView {
    private final List<String> presetelems; // 存储预设元素的列表
    private final Map<String, String> elemContentMap; // 存储元素名称和内容的映射
    private final SharedPreferences sharedPreferences;
    private EditText editText;
    private static String preContent;
    private static Dialog elem_dialog = null;
    private static View decorView;

    public ElementSender(@NonNull Context context) {
        super(context);
        sharedPreferences = context.getSharedPreferences("OUO_Presetelems", Context.MODE_PRIVATE);
        presetelems = loadPresetElem();
        elemContentMap = loadElemContentMap();
    }

    public static void createView(Activity activity, Context context, String content) {
        preContent =  content;
        Context fixContext = CommonContextWrapper.createAppCompatContext(context);
        XPopup.Builder NewPop = new XPopup.Builder(fixContext).moveUpToKeyboard(true).isDestroyOnDismiss(true);
        NewPop.maxHeight((int) (XPopupUtils.getScreenHeight(context) * .7f));
        NewPop.popupHeight((int) (XPopupUtils.getScreenHeight(context) * .63f));


        decorView = activity.getWindow().getDecorView();
        animateBlurEffect(decorView);

        BasePopupView popupView = NewPop.asCustom(new ElementSender(fixContext));
        popupView.show();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate() {
        super.onCreate();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            RadioGroup mRgSendType = findViewById(R.id.rg_send_type);
            editText = findViewById(R.id.content);
            TextView tvTarget = findViewById(R.id.tv_target);
            Button btnSend = findViewById(R.id.btn_send);
            Button btnCustom = findViewById(R.id.btn_custom);

            editText.clearFocus();
            editText.setVisibility(VISIBLE);
            editText.setText(preContent);

            try {
                mRgSendType.check(R.id.rb_element);
                editText.setHint("Raw(array)...");
                JSONObject jsonObject = new JSONObject(preContent);
                try {
                    jsonObject.get("app");
                    mRgSendType.check(R.id.rb_ark);
                    editText.setHint("Json...");
                } catch (JSONException ignored) {}
            } catch (Exception e) {
                if (!Objects.equals(preContent, "")){
                    try {
                        new JSONArray(preContent);
                    } catch (JSONException ex) {
                        editText.setHint("纯文本...");
                        mRgSendType.check(R.id.rb_text);
                    }
                }
            }

            int chat_type = getCurrentChatType();
            if (chat_type == 1) {
                tvTarget.setText("当前会话: " + getCurrentPeerID() + " | " + "好友");
            } else if (chat_type == 2) {
                tvTarget.setText("当前会话: " + getCurrentPeerID() + " | " + "群聊");
            } else {
                tvTarget.setText("当前会话: " + getCurrentPeerID() + " | " + "未知");
            }

            btnCustom.setOnClickListener(v -> {
                showCustomElemDialog();
            });

            final RadioButton[] rb = {findViewById(mRgSendType.getCheckedRadioButtonId())};



            mRgSendType.setOnCheckedChangeListener((group, checkedId) -> {
                rb[0] = findViewById(mRgSendType.getCheckedRadioButtonId());
                String send_type = rb[0].getText().toString();
                switch (send_type){
                    case "element":
                        editText.setHint("Raw(array)...");
                        break;
                    case "ark":
                        editText.setHint("Json...");
                        break;
                    case "text":
                        editText.setHint("纯文本...");
                        break;
                }
            });

            btnSend.setOnClickListener(v -> {
                String text = editText.getText().toString();
                String send_type = rb[0].getText().toString();
                ContactCompat contactCompat = getContact();
                if (send_type.equals("ark")) {
                    try {
                        send_ark_msg(text, contactCompat);
                    } catch (JSONException e) {
                        Toasts.error(getContext(), "JSON语法错误");
                    }

                    return;
                } else if (send_type.equals("text")){
                    send_text_msg(text, contactCompat);

                    return;
                }

                try {
                    if (chat_type == 1) {
                        ElemHelperKt.sendMessage(text, getCurrentPeerID(), false, send_type);
                    } else if (chat_type == 2) {
                        ElemHelperKt.sendMessage(text, getCurrentPeerID(), true, send_type);
                    } else {
                        Toasts.error(getContext(), "失败");
                        return;
                    }
                    Toasts.success(getContext(), "请求成功");
                    dialog.dismiss();
                    fadeOutAndClearBlur(decorView);
                } catch (Exception e) {
                    Logger.e("未适配的消息结构", e);
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("未适配的消息结构，请联系开发者");
                    builder.setMessage(e.toString());
                    builder.show();
                    Toasts.info(getContext(), "未适配的消息结构，请联系开发者");
                }

            });

            btnSend.setOnLongClickListener(v -> {
                String send_type = rb[0].getText().toString();
                String text = editText.getText().toString();
                ContactCompat contactCompat = getContact();
                try {
                    if (chat_type == 1) {
                        showRepeatSendDialog(text, getCurrentPeerID(), false, send_type, contactCompat);
                    } else if (chat_type == 2) {
                        showRepeatSendDialog(text, getCurrentPeerID(), true, send_type, contactCompat);
                    } else {
                        Toasts.error(getContext(), "失败");
                        return true;
                    }
                } catch (Exception e) {
                    Logger.e("未适配的消息结构", e);
                    Toasts.info(getContext(), "未适配的消息结构，请联系开发者");
                }
                return true;
            });
    }, 100);


    }


    private void send_ark_msg(String text, ContactCompat contactCompat) throws JSONException {
        new JSONObject(text);
        ArrayList<MsgElement> elements = new ArrayList<>();
        MsgElement msgElement = getArkMsgElement(text);
        elements.add(msgElement);
        Nt_kernel_bridge.send_msg(contactCompat, elements);
    }

    private void send_text_msg(String text, ContactCompat contactCompat) {
        ArrayList<MsgElement> elements = new ArrayList<>();
        MsgElement msgElement = nt_build_text(text);
        elements.add(msgElement);
        Nt_kernel_bridge.send_msg(contactCompat, elements);
    }

    @NonNull
    private static MsgElement getArkMsgElement(@NonNull String text) {
        MsgElement msgElement = new MsgElement();
        ArkElement arkElement = new ArkElement(text,null,null);
        msgElement.setArkElement(arkElement);
        msgElement.setElementType(10);
        return msgElement;
    }

    @SuppressLint("SetTextI18n")
    private void showRepeatSendDialog(String content, String uid, boolean isGroupMsg, String type, ContactCompat contactCompat) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("重复发包");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText inputCount = new EditText(getContext());
        inputCount.setHint("次数");
        inputCount.setText("1");
        layout.addView(inputCount);

        final EditText inputInterval = new EditText(getContext());
        inputInterval.setHint("发包间隔 (毫秒)");
        inputInterval.setText("500");
        layout.addView(inputInterval);

        builder.setView(layout);

        final TextView warningText = new TextView(getContext());
        warningText.setText("警告：滥用此功能会导致封号");
        warningText.setTextSize(12);
        warningText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        layout.addView(warningText);

        builder.setPositiveButton("确定", (d, which) -> {
            String countStr = inputCount.getText().toString();
            String intervalStr = inputInterval.getText().toString();

            if (!countStr.isEmpty() && !intervalStr.isEmpty()) {
                int count = Integer.parseInt(countStr);
                long interval = Long.parseLong(intervalStr);

                repeatSendMessages(count, interval, content, uid, isGroupMsg, type, contactCompat);
                dialog.dismiss();
                fadeOutAndClearBlur(decorView);
            } else {
                Toasts.error(getContext(), "请填写所有字段");
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void repeatSendMessages(int count, long interval, String content, String uid, boolean isGroupMsg, String type, ContactCompat contactCompat) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            int currentCount = 0;

            @Override
            public void run() {
                if (currentCount < count) {
                    try {
                        if (type.equals("ark")) {
                            try {
                                send_ark_msg(content, contactCompat);
                            } catch (JSONException e) {
                                Toasts.error(getContext(), "JSON语法错误");
                            }
                        } else if (type.equals("text")){
                            send_text_msg(content, contactCompat);
                        } else {
                            ElemHelperKt.sendMessage(content, uid, isGroupMsg, type);
                        }

                        currentCount++;
                        new Handler(Looper.getMainLooper()).postDelayed(this, interval);
                    } catch (Exception e) {
                        Logger.e("发送消息失败", e);
                        Toasts.error(getContext(), "发送消息失败");
                    }
                } else {
                    Toasts.success(getContext(), "重复发包完成");
                }
            }
        }, interval);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void showCustomElemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("元素管理");

        RecyclerView recyclerView = new RecyclerView(getContext());
        CustomRecyclerViewAdapter adapter = new CustomRecyclerViewAdapter(presetelems, new CustomRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String item) {
                String content = elemContentMap.get(item);
                editText.setText(content);
                elem_dialog.dismiss();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);


        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                // 更新数据源
                String movedItem = presetelems.remove(fromPosition);
                presetelems.add(toPosition, movedItem);
                adapter.notifyItemMoved(fromPosition, toPosition);
                savePresetElem(); // 保存顺序
                return true;
            }


            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                String deletedItem = presetelems.get(position);


                new AlertDialog.Builder(getContext())
                        .setTitle("确认删除")
                        .setMessage("你确定要删除元素 " + deletedItem + " 吗？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            presetelems.remove(position);
                            elemContentMap.remove(deletedItem);
                            adapter.notifyItemRemoved(position);
                            deleteElement(deletedItem);
                            Toasts.success(getContext(), "已删除元素: " + deletedItem);
                        })
                        .setNegativeButton("取消", (dialog, which) -> {
                            adapter.notifyItemChanged(position); // 恢复滑动前的状态
                            dialog.cancel();
                        })
                        .show();
            }

        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(recyclerView);

        builder.setView(layout);

        builder.setNegativeButton("添加", (dialog, which) -> {
            AlertDialog.Builder addElemDialog = new AlertDialog.Builder(getContext());
            addElemDialog.setTitle("添加新元素");

            LinearLayout addLayout = new LinearLayout(getContext());
            addLayout.setOrientation(LinearLayout.VERTICAL);

            EditText inputElemName = new EditText(getContext());
            inputElemName.setHint("输入新元素名称");
            addLayout.addView(inputElemName);

            EditText inputElemContent = new EditText(getContext());
            inputElemContent.setHint("输入元素内容");
            addLayout.addView(inputElemContent);

            addElemDialog.setView(addLayout);
            addElemDialog.setPositiveButton("确定", (d, w) -> {
                String newElemName = inputElemName.getText().toString();
                String newElemContent = inputElemContent.getText().toString();

                if (presetelems.contains(newElemName)) {
                    Toasts.error(getContext(), "元素名称已存在，请使用其他名称");
                    return;
                }

                if (!newElemName.isEmpty() && !newElemContent.isEmpty()) {
                    elemContentMap.put(newElemName, newElemContent);
                    presetelems.add(newElemName);
                    savePresetElem();
                    saveElemContentMap();
                    adapter.notifyDataSetChanged(); // 刷新列表
                    Toasts.success(getContext(), "已保存元素: " + newElemName);
                } else {
                    Toasts.error(getContext(), "请填写所有字段");
                }
            });
            addElemDialog.setNegativeButton("取消", (dialog2, which2) -> dialog2.cancel());
            addElemDialog.show();
        });

        builder.setNeutralButton("取消", (dialog, which) -> dialog.cancel());
        elem_dialog = builder.show();
    }


    private void savePresetElem() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("elem_count", presetelems.size());
        for (int i = 0; i < presetelems.size(); i++) {
            editor.putString("ouo_elem_" + i, presetelems.get(i));
        }
        editor.apply();
    }

    private void saveElemContentMap() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (Map.Entry<String, String> entry : elemContentMap.entrySet()) {
            editor.putString("ouo_content_" + entry.getKey(), entry.getValue());
        }
        editor.apply();
    }

    private List<String> loadPresetElem() {
        List<String> elems = new ArrayList<>();
        int count = sharedPreferences.getInt("elem_count", 0);
        for (int i = 0; i < count; i++) {
            String elem = sharedPreferences.getString("ouo_elem_" + i, null);
            if (elem != null) {
                elems.add(elem);
            }
        }
        return elems;
    }

    private Map<String, String> loadElemContentMap() {
        Map<String, String> map = new HashMap<>();
        for (String elem : presetelems) {
            String content = sharedPreferences.getString("ouo_content_" + elem, null);
            if (content != null) {
                map.put(elem, content);
            }
        }
        return map;
    }

    private void deleteElement(String element) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("ouo_content_" + element);
        editor.remove("ouo_elem_" + element);
        editor.apply();

        presetelems.remove(element);
        elemContentMap.remove(element);

        savePresetElem();
    }


    private static void animateBlurEffect(View decorView) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
            return;
        }

        ValueAnimator blurAnimator = ValueAnimator.ofFloat(1f, 25f);
        blurAnimator.setDuration(200);
        blurAnimator.setInterpolator(new AccelerateInterpolator());
        blurAnimator.addUpdateListener(animation -> {
            float blurRadius = (float) animation.getAnimatedValue();
            decorView.setRenderEffect(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP));
        });
        blurAnimator.start();
    }

    private static void fadeOutAndClearBlur(View decorView) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
            return;
        }

        ValueAnimator fadeOutAnimator = ValueAnimator.ofFloat(25f, 1f);
        fadeOutAnimator.setDuration(100);
        fadeOutAnimator.setInterpolator(new DecelerateInterpolator());
        fadeOutAnimator.addUpdateListener(animation -> {
            float blurRadius = (float) animation.getAnimatedValue();
            decorView.setRenderEffect(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP));
        });

        fadeOutAnimator.addListener(new AnimatorListenerAdapter() {
            @RequiresApi(api = Build.VERSION_CODES.S)
            @Override
            public void onAnimationEnd(Animator animation) {
                decorView.setRenderEffect(null);
            }
        });

        fadeOutAnimator.start();
    }

    @Override
    protected void beforeDismiss() {
        fadeOutAndClearBlur(decorView);
        super.beforeDismiss();
    }

    @Override
    protected void onDismiss() {
        fadeOutAndClearBlur(decorView);
        super.onDismiss();
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.element_sender_layout;
    }
}


