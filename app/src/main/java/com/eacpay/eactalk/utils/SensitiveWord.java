package com.eacpay.eactalk.utils;

import android.content.Context;
import android.util.Log;

import com.blankj.utilcode.util.LanguageUtils;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class SensitiveWord {
    private static final String TAG = "oldfeel";

    public static Set<String> list(Context context, String str) {
        Log.e(TAG, "list: " + str);
        int replaceSize = 500;
        String replaceStr = "*";
        StringBuilder replaceAll = new StringBuilder(replaceSize);
        for (int x = 0; x < replaceSize; x++) {
            replaceAll.append(replaceStr);
        }
        //加载词库
        List<String> arrayList = new ArrayList<>();
        InputStreamReader read = null;
        BufferedReader bufferedReader = null;
        try {
            read = new InputStreamReader(context.getAssets().open("CensorWords.txt"));
            bufferedReader = new BufferedReader(read);
            for (String txt = null; (txt = bufferedReader.readLine()) != null; ) {
                if (!arrayList.contains(txt))
                    arrayList.add(txt);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != bufferedReader)
                    bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (null != read)
                    read.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Set<String> sensitiveWordSet = new HashSet<String>();//包含的敏感词列表，过滤掉重复项
        List<String> sensitiveWordList = new ArrayList<>();//包含的敏感词列表，包括重复项，统计次数
        StringBuilder buffer = new StringBuilder(str);
        HashMap<Integer, Integer> hash = new HashMap<Integer, Integer>(arrayList.size());
        String temp;
        for (int x = 0; x < arrayList.size(); x++) {
            temp = arrayList.get(x);
            int findIndexSize = 0;
            for (int start = -1; (start = buffer.indexOf(temp, findIndexSize)) > -1; ) {
                //System.out.println("###replace="+temp);
                findIndexSize = start + temp.length();//从已找到的后面开始找
                Integer mapStart = hash.get(start);//起始位置
                if (mapStart == null || (mapStart != null && findIndexSize > mapStart))//满足1个，即可更新map
                {
                    hash.put(start, findIndexSize);
                    //System.out.println("###敏感词："+buffer.substring(start, findIndexSize));
                }
            }
        }
        Collection<Integer> values = hash.keySet();
        for (Integer startIndex : values) {
            Integer endIndex = hash.get(startIndex);
            //获取敏感词，并加入列表，用来统计数量
            String sensitive = buffer.substring(startIndex, endIndex);
            //System.out.println("###敏感词："+sensitive);
            Log.e(TAG, "list: sensitive " + sensitive);
            if (!sensitive.contains("*")) {//添加敏感词到集合
                sensitiveWordSet.add(sensitive);
                sensitiveWordList.add(sensitive);
            }
            buffer.replace(startIndex, endIndex, replaceAll.substring(0, endIndex - startIndex));
        }
        hash.clear();
        Log.e(TAG, "list: sensitiveWordSet " + new Gson().toJson(sensitiveWordSet));
        return sensitiveWordSet;
    }

    /**
     * 用 * 替换敏感词
     *
     * @param str
     * @return
     */
    public static String replace(Context context, String str) {
        if (str == null) {
            return "";
        }
        // 非中文系统,不替换
        if (!LanguageUtils.getSystemLanguage().getLanguage().startsWith("zh")) {
            return str;
        }
        Log.e(TAG, "list: " + str);
        int replaceSize = 500;
        String replaceStr = "*";
        StringBuilder replaceAll = new StringBuilder(replaceSize);
        for (int x = 0; x < replaceSize; x++) {
            replaceAll.append(replaceStr);
        }
        //加载词库
        List<String> arrayList = new ArrayList<>();
        InputStreamReader read = null;
        BufferedReader bufferedReader = null;
        try {
            read = new InputStreamReader(context.getAssets().open("CensorWords.txt"));
            bufferedReader = new BufferedReader(read);
            for (String txt = null; (txt = bufferedReader.readLine()) != null; ) {
                if (!arrayList.contains(txt))
                    arrayList.add(txt);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != bufferedReader)
                    bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (null != read)
                    read.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Set<String> sensitiveWordSet = new HashSet<String>();//包含的敏感词列表，过滤掉重复项
        List<String> sensitiveWordList = new ArrayList<>();//包含的敏感词列表，包括重复项，统计次数
        StringBuilder buffer = new StringBuilder(str);
        HashMap<Integer, Integer> hash = new HashMap<Integer, Integer>(arrayList.size());
        String temp;
        for (int x = 0; x < arrayList.size(); x++) {
            temp = arrayList.get(x);
            int findIndexSize = 0;
            for (int start = -1; (start = buffer.indexOf(temp, findIndexSize)) > -1; ) {
                //System.out.println("###replace="+temp);
                findIndexSize = start + temp.length();//从已找到的后面开始找
                Integer mapStart = hash.get(start);//起始位置
                if (mapStart == null || (mapStart != null && findIndexSize > mapStart))//满足1个，即可更新map
                {
                    hash.put(start, findIndexSize);
                    //System.out.println("###敏感词："+buffer.substring(start, findIndexSize));
                }
            }
        }
        Collection<Integer> values = hash.keySet();
        for (Integer startIndex : values) {
            Integer endIndex = hash.get(startIndex);
            //获取敏感词，并加入列表，用来统计数量
            String sensitive = buffer.substring(startIndex, endIndex);
            //System.out.println("###敏感词："+sensitive);
            Log.e(TAG, "list: sensitive " + sensitive);
            if (!sensitive.contains("*")) {//添加敏感词到集合
                sensitiveWordSet.add(sensitive);
                sensitiveWordList.add(sensitive);
            }
            buffer.replace(startIndex, endIndex, replaceAll.substring(0, endIndex - startIndex));
        }
        hash.clear();
        Log.e(TAG, "list: sensitiveWordSet " + new Gson().toJson(sensitiveWordSet));
        return buffer.toString();
    }
}

