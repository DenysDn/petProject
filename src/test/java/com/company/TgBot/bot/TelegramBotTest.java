package com.company.TgBot.bot;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@SpringBootTest
class TelegramBotTest {
    @InjectMocks
    private TelegramBot telegramBot;


    @Test
    void parseStringToDoubleTest() {
        Double aDouble = telegramBot.parseStringToDouble("83 jia");
        Double aDouble2 = telegramBot.parseStringToDouble("8313.jia2.31@");

        assertEquals(83, aDouble);
        assertNull(aDouble2);
    }

}