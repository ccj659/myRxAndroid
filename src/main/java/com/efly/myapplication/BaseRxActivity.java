package com.efly.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Observer;
import rx.Single;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class BaseRxActivity extends AppCompatActivity {
    private static final String TAG = "RXActivity-->";
    @Bind(R.id.textView)
    TextView textView;
    @Bind(R.id.button1)
    Button button1;
    @Bind(R.id.button2)
    Button button2;
    @Bind(R.id.button3)
    Button button3;
    @Bind(R.id.button4)
    Button button4;
    private Bitmap bitmap;
    private File file;
    private Observable<String> saveObservable;
    private Subscription saveSubscription;
    private Subscription mSingleSubscriber;
    private PublishSubject<Integer> publishSubject;
    private int mCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        init();

    }

    private void init() {//将bitmap保存到本地
        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.msglist);//
        file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "rxFile");
    }

    @OnClick({R.id.button1, R.id.button2, R.id.button3,R.id.button4})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button1:
                asyncSave();// //标准版observable 新开辟线程保存
                break;
            case R.id.button2:
                singleObservable();//精简版observable single
                break;
            case R.id.button3:
                if (publishSubject == null) {
                    subjectsDemo();//管道传输*/
                }
                onIncrementButtonClick();//当点击时,让它执行next
                break;
            case R.id.button4:
                Intent intent =new Intent(getBaseContext(),MapRxActivity.class);
                startActivity(intent);
                break;
        }
    }

    private void onIncrementButtonClick() {
        mCounter++;
        publishSubject.onNext(mCounter);
    }


    private void singleObservable() {
        Single<String> stringSingle = Single.fromCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return savePic(file, bitmap);
            }
        });
        mSingleSubscriber = stringSingle
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<String>() {
                    @Override
                    public void onSuccess(String value) {
                        Log.e(TAG, "mSingleSubscriber: save sucessful ,the file name is " + value);
                        textView.setText("mSingleSubscriber: save sucessful ,the file name is " + value);
                    }

                    @Override
                    public void onError(Throwable error) {

                    }
                })
        ;

    }

    private void subjectsDemo() {
        publishSubject = PublishSubject.create();
        publishSubject.subscribe(new Observer<Integer>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {
                textView.setText(String.valueOf(integer));
            }
        });

    }


    private void asyncSave() {
        saveObservable = Observable.fromCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {//通知调用  并返回string

                return savePic(file, bitmap);//此方法在io线程中调用 并返回
            }
        });

        saveSubscription = saveObservable
                .subscribeOn(Schedulers.io())//observable在调度中的IO线程中进行调度进行
                .observeOn(AndroidSchedulers.mainThread())//在主线程中进行观察
                .subscribe(new Observer<String>() { //订阅观察者
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String s) {//带参数的下一步,在此就是当
                        Log.e(TAG, "saveSubscription: save sucessful ,the file name is " + s);
                        textView.setText("saveSubscription: save sucessful ,the file name is " + s);

                    }
                });
    }


    public String savePic(final File file, final Bitmap bitmap) {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);

            bos.flush();
            bos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.getName();
    }


    @Override
    protected void onDestroy() {//注意解除订阅~~
        if (saveSubscription != null || !saveSubscription.isUnsubscribed()) {
            saveSubscription.unsubscribe();
        }
        if (mSingleSubscriber != null || !mSingleSubscriber.isUnsubscribed()) {
            mSingleSubscriber.unsubscribe();
        }
        super.onDestroy();
    }



}
