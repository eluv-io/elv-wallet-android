// Source: https://github.com/JakeWharton/RxReplayingShare
/*
 * Copyright 2016 Jake Wharton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.eluvio.wallet.util.rx.replayingshare;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.disposables.Disposable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A transformer which combines the {@code replay(1)}, {@code publish()}, and {@code refCount()}
 * operators.
 * <p>
 * Unlike traditional combinations of these operators, `ReplayingShare` caches the last emitted
 * value from the upstream observable or flowable *only* when one or more downstream subscribers
 * are connected. This allows expensive upstream sources to be shut down when no one is listening
 * while also replaying the last value seen by *any* subscriber to new ones.
 */
public final class ReplayingShare<T>
    implements ObservableTransformer<T, T>, FlowableTransformer<T, T> {
  private static final ReplayingShare<Object> INSTANCE = new ReplayingShare<>(null);

  /** The singleton instance of this transformer. */
  @NonNull
  @SuppressWarnings("unchecked") // Safe because of erasure.
  public static <T> ReplayingShare<T> instance() {
    return (ReplayingShare<T>) INSTANCE;
  }

  /**
   * Creates a `ReplayingShare` transformer with a default value which will be emitted downstream
   * on subscription if there is not any cached value yet.
   *
   * @param defaultValue the initial value delivered to new subscribers before any events are
   * cached.
   */
  @NonNull
  public static <T> ReplayingShare<T> createWithDefault(@NonNull T defaultValue) {
    if (defaultValue == null) throw new NullPointerException("defaultValue == null");
    return new ReplayingShare<>(defaultValue);
  }

  private final @Nullable T defaultValue;

  private ReplayingShare(@Nullable T defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override public Observable<T> apply(Observable<T> upstream) {
    LastSeen<T> lastSeen = new LastSeen<>(defaultValue);
    return new LastSeenObservable<>(upstream.doOnEach(lastSeen).share(), lastSeen);
  }

  @Override public Flowable<T> apply(Flowable<T> upstream) {
    LastSeen<T> lastSeen = new LastSeen<>(defaultValue);
    return new LastSeenFlowable<>(upstream.doOnEach(lastSeen).share(), lastSeen);
  }

  static final class LastSeen<T> implements Observer<T>, Subscriber<T> {
    private final @Nullable T defaultValue;
    volatile @Nullable T value;

    LastSeen(@Nullable T defaultValue) {
      this.defaultValue = defaultValue;
      value = defaultValue;
    }

    @Override public void onNext(T value) {
        this.value = value;
    }

    @Override public void onError(Throwable e) {
      value = defaultValue;
    }

    @Override public void onComplete() {
      value = defaultValue;
    }

    @Override public void onSubscribe(Subscription ignored) {}
    @Override public void onSubscribe(Disposable ignored) {}
  }

  static final class LastSeenObservable<T> extends Observable<T> {
    private final Observable<T> upstream;
    private final LastSeen<T> lastSeen;

    LastSeenObservable(Observable<T> upstream, LastSeen<T> lastSeen) {
      this.upstream = upstream;
      this.lastSeen = lastSeen;
    }

    @Override protected void subscribeActual(Observer<? super T> observer) {
      upstream.subscribe(new LastSeenObserver<>(observer, lastSeen));
    }
  }

  static final class LastSeenObserver<T> implements Observer<T> {
    private final Observer<? super T> downstream;
    private final LastSeen<T> lastSeen;

    LastSeenObserver(Observer<? super T> downstream, LastSeen<T> lastSeen) {
      this.downstream = downstream;
      this.lastSeen = lastSeen;
    }

    @Override public void onSubscribe(Disposable d) {
      downstream.onSubscribe(d);

      T value = lastSeen.value;
      if (value != null && !d.isDisposed()) {
        downstream.onNext(value);
      }
    }

    @Override public void onNext(T value) {
      downstream.onNext(value);
    }

    @Override public void onComplete() {
      downstream.onComplete();
    }

    @Override public void onError(Throwable e) {
      downstream.onError(e);
    }
  }

  static final class LastSeenFlowable<T> extends Flowable<T> {
    private final Flowable<T> upstream;
    private final LastSeen<T> lastSeen;

    LastSeenFlowable(Flowable<T> upstream, LastSeen<T> lastSeen) {
      this.upstream = upstream;
      this.lastSeen = lastSeen;
    }

    @Override protected void subscribeActual(Subscriber<? super T> subscriber) {
      upstream.subscribe(new LastSeenSubscriber<>(subscriber, lastSeen));
    }
  }

  static final class LastSeenSubscriber<T> implements Subscriber<T>, Subscription {
    private final Subscriber<? super T> downstream;
    private final LastSeen<T> lastSeen;

    private @Nullable Subscription subscription;
    private volatile boolean cancelled;
    private boolean first = true;

    LastSeenSubscriber(Subscriber<? super T> downstream, LastSeen<T> lastSeen) {
      this.downstream = downstream;
      this.lastSeen = lastSeen;
    }

    @Override public void onSubscribe(Subscription subscription) {
      this.subscription = subscription;
      downstream.onSubscribe(this);
    }

    @Override public void request(long amount) {
      if (amount == 0) return;

      if (first) {
        first = false;

        T value = lastSeen.value;
        if (value != null && !cancelled) {
          downstream.onNext(value);

          if (amount != Long.MAX_VALUE && --amount == 0) {
            return;
          }
        }
      }
      Subscription subscription = this.subscription;
      assert subscription != null;
      subscription.request(amount);
    }

    @Override public void cancel() {
      Subscription subscription = this.subscription;
      assert subscription != null;
      cancelled = true;
      subscription.cancel();
    }

    @Override public void onNext(T value) {
      downstream.onNext(value);
    }

    @Override public void onComplete() {
      downstream.onComplete();
    }

    @Override public void onError(Throwable t) {
      downstream.onError(t);
    }
  }
}