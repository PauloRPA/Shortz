package com.prpa.Shortz.controller;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.dto.ShortzUserDTO;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/user")
public class ShortzUserController {

    public static final int DEFAULT_PAGE_SIZE = 8;
    public static final int NUMBER_PAGINATION_OPTIONS = 5;

    private final ShortzUserService shortzUserService;

    @Autowired
    public ShortzUserController(ShortzUserService shortzUserService) {
        this.shortzUserService = shortzUserService;
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
    public String adminPanel(@RequestParam("p") Optional<Integer> pageParam, Model model){
        final int page = pageParam.orElseGet(() -> 0);
        Page<ShortzUserDTO> users = shortzUserService.findAll(page, DEFAULT_PAGE_SIZE);
        model.addAttribute("userPage", users);

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



}
