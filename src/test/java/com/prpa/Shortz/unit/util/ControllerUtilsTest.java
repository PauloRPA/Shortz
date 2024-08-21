package com.prpa.Shortz.unit.util;

import com.prpa.Shortz.util.ControllerUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ControllerUtilsTest {

    @Test
    @DisplayName("Deve retornar todos os itens de paginação disponíveis corretamente.")
    public void whenPagesAvailable_shouldReturnAllPaginationOptions() {
        //given
        final int NUMBER_PAGINATION_OPTIONS = 5; // show at most 5 pages at once for the user to select.
        final int PAGES_TOTAL = 99;
        final int CURRENT_PAGE = 10;
        final Integer[] expectedPaginationSequence = {8, 9, 10, 11, 12};

        //when
        List<Integer> pagination = ControllerUtils.getPagination(NUMBER_PAGINATION_OPTIONS, PAGES_TOTAL, CURRENT_PAGE);

        //then
        assertThat(pagination.size()).isEqualTo(expectedPaginationSequence.length);
        assertThat(pagination).containsSequence(expectedPaginationSequence);
        assertThat(pagination).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Deve retornar os itens de paginação que existem, excluindo o limite superior.")
    public void whenReachingTopPageLimit_shouldReturnAllPaginationOptionsButTheOnesThatDoNotExist() {
        //given
        final int NUMBER_PAGINATION_OPTIONS = 5;
        final int PAGES_TOTAL = 12; // Pagina 12 (indexando por 0) não existe, deve retornar 4 paginas
        final int CURRENT_PAGE = 10;
        final Integer[] expectedPaginationSequence = {8, 9, 10, 11};

        //when
        List<Integer> pagination = ControllerUtils.getPagination(NUMBER_PAGINATION_OPTIONS, PAGES_TOTAL, CURRENT_PAGE);

        //then
        assertThat(pagination.size()).isEqualTo(expectedPaginationSequence.length);
        assertThat(pagination).containsSequence(expectedPaginationSequence);
        assertThat(pagination).doesNotHaveDuplicates();
    }


    @Test
    @DisplayName("Deve retornar os itens de paginação que existem, excluindo o limite inferior.")
    public void whenReachingBottomPageLimit_shouldReturnAllPaginationOptionsButTheOnesThatDoNotExist() {
        //given
        final int NUMBER_PAGINATION_OPTIONS = 5;
        final int PAGES_TOTAL = 99;
        final int CURRENT_PAGE = 1; // Pagina -1 não existe, deve retornar 4 paginas
        final Integer[] expectedPaginationSequence = {0, 1, 2, 3};

        //when
        List<Integer> pagination = ControllerUtils.getPagination(NUMBER_PAGINATION_OPTIONS, PAGES_TOTAL, CURRENT_PAGE);

        //then
        assertThat(pagination.size()).isEqualTo(expectedPaginationSequence.length);
        assertThat(pagination).containsSequence(expectedPaginationSequence);
        assertThat(pagination).doesNotHaveDuplicates();
    }


    @Test
    @DisplayName("Deve retornar todas as paginas acima da atual, excluindo limite inferior.")
    public void whenOnFirstPage_shouldReturnJustPagesForward() {
        //given
        final int NUMBER_PAGINATION_OPTIONS = 5;
        final int PAGES_TOTAL = 99;
        final int CURRENT_PAGE = 0;
        final Integer[] expectedPaginationSequence = {0, 1, 2};

        //when
        List<Integer> pagination = ControllerUtils.getPagination(NUMBER_PAGINATION_OPTIONS, PAGES_TOTAL, CURRENT_PAGE);

        //then
        assertThat(pagination.size()).isEqualTo(expectedPaginationSequence.length);
        assertThat(pagination).containsSequence(expectedPaginationSequence);
        assertThat(pagination).doesNotHaveDuplicates();
    }


    @Test
    @DisplayName("Deve retornar todas as paginas abaixo da atual, excluindo limite superior.")
    public void whenOnLastPage_shouldReturnJustPagesBackward() {
        //given
        final int NUMBER_PAGINATION_OPTIONS = 5;
        final int PAGES_TOTAL = 10;
        final int CURRENT_PAGE = 9;
        final Integer[] expectedPaginationSequence = {7, 8, 9};

        //when
        List<Integer> pagination = ControllerUtils.getPagination(NUMBER_PAGINATION_OPTIONS, PAGES_TOTAL, CURRENT_PAGE);

        //then
        assertThat(pagination.size()).isEqualTo(expectedPaginationSequence.length);
        assertThat(pagination).containsSequence(expectedPaginationSequence);
        assertThat(pagination).doesNotHaveDuplicates();
    }

}
