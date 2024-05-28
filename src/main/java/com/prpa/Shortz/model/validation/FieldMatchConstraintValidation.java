package com.prpa.Shortz.model.validation;

import com.prpa.Shortz.model.annotations.FieldMatch;
import com.prpa.Shortz.model.exceptions.NoGettersFoundForMatchingFields;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static java.lang.Character.toUpperCase;


public class FieldMatchConstraintValidation implements ConstraintValidator<FieldMatch, Object> {

    private String getterFieldName;

    private String getterConfirmFieldName;

    private String fieldName;

    private String confirmFieldName;

    @Override
    public void initialize(FieldMatch constraintAnnotation) {
        fieldName = constraintAnnotation.fieldName();
        confirmFieldName = constraintAnnotation.confirmFieldName();

        getterFieldName = "get" + toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        getterConfirmFieldName = "get" + toUpperCase(confirmFieldName.charAt(0)) + confirmFieldName.substring(1);

        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Object toBeValidated, ConstraintValidatorContext context) {
        Method[] declaredMethods = toBeValidated.getClass().getDeclaredMethods();
        List<Method> fieldMethods = Arrays.stream(declaredMethods)
                .filter(method -> method.getName().equals(getterFieldName) ||
                        method.getName().equals(getterConfirmFieldName))
                .toList();

        if (fieldMethods.size() < 2) {
            String msg = "No getters found for fields: %s and %s. The getter methods are %s and %s.";
            throw new NoGettersFoundForMatchingFields(String.format(msg,
                    fieldName, confirmFieldName, getterFieldName, getterConfirmFieldName));
        }

        try {
            return fieldMethods.get(0).invoke(toBeValidated).equals(fieldMethods.get(1).invoke(toBeValidated));
        } catch (IllegalAccessException | InvocationTargetException  e) {
            return false;
        }
    }

}
