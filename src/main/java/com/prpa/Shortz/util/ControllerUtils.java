package com.prpa.Shortz.util;

import org.springframework.validation.ObjectError;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ControllerUtils {
    /**
     * @param globalError     Erro o qual a partir de sua mensagem sera extraída o campo que pertence.
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

    public static List<Integer> getPagination(int numberPaginationOptions, int maxNumberOfPagesAvailable, int currentPage) {
        return IntStream.rangeClosed(
                                currentPage - numberPaginationOptions / 2,
                                currentPage + numberPaginationOptions / 2)
                        .boxed()
                        .filter(pag -> pag < maxNumberOfPagesAvailable)
                        .filter(pag -> pag >= 0)
                        .collect(Collectors.toList());
    }

}
