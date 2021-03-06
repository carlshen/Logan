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

package test.logan.dianping.com.logan;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dianping.logan.Logan;
import com.dianping.logan.LoganConfig;
import com.dianping.logan.OnLoganProtocolStatus;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String FILE_NAME = "logan_v1";
    private static final String TAG = MainActivity.class.getName();

    private TextView mTvInfo;
    private TextView mTvContent;
    private EditText mEditIp;
    private RealSendLogRunnable mSendLogRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }

    private void initData() {
        LoganConfig config = new LoganConfig.Builder()
                .setCachePath(getApplicationContext().getFilesDir().getAbsolutePath())
                .setPath(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()
                        + File.separator + FILE_NAME)
                .setEncryptKey16("0123456789012345".getBytes())
                .setEncryptIV16("0123456789012345".getBytes())
                .build();
        Logan.init(config);
        Logan.setDebug(true);
        Logan.setOnLoganProtocolStatus(new OnLoganProtocolStatus() {
            @Override
            public void loganProtocolStatus(String cmd, int code) {
                Log.d(TAG, "clogan > cmd : " + cmd + " | " + "code : " + code);
            }
        });
        mSendLogRunnable = new RealSendLogRunnable();
    }

    private void initView() {
        Button button = (Button) findViewById(R.id.write_btn);
        Button sendBtn = (Button) findViewById(R.id.send_btn);
        Button logFileBtn = (Button) findViewById(R.id.show_log_file_btn);
        Button logContentBtn = (Button) findViewById(R.id.show_log_content_btn);
        mTvInfo = (TextView) findViewById(R.id.info);
        mTvContent = (TextView) findViewById(R.id.content);
        mTvContent.setMovementMethod(ScrollingMovementMethod.getInstance());
        mEditIp = (EditText) findViewById(R.id.send_ip);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loganTest();
            }
        });
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loganSend();
            }
        });
        logFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loganFilesInfo();
            }
        });
        logContentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loganFilesContent();
            }
        });
    }

    private void loganTest() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    for (int i = 0; i < 9; i++) {
                        Log.d(TAG, "times : " + i);
                        Logan.w(String.valueOf(i), 1);
                        Thread.sleep(5);
                    }
                    Log.d(TAG, "write log end");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void loganSend() {
        SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd");
        String d = dataFormat.format(new Date(System.currentTimeMillis()));
        String[] temp = new String[1];
        temp[0] = d;
        String ip = mEditIp.getText().toString().trim();
        if (!TextUtils.isEmpty(ip)) {
            mSendLogRunnable.setIp(ip);
        }
        Logan.s(temp, mSendLogRunnable);
    }

    private void loganFilesInfo() {
        Map<String, Long> map = Logan.getAllFilesInfo();
        if (map != null) {
            StringBuilder info = new StringBuilder();
            for (Map.Entry<String, Long> entry : map.entrySet()) {
                info.append("文件日期：").append(entry.getKey()).append("  文件大小（bytes）：").append(
                        entry.getValue()).append("\n");
            }
            mTvInfo.setText(info.toString());
        }
    }

    private void loganFilesContent() {
        Map<String, String> map = Logan.getAllFilesContent();
        if (map != null) {
            StringBuilder info = new StringBuilder();
            for (String entry : map.keySet()) {
                info.append("文件名称：").append(entry).append("\n");
                String content = map.get(entry);
                info.append(content).append("\n");
            }
            mTvContent.setText(info.toString());
        }
    }

}
