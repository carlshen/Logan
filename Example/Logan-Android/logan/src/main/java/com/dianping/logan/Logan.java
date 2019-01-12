/*
 * Copyright (c) 2018-present, 美团点评
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.dianping.logan;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class Logan {

    private static OnLoganProtocolStatus sLoganProtocolStatus;
    private static LoganControlCenter sLoganControlCenter;
    static boolean sDebug = false;

    /**
     * @brief Logan初始化
     */
    public static void init(LoganConfig loganConfig) {
        sLoganControlCenter = LoganControlCenter.instance(loganConfig);
    }

    /**
     * @param log  表示日志内容
     * @param type 表示日志类型
     * @brief Logan写入日志
     */
    public static void w(String log, int type) {
        sLoganControlCenter.write(log, type);
    }

    /**
     * @brief 立即写入日志文件
     */
    public static void f() {
        sLoganControlCenter.flush();
    }

    /**
     * @param dates    日期数组，格式：“2018-07-27”
     * @param runnable 发送操作
     * @brief 发送日志
     */
    public static void s(String dates[], SendLogRunnable runnable) {
        sLoganControlCenter.send(dates, runnable);
    }

    /**
     * @brief 返回所有日志文件信息
     */
    public static Map<String, Long> getAllFilesInfo() {
        File dir = sLoganControlCenter.getDir();
        if (!dir.exists()) {
            return null;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        Map<String, Long> allFilesInfo = new HashMap<>();
        for (File file : files) {
            try {
                allFilesInfo.put(Util.getDateStr(Long.parseLong(file.getName())), file.length());
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return allFilesInfo;
    }

    /**
     * @brief 返回最新日志文件内容
     */
    public static Map<String, String> getAllFilesContent() {
        File dir = sLoganControlCenter.getDir();
        if (!dir.exists()) {
            return null;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        Map<String, String> allFilesInfo = new HashMap<>();
        OutputStream os = null;
        for (File file : files) {
            try {
                os = new ByteArrayOutputStream();
                parse(new FileInputStream(file), os);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            if (os != null) {
                allFilesInfo.put(Util.getDateStr(Long.parseLong(file.getName())), os.toString());
            }
        }
        try {
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allFilesInfo;
    }

    public static void parse(InputStream is, OutputStream os) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        int t = 0;
        try {
            while ((t = is.read(buffer)) >= 0) {
                bos.write(buffer, 0, t);
                bos.flush();
            }
            byte[] content = bos.toByteArray();
            bos.close();
            for (int i = 0; i < content.length; i++) {
                byte start = content[i];
                if (start == '\1') {
                    i++;
                    int length = (content[i] & 0xFF) << 24 |
                            (content[i + 1] & 0xFF) << 16 |
                            (content[i + 2] & 0xFF) << 8 |
                            (content[i + 3] & 0xFF);
                    i += 3;
                    int type;
                    if (length > 0) {
                        int temp = i + length + 1;
                        if (content.length - i - 1 == length) { //异常
                            type = 0;
                        } else if (content.length - i - 1 > length && '\0' == content[temp]) {
                            type = 1;
                        } else if (content.length - i - 1 > length && '\1' == content[temp]) { //异常
                            type = 2;
                        } else {
                            i -= 4;
                            continue;
                        }
                        byte[] dest = new byte[length];
                        System.arraycopy(content, i + 1, dest, 0, length);
                        ByteArrayOutputStream uncompressBytesArray = new ByteArrayOutputStream();
                        InflaterInputStream inflaterOs = null;
                        byte[] uncompressByte;
                        try {
                            uncompressBytesArray.reset();
                            inflaterOs = new GZIPInputStream(new ByteArrayInputStream(dest));
                            int e = 0;
                            while ((e = inflaterOs.read(buffer)) >= 0) {
                                uncompressBytesArray.write(buffer, 0, e);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        uncompressByte = uncompressBytesArray.toByteArray();
                        uncompressBytesArray.reset();
                        os.write(uncompressByte);
                        if (inflaterOs != null) {
                            inflaterOs.close();
                        }
                        i += length;
                        if (type == 1) {
                            i++;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @brief Logan Debug开关
     */
    public static void setDebug(boolean debug) {
        Logan.sDebug = debug;
    }

    static void onListenerLogWriteStatus(String name, int status) {
        if (sLoganProtocolStatus != null) {
            sLoganProtocolStatus.loganProtocolStatus(name, status);
        }
    }

    public static void setOnLoganProtocolStatus(OnLoganProtocolStatus listener) {
        sLoganProtocolStatus = listener;
    }
}
