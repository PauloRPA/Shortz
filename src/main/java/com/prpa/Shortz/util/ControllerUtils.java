package com.prpa.Shortz.util;

import org.springframework.validation.ObjectError;

public class ControllerUtils {
    /**
     * @param globalError     Erro o qual a partir de sua mensagem sera extraido o campo que pertence.
     * @param fieldIdentifier Bloco de texto da mensagem (separado por pontos) anterior ao nome do campo
     *                        ex: error.match.username -> (fieldIdentifier = match) -> username
     *                        ex: error.yay.password.confirm -> (fieldIdentifier = yay) -> password.confirm
     * @return String representando os campos descrito após o "fieldIdentifier"
     */
    public static String globalErrorToFieldByMessage(ObjectError globalError, String fieldIdentifier) {
        if (globalError.getDefaultMessage() == null || globalError.getDefaultMessage().isBlank()) return "";

        String message = globalError.getDefaultMessage();
        message = message.substring(message.indexOf(fieldIdentifier));

        final int dotIndex = message.indexOf('.');
        return dotIndex == -1 ? "" : message.substring(dotIndex + 1);
    }
}
