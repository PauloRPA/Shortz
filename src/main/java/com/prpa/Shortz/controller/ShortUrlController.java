package com.prpa.Shortz.controller;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.dto.ShortUrlDTO;
import com.prpa.Shortz.model.exceptions.EmptyUriException;
import com.prpa.Shortz.model.exceptions.InvalidUriException;
import com.prpa.Shortz.model.form.ShortUrlForm;
import com.prpa.Shortz.model.shortener.contract.UrlShortener;
import com.prpa.Shortz.service.ShortUrlService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponents;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/user")
@SessionAttributes("uuidShortUrlIdMap")
public class ShortUrlController {

    private static final Integer DEFAULT_PAGE_SIZE = 15;
    private static final Integer NUMBER_PAGINATION_OPTIONS = 5;

    private final ShortUrlService shortUrlService;
    private final UrlShortener urlShortener;

    @Autowired
    public ShortUrlController(ShortUrlService shortUrlService, UrlShortener urlShortener) {
        this.shortUrlService = shortUrlService;
        this.urlShortener = urlShortener;
    }

    @ModelAttribute("uuidShortUrlIdMap")
    public Map<UUID, Long> getUUIDShortUrlMap() {
        return new HashMap<>();
    }

    @GetMapping("/uris/new")
    public ModelAndView getNewUri(Model model) {
        model.addAttribute("newUriForm", new ShortUrlForm());
        ModelAndView mav = new ModelAndView();
        mav.setViewName("user/uris/newUri");
        return mav;
    }

    @PostMapping("/uris/new")
    public ModelAndView postNewUri(@Valid @ModelAttribute("newUriForm") ShortUrlForm form,
                                   BindingResult result,
                                   @AuthenticationPrincipal ShortzUser owner) {
        ModelAndView mav = new ModelAndView();

        Optional<URI> uri = Optional.empty();
        if (form.getUri() != null) {
            uri = validateUri(form.getUri().trim());
        }

        if (uri.isEmpty() && result.getFieldErrors("uri").isEmpty()) {
            result.rejectValue("uri", "error.newUriForm.uri.invalid");
        }

        if (uri.isPresent() && !urlShortener.getSupportedProtocols().contains(uri.get().getScheme())) {
            result.rejectValue("uri", "error.newUriForm.uri.unsupported.protocol");
        }

        if (!result.getFieldErrors("uri").isEmpty()) {
            result.getModel().forEach(mav.getModelMap()::addAttribute);
            mav.setViewName("/user/uris/newUri");
            mav.setStatus(HttpStatus.BAD_REQUEST);
            return mav;
        }

        String slug = form.getSlug();
        if ((slug == null || slug.isBlank()) && uri.isPresent()) {
            slug = urlShortener.encodeUri(uri.get()).orElse("");
        }

        if (slug == null || slug.isBlank()) {
            result.rejectValue("slug", "error.newUriForm.slug.encoding");
            mav.setViewName("/user/uris/newUri");
            mav.setStatus(HttpStatus.BAD_REQUEST);
            return mav;
        }

        if (result.getFieldErrors("slug").isEmpty() && shortUrlService.existsBySlug(slug)) {
            result.rejectValue("slug", "slug.exists");
        }

        Set<String> invalidChars = shortUrlService.filterInvalidCharsFromSlug(slug);
        if (!invalidChars.isEmpty()) {
            String invalidCharsStr = String.join("", invalidChars);
            result.rejectValue("slug", "error.slug.character", new String[]{invalidCharsStr}, "The slug contains invalid characters");
        }

        if (shortUrlService.isUrlCreationOverLimit(owner, owner.getUrlCreationLimit())) {
            result.rejectValue("slug", "error.newUriForm.user.limit");
        }

        if (result.hasErrors()) {
            result.getModel().forEach(mav.getModelMap()::addAttribute);
            mav.setViewName("/user/uris/newUri");
            mav.setStatus(HttpStatus.BAD_REQUEST);
            return mav;
        }
        form.setSlug(slug);

        shortUrlService.save(uri.get(), form, owner);

        mav.setViewName("redirect:/user/uris");
        return mav;
    }

    @GetMapping("/uris")
    public String getUris(@RequestParam(name = "p", defaultValue = "0") Integer pageParam,
                          @ModelAttribute("uuidShortUrlIdMap") Map<UUID, Long> uuidShortUrlIdMap,
                          Model model) {

        ShortzUser currentUser = (ShortzUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Page<ShortUrlDTO> uris = shortUrlService.findAllUrlsByUserToDTO(pageParam, DEFAULT_PAGE_SIZE, currentUser);
        model.addAttribute("urisPage", uris);

        if (uris.getTotalPages() > 0 && pageParam > uris.getTotalPages()) {
            return "redirect:/user/uris";
        }

        uuidShortUrlIdMap.clear();
        uris.forEach(url -> {
            UUID randomUUID = UUID.randomUUID();
            String urlId = url.getId();
            url.setId(randomUUID.toString());

            uuidShortUrlIdMap.put(randomUUID, Long.parseLong(urlId));
        });

        if (uris.getTotalPages() > 1) {
            int currentPage = uris.getNumber();

            List<Integer> pagination =
                    IntStream.rangeClosed(
                                    currentPage - NUMBER_PAGINATION_OPTIONS / 2,
                                    currentPage + NUMBER_PAGINATION_OPTIONS / 2)
                            .boxed()
                            .filter(pag -> pag < uris.getTotalPages())
                            .filter(pag -> pag >= 0)
                            .collect(Collectors.toList());

            model.addAttribute("pagination", pagination);
        }

        return "user/uris/uri_management";
    }

    @PostMapping("/uris/delete")
    public ModelAndView postDeleteUser(@RequestParam("id") UUID shortUrlUUID,
                                       @ModelAttribute("uuidShortUrlIdMap") Map<UUID, Long> uuidShortUrlIdMap,
                                       RedirectAttributes redirectAttributes) {

        ModelAndView mav = new ModelAndView();

        final UriComponents urlManagementUri = MvcUriComponentsBuilder
                .fromMethodName(ShortUrlController.class, "getUris", null, null, null)
                .buildAndExpand();

        if (!uuidShortUrlIdMap.containsKey(shortUrlUUID)) {
            uuidShortUrlIdMap.clear();
            mav.setViewName("redirect:" + urlManagementUri);
            return mav;
        }

        Long shortUrlId = uuidShortUrlIdMap.get(shortUrlUUID);

        shortUrlService.deleteById(shortUrlId);
        redirectAttributes.addFlashAttribute("message", "uri.deleted");
        redirectAttributes.addFlashAttribute("messageType", "success");
        mav.setViewName("redirect:" + urlManagementUri);
        return mav;
    }

    @GetMapping("/adm/uris")
    public String getSystemUris(@RequestParam(name = "p", defaultValue = "0") Integer pageParam,
                                @ModelAttribute("uuidShortUrlIdMap") Map<UUID, Long> uuidShortUrlIdMap,
                                Model model) {

        Page<ShortUrlDTO> uris = shortUrlService.findAllToDTO(pageParam, DEFAULT_PAGE_SIZE);
        model.addAttribute("urisPage", uris);

        if (uris.getTotalPages() > 0 && pageParam > uris.getTotalPages()) {
            return "redirect:/user/adm/uris";
        }

        uuidShortUrlIdMap.clear();
        uris.forEach(uri -> {
            UUID randomUUID = UUID.randomUUID();
            String uriId = uri.getId();
            uri.setId(randomUUID.toString());

            uuidShortUrlIdMap.put(randomUUID, Long.parseLong(uriId));
        });

        if (uris.getTotalPages() > 1) {
            int currentPage = uris.getNumber();

            List<Integer> pagination =
                    IntStream.rangeClosed(
                                    currentPage - NUMBER_PAGINATION_OPTIONS / 2,
                                    currentPage + NUMBER_PAGINATION_OPTIONS / 2)
                            .boxed()
                            .filter(pag -> pag < uris.getTotalPages())
                            .filter(pag -> pag >= 0)
                            .collect(Collectors.toList());

            model.addAttribute("pagination", pagination);
        }

        return "user/adm/adm_uri_management";
    }

    @PostMapping("adm/uris/delete")
    public ModelAndView postDeleteSystemUrl(@RequestParam("id") UUID shortUrlUUID,
                                            @ModelAttribute("uuidShortUrlIdMap") Map<UUID, Long> uuidShortUrlIdMap,
                                            RedirectAttributes redirectAttributes) {

        ModelAndView mav = new ModelAndView();

        final UriComponents systemUriManagementUri = MvcUriComponentsBuilder
                .fromMethodName(ShortUrlController.class, "getSystemUris", null, null, null)
                .buildAndExpand();

        if (!uuidShortUrlIdMap.containsKey(shortUrlUUID)) {
            uuidShortUrlIdMap.clear();
            mav.setViewName("redirect:" + systemUriManagementUri);
            return mav;
        }

        Long shortUrlId = uuidShortUrlIdMap.get(shortUrlUUID);

        shortUrlService.deleteById(shortUrlId);
        redirectAttributes.addFlashAttribute("message", "uri.deleted");
        redirectAttributes.addFlashAttribute("messageType", "success");
        mav.setViewName("redirect:" + systemUriManagementUri);
        return mav;
    }

    @ResponseBody
    @PostMapping(value = "/uris/generate", consumes = "application/json", produces = "text/plain")
    public String postGenerateSlug(@RequestBody(required = false) Map<String, String> bodyParams) {
        if (bodyParams == null) throw new EmptyUriException("");
        if (!bodyParams.containsKey("uri") || bodyParams.get("uri").isBlank()) throw new EmptyUriException("");

        String encoded = validateUri(bodyParams.get("uri"))
                .map(validateUri -> urlShortener.encodeUri(validateUri).orElse(""))
                .orElse("");

        if (encoded.isBlank()) throw new InvalidUriException("");
        return encoded;
    }

    private Optional<URI> validateUri(String uriToValidate) {
        String DEFAULT_SCHEME = "https://";
        final URI uri;

        try {
            final boolean hasScheme = uriToValidate.contains("://");
            String schemePrefix = hasScheme ? "" : DEFAULT_SCHEME;
            String fullUri = schemePrefix + uriToValidate;

            if (!hasScheme) {
                new URL(fullUri);
            }

            if (!fullUri.contains(".") && !hasScheme) return Optional.empty();

            return Optional.of(new URI(fullUri));
        } catch (URISyntaxException | MalformedURLException  e) {
            return Optional.empty();
        }
    }

}