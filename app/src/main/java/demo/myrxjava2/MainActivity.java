package demo.myrxjava2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import demo.myrxjava2.myrxjava2lib.Demo;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Demo.Observable.create(new Demo.ObservableOnSubscribe() {
            @Override
            public void subscribe(Demo.ObservableEmitter observableEmitter) {
                System.out.println("subscribe run " + Thread.currentThread().getId());
                observableEmitter.onNext("abc");
                observableEmitter.onComplete();
            }
        })
                .subscribeOn(Demo.Schedulers.io())
                .observeOn(Demo.Schedulers.androidMainThread())
                .subscribe(new Demo.Observer(){
                    @Override
                    public void onSubscribe(Demo.Disposable disposable) {
                        System.out.println("onSubscribe " + Thread.currentThread().getId());
                    }
                    @Override
                    public void onNext(Object o) {
                        System.out.println("onNext " + o + " " + Thread.currentThread().getId());
                    }
                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println("onError " + Thread.currentThread().getId());
                    }
                    @Override
                    public void onComplete() {
                        System.out.println("onComplete " + Thread.currentThread().getId());
                    }
                });
        ;

    }

}
