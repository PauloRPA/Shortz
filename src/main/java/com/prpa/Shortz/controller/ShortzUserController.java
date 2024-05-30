package com.prpa.Shortz.controller;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.form.ShortzUserForm;
import com.prpa.Shortz.model.validation.ShortzUserFormValidator;
import com.prpa.Shortz.service.ShortzUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/user")
public class ShortzUserController {

    private final ShortzUserService shortzUserService;

    private final ShortzUserFormValidator shortzUserFormValidator;

    @Autowired
    public ShortzUserController(ShortzUserService shortzUserService, ShortzUserFormValidator shortzUserFormValidator) {
        this.shortzUserService = shortzUserService;
        this.shortzUserFormValidator = shortzUserFormValidator;
    }

    @ModelAttribute("userForm")
    public ShortzUserForm addDefaultShortzUserForm() {
        return new ShortzUserForm();
    }

    @InitBinder
    public void initBinderValidator(WebDataBinder binder) {
        binder.addValidators(shortzUserFormValidator);
    }

    @GetMapping("/login")
    public String login(){
        return "login";
    }

    @GetMapping("/register")
    public String register(){
        return "register";
    }

    @PostMapping("/register")
    public ModelAndView registerNewUser(@Valid @ModelAttribute("userForm") ShortzUserForm form) {
        ModelAndView mav = new ModelAndView();
        mav.getModel().put("userForm", form);

        ShortzUser newUser = new ShortzUser();
        newUser.setUsername(form.getUsername());
        newUser.setPassword(form.getPassword());
        newUser.setEmail(form.getEmail());
        shortzUserService.save(newUser);

        mav.setViewName("redirect:/");
        return mav;
    }



}
