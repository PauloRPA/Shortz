package com.prpa.Shortz.model.shortener.contract;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

public interface UrlShortener {

    Optional<String> encodeUri(URI uri);
    Optional<String> encodeUri(URI uri, int increaseWindowBy);

    int getWindowSize();

    Set<String> getSupportedProtocols();

}
