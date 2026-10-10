package com.example.dailyquotes.service;

import com.example.dailyquotes.domain.Quote;
import com.example.dailyquotes.dto.QuoteRequest;
import com.example.dailyquotes.exception.DuplicateQuoteException;
import com.example.dailyquotes.mapper.QuoteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    @Mock
    private QuoteMapper quoteMapper;

    @InjectMocks
    private QuoteService quoteService;

    @Test
    void shouldRejectDuplicateQuote() {
        QuoteRequest request = new QuoteRequest("千里之行，始于足下。", "老子", "《道德经》", 98, false);
        when(quoteMapper.existsByFingerprint(anyString())).thenReturn(true);

        assertThatThrownBy(() -> quoteService.create(request))
                .isInstanceOf(DuplicateQuoteException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    void shouldCreateManualQuote() {
        QuoteRequest request = new QuoteRequest("测试内容", "测试作者", "测试出处", 80, false);
        when(quoteMapper.existsByFingerprint(anyString())).thenReturn(false);
        when(quoteMapper.insert(any(Quote.class))).thenAnswer(invocation -> {
            Quote q = invocation.getArgument(0);
            q.setId(100L);
            return 1;
        });

        var response = quoteService.create(request);

        assertThat(response.origin()).isEqualTo("MANUAL");
        assertThat(response.content()).isEqualTo("测试内容");
    }
}
