package com.prpa.Shortz.controller;

import com.prpa.Shortz.model.ShortUrl;
import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.dto.ShortUrlDTO;
import com.prpa.Shortz.model.exceptions.EmptyUriException;
import com.prpa.Shortz.model.exceptions.InvalidUriException;
import com.prpa.Shortz.model.form.ShortUrlForm;
import com.prpa.Shortz.model.shortener.contract.UrlShortener;
import com.prpa.Shortz.model.validation.ShortUrlFormValidator;
import com.prpa.Shortz.repository.query.Search;
import com.prpa.Shortz.repository.specification.ShortUrlSpecification;
import com.prpa.Shortz.service.ShortUrlService;
import com.prpa.Shortz.util.ControllerUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

import static com.prpa.Shortz.model.validation.ShortUrlFormValidator.toValidUriFormat;
import static com.prpa.Shortz.repository.query.SearchOperation.EQUALS;
import static com.prpa.Shortz.repository.query.SearchOperation.LIKE;

@Controller
@RequestMapping("/user")
@SessionAttributes("urlDTOIdMap")
public class ShortUrlController {

    public static final String USER_URIS = "/user/uris";
    public static final String SYSTEM_URIS = "/user/adm/uris";

    private static final Integer DEFAULT_PAGE_SIZE = 2;
    private static final Integer NUMBER_PAGINATION_OPTIONS = 5;

    private final ShortUrlService shortUrlService;
    private final UrlShortener urlShortener;
    private final ShortUrlFormValidator shortUrlFormValidator;

    @Autowired
    public ShortUrlController(ShortUrlService shortUrlService, UrlShortener urlShortener, ShortUrlFormValidator shortUrlFormValidator) {
        this.shortUrlService = shortUrlService;
        this.urlShortener = urlShortener;
        this.shortUrlFormValidator = shortUrlFormValidator;
    }

    @ModelAttribute("urlDTOIdMap")
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
                                   Errors errors,
                                   @AuthenticationPrincipal ShortzUser owner) {
        ModelAndView mav = new ModelAndView();
        shortUrlFormValidator.validate(form, errors);

        if (errors.hasErrors()) {
            errors.getAllErrors().forEach(mav.getModelMap()::addAttribute);
            mav.setViewName("/user/uris/newUri");
            mav.setStatus(HttpStatus.BAD_REQUEST);
            return mav;
        }

        shortUrlService.save(form, owner);

        mav.setViewName("redirect:/user/uris");
        return mav;
    }

    @GetMapping("/uris")
    public String getUris(@RequestParam(name = "p", defaultValue = "0") Integer pageParam,
                          @ModelAttribute("urlDTOIdMap") Map<UUID, Long> urlDTOIdMap,
                          @RequestParam(name = "search", defaultValue = "") String searchParam,
                          Model model) {

        Optional<Specification<ShortUrl>> search = Optional.empty();
        if (!searchParam.isBlank()) {
            searchParam = searchParam.trim();
            var slugEquals = new ShortUrlSpecification(new Search("slug", EQUALS, searchParam));
            var uriContains = new ShortUrlSpecification(new Search("uri", LIKE, searchParam));
            search = Optional.of(Specification.where(uriContains).or(slugEquals));
        }

        Page<ShortUrl> uriPage = search.isPresent() ?
                shortUrlService.findAll(pageParam, DEFAULT_PAGE_SIZE, search.get()) :
                shortUrlService.findAll(pageParam, DEFAULT_PAGE_SIZE);

        Page<ShortUrlDTO> uriDTOPage = shortUrlService.urlToDTOPage(uriPage);

        model.addAttribute("urisPage", uriDTOPage);
        if (uriPage.getTotalPages() > 0 && pageParam > uriPage.getTotalPages()) {
            return "redirect:/user/uris";
        }

        urlDTOIdMap.clear();
        Iterator<ShortUrl> uriIterator = uriPage.iterator();
        Iterator<ShortUrlDTO> dtoIterator = uriDTOPage.iterator();
        while (uriIterator.hasNext()) {
            UUID dtoId = UUID.fromString(dtoIterator.next().getId());
            urlDTOIdMap.put(dtoId, uriIterator.next().getId());
        }

        if (uriPage.getTotalPages() > 1) {
            final int currentPage = uriPage.getNumber();
            final int totalPages = uriPage.getTotalPages();

            var pagination = ControllerUtils.getPagination(NUMBER_PAGINATION_OPTIONS, totalPages, currentPage);
            model.addAttribute("pagination", pagination);
        }

        return "user/uris/uri_management";
    }

    @PostMapping("/uris/delete")
    public ModelAndView postDeleteUser(@RequestParam("id") UUID shortUrlUUID,
                                       @ModelAttribute("urlDTOIdMap") Map<UUID, Long> urlDTOIdMap,
                                       RedirectAttributes redirectAttributes) {

        ModelAndView mav = new ModelAndView();

        if (!urlDTOIdMap.containsKey(shortUrlUUID)) {
            urlDTOIdMap.clear();
            mav.setStatus(HttpStatus.FOUND);
            mav.setViewName("redirect:" + USER_URIS);
            return mav;
        }

        Long shortUrlId = urlDTOIdMap.get(shortUrlUUID);

        shortUrlService.deleteById(shortUrlId);
        redirectAttributes.addFlashAttribute("message", "uri.deleted");
        redirectAttributes.addFlashAttribute("messageType", "success");
        mav.setStatus(HttpStatus.FOUND);
        mav.setViewName("redirect:" + USER_URIS);
        return mav;
    }

    @GetMapping("/adm/uris")
    public String getSystemUris(@RequestParam(name = "p", defaultValue = "0") Integer pageParam,
                                @ModelAttribute("urlDTOIdMap") Map<UUID, Long> urlDTOIdMap,
                                @RequestParam(name = "search", defaultValue = "") String searchParam,
                                Model model) {

        Optional<Specification<ShortUrl>> search = Optional.empty();
        if (!searchParam.isBlank()) {
            searchParam = searchParam.trim();
            var slugEquals = new ShortUrlSpecification(new Search("slug", EQUALS, searchParam));
            var uriContains = new ShortUrlSpecification(new Search("uri", LIKE, searchParam));
            search = Optional.of(Specification.where(uriContains).or(slugEquals));
        }

        Page<ShortUrl> uriPage = search.isPresent() ?
                shortUrlService.findAll(pageParam, DEFAULT_PAGE_SIZE, search.get()) :
                shortUrlService.findAll(pageParam, DEFAULT_PAGE_SIZE);

        Page<ShortUrlDTO> uriDTOPage = shortUrlService.urlToDTOPage(uriPage);

        model.addAttribute("urisPage", uriDTOPage);
        if (uriPage.getTotalPages() > 0 && pageParam > uriPage.getTotalPages()) {
            return "redirect:/user/adm/uris";
        }

        urlDTOIdMap.clear();
        Iterator<ShortUrl> uriIterator = uriPage.iterator();
        Iterator<ShortUrlDTO> dtoIterator = uriDTOPage.iterator();
        while (uriIterator.hasNext()) {
            UUID dtoId = UUID.fromString(dtoIterator.next().getId());
            urlDTOIdMap.put(dtoId, uriIterator.next().getId());
        }

        if (uriPage.getTotalPages() > 1) {
            final int currentPage = uriPage.getNumber();
            final int totalPages = uriPage.getTotalPages();

            var pagination = ControllerUtils.getPagination(NUMBER_PAGINATION_OPTIONS, totalPages, currentPage);

            model.addAttribute("pagination", pagination);
        }

        return "user/adm/adm_uri_management";
    }

    @PostMapping("adm/uris/delete")
    public ModelAndView postDeleteSystemUrl(@RequestParam("id") UUID shortUrlUUID,
                                            @ModelAttribute("urlDTOIdMap") Map<UUID, Long> urlDTOIdMap,
                                            RedirectAttributes redirectAttributes) {

        ModelAndView mav = new ModelAndView();

        if (!urlDTOIdMap.containsKey(shortUrlUUID)) {
            urlDTOIdMap.clear();
            mav.setViewName("redirect:" + SYSTEM_URIS);
            return mav;
        }

        Long shortUrlId = urlDTOIdMap.get(shortUrlUUID);

        shortUrlService.deleteById(shortUrlId);
        redirectAttributes.addFlashAttribute("message", "uri.deleted");
        redirectAttributes.addFlashAttribute("messageType", "success");
        mav.setViewName("redirect:" + SYSTEM_URIS);
        return mav;
    }

    @ResponseBody
    @PostMapping(value = "/uris/generate", consumes = "application/json", produces = "text/plain")
    public String postGenerateSlug(@RequestBody(required = false) Map<String, String> bodyParams) {
        if (bodyParams == null) throw new EmptyUriException("");
        if (!bodyParams.containsKey("uri") || bodyParams.get("uri").isBlank()) throw new EmptyUriException("");

        String encoded = toValidUriFormat(bodyParams.get("uri"))
                .map(validateUri -> urlShortener.encodeUri(validateUri).orElse(""))
                .orElse("");

        if (encoded.isBlank()) throw new InvalidUriException("");
        return encoded;
    }

}