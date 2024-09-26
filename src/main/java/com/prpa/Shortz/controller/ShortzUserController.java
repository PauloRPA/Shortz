package com.prpa.Shortz.controller;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.dto.ShortzUserDTO;
import com.prpa.Shortz.model.enums.Role;
import com.prpa.Shortz.model.form.ShortzUserEditForm;
import com.prpa.Shortz.model.form.ShortzUserForm;
import com.prpa.Shortz.model.validation.ShortzUserEditFormValidator;
import com.prpa.Shortz.model.validation.ShortzUserFormValidator;
import com.prpa.Shortz.repository.specification.ResourceSpecification;
import com.prpa.Shortz.service.ShortzUserService;
import com.prpa.Shortz.util.ControllerUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

import static com.prpa.Shortz.model.enums.Role.ADMIN;
import static com.prpa.Shortz.repository.query.SearchOperation.LIKE;

@Controller
@RequestMapping("/user")
@SessionAttributes("uuidToUsernameMap")
public class ShortzUserController {

    public static final int DEFAULT_PAGE_SIZE = 8;
    public static final int NUMBER_PAGINATION_OPTIONS = 5;

    private final ShortzUserService shortzUserService;
    private final ShortzUserFormValidator shortzUserFormValidator;
    private final ShortzUserEditFormValidator shortzUserEditFormValidator;

    @Autowired
    public ShortzUserController(ShortzUserService shortzUserService, ShortzUserFormValidator shortzUserFormValidator, ShortzUserEditFormValidator shortzUserEditFormValidator) {
        this.shortzUserService = shortzUserService;
        this.shortzUserFormValidator = shortzUserFormValidator;
        this.shortzUserEditFormValidator = shortzUserEditFormValidator;
    }

    @ModelAttribute("uuidToUsernameMap")
    public Map<UUID, String> uuidToUsernameMap() {
        return new HashMap<>();
    }

    @GetMapping("/login")
    public String getLogin(){
        return "user/login";
    }

    @GetMapping("/register")
    public String getRegister(Model model){
        model.addAttribute("userForm", new ShortzUserForm());
        return "user/register";
    }

    @PostMapping("/register")
    public ModelAndView postNewUser(@Valid @ModelAttribute("userForm") ShortzUserForm form,
                                    Errors errors) {
        ModelAndView mav = new ModelAndView();
        shortzUserFormValidator.validate(form, errors);

        if (errors.hasErrors()) {
            errors.getAllErrors().forEach(mav.getModelMap()::addAttribute);
            mav.getModel().put("userForm", form);
            mav.setViewName("user/register");
            return mav;
        }

        ShortzUser newUser = new ShortzUser();
        newUser.setUsername(form.getUsername().trim());
        newUser.setPassword(form.getPassword());
        newUser.setEmail(form.getEmail().trim());
        shortzUserService.save(newUser);

        mav.setViewName("redirect:/");
        return mav;
    }

    @GetMapping("/adm")
    public ModelAndView getUserManagement(@RequestParam(value = "p", defaultValue = "0") int page,
                                          @ModelAttribute("uuidToUsernameMap") Map<UUID, String> idUsernameMap,
                                          @RequestParam(name = "search", defaultValue = "") String searchParam) {

        ModelAndView mav = new ModelAndView();

        Optional<Specification<ShortzUser>> search = ResourceSpecification.builder(ShortzUser.class)
                .or("username", LIKE, searchParam)
                .or("email", LIKE, searchParam)
                .build();

        Page<ShortzUserDTO> usersPage = search.isPresent() ?
                shortzUserService.findAll(page, DEFAULT_PAGE_SIZE, search.get()) :
                shortzUserService.findAll(page, DEFAULT_PAGE_SIZE);
        mav.getModelMap().addAttribute("userPage", usersPage);

        if (usersPage.getTotalPages() > 0 && page > usersPage.getTotalPages()) {
            mav.setViewName("redirect:/user/adm");
            return mav;
        }

        idUsernameMap.clear();
        usersPage.forEach(user -> {
            idUsernameMap.put(user.getId(), user.getUsername());
        });

        if (usersPage.getTotalPages() > 1) {
            int currentPage = usersPage.getNumber();
            int totalPages = usersPage.getTotalPages();

            List<Integer> pagination = ControllerUtils.getPagination(NUMBER_PAGINATION_OPTIONS, totalPages, currentPage);

            mav.getModelMap().addAttribute("pagination", pagination);
        }

        mav.setViewName("user/adm/user_management");
        return mav;
    }

    @GetMapping("/adm/edit")
    public ModelAndView getEditUserForm(@RequestParam("id") UUID userUUID,
                                        @ModelAttribute("uuidToUsernameMap") Map<UUID, String> uuidUsernameMap,
                                        ModelAndView mav) {

        if (!uuidUsernameMap.containsKey(userUUID)) {
            return redirectToManagementAndClear(mav, uuidUsernameMap);
        }

        Optional<ShortzUser> userFound = shortzUserService.findByUsername(uuidUsernameMap.get(userUUID));
        if (userFound.isEmpty()) {
            return redirectToManagementAndClear(mav, uuidUsernameMap);
        }

        ShortzUser user = userFound.get();
        ShortzUserEditForm form = ShortzUserEditForm.builder()
                .id(userUUID.toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .urlCreationLimit(user.getUrlCreationLimit())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .build();

        mav.getModel().put("editForm", form);
        mav.getModel().put("allRoles", Role.values());
        mav.setViewName("user/adm/edit");

        return mav;
    }

    @PostMapping("/adm/update")
    public ModelAndView postUpdateUser(@Valid @ModelAttribute("editForm") ShortzUserEditForm editFormUser,
                                       Errors errors,
                                 @ModelAttribute("uuidToUsernameMap") Map<UUID, String> idmap,
                                       ModelAndView mav) {

        String targetUserUsername = idmap.get(UUID.fromString(editFormUser.getId()));
        Optional<ShortzUser> userFound = shortzUserService.findByUsername(targetUserUsername);

        String targetUserId = userFound
                .map(ShortzUser::getId)
                .map(String::valueOf)
                .orElse(null);
        editFormUser.setId(targetUserId);
        shortzUserEditFormValidator.validate(editFormUser, errors);

        if (userFound.isEmpty()) {
            return redirectToManagementAndClear(mav, idmap);
        }

        if (errors.hasErrors()) {
            errors.getAllErrors().forEach(mav.getModelMap()::addAttribute);
            mav.getModelMap().addAttribute("allRoles", Role.values());
            mav.setViewName("user/adm/edit");
            return mav;
        }

        shortzUserService.update(targetUserUsername, editFormUser);

        return redirectToManagementAndClear(mav, idmap);
    }

    @PostMapping("/adm/delete")
    public ModelAndView postDeleteUser(@RequestParam("id") UUID userUUID,
                                       @ModelAttribute("uuidToUsernameMap") Map<UUID, String> uuidUsernameMap,
                                       ModelAndView mav,
                                       RedirectAttributes redirectAttributes) {

        if (!uuidUsernameMap.containsKey(userUUID)) {
            return redirectToManagementAndClear(mav, uuidUsernameMap);
        }

        Optional<ShortzUser> userFound = shortzUserService.findByUsername(uuidUsernameMap.get(userUUID));
        if (userFound.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", false);
            return redirectToManagementAndClear(mav, uuidUsernameMap);
        }

        final boolean isLastAdmin = userFound.get().getRole().equals(ADMIN) &&
                shortzUserService.countUsersByRole(ADMIN) == 1;

        if (isLastAdmin) {
            redirectAttributes.addFlashAttribute("message", "error.delete.admin");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return redirectToManagementAndClear(mav, uuidUsernameMap);
        }

        boolean isDeleted = shortzUserService.deleteByUsername(uuidUsernameMap.get(userUUID));
        redirectAttributes.addFlashAttribute("message", "user.deleted");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return redirectToManagementAndClear(mav, uuidUsernameMap);
    }

    private ModelAndView redirectToManagementAndClear(ModelAndView mav, Map<UUID, String> idmap) {
        idmap.clear();
        mav.setViewName("redirect:" + MvcUriComponentsBuilder
                .fromMethodName(ShortzUserController.class, "getUserManagement", null, null, null)
                .buildAndExpand().getPath());

        return mav;
    }
}
