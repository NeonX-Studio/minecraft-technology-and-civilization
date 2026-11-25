package com.nightcity.tcivilization.client.api;

@FunctionalInterface
public interface DeepLinkHandler {
    boolean handle(DeepLink link);
}
