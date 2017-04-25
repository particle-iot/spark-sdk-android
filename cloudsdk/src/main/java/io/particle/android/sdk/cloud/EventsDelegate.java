package io.particle.android.sdk.cloud;


import android.net.Uri;
import android.net.Uri.Builder;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.util.LongSparseArray;

import com.google.gson.Gson;

import org.kaazing.net.sse.SseEventReader;
import org.kaazing.net.sse.SseEventSource;
import org.kaazing.net.sse.SseEventSourceFactory;
import org.kaazing.net.sse.SseEventType;
import org.kaazing.net.sse.impl.AuthenticatedEventSourceFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.cloud.ApiDefs.CloudApi;
import io.particle.android.sdk.utils.TLog;
import retrofit.RetrofitError;

import static io.particle.android.sdk.utils.Py.truthy;


// See javadoc on ParticleCloud for the intended behavior of these methods
@ParametersAreNonnullByDefault
class EventsDelegate {

    private static final TLog log = TLog.get(EventsDelegate.class);

    private final CloudApi cloudApi;
    private final EventApiUris uris;
    private final Gson gson;
    private final ExecutorService executor;
    private final SseEventSourceFactory eventSourceFactory;

    private final AtomicLong subscriptionIdGenerator = new AtomicLong(1);
    private final LongSparseArray<EventReader> eventReaders = new LongSparseArray<>();

    EventsDelegate(CloudApi cloudApi, Uri baseApiUri, Gson gson, ExecutorService executor,
                   ParticleCloud cloud) {
        this.cloudApi = cloudApi;
        this.gson = gson;
        this.executor = executor;
        this.eventSourceFactory = new AuthenticatedEventSourceFactory(cloud);
        this.uris = new EventApiUris(baseApiUri);
    }

    @WorkerThread
    void publishEvent(String eventName, String event,
                      @ParticleEventVisibility int eventVisibility, int timeToLive)
            throws ParticleCloudException {

        boolean isPrivate = eventVisibility != ParticleEventVisibility.PUBLIC;
        try {
            cloudApi.publishEvent(eventName, event, isPrivate, timeToLive);
        } catch (RetrofitError error) {
            throw new ParticleCloudException(error);
        }
    }

    @WorkerThread
    long subscribeToAllEvents(@Nullable String eventNamePrefix,
                              ParticleEventHandler handler) throws IOException {
        return subscribeToEventWithUri(uris.buildAllEventsUri(eventNamePrefix), handler);
    }

    @WorkerThread
    long subscribeToMyDevicesEvents(@Nullable String eventNamePrefix,
                                    ParticleEventHandler handler) throws IOException {
        return subscribeToEventWithUri(uris.buildMyDevicesEventUri(eventNamePrefix), handler);
    }

    @WorkerThread
    long subscribeToDeviceEvents(@Nullable String eventNamePrefix, String deviceID,
                                 ParticleEventHandler eventHandler) throws IOException {
        return subscribeToEventWithUri(
                uris.buildSingleDeviceEventUri(eventNamePrefix, deviceID),
                eventHandler);
    }

    @WorkerThread
    void unsubscribeFromEventWithID(long eventListenerID) throws ParticleCloudException {
        synchronized (eventReaders) {
            EventReader reader = eventReaders.get(eventListenerID);
            if (reader == null) {
                log.w("No event listener subscription found for ID '" + eventListenerID + "'!");
                return;
            }
            eventReaders.remove(eventListenerID);
            try {
                reader.stopListening();
            } catch (IOException e) {
                // handling the exception here instead of putting it in the method signature
                // is inconsistent, but SDK consumers aren't going to care about receiving
                // this exception, so just swallow it here.
                log.w("Error while trying to stop event listener", e);
            }
        }
    }

    @WorkerThread
    void unsubscribeFromEventWithHandler(SimpleParticleEventHandler handler) throws ParticleCloudException {
        synchronized (eventReaders) {
            for (int i = 0; i < eventReaders.size(); i++) {
                EventReader reader = eventReaders.valueAt(i);

                if (reader.handler == handler) {
                    eventReaders.remove(i);
                    try {
                        reader.stopListening();
                    } catch (IOException e) {
                        // handling the exception here instead of putting it in the method signature
                        // is inconsistent, but SDK consumers aren't going to care about receiving
                        // this exception, so just swallow it here.
                        log.w("Error while trying to stop event listener", e);
                    }
                    return;
                }
            }
        }
    }

    private long subscribeToEventWithUri(Uri uri, ParticleEventHandler handler) throws IOException {
        synchronized (eventReaders) {

            long subscriptionId = subscriptionIdGenerator.getAndIncrement();
            EventReader reader = new EventReader(handler, executor, gson, uri, eventSourceFactory);
            eventReaders.put(subscriptionId, reader);

            log.d("Created event subscription with ID " + subscriptionId + " for URI " + uri);

            reader.startListening();

            return subscriptionId;
        }
    }


    private static class EventReader {

        final ParticleEventHandler handler;
        final SseEventSource sseEventSource;
        final ExecutorService executor;
        final Gson gson;

        volatile Future<?> future;

        private EventReader(ParticleEventHandler handler, ExecutorService executor, Gson gson,
                            Uri uri, SseEventSourceFactory factory) {
            this.handler = handler;
            this.executor = executor;
            this.gson = gson;
            try {
                sseEventSource = factory.createEventSource(URI.create(uri.toString()));
            } catch (URISyntaxException e) {
                // I don't like throwing exceptions in constructors, but this URI shouldn't be in
                // the wrong format...
                throw new RuntimeException(e);
            }
        }

        void startListening() throws IOException {
            sseEventSource.connect();
            final SseEventReader sseEventReader = sseEventSource.getEventReader();
            future = executor.submit(() -> startHandlingEvents(sseEventReader));
        }

        void stopListening() throws IOException {
            future.cancel(false);
            sseEventSource.close();
        }


        private void startHandlingEvents(SseEventReader sseEventReader) {
            SseEventType type;
            try {
                type = sseEventReader.next();
                while (type != SseEventType.EOS) {

                    if (type != null && type.equals(SseEventType.DATA)) {
                        CharSequence data = sseEventReader.getData();
                        String asStr = data.toString();

                        ParticleEvent event = gson.fromJson(asStr, ParticleEvent.class);

                        try {
                            handler.onEvent(sseEventReader.getName(), event);
                        } catch (Exception ex) {
                            handler.onEventError(ex);
                        }
                    } else {
                        log.w("type null or not data: " + type);
                    }
                    type = sseEventReader.next();
                }
            } catch (IOException e) {
                handler.onEventError(e);
            }
        }
    }


    // FIXME: Start sharing some of the strings with the constants that need to be defined in ApiDefs
    private static class EventApiUris {

        private final String EVENTS = "events";

        private final Uri allEventsUri;
        private final Uri devicesBaseUri;
        private final Uri myDevicesEventsUri;

        EventApiUris(Uri baseUri) {
            allEventsUri = baseUri.buildUpon().path("/v1/" + EVENTS).build();
            devicesBaseUri = baseUri.buildUpon().path("/v1/devices").build();
            myDevicesEventsUri = devicesBaseUri.buildUpon().appendPath(EVENTS).build();
        }

        Uri buildAllEventsUri(@Nullable String eventNamePrefix) {
            if (truthy(eventNamePrefix)) {
                return allEventsUri.buildUpon().appendPath(eventNamePrefix).build();
            } else {
                return allEventsUri;
            }
        }

        Uri buildMyDevicesEventUri(@Nullable String eventNamePrefix) {
            if (truthy(eventNamePrefix)) {
                return myDevicesEventsUri.buildUpon().appendPath(eventNamePrefix).build();
            } else {
                return myDevicesEventsUri;
            }
        }

        Uri buildSingleDeviceEventUri(@Nullable String eventNamePrefix, String deviceId) {
            Builder builder = devicesBaseUri.buildUpon()
                    .appendPath(deviceId)
                    .appendPath(EVENTS);
            if (truthy(eventNamePrefix)) {
                builder.appendPath(eventNamePrefix);
            }
            return builder.build();
        }
    }

}
