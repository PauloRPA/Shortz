package com.prpa.Shortz.controller;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.form.ShortzUserForm;
import com.prpa.Shortz.model.validation.ShortzUserFormValidator;
import com.prpa.Shortz.service.ShortzUserService;
import com.prpa.Shortz.util.ControllerUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

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

    @InitBinder
    public void initBinderValidator(WebDataBinder binder) {
        binder.addValidators(shortzUserFormValidator);
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



}
