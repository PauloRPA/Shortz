package com.prpa.Shortz.controller;

import com.prpa.Shortz.model.ShortUrl;
import com.prpa.Shortz.service.ShortUrlService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Optional;

@Controller()
public class RedirectController {

    private final ShortUrlService shortUrlService;

    public RedirectController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @GetMapping("/")
    public String redirect() {
        return "index";
    }

    @GetMapping("/{slug}")
    public View redirect(@PathVariable("slug") String slug) {
        if (slug.equals("favicon.ico"))
            return new RedirectView("/images/favicon.ico");
        Optional<ShortUrl> urlFound = shortUrlService.findUrlBySlug(slug);

        if (urlFound.isEmpty()) {
            return new RedirectView("/");
        }

        shortUrlService.onRedirect(urlFound.get());
        RedirectView externalWebsite = new RedirectView(urlFound.get().getUri());
        externalWebsite.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
        return externalWebsite;
    }

}
