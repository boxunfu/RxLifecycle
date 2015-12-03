/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trello.rxlifecycle.components;

import com.trello.navi.Event;
import com.trello.navi.NaviComponent;
import com.trello.navi.internal.NaviEmitter;
import com.trello.rxlifecycle.ActivityEvent;
import com.trello.rxlifecycle.ActivityLifecycleProvider;
import com.trello.rxlifecycle.navi.NaviLifecycle;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.HashSet;

import static org.junit.Assert.assertFalse;

public class NaviActivityLifecycleTest {

    @Test
    public void testLifecycle() {
        NaviEmitter activity = NaviEmitter.createActivityEmitter();
        ActivityLifecycleProvider provider = NaviLifecycle.createActivityLifecycleProvider(activity);

        TestSubscriber<ActivityEvent> testSubscriber = new TestSubscriber<>();
        provider.lifecycle().subscribe(testSubscriber);

        activity.onCreate(null);
        activity.onStart();
        activity.onResume();
        activity.onPause();
        activity.onStop();
        activity.onDestroy();

        testSubscriber.assertValues(
            ActivityEvent.CREATE,
            ActivityEvent.START,
            ActivityEvent.RESUME,
            ActivityEvent.PAUSE,
            ActivityEvent.STOP,
            ActivityEvent.DESTROY
        );
    }

    @Test
    public void testBindUntilEvent() {
        NaviEmitter activity = NaviEmitter.createActivityEmitter();
        ActivityLifecycleProvider provider = NaviLifecycle.createActivityLifecycleProvider(activity);

        Observable<Object> observable = PublishSubject.create().asObservable();
        TestSubscriber<Object> testSubscriber = new TestSubscriber<>();
        observable.compose(provider.bindUntilEvent(ActivityEvent.STOP)).subscribe(testSubscriber);

        activity.onCreate(null);
        assertFalse(testSubscriber.isUnsubscribed());
        activity.onStart();
        assertFalse(testSubscriber.isUnsubscribed());
        activity.onResume();
        assertFalse(testSubscriber.isUnsubscribed());
        activity.onPause();
        assertFalse(testSubscriber.isUnsubscribed());
        activity.onStop();
        testSubscriber.assertCompleted();
        testSubscriber.assertUnsubscribed();
    }

    @Test
    public void testBindToLifecycle() {
        NaviEmitter activity = NaviEmitter.createActivityEmitter();
        ActivityLifecycleProvider provider = NaviLifecycle.createActivityLifecycleProvider(activity);

        Observable<Object> observable = PublishSubject.create().asObservable();

        activity.onCreate(null);
        TestSubscriber<Object> createTestSub = new TestSubscriber<>();
        observable.compose(provider.bindToLifecycle()).subscribe(createTestSub);

        activity.onStart();
        assertFalse(createTestSub.isUnsubscribed());
        TestSubscriber<Object> startTestSub = new TestSubscriber<>();
        observable.compose(provider.bindToLifecycle()).subscribe(startTestSub);

        activity.onResume();
        assertFalse(createTestSub.isUnsubscribed());
        assertFalse(startTestSub.isUnsubscribed());
        TestSubscriber<Object> resumeTestSub = new TestSubscriber<>();
        observable.compose(provider.bindToLifecycle()).subscribe(resumeTestSub);

        activity.onPause();
        assertFalse(createTestSub.isUnsubscribed());
        assertFalse(startTestSub.isUnsubscribed());
        resumeTestSub.assertCompleted();
        resumeTestSub.assertUnsubscribed();
        TestSubscriber<Object> pauseTestSub = new TestSubscriber<>();
        observable.compose(provider.bindToLifecycle()).subscribe(pauseTestSub);

        activity.onStop();
        assertFalse(createTestSub.isUnsubscribed());
        startTestSub.assertCompleted();
        startTestSub.assertUnsubscribed();
        pauseTestSub.assertCompleted();
        pauseTestSub.assertUnsubscribed();
        TestSubscriber<Object> stopTestSub = new TestSubscriber<>();
        observable.compose(provider.bindToLifecycle()).subscribe(stopTestSub);

        activity.onDestroy();
        createTestSub.assertCompleted();
        createTestSub.assertUnsubscribed();
        stopTestSub.assertCompleted();
        stopTestSub.assertUnsubscribed();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadHandler() {
        HashSet<Event<?>> notEnoughEvents = new HashSet<>();
        notEnoughEvents.add(Event.CREATE);
        NaviComponent badHandler = new NaviEmitter(notEnoughEvents);
        NaviLifecycle.createActivityLifecycleProvider(badHandler);
    }
}
