package com.prpa.Shortz.controller;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.dto.ShortzUserDTO;
import com.prpa.Shortz.model.enums.Role;
import com.prpa.Shortz.model.form.ShortzUserForm;
import com.prpa.Shortz.service.ShortzUserService;
import com.prpa.Shortz.util.ControllerUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/user")
@SessionAttributes("uuidToUsernameMap")
public class ShortzUserController {

    public static final int DEFAULT_PAGE_SIZE = 8;
    public static final int NUMBER_PAGINATION_OPTIONS = 5;

    private final ShortzUserService shortzUserService;

    @Autowired
    public ShortzUserController(ShortzUserService shortzUserService) {
        this.shortzUserService = shortzUserService;
    }

    @ModelAttribute("uuidToUsernameMap")
    public Map<UUID, String> idMap() {
        return new HashMap<>();
    }

    @GetMapping("/login")
    public String login(){
        return "user/login";
    }

    @GetMapping("/register")
    public String register(Model model){
        model.addAttribute("userForm", new ShortzUserForm());
        return "user/register";
    }

    @PostMapping("/register")
    public ModelAndView registerNewUser(@Valid @ModelAttribute("userForm") ShortzUserForm form,
                                        BindingResult result) {
        ModelAndView mav = new ModelAndView();

        result.getGlobalErrors().stream()
                .forEach(globalFieldMatchError -> {
                    String field = ControllerUtils.globalErrorToFieldByMessage(globalFieldMatchError, "match");
                    if (!field.isBlank())
                        result.rejectValue(field, globalFieldMatchError.getDefaultMessage(), "The fields must match.");
                });

        if (shortzUserService.existsByUsernameIgnoreCase(form.getUsername())) {
            result.rejectValue("username", "error.exists");
        }

        if (shortzUserService.existsByEmailIgnoreCase(form.getEmail())) {
            result.rejectValue("email","error.exists");
        }

        if (result.hasErrors()) {
            UriComponents registerNewUserUri = MvcUriComponentsBuilder
                    .fromMethodName(ShortzUserController.class, "registerNewUser", form, result)
                    .buildAndExpand();

            result.getModel().forEach(mav.getModel()::put);
            mav.getModel().put("userForm", form);
            mav.setViewName("user/register");
            return mav;
        }

        ShortzUser newUser = new ShortzUser();
        newUser.setUsername(form.getUsername());
        newUser.setPassword(form.getPassword());
        newUser.setEmail(form.getEmail());
        shortzUserService.save(newUser);

        mav.setViewName("redirect:/");
        return mav;
    }

    @GetMapping("/adm/admin_panel")
    public String adminPanel(@RequestParam("p") Optional<Integer> pageParam, Model model,
                             @ModelAttribute("uuidToUsernameMap") Map<UUID, String> idUsernameMap){
        final int page = pageParam.orElseGet(() -> 0);
        Page<ShortzUserDTO> users = shortzUserService.findAll(page, DEFAULT_PAGE_SIZE);
        model.addAttribute("userPage", users);

        idUsernameMap.clear();
        users.forEach(user -> {
            idUsernameMap.put(user.getId(), user.getUsername());
        });

        if (users.getTotalPages() > 1) {
            int currentPage = users.getNumber();

            List<Integer> pagination =
                    IntStream.rangeClosed(
                                    currentPage - NUMBER_PAGINATION_OPTIONS / 2,
                                    currentPage + NUMBER_PAGINATION_OPTIONS / 2)
                            .boxed()
                            .filter(pag -> pag < users.getTotalPages())
                            .filter(pag -> pag >= 0)
                            .collect(Collectors.toList());

            model.addAttribute("pagination", pagination);
        }

        String shouldRedirect = users.getNumberOfElements() == 0 ? "redirect:/" : "";
        return shouldRedirect + "user/adm/admin_panel";
    }

    @GetMapping("/adm/edit")
    public ModelAndView editUser(@RequestParam("id") UUID userUUID,
                                 @ModelAttribute("uuidToUsernameMap") Map<UUID, String> uuidUsernameMap,
                                 ModelAndView mav) {

        final UriComponents adminPanelUri = MvcUriComponentsBuilder
                .fromMethodName(ShortzUserController.class, "adminPanel", null, null, null)
                .buildAndExpand();


        if (!uuidUsernameMap.containsKey(userUUID)) {
            uuidUsernameMap.clear();
            mav.setViewName("redirect:" + adminPanelUri);
            return mav;
        }

        Optional<ShortzUser> userFound = shortzUserService.findByUsername(uuidUsernameMap.get(userUUID));
        if (userFound.isEmpty()) {
            uuidUsernameMap.clear();
            mav.setViewName("redirect:" + adminPanelUri);
            return mav;
        }

        ShortzUser user = userFound.get();
        ShortzUserDTO form = ShortzUserDTO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .urlCount(user.getUrlCount())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .id(userUUID)
                .build();

        mav.getModel().put("editForm", form);
        mav.getModel().put("allRoles", Role.values());
        mav.setViewName("user/adm/edit");

        return mav;
    }

    @PostMapping("/adm/update")
    public String updateUser(@Valid @ModelAttribute("editForm") ShortzUserDTO editFormUser, BindingResult result,
                             @ModelAttribute("uuidToUsernameMap") Map<UUID, String> idmap,
                             Model model) {

        Optional<ShortzUser> userFound = shortzUserService.findByUsername(idmap.get(editFormUser.getId()));

        if (userFound.isEmpty()) {
            idmap.clear();
            return "redirect:" + MvcUriComponentsBuilder
                    .fromMethodName(ShortzUserController.class,"adminPanel",null, null, null)
                    .buildAndExpand().getPath();
        }

        if (editFormUser.getUsername() != null) {
            final boolean isNotRenamingSameUser = !editFormUser.getUsername().equalsIgnoreCase(userFound.get().getUsername());

            if (shortzUserService.existsByUsernameIgnoreCase(editFormUser.getUsername()) && isNotRenamingSameUser) {
                result.rejectValue("username", "error.exists");
            }
        }

        if (editFormUser.getEmail() != null) {
            final boolean isNotChangingCurrentUserEmail = !editFormUser.getEmail().equalsIgnoreCase(userFound.get().getEmail());

            if (shortzUserService.existsByEmailIgnoreCase(editFormUser.getEmail()) && isNotChangingCurrentUserEmail) {
                result.rejectValue("email","error.exists");
            }
        }

        if (result.hasErrors()) {
            result.getModel().forEach(model::addAttribute);
            model.addAttribute("allRoles", Role.values());
            return "user/adm/edit";
        }

        shortzUserService.update(idmap.get(editFormUser.getId()), editFormUser);

        idmap.clear();
        return "redirect:" + MvcUriComponentsBuilder
                .fromMethodName(ShortzUserController.class,"adminPanel",null, null, null)
                .buildAndExpand().getPath();
    }
}
