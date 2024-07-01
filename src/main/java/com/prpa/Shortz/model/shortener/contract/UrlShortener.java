package com.prpa.Shortz.model.shortener.contract;

import java.net.URL;
import java.util.Optional;
import java.util.Set;

public interface UrlShortener {

    Optional<String> encodeUrl(URL url);
    Optional<String> encodeUrl(URL url, int increaseWindowBy);

    int getWindowSize();

    Set<String> getSupportedProtocols();

}
