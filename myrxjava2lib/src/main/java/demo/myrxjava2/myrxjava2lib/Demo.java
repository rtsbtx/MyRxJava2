package demo.myrxjava2.myrxjava2lib;

import android.os.Looper;

import java.io.Serializable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by alex on 17/02/2018.
 */

public class Demo {

    public interface Disposable {
        void dispose();
        boolean isDisposed();
    }

    interface ObservableSource {
        void subscribe(Observer observer);
    }

    public interface Observer {
        void onSubscribe(Disposable disposable);
        void onNext(Object o);
        void onError(Throwable throwable);
        void onComplete();
    }

    public interface ObservableEmitter {
        void onNext(Object o);
        void onError(Throwable error);
        void onComplete();
    }

    public interface ObservableOnSubscribe {
        void subscribe(ObservableEmitter observableEmitter);
    }

    public static class Schedulers {
        public static IoSchedule io(){
            return new IoSchedule();
        }
        public static AndroidSchedule androidMainThread(){
            return new AndroidSchedule();
        }
    }

    static abstract class Scheduler {
        abstract void exe(Runnable runnable);
    }

    static class IoSchedule extends Scheduler {
        final ScheduledExecutorService scheduledExecutorService;
        IoSchedule(){
            scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
        }
        @Override
        void exe(Runnable runnable) {
            scheduledExecutorService.schedule(runnable, 0L, TimeUnit.MILLISECONDS);
        }
    }

    static class AndroidSchedule extends Scheduler {
        final android.os.Handler handler;
        AndroidSchedule(){
            handler = new android.os.Handler(Looper.getMainLooper());
        }
        @Override
        void exe(Runnable runnable) {
            handler.post(runnable);
        }
    }

    static class ObservableSubscribeOn extends Observable{
        Observable observable;
        Scheduler scheduler;
        ObservableSubscribeOn(Observable observable, Scheduler scheduler){
            this.observable = observable;
            this.scheduler = scheduler;
        }
        @Override
        void subscribeActual(Observer observer) {
            SubscribeOnObserver subscribeOnObserver = new SubscribeOnObserver(observer);
            observer.onSubscribe(subscribeOnObserver);
            this.scheduler.exe(new SubscribeTask(subscribeOnObserver));
        }
        static final class SubscribeOnObserver implements Observer, Disposable {
            Observer observer;
            SubscribeOnObserver(Observer observer){
                this.observer = observer;
            }
            @Override
            public void dispose() {
            }
            @Override
            public boolean isDisposed() {
                return false;
            }
            @Override
            public void onSubscribe(Disposable disposable) {

            }
            @Override
            public void onNext(Object o) {
                this.observer.onNext(o);
            }
            @Override
            public void onError(Throwable throwable) {
                this.observer.onError(throwable);
            }
            @Override
            public void onComplete() {
                this.observer.onComplete();
            }
        }
        class SubscribeTask implements Runnable{
            SubscribeOnObserver subscribeOnObserver;
            SubscribeTask(SubscribeOnObserver subscribeOnObserver){
                this.subscribeOnObserver = subscribeOnObserver;
            }
            @Override
            public void run() {
                observable.subscribe(this.subscribeOnObserver);
            }
        }
    }

    static class ObservableObserveOn extends Observable{
        Observable observable;
        Scheduler scheduler;
        ObservableObserveOn(Observable observable, Scheduler scheduler){
            this.observable = observable;
            this.scheduler = scheduler;
        }
        @Override
        void subscribeActual(Observer observer) {
            observable.subscribe(new ObserveOnObserver(observer, this.scheduler));
        }
        static final class ObserveOnObserver implements Observer, Runnable {
            Observer observer;
            Scheduler scheduler;
            Object o;
            volatile boolean done;
            Throwable error;
            ObserveOnObserver(Observer observer, Scheduler scheduler){
                this.observer = observer;
                this.scheduler = scheduler;
            }
            @Override
            public void onSubscribe(Disposable disposable) {
                observer.onSubscribe(disposable);
            }
            @Override
            public void onNext(Object o) {
                this.o = o;
                this.scheduler.exe(this);
            }
            @Override
            public void onError(Throwable throwable) {
                done = true;
                error = throwable;
                this.scheduler.exe(this);
            }
            @Override
            public void onComplete() {
                done = true;
                this.scheduler.exe(this);
            }
            @Override
            public void run() {
                if(this.o != null){
                    this.observer.onNext(this.o);
                    this.o = null;
                    return;
                }
                if(done){
                    if(error == null){
                        this.observer.onComplete();
                    }else{
                        this.observer.onError(error);
                    }
                }
            }
        }
    }

    public static abstract class Observable implements ObservableSource {

        @Override
        public void subscribe(Observer observer) {
            subscribeActual(observer);
        }

        abstract void subscribeActual(Observer observer);

        public static Observable create(ObservableOnSubscribe observableOnSubscribe){
            return new ObservableCreate(observableOnSubscribe);
        }

        public final Observable subscribeOn(Scheduler scheduler) {
            return new ObservableSubscribeOn(this, scheduler);
        }

        public final Observable observeOn(Scheduler scheduler) {
            return new ObservableObserveOn(this, scheduler);
        }

    }

    static class ObservableCreate extends Observable {
        ObservableOnSubscribe observableOnSubscribe;
        public ObservableCreate(ObservableOnSubscribe observableOnSubscribe){
            this.observableOnSubscribe = observableOnSubscribe;
        }
        @Override
        void subscribeActual(Observer observer) {
            CreateEmitter createEmitter = new CreateEmitter(observer);
            observer.onSubscribe(createEmitter);
            try{
                this.observableOnSubscribe.subscribe(createEmitter);
            }catch (Throwable e){
                createEmitter.onError(e);
            }
        }

        static class CreateEmitter implements Disposable, ObservableEmitter, Serializable {
            final Observer observer;
            public CreateEmitter(Observer observer){
                this.observer = observer;
            }
            @Override
            public void dispose() {
                //todo
            }
            @Override
            public boolean isDisposed() {
                return false;
            }
            @Override
            public void onNext(Object value) {
                this.observer.onNext(value);
            }
            @Override
            public void onError(Throwable error) {
                this.observer.onError(error);
            }
            @Override
            public void onComplete() {
                try{
                    this.observer.onComplete();
                }finally {
                    dispose();
                }
            }
        }

    }

}
