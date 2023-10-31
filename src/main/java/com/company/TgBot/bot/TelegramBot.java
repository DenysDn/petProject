package com.company.TgBot.bot;


import com.company.TgBot.entity.Product;
import com.company.TgBot.service.WebScraperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    static final String BOT_NAME = "ez_sushi_bot";
    static final String START = "/start";
    static final String GET = "/get";
    static final String FILTER = "/filter";
    static final String HELP = "/help";
    static final String WEIGHT = "weight";
    static final String PRICE = "price";
    static final String BEFORE = "before";
    static final String FROM = "from";
    static final String ERROR_TEXT = "Error occurred: ";
    String textHelpMessage = "Что бы быстро найти желаемое блюдо выполните несколько действий : \n" +
            "1. Выберите из меню или напишите /filter \n" +
            "2. Укажите фильтры, если нужно (Желательно указывать, потому что список предложений не мал) \n" +
            "Конечно на свой страх и риск можете выбрать или написать команду /get" +
            " тогда вас засыпет всеми предложениями без фильтров \uD83D\uDE31 \n" +
            "Если выбрали /filter следуйте инструкциям что там будут. \n" +
            "В конце вы получите -имя товара- , -цену- , -вес- и -ссылку заведения- которое вас накормит. \n" +
            "От нас Приятного Аппетита и хорошего времени суток \uD83E\uDD17\uD83D\uDE0C";
    String textStartMessage = "Привет, %s \n" +
            "Давай приступим к поиску подходящего для тебя предложения суши-сетов \uD83D\uDE09 \n" +
            "Выбери /filter в меню или нажми на него в этом сообщении";
    static private int priceOrWeightState;
    private final WebScraperService webScraperService;
    private BotState botState = BotState.IDLE;

    public TelegramBot(@Value("${bot.token}") String botToken, WebScraperService webScraperService) {
        super(botToken);
        this.webScraperService = webScraperService;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Начать работу по поиску сетов "));
        listOfCommands.add(new BotCommand("/filter", "Указать фильтры( цена / вес ) "));
        listOfCommands.add(new BotCommand("/help", "Получить рекомендаций в использовании"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bots command list: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            var message = update.getMessage().getText();
            var chatId = update.getMessage().getChatId();
            if (botState == BotState.IDLE) {
                switch (message) {
                    case (START):
                        String userName = update.getMessage().getChat().getUserName();
                        getStartCommand(chatId, userName);
                        break;
                    case (FILTER):
                        log.info("use Filter");
                        replyKeyboardMarkup(chatId);
                        break;
                    case (GET):
                        getProduct(chatId, webScraperService.getSushi());
                        break;
                    case (HELP):
                        getHelpCommand(chatId);
                        break;
                    default:
                        sendMessage(chatId, "Извините эта команда не поддерживается, повторите другой запрос ");
                }
            } else if (botState == BotState.AWAITING_NUMBER_PRICE) {
                sortSushiSets(chatId, message, PRICE);
                botState = BotState.IDLE;
            } else if (botState == BotState.AWAITING_NUMBER_WEIGHT) {
                sortSushiSets(chatId, message, WEIGHT);
                botState = BotState.IDLE;
            }
        } else if (update.hasCallbackQuery()) {
            var chatId = update.getCallbackQuery().getMessage().getChatId();
            var messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callBackData = update.getCallbackQuery().getData();
            // create a couple of auxiliary buttons
            InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup("До", BEFORE,
                    "От", FROM);

            switch (callBackData) {

                case PRICE: {
                    String text = "Выберите ДО или ОТ какой суммы будем сортировать (грн): ";
                    executeEditMassageText(chatId, text, messageId, inlineKeyboardMarkup);
                    botState = BotState.AWAITING_NUMBER_PRICE;
                    break;
                }
                case WEIGHT: {
                    String text = "Выберите ДО или ОТ какого веса будем сортировать (г): ";
                    executeEditMassageText(chatId, text, messageId, inlineKeyboardMarkup);
                    botState = BotState.AWAITING_NUMBER_WEIGHT;
                    break;
                }
                case BEFORE: {
                    String text = "Теперь сумму/вес цифрами ДО какого кол-ва грн/г ";
                    executeEditMassageText(chatId, text, messageId, null);
                    priceOrWeightState = 0;
                    break;
                }
                case FROM: {
                    String text = "Теперь сумму/вес цифрами ОТ какого кол-ва грн/г ";
                    executeEditMassageText(chatId, text, messageId, null);
                    priceOrWeightState = 1;
                    break;
                }
            }
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    private void getStartCommand(Long chatId, String userName) {
        String formattedText = String.format(textStartMessage, userName);
        log.info("Replied to user : " + userName);
        sendMessage(chatId, formattedText);
    }

    private void getHelpCommand(Long chatId) {
        sendMessage(chatId, textHelpMessage);
    }

    // The method through which we will always send any response to the user
    private void sendMessage(Long chatId, String text) {
        var chatIdStr = String.valueOf(chatId);
        var sendMessage = new SendMessage(chatIdStr, text);
        executeMessage(sendMessage);
    }

    // Method for obtaining a list of products by the user
    private void getProduct(Long chatId, List<Product> listProduct) {
        for (Product product : listProduct) {
            var text = product.getLink() + "\n" + product.getName() + "\n Стоимость: " + product.getPrice() +
                    "\n Вес : " + product.getWeight();
            sendMessage(chatId, text);

            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                log.error("Error delay to send message: " + e.getMessage());
            }
        }
    }

    // Filtering method
    private void sortSushiSets(Long chatId, String filterNum, String nameSortBy) {
        Double numberToFilter = parseStringToDouble(filterNum);

        if (numberToFilter != null && numberToFilter >= 0) {
            List<Product> productList = webScraperService.getSushi();

            switch (nameSortBy) {
                case WEIGHT: {
                    if (priceOrWeightState == 0) {
                        List<Product> filterProduct = productList.stream()
                                .filter(productPrice -> parseStringToDouble(productPrice.getPrice()) != null &&
                                        parseStringToDouble(productPrice.getWeight()) <= numberToFilter)
                                .collect(Collectors.toList());
                        setErrorFilterTextOrGetProduct(chatId, filterProduct);
                        break;
                    }
                    List<Product> filterProduct = productList.stream()
                            .filter(productPrice -> parseStringToDouble(productPrice.getPrice()) != null &&
                                    parseStringToDouble(productPrice.getWeight()) >= numberToFilter)
                            .collect(Collectors.toList());
                    setErrorFilterTextOrGetProduct(chatId, filterProduct);
                    break;
                }

                case PRICE: {
                    if (priceOrWeightState == 0) {
                        List<Product> filterProduct = productList.stream()
                                .filter(productPrice -> parseStringToDouble(productPrice.getPrice()) != null &&
                                        parseStringToDouble(productPrice.getPrice()) <= numberToFilter)
                                .collect(Collectors.toList());
                        setErrorFilterTextOrGetProduct(chatId, filterProduct);
                        break;
                    }
                    List<Product> filterProduct = productList.stream()
                            .filter(productPrice -> parseStringToDouble(productPrice.getPrice()) != null &&
                                    parseStringToDouble(productPrice.getPrice()) >= numberToFilter)
                            .collect(Collectors.toList());
                    setErrorFilterTextOrGetProduct(chatId, filterProduct);
                    break;
                }
                default:
                    log.error("Sort criterion error");
            }
        } else {
            sendMessage(chatId, "Вы ввели некорректные данные");
        }
    }

    private void replyKeyboardMarkup(Long chatId) {

        SendMessage message = new SendMessage();
        String text = "Выберите по каким критерием хотите отсортировать : ";
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(inlineKeyboardMarkup("Цена", PRICE, "Вес", WEIGHT));

        executeMessage(message);
    }

    // Parsing lines into a double for filters
     Double parseStringToDouble(String str) {
        String parseString = str.replaceAll("[^\\d.]", "");
        if (parseString.matches("^[0-9]*\\.?[0-9]*$")) {
            return Double.parseDouble(parseString);
        }
        return null; // Be careful with NULL!!
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeEditMassageText(Long chatId, String text, int messageId,
                                        InlineKeyboardMarkup inlineKeyboardMarkup) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId(messageId);
        message.setReplyMarkup(inlineKeyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void setErrorFilterTextOrGetProduct(Long chatId, List<Product> filterProduct) {
        if (filterProduct.isEmpty()) {
            sendMessage(chatId, "По вашему запросу нет предложений \uD83D\uDE14 \n" +
                    "Попробуйте указать чуть больше(меньше) цену(вес) еще раз в /filter");
        }
        getProduct(chatId, filterProduct);
    }

    private InlineKeyboardMarkup inlineKeyboardMarkup(String firstNameButton, String firstNameCallbackData,
                                                      String secondNameButton, String secondNameCallbackData) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        // Add a "Price" button with a command
        InlineKeyboardButton firstButton = new InlineKeyboardButton();
        firstButton.setText(firstNameButton);
        firstButton.setCallbackData(firstNameCallbackData);
        row.add(firstButton);

        // Add a "Weight" button with a command
        InlineKeyboardButton secondButton = new InlineKeyboardButton();
        secondButton.setText(secondNameButton);
        secondButton.setCallbackData(secondNameCallbackData);
        row.add(secondButton);

        keyboard.add(row);
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }
}
