package asia.buildtheearth.asean.discord.plotsystem.api;

import asia.buildtheearth.asean.discord.plotsystem.api.events.ApiEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


final class ApiManager {

    private final List<Object> apiListeners = new CopyOnWriteArrayList<>();

    public void subscribe(Object listener) {
        // ensure at least one method available in given object that is annotated with Subscribe
        int methodsAnnotatedSubscribe = 0;
        for (Method method : listener.getClass().getMethods())
            if (method.isAnnotationPresent(ApiSubscribe.class)) methodsAnnotatedSubscribe++;
        if (methodsAnnotatedSubscribe == 0)
            throw new IllegalArgumentException(listener.getClass().getName()
                    + " attempted DiscordPlotSystem API registration but no public methods inside of it"
                    + " were annotated @ApiSubscribe (github.tintinkung.discordps.api.ApiSubscribe)");

        if (!listener.getClass().getPackage().getName().contains("github.tintinkung")) {
            DiscordPlotSystemAPI.info("Subscribed to external listener: " + listener.getClass().getName()
                    + " (" + String.valueOf(methodsAnnotatedSubscribe) + " methods)");
        }
        apiListeners.add(listener);
    }

    public boolean unsubscribe(Object listener) {
        DiscordPlotSystemAPI.info("Unsubscribed from class " + listener.getClass().getName());
        return apiListeners.remove(listener);
    }

    public <E extends ApiEvent> E callEvent(E event) {
        for (Object apiListener : apiListeners) {
            for (Method method : apiListener.getClass().getMethods()) {
                if (method.getParameters().length != 1)
                    continue; // api listener methods always take one parameter
                if (!method.getParameters()[0].getType().isAssignableFrom(event.getClass()))
                    continue; // make sure this method wants this event

                ApiSubscribe subscribeAnnotation = method.getAnnotation(ApiSubscribe.class);
                if (subscribeAnnotation == null) continue;

                invokeMethod(method, apiListener, event);
            }
        }

        return event;
    }

    /**
     * Invoke the given method on the given instance with the given args
     * @param method the method to invoke
     * @param instance the instance of the class to invoke on
     * @param args arguments for the method
     * @return whether the method executed without exception
     */
    @SuppressWarnings("UnusedReturnValue")
    private boolean invokeMethod(Method method, Object instance, Object... args) {
        // make sure method is accessible
        // noinspection deprecation
        if (!method.isAccessible()) method.setAccessible(true);

        try {
            method.invoke(instance, method.getParameterCount() == 0 ? null : args);
            return true;
        } catch (InvocationTargetException ex) {
            DiscordPlotSystemAPI.error(instance.getClass().getName() + "#" + method.getName() + " threw an error: ", ex);

        } catch (IllegalAccessException ex) {
            // this should never happen
            DiscordPlotSystemAPI.error("Failed to invoke method: " + method + " in API listener: " + method.getClass().getName(), ex);
        }
        return false;
    }
}
