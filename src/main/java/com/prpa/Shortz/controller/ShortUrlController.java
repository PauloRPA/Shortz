package com.prpa.Shortz.controller;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.dto.ShortUrlDTO;
import com.prpa.Shortz.service.ShortUrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/user")
@SessionAttributes("uuidShortUrlIdMap")
public class ShortUrlController {

    private static final Integer DEFAULT_PAGE_SIZE = 15;
    private static final Integer NUMBER_PAGINATION_OPTIONS = 5;

    private final ShortUrlService shortUrlService;

    @Autowired
    public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @ModelAttribute("uuidShortUrlIdMap")
    public Map<UUID, Long> getUUIDShortUrlMap() {
        return new HashMap<>();
    }

    @GetMapping("/urls")
    public String getUrls(@RequestParam(name = "p", defaultValue = "0") Integer pageParam,
                          @ModelAttribute("uuidShortUrlIdMap") Map<UUID, Long> uuidShortUrlIdMap,
                          Model model) {

        ShortzUser currentUser = (ShortzUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Page<ShortUrlDTO> urls = shortUrlService.findAllUrlsByUserToDTO(pageParam, DEFAULT_PAGE_SIZE, currentUser);
        model.addAttribute("urlsPage", urls);

        if (urls.getTotalPages() > 0 && pageParam > urls.getTotalPages()) {
            return "redirect:/user/urls";
        }

        uuidShortUrlIdMap.clear();
        urls.forEach(url -> {
            UUID randomUUID = UUID.randomUUID();
            String urlId = url.getId();
            url.setId(randomUUID.toString());

            uuidShortUrlIdMap.put(randomUUID, Long.parseLong(urlId));
        });

        if (urls.getTotalPages() > 1) {
            int currentPage = urls.getNumber();

            List<Integer> pagination =
                    IntStream.rangeClosed(
                                    currentPage - NUMBER_PAGINATION_OPTIONS / 2,
                                    currentPage + NUMBER_PAGINATION_OPTIONS / 2)
                            .boxed()
                            .filter(pag -> pag < urls.getTotalPages())
                            .filter(pag -> pag >= 0)
                            .collect(Collectors.toList());

            model.addAttribute("pagination", pagination);
        }

        return "user/urls/url_management";
    }

    @PostMapping("/urls/delete")
    public ModelAndView postDeleteUser(@RequestParam("id") UUID shortUrlUUID,
                                       @ModelAttribute("uuidShortUrlIdMap") Map<UUID, Long> uuidShortUrlIdMap,
                                       RedirectAttributes redirectAttributes) {

        ModelAndView mav = new ModelAndView();

        final UriComponents urlManagementUri = MvcUriComponentsBuilder
                .fromMethodName(ShortUrlController.class, "getUrls", null, null, null)
                .buildAndExpand();

        if (!uuidShortUrlIdMap.containsKey(shortUrlUUID)) {
            uuidShortUrlIdMap.clear();
            mav.setViewName("redirect:" + urlManagementUri);
            return mav;
        }

        Long shortUrlId = uuidShortUrlIdMap.get(shortUrlUUID);

        shortUrlService.deleteById(shortUrlId);
        redirectAttributes.addFlashAttribute("message", "url.deleted");
        redirectAttributes.addFlashAttribute("messageType", "success");
        mav.setViewName("redirect:" + urlManagementUri);
        return mav;
    }

    @GetMapping("/adm/urls")
    public String getSystemUrls(@RequestParam(name = "p", defaultValue = "0") Integer pageParam,
                                @ModelAttribute("uuidShortUrlIdMap") Map<UUID, Long> uuidShortUrlIdMap,
                                Model model) {

        Page<ShortUrlDTO> urls = shortUrlService.findAllToDTO(pageParam, DEFAULT_PAGE_SIZE);
        model.addAttribute("urlsPage", urls);

        if (urls.getTotalPages() > 0 && pageParam > urls.getTotalPages()) {
            return "redirect:/user/adm/urls";
        }

        uuidShortUrlIdMap.clear();
        urls.forEach(url -> {
            UUID randomUUID = UUID.randomUUID();
            String urlId = url.getId();
            url.setId(randomUUID.toString());

            uuidShortUrlIdMap.put(randomUUID, Long.parseLong(urlId));
        });

        if (urls.getTotalPages() > 1) {
            int currentPage = urls.getNumber();

            List<Integer> pagination =
                    IntStream.rangeClosed(
                                    currentPage - NUMBER_PAGINATION_OPTIONS / 2,
                                    currentPage + NUMBER_PAGINATION_OPTIONS / 2)
                            .boxed()
                            .filter(pag -> pag < urls.getTotalPages())
                            .filter(pag -> pag >= 0)
                            .collect(Collectors.toList());

            model.addAttribute("pagination", pagination);
        }

        return "user/adm/adm_url_management";
    }

    @PostMapping("adm/urls/delete")
    public ModelAndView postDeleteSystemUrl(@RequestParam("id") UUID shortUrlUUID,
                                            @ModelAttribute("uuidShortUrlIdMap") Map<UUID, Long> uuidShortUrlIdMap,
                                            RedirectAttributes redirectAttributes) {

        ModelAndView mav = new ModelAndView();

        final UriComponents systemUrlManagementUri = MvcUriComponentsBuilder
                .fromMethodName(ShortUrlController.class, "getSystemUrls", null, null, null)
                .buildAndExpand();

        if (!uuidShortUrlIdMap.containsKey(shortUrlUUID)) {
            uuidShortUrlIdMap.clear();
            mav.setViewName("redirect:" + systemUrlManagementUri);
            return mav;
        }

        Long shortUrlId = uuidShortUrlIdMap.get(shortUrlUUID);

        shortUrlService.deleteById(shortUrlId);
        redirectAttributes.addFlashAttribute("message", "url.deleted");
        redirectAttributes.addFlashAttribute("messageType", "success");
        mav.setViewName("redirect:" + systemUrlManagementUri);
        return mav;
    }

}