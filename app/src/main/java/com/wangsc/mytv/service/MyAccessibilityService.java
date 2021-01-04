package com.wangsc.mytv.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.wangsc.mytv.model.DataContext;
import com.wangsc.mytv.model.Setting;
import com.wangsc.mytv.util._Utils;

import java.util.ArrayList;
import java.util.List;

import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;

public class MyAccessibilityService extends AccessibilityService {

    private int clickTag = 0;
    private DataContext dataContext;
    private int eventType;
    private String className, packageName;

    private void e(Object log){
        Log.e("wangsc",log.toString());
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        dataContext = new DataContext(this);

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        List<String> apps = _Utils.INSTANCE.getAppInfos(getApplication());
        info.packageNames = apps.toArray(new String[apps.size()]); //监听过滤的包名
        for(String packageName : apps){
            Log.e("wangsc","包名："+packageName);
        }
//        info.packageNames = new String[]{"com.alibaba.android.rimet"}; //监听过滤的包名
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK; //监听哪些行为
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN; //反馈
        info.notificationTimeout = 100; //通知的时间
        setServiceInfo(info);
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            packageName = event.getPackageName().toString();
            eventType = event.getEventType();
            className = event.getClassName().toString();

            if (eventType == TYPE_WINDOW_STATE_CHANGED) {
            } else if (eventType == TYPE_WINDOW_CONTENT_CHANGED) {
                e(packageName);
                e(className);
                printNodeInfo();
            } else {
                if (dataContext.getSetting(Setting.KEYS.is_print_other_all, true).getBoolean() == true && !className.contains("com.wang17.myphone")) {
                    printNodeInfo();
                }
            }

        } catch (Exception e) {
        }
    }

    private boolean clickView(AccessibilityNodeInfo node) {
        if (node.isClickable()) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        } else {
            AccessibilityNodeInfo parent = node.getParent();
            while (parent != null) {
                if (parent.isClickable()) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
                parent = parent.getParent();
            }
        }
        return false;
    }

    /**
     * 查找到
     */
    List<AccessibilityNodeInfo> allNodesInActiveWindow = new ArrayList<AccessibilityNodeInfo>();

    /**
     * 得到当前屏幕中所有节点数量。
     *
     * @return
     */
    private int nodesNumInActiveWindow;

    private int getNodesNum() {
        nodesNumInActiveWindow = 0;
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            recursionNodeForCount(rootNode);
            return nodesNumInActiveWindow;
        } else {
            return 0;
        }
    }

    private boolean recursionNodeForCount(AccessibilityNodeInfo info) {
        if (info.getChildCount() == 0) {
            nodesNumInActiveWindow++;
        } else {
            for (int i = 0; i < info.getChildCount(); i++) {
                if (info.getChild(i) != null) {
                    recursionNodeForCount(info.getChild(i));
                }
            }
        }
        return false;
    }


    /**
     * 点击Text = btnText的按钮，只点击一个view即返回。
     *
     * @param viewText
     * @return
     */
    private boolean clickViewByEqualText(String viewText) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            AccessibilityNodeInfo node = getNodeByEqualsText(nodeInfo, viewText);
            if (node != null) {
                return clickView(node);
            }
        }
        return false;
    }

    /**
     * 点击Text = btnText的按钮，只点击一个view即返回。
     *
     * @param viewText
     * @return
     */
    private boolean clickViewByContainsText(String viewText) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            AccessibilityNodeInfo node = getNodeByContains(nodeInfo, viewText);
            if (node != null) {
                return clickView(node);
            }
        }
        return false;
    }


    /**
     * 循环点击指定view之后的第after个view;用于指定的viewText存在多个，例如滴滴的“系统消息”。
     *
     * @param viewText
     * @param after
     * @return
     */
    private void clickViewListByText(String viewText, int after) {
        String text = "";
        int targetIndex = 0;
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        AccessibilityNodeInfo node = null;
        if (nodeInfo != null) {
            allNodesInActiveWindow.clear();
            getAllNodesToList(nodeInfo);
            for (int i = 0; i < allNodesInActiveWindow.size(); i++) {
                node = allNodesInActiveWindow.get(i);
                if (node.getText() != null) {
                    text = node.getText().toString();
                }
                if (text.equals(viewText)) {
                    targetIndex = i + after;
                    clickView(allNodesInActiveWindow.get(targetIndex));
                }
            }
        }
    }


    /**
     * 点击ContentDescription = viewDescription
     *
     * @param viewDescription
     * @return
     */
    private boolean clickViewByEqualsDescription(String viewDescription) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            AccessibilityNodeInfo node = getNodeByEqualsDescription(nodeInfo, viewDescription);
            if (node != null) {
                return clickView(node);
            }
        }
        return false;
    }

    /**
     * 检查当前窗口中是否有某按钮。
     *
     * @param viewText
     * @return
     */
    private boolean isExsitNodeByContainsText(String viewText) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            if (recursionNodeForExsitNodeByContainsText(nodeInfo, viewText)) {
                return true;
            }
        }
        return false;
    }

    public boolean recursionNodeForExsitNodeByContainsText(AccessibilityNodeInfo node, String viewText) {
        if (node.getChildCount() == 0) {
            String text = "";
            if (node.getText() != null) {
                text = node.getText().toString();
            }
            if (text.contains(viewText)) {
                return true;
            }
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                if (node.getChild(i) != null) {
                    if (recursionNodeForExsitNodeByContainsText(node.getChild(i), viewText)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检查当前窗口中是否有某按钮。
     *
     * @param viewText
     * @return
     */
    private boolean isExsitNodeByEqualText(String viewText) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            if (recursionNodeForExsitNodeByEqualText(nodeInfo, viewText)) {
                return true;
            }
        }
        return false;
    }

    public boolean recursionNodeForExsitNodeByEqualText(AccessibilityNodeInfo node, String viewText) {
        if (node.getChildCount() == 0) {
            String text = "";
            if (node.getText() != null) {
                text = node.getText().toString();
            }
            if (text.equals(viewText)) {
                return true;
            }
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                if (node.getChild(i) != null) {
                    if (recursionNodeForExsitNodeByEqualText(node.getChild(i), viewText)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void printNodeInfo() {
//        packageName.contains("com.android.systemui") ||
        if (packageName.contains("com.sec.android.app.launcher") || packageName.contains("com.wang17.myphone")) {
            return;
        }

//        if (dataContext.getSetting(Setting.KEYS.is_print_ifClassName, true).getBoolean() == false || packageName.equals("com.wang17.myphone")) {
//            return;
//        }

        //
        StringBuilder texts = new StringBuilder();
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        getAllNodesPrintInfo(nodeInfo, texts);
        Log.e("wangsc",AccessibilityEvent.eventTypeToString(eventType)+"\n"+className + " ：" + packageName + "\n" + texts.toString());
//        dataContext.addRunLog2File(AccessibilityEvent.eventTypeToString(eventType), className + " ：" + packageName + "\n" + texts.toString());
    }

    public void getAllNodesPrintInfo(AccessibilityNodeInfo info, StringBuilder texts) {
        if (info != null) {
            if (info.getChildCount() == 0) {
                texts.append("【");
                if (info.getText() != null) {
                    texts.append(info.getText().toString());
                }
                if (info.getContentDescription() != null) {
                    texts.append("*" + info.getContentDescription().toString());
                }
                texts.append("】");
            } else {
                for (int i = 0; i < info.getChildCount(); i++) {
                    if (info.getChild(i) != null) {
                        getAllNodesPrintInfo(info.getChild(i), texts);
                    }
                }
            }
        }
    }

    public void getAllNodesToList(AccessibilityNodeInfo info) {
        if (info != null) {
            if (info.getChildCount() == 0) {
                allNodesInActiveWindow.add(info);
            } else {
                for (int i = 0; i < info.getChildCount(); i++) {
                    if (info.getChild(i) != null) {
                        getAllNodesToList(info.getChild(i));
                    }
                }
            }
        }
    }

    private void getAllNodesToListByEqualsDescription(AccessibilityNodeInfo nodeInfo, String viewDescription, List<AccessibilityNodeInfo> nodeInfoList) {
        if (nodeInfo.getChildCount() == 0) {
            if (nodeInfo.getContentDescription() != null) {
                String description = nodeInfo.getContentDescription().toString();
                if (description.equals(viewDescription)) {
                    nodeInfoList.add(nodeInfo);
                }
            }
        } else {
            for (int i = 0; i < nodeInfo.getChildCount(); i++) {
                if (nodeInfo.getChild(i) != null) {
                    getAllNodesToListByEqualsDescription(nodeInfo.getChild(i), viewDescription, nodeInfoList);
                }
            }
        }
    }

    /**
     *
     * @param info
     * @param viewText
     * @return
     */
    public AccessibilityNodeInfo getNodeByEqualsText(AccessibilityNodeInfo info, String viewText) {
        if (info != null) {
            if (info.getChildCount() == 0) {
                if (info.getText() != null) {
                    String text = info.getText().toString();
                    if (text.equals(viewText))
                        return info;
                }
            } else {
                for (int i = 0; i < info.getChildCount(); i++) {
                    if (info.getChild(i) != null) {
                        AccessibilityNodeInfo node = getNodeByEqualsText(info.getChild(i), viewText);
                        if (node != null) {
                            return node;
                        }
                    }
                }
            }
        }
        return null;
    }


    /**
     *
     * @param info
     * @param viewText
     * @return
     */
    public AccessibilityNodeInfo getNodeByContains(AccessibilityNodeInfo info, String viewText) {
        if (info != null) {
            if (info.getChildCount() == 0) {
                if (info.getText() != null) {
                    String text = info.getText().toString();
                    if (text.contains(viewText))
                        return info;
                }
            } else {
                for (int i = 0; i < info.getChildCount(); i++) {
                    if (info.getChild(i) != null) {
                        AccessibilityNodeInfo node = getNodeByContains(info.getChild(i), viewText);
                        if (node != null) {
                            return node;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     *
     * @param info
     * @param viewDescription
     * @return
     */
    public AccessibilityNodeInfo getNodeByEqualsDescription(AccessibilityNodeInfo info, String viewDescription) {
        if (info != null) {
            if (info.getChildCount() == 0) {
                if (info.getContentDescription() != null) {
                    String description = info.getContentDescription().toString();
                    if (description.equals(viewDescription))
                        return info;
                }
            } else {
                for (int i = 0; i < info.getChildCount(); i++) {
                    if (info.getChild(i) != null) {
                        AccessibilityNodeInfo node = getNodeByEqualsDescription(info.getChild(i), viewDescription);
                        if (node != null) {
                            return node;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void onInterrupt() {
    }


}
