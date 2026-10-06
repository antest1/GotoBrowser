package com.antest1.gotobrowser.Helpers;

/**
 * Receives the outcome text of an asynchronous operation so the caller can
 * decide where to display it.
 *
 * <p>This is a project-local equivalent of {@code java.util.function.Consumer},
 * which is only available from API 24 while this app supports API 21.</p>
 *
 * <p>A {@code null} message means "the operation finished but there is nothing to
 * show" (for example, the user chose an action that dismisses the caller).</p>
 */
public interface MessageCallback {
    void onMessage(String message);
}
