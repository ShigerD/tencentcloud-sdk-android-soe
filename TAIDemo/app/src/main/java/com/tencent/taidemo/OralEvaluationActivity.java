package com.tencent.taidemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.gson.Gson;
import com.tencent.taidemo.utils.AudioUtil;
import com.tencent.taidemo.utils.FileUtils;
import com.tencent.taisdk.TAIErrCode;
import com.tencent.taisdk.TAIError;
import com.tencent.taisdk.TAIOralEvaluation;
import com.tencent.taisdk.TAIOralEvaluationCallback;
import com.tencent.taisdk.TAIOralEvaluationData;
import com.tencent.taisdk.TAIOralEvaluationEvalMode;
import com.tencent.taisdk.TAIOralEvaluationFileType;
import com.tencent.taisdk.TAIOralEvaluationListener;
import com.tencent.taisdk.TAIOralEvaluationParam;
import com.tencent.taisdk.TAIOralEvaluationRet;
import com.tencent.taisdk.TAIOralEvaluationServerType;
import com.tencent.taisdk.TAIOralEvaluationStorageMode;
import com.tencent.taisdk.TAIOralEvaluationTextMode;
import com.tencent.taisdk.TAIOralEvaluationWorkMode;
import com.tencent.taisdk.TAIRecorderParam;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;


public class OralEvaluationActivity extends AppCompatActivity {

    private static final String FOLDER = "com.tencent.taidemo";
    private static final String HELLO_GUAGUA = "hello_guagua.mp3";
    private static String TAG = OralEvaluationActivity.class.getSimpleName();

    private TAIOralEvaluation oral;
    private EditText refText;
    private TextView logText;
    private Button recordBtn;
    private Button localRecordBtn;
    private RadioButton workOnceBtn;
    private RadioButton workStreamBtn;
    private RadioButton evalWordBtn;
    private RadioButton evalSentenceBtn;
    private RadioButton evalParagraphBtn;
    private RadioButton evalFreeBtn;
    private RadioButton storageDisableBtn;
    private RadioButton storageEnableBtn;
    private RadioButton typeEnglishBtn;
    private RadioButton typeChineseBtn;
    private RadioButton textModeNoramlBtn;
    private RadioButton textModePhonemeBtn;
    private EditText scoreCoeff;
    private EditText fragSize;
    private EditText vadInterval;
    private ProgressBar vadVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oralevaluation);
        this.refText = this.findViewById(R.id.refText);
        this.refText.setText("how are you");
        this.logText = this.findViewById(R.id.logText);
        this.recordBtn = this.findViewById(R.id.recordBtn);
        this.localRecordBtn = this.findViewById(R.id.localRecordBtn);
        this.workOnceBtn = this.findViewById(R.id.workOnceBtn);
        this.workStreamBtn = this.findViewById(R.id.workStreamBtn);
        this.workStreamBtn.setChecked(true);
        this.evalWordBtn = this.findViewById(R.id.evalWordBtn);
        this.evalSentenceBtn = this.findViewById(R.id.evalSentenceBtn);
        this.evalSentenceBtn.setChecked(true);
        this.evalParagraphBtn = this.findViewById(R.id.evalParagraphBtn);
        this.evalFreeBtn = this.findViewById(R.id.evalFreeBtn);
        this.storageDisableBtn = this.findViewById(R.id.storageDisable);
        this.storageEnableBtn = this.findViewById(R.id.storageEnable);
        this.storageDisableBtn.setChecked(true);
        this.typeEnglishBtn = this.findViewById(R.id.typeEnglish);
        this.typeChineseBtn = this.findViewById(R.id.typeChinese);
        this.typeEnglishBtn.setChecked(true);
        this.textModeNoramlBtn = this.findViewById(R.id.textModeNormal);
        this.textModeNoramlBtn.setChecked(true);
        this.textModePhonemeBtn = this.findViewById(R.id.textModePhoneme);

        this.scoreCoeff = this.findViewById(R.id.scoreCoeff);
        this.scoreCoeff.setText("1.0");

        this.fragSize = this.findViewById(R.id.fragSize);
        this.fragSize.setText("1.0");

        this.vadVolume = this.findViewById(R.id.vadVolume);

        this.vadInterval = this.findViewById(R.id.vadInterval);
        this.vadInterval.setText("5000");
        this.requestPermission();

//

        AudioUtil.getInstance().startRecord();


        AudioUtil.getInstance().stopRecord();

    }

    private void setOralListener() {
        oral.setListener(new TAIOralEvaluationListener() {
            @Override
            public void onEvaluationData(final TAIOralEvaluationData data,
                                         final TAIOralEvaluationRet result,
                                         final TAIError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (error.code != TAIErrCode.SUCC) {
                            OralEvaluationActivity.this.recordBtn.setText(R.string.start_record);
                        }
                        Gson gson = new Gson();
                        String errString = gson.toJson(error);
                        String retString = gson.toJson(result);
                        if (result != null) {
                            Log.w(TAG, "run: retString " + retString);
                            String pronAccuracy = String.valueOf(result.pronAccuracy);
                            Log.w(TAG, "run: pronAccuracy " + pronAccuracy);
                            TextView textView = findViewById(R.id.pronAccuracy);
                            textView.setText(pronAccuracy);
                            //      store
                            if (storageEnableBtn.isChecked()) {
                                final String mp3FileName = String.format("taisdk_%d.mp3",
                                        System.currentTimeMillis() / 1000);
                                writeFileToSDCard(data.audio, FOLDER, mp3FileName, true, false);
                            }
                        }

                        OralEvaluationActivity.this.setResponse(String.format("oralEvaluation" +
                                ":seq:%d, end:%d, error:%s, ret:%s", data.seqId, data.bEnd ?
                                1 : 0, errString, retString));
                    }
                });
            }

            @Override
            public void onEndOfSpeech() {
                OralEvaluationActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OralEvaluationActivity.this.setResponse("onEndOfSpeech");
                        OralEvaluationActivity.this.onRecord(null);
                    }
                });
            }

            @Override
            public void onVolumeChanged(final int volume) {
                OralEvaluationActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OralEvaluationActivity.this.vadVolume.setProgress(volume);
                    }
                });
            }
        });
    }

    public void onRecord(View view) {
        if (oral == null) {
            oral = new TAIOralEvaluation();
            setOralListener();
        }
        if (oral.isRecording()) {
            oral.stopRecordAndEvaluation(new TAIOralEvaluationCallback() {
                @Override
                public void onResult(final TAIError error) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Gson gson = new Gson();
                            String string = gson.toJson(error);
                            setResponse(String.format("stopRecordAndEvaluation:%s", string));
                            recordBtn.setText(R.string.start_record);
                        }
                    });
                }
            });
        } else {
            if (this.scoreCoeff.getText().toString().equals("")) {
                this.setResponse("startRecordAndEvaluation:scoreCoeff invalid");
                return;
            }
            if (this.fragSize.getText().toString().equals("")) {
                this.setResponse("startRecordAndEvaluation:fragSize invalid");


                return;
            }
            this.logText.setText("");
            TAIOralEvaluationParam param = new TAIOralEvaluationParam();
            param.context = this;
            param.sessionId = UUID.randomUUID().toString();
            param.appId = PrivateInfo.appId;
            param.soeAppId = PrivateInfo.soeAppId;
            param.secretId = PrivateInfo.secretId;
            param.secretKey = PrivateInfo.secretKey;
            param.token = PrivateInfo.token;
            int evalMode = TAIOralEvaluationEvalMode.SENTENCE;
            if (this.evalWordBtn.isChecked()) {
                evalMode = TAIOralEvaluationEvalMode.WORD;
            } else if (this.evalSentenceBtn.isChecked()) {
                evalMode = TAIOralEvaluationEvalMode.SENTENCE;
            } else if (this.evalParagraphBtn.isChecked()) {
                evalMode = TAIOralEvaluationEvalMode.PARAGRAPH;
            } else if (this.evalFreeBtn.isChecked()) {
                evalMode = TAIOralEvaluationEvalMode.FREE;
            }
            param.workMode = this.workOnceBtn.isChecked() ? TAIOralEvaluationWorkMode.ONCE :
                    TAIOralEvaluationWorkMode.STREAM;
            param.evalMode = evalMode;
            param.storageMode = this.storageDisableBtn.isChecked() ?
                    TAIOralEvaluationStorageMode.DISABLE : TAIOralEvaluationStorageMode.ENABLE;
            param.fileType = TAIOralEvaluationFileType.MP3;
            param.serverType = this.typeChineseBtn.isChecked() ?
                    TAIOralEvaluationServerType.CHINESE : TAIOralEvaluationServerType.ENGLISH;
            param.textMode = this.textModeNoramlBtn.isChecked() ?
                    TAIOralEvaluationTextMode.NORMAL : TAIOralEvaluationTextMode.PHONEME;
            param.scoreCoeff = Double.parseDouble(this.scoreCoeff.getText().toString());
            param.refText = this.refText.getText().toString();
            if (param.workMode == TAIOralEvaluationWorkMode.STREAM) {
                param.timeout = 5;
                param.retryTimes = 5;
            } else {
                param.timeout = 30;
                param.retryTimes = 0;
            }

            //start
            TAIRecorderParam recordParam = new TAIRecorderParam();
            recordParam.fragSize =
                    (int) (Double.parseDouble(this.fragSize.getText().toString()) * 1024);
            recordParam.fragEnable = !this.workOnceBtn.isChecked();
            recordParam.vadEnable = true;
            recordParam.vadInterval = 4000;
            oral.setRecorderParam(recordParam);
            oral.startRecordAndEvaluation(param, new TAIOralEvaluationCallback() {
                @Override
                public void onResult(final TAIError error) {
                    OralEvaluationActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (error.code == TAIErrCode.SUCC) {
                                OralEvaluationActivity.this.recordBtn.setText(R.string.stop_record);
                            }
                            Gson gson = new Gson();
                            String string = gson.toJson(error);
                            setResponse(String.format("startRecordAndEvaluation:%s", string));
                        }
                    });
                }
            });
        }
    }

    public void onLocalRecord(View view) {
//        TAIOralEvaluation oral = new TAIOralEvaluation();
        if (oral == null) {
            oral = new TAIOralEvaluation();
            setOralListener();
        }
        this.logText.setText("");
        TAIOralEvaluationParam param = new TAIOralEvaluationParam();
        param.context = this;
        param.sessionId = UUID.randomUUID().toString();
        param.appId = PrivateInfo.appId;
        param.soeAppId = PrivateInfo.soeAppId;
        param.secretId = PrivateInfo.secretId;
        param.secretKey = PrivateInfo.secretKey;
        param.workMode = TAIOralEvaluationWorkMode.ONCE;
        param.evalMode = TAIOralEvaluationEvalMode.SENTENCE;
        param.storageMode = TAIOralEvaluationStorageMode.DISABLE;
        param.fileType = TAIOralEvaluationFileType.MP3;
        param.serverType = TAIOralEvaluationServerType.ENGLISH;
        param.textMode = TAIOralEvaluationTextMode.NORMAL;
        param.scoreCoeff = Double.parseDouble(this.scoreCoeff.getText().toString());
        param.refText = "hello guagua";
        //assets hello_guagua.mp3
//        byte[] buffer = FileUtils.readBytesFromAssets(HELLO_GUAGUA);
        File file = getStorePath(FOLDER, HELLO_GUAGUA);
        byte[] buffer = FileUtils.readBytesFromFile(file);
        Log.d(TAG, "onLocalRecord: buffer " + buffer);
        TAIOralEvaluationData data = new TAIOralEvaluationData();
        data.seqId = 1;
        data.bEnd = true;
        data.audio = buffer;
        oral.oralEvaluation(param, data, new TAIOralEvaluationCallback() {
            @Override
            public void onResult(final TAIError error) {
                OralEvaluationActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Gson gson = new Gson();
                        String string = gson.toJson(error);
                        OralEvaluationActivity.this.setResponse(String.format("oralEvaluation" +
                                ":%s", string));
                    }
                });
            }
        });
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1234);
        }
    }

    private void setResponse(String rsp) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS");
        String date = format.format(new Date(System.currentTimeMillis()));
        String newS = String.format("%s %s", date, rsp);
        String old = this.logText.getText().toString();
        this.logText.setText(String.format("%s\n%s", old, newS));
    }


    private File getStorePath(final String folder, final String fileName) {
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        String folderPath = "";
        if (sdCardExist) {
            //TextUtils为android自带的帮助类
            if (TextUtils.isEmpty(folder)) {
                //如果folder为空，则直接保存在sd卡的根目录
                folderPath = Environment.getExternalStorageDirectory()
                        + File.separator;
            } else {
                folderPath = Environment.getExternalStorageDirectory()
                        + File.separator + folder + File.separator;
            }
        } else {
//            return folderPath;
        }
        Log.d(TAG, "getStorePath: " + folderPath + fileName);
        File file;
        //判断文件名是否为空
        if (TextUtils.isEmpty(fileName)) {
            file = new File(folderPath + "app_log.txt");
        } else {
            file = new File(folderPath + fileName);
        }
        return file;
    }

    public synchronized static void writeFileToSDCard(final byte[] buffer, final String folder,
                                                      final String fileName, final boolean append
            , final boolean autoLine) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean sdCardExist = Environment.getExternalStorageState().equals(
                        Environment.MEDIA_MOUNTED);
                String folderPath = "";
                if (sdCardExist) {
                    //TextUtils为android自带的帮助类
                    if (TextUtils.isEmpty(folder)) {
                        //如果folder为空，则直接保存在sd卡的根目录
                        folderPath = Environment.getExternalStorageDirectory()
                                + File.separator;
                    } else {
                        folderPath = Environment.getExternalStorageDirectory()
                                + File.separator + folder + File.separator;
                    }
                } else {
                    return;
                }

                Log.d(TAG, "run:folderPath: " + folderPath);
                Log.d(TAG, "run:fileName: " + fileName);
                File fileDir = new File(folderPath);
                if (!fileDir.exists()) {
                    if (!fileDir.mkdirs()) {
                        return;
                    }
                }
                File file;
                //判断文件名是否为空
                if (TextUtils.isEmpty(fileName)) {
                    file = new File(folderPath + "app_log.txt");
                } else {
                    file = new File(folderPath + fileName);
                }
                RandomAccessFile raf = null;
                FileOutputStream out = null;
                try {
                    if (append) {
                        //如果为追加则在原来的基础上继续写文件
                        raf = new RandomAccessFile(file, "rw");
                        raf.seek(file.length());
                        raf.write(buffer);
                        if (autoLine) {
                            raf.write("\n".getBytes());
                        }
                    } else {
                        //重写文件，覆盖掉原来的数据
                        out = new FileOutputStream(file);
                        out.write(buffer);
                        out.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (raf != null) {
                            raf.close();
                        }
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
