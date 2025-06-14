package asia.buildtheearth.asean.discord.plotsystem.test.mock;

import github.scarsz.discordsrv.api.ApiManager;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.Event;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MockApiManager extends ApiManager {
    private final List<Object> apiListeners = new CopyOnWriteArrayList<>();

    @Override
    public void subscribe(@NotNull Object listener) {
        int methodsAnnotatedSubscribe = 0;
        for (Method method : listener.getClass().getMethods()) if (method.isAnnotationPresent(Subscribe.class)) methodsAnnotatedSubscribe++;
        if (methodsAnnotatedSubscribe == 0) throw new IllegalArgumentException(listener.getClass().getName() + " attempted DiscordSRV API registration but no public methods inside of it were annotated @Subscribe (github.scarsz.discordsrv.api.Subscribe)");
        apiListeners.add(listener);
    }

    @Override
    public boolean unsubscribe(Object listener) {
        return apiListeners.remove(listener);
    }

    @Override
    public <E extends Event> E callEvent(E event) {
        for (Object apiListener : apiListeners) {
            for (Method method : apiListener.getClass().getMethods()) {
                if (method.getParameters().length != 1)
                    continue; // api listener methods always take one parameter
                if (!method.getParameters()[0].getType().isAssignableFrom(event.getClass()))
                    continue; // make sure this method wants this event

                Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
                if (subscribeAnnotation == null) continue;

                try { invokeMethod(method, apiListener, event); }
                catch (InvocationTargetException | IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        return event;
    }

    private void invokeMethod(@NotNull Method method, @NotNull Object instance, Object... args) throws InvocationTargetException, IllegalAccessException {
        method.invoke(instance, method.getParameterCount() == 0 ? null : args);
    }
}
