package com.puzzlechannel.vmtrade;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runtime-only Russian localization for the original Open Trade UI.
 *
 * Important: this class never rewrites a vendor .class file. The client/server
 * mixins redirect Component.literal(...) calls in memory and pass the final
 * display string through this translator.
 */
public final class UiTextTranslations {
    private static final Pattern TIME = Pattern.compile("(\\d+)(min|ms|s|m|h|d|y)\\b");
    private static final Pattern TRADE_SUMMARY = Pattern.compile("^(\\d+)x for (.+?) (\\d+(?:[.,]\\d+)?) (.+)$");
    private static final Method LITERAL = findLiteralMethod();

    private UiTextTranslations() {}

    private static Method findLiteralMethod() {
        try {
            return Component.class.getMethod("literal", String.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** Create the real Minecraft MutableComponent without a compile-time call-site dependency. */
    public static MutableComponent componentLiteral(String original) {
        String translated = translate(original);
        try {
            return (MutableComponent) LITERAL.invoke(null, translated);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to create Minecraft text component", e);
        }
    }

    public static String translate(String original) {
        if (original == null || original.isEmpty()) return original;
        String s = original;

        // 1) Exact short labels.
        String[][] exact = {
                {"Market", "Маркет"},
                {"Trade Chest", "Торговый сундук"},
                {"Incoming", "Входящие доставки"},
                {"Back", "Назад"},
                {"Place Offer", "Выставить товар"},
                {"Price", "Цена"}, {"price", "цена"},
                {"Qty", "Кол-во"}, {"qty", "кол-во"},
                {"List", "Выставить"},
                {"Cancel", "Отменить"},
                {"Cancel buy order", "Отменить заявку"},
                {"Search", "Поиск"},
                {"Search items…", "Поиск предметов…"},
                {"Search items?", "Поиск предметов…"},
                {"Sell Orders", "Заявки на продажу"},
                {"Buy Orders", "Заявки на покупку"},
                {"Recent Trades", "Последние сделки"},
                {"Trade", "Торговля"},
                {"Trades", "Сделки"},
                {"Villager", "Торговец"},
                {"Wallet", "Кошелёк"},
                {"Buy", "Купить"}, {"Sell", "Продать"},
                {"Buy ", "Купить "}, {"Sell ", "Продать "},
                {"for", "за"},
                {"All", "Все"},
                {"D", "Д"}, {"W", "Н"}, {"M", "М"}, {"Y", "Г"},
                {"(no item)", "(нет предмета)"},
                {"Item Detail", "Информация о предмете"},
                {"No demand", "Нет спроса"},
                {"Out of stock", "Нет в наличии"},
        };
        for (String[] pair : exact) s = exact(s, pair[0], pair[1]);

        // 2) Exact/full-sentence translations BEFORE generic connector replacements.
        String[][] full = {
                {"§7No trades yet.", "§7Сделок пока нет."},
                {"No trades yet", "Сделок пока нет"},
                {"§7No incoming deliveries.", "§7Нет входящих доставок."},
                {"No incoming deliveries", "Нет входящих доставок"},
                {"§7No listings yet.", "§7Объявлений пока нет."},
                {"§7No matches.", "§7Совпадений нет."},
                {"No offers — click to place a buy order", "Нет предложений — нажмите, чтобы разместить заявку на покупку"},
                {"§7Click to manage.", "§7Нажмите, чтобы управлять."},
                {"§7Click to buy the whole stack (", "§7Нажмите, чтобы купить весь стак ("},
                {"§7No demand", "§7Нет спроса"},
                {"§7Click to buy 1. §8Shift = whole stack.", "§7Нажмите, чтобы купить 1 шт. §8Shift — весь стак."},
                {"§8Hold right-click for item details.", "§8Удерживайте ПКМ для информации о предмете."},
                {"§7Remove your sell order before buying this item.", "§7Отмените своё объявление о продаже перед покупкой."},
                {"§cYou are selling this item.", "§cВы уже продаёте этот предмет."},
                {"§7Select a trade.", "§7Выберите сделку."},
                {"§cChest converted to Trade Chest.", "§aСундук преобразован в торговый сундук."},
                {"§cCan't buy the whole stack — buy one at a time.", "§cНельзя купить весь стак — покупайте по одному."},
                {"§cNo listings at that price anymore.", "§cПо этой цене объявлений больше нет."},
                {"§cNot enough emeralds or chest space.", "§cНедостаточно ВК или места в торговом сундуке."},
                {"§cOpen the market from your Trade Chest first.", "§cСначала откройте маркет из своего торгового сундука."},
                {"§cOpen the market from your Trade Chest to buy.", "§cЧтобы покупать, сначала откройте маркет из своего торгового сундука."},
                {"§cThat listing is empty.", "§cЭто объявление пусто."},
                {"§cThat listing is no longer available.", "§cЭто объявление больше недоступно."},
                {"§cThat listing's price changed or it was removed.", "§cЦена объявления изменилась или оно было снято."},
                {"§cTrade chest is full.", "§cТорговый сундук заполнен."},
                {"§cYou can't buy your own listing.", "§cНельзя покупать собственное объявление."},
                {"§cNo free slot in your Trade Chest to reserve for this order.", "§cВ торговом сундуке нет свободного слота для этой заявки."},
                {"§7This is a Trade Chest owned by another player.", "§7Это не ваш Магазин"},
                {"Trade Chest is owned by another player.", "Это не ваш Магазин"},
                {"This trade chest is owned by another player.", "Это не ваш Магазин"},
                {"§8Click to select. Shift-click Buy/Sell for up to 64.", "§8Нажмите для выбора. Shift + клик — купить или продать до 64 шт."}
        };
        for (String[] pair : full) s = replace(s, pair[0], pair[1]);

        // 3) Structured dynamic trade/listing summaries.
        s = translateTradeSummary(s);
        s = replace(s, "Incoming — ETA:", "Входящая доставка — прибытие через:");
        s = replace(s, "ETA:", "Прибытие через:");
        s = replace(s, "Wanted: ", "Нужно: ");
        s = replace(s, "Sell For: ", "Продажа за: ");
        s = replace(s, "Buy for: ", "Покупка за: ");
        s = replace(s, "Market: ", "Маркет: ");
        s = replace(s, "§eBuy order:", "§eЗаявка на покупку:");
        s = replace(s, "§7Delivery Time:", "§7Время доставки:");
        s = replace(s, "§7Price:", "§7Цена:");
        s = replace(s, "§7Stock:", "§7В наличии:");
        s = replace(s, "§7Requires:", "§7Требуется:");

        // 4) Server messages with dynamic values. These run before generic English
        // connector replacements so phrases such as "at a time" stay intact.
        s = replace(s, "§aChest converted to Trade Chest.", "§aСундук преобразован в торговый сундук.");
        s = replace(s, "§aOrder cancelled. Refunded ", "§aЗаявка отменена. Возвращено ");
        s = replace(s, "§aOrder placed: filled ", "§aЗаявка размещена: сразу исполнено ");
        s = replace(s, "§aBuy order placed for ", "§aЗаявка на покупку размещена: ");
        s = replace(s, "§aPurchased ", "§aКуплено ");
        s = replace(s, "§cCouldn't buy any — out of ", "§cНе удалось ничего купить — закончились ");
        s = replace(s, "§cMinimum order price is ", "§cМинимальная цена заявки: ");
        s = replace(s, "§cMinimum sale price is ", "§cМинимальная цена продажи: ");
        s = replace(s, "§cNot enough  to escrow that order.", "§cНедостаточно  для резервирования этой заявки.");
        s = replace(s, "§cNot enough .", "§cНедостаточно .");
        s = replace(s, "§cYou have an active sell listing for this item. Cancel it before buying.", "§cУ вас есть активное объявление о продаже этого предмета. Отмените его перед покупкой.");
        s = replace(s, "§cYou have an open buy order for this item. Cancel it before selling.", "§cУ вас есть открытая заявка на покупку этого предмета. Отмените её перед продажей.");
        s = replace(s, "§cYour Trade Chest has no room for this item.", "§cВ вашем торговом сундуке нет места для этого предмета.");
        s = replace(s, "§cYour Trade Chest is no longer available.", "§cВаш торговый сундук больше недоступен.");

        // 5) Safe generic connectors used only after full sentences are localized.
        s = replace(s, "@ ", "по ");
        s = replace(s, " at ", " по ");
        s = replace(s, " x for ", " × за ");
        s = replace(s, "x for ", "× за ");
        s = replace(s, " each.", " за штуку.");
        s = replace(s, " immediately.", " сразу.");
        s = replace(s, " of ", " из ");
        s = replace(s, " more", " ещё");
        s = replace(s, " ago", " назад");
        s = replace(s, " E.", " ВК.");
        s = replace(s, " E", " ВК");

        // 6) Known player-facing ownership/status messages and remaining fragments.
        s = replace(s, "Trade Chest has active sale offers.", "В торговом сундуке есть активные объявления о продаже.");
        s = replace(s, "Trade Chest has an incoming delivery.", "В торговом сундуке есть входящая доставка.");
        s = replace(s, "Trade Chest has an open buy order.", "В торговом сундуке есть открытая заявка на покупку.");
        s = replace(s, "Trade Chest is owned by another player.", "Это не ваш Магазин");
        s = replace(s, "§7+ ", "§7+ ");

        // 7) Currency normalization, including the original plural suffix that was
        // displayed as "VMCoin (VC)s". Never alter JVM descriptors: this is a String only.
        s = translateCurrencyName(s);
        s = translateTradeSummary(s);
        s = translateTimeUnits(s);
        s = addBalanceLabel(s);

        return s;
    }

    private static String addBalanceLabel(String value) {
        if (value == null) return null;
        // MarketScreen renders the wallet as "<amount> <currency>". Make the
        // meaning explicit without touching the vendor class or its bytecode.
        if (value.matches("^[0-9]+(?:[.,][0-9]{1,2})? ВМуткоин \\(ВК\\)$")) {
            return "\u0411\u0430\u043b\u0430\u043d\u0441: " + value;
        }
        return value;
    }

    private static String translateTradeSummary(String value) {
        Matcher matcher = TRADE_SUMMARY.matcher(value);
        if (matcher.matches()) {
            String qty = matcher.group(1);
            String item = matcher.group(2);
            String price = matcher.group(3);
            String currency = translateCurrencyName(matcher.group(4));
            return qty + "× " + item + " за " + price + " " + currency;
        }
        return value;
    }

    public static String translateCurrencyName(String value) {
        if (value == null || value.isEmpty()) return value;
        String s = value;
        s = s.replace("ВМуткоин (ВК)ss", "ВМуткоин (ВК)");
        s = s.replace("ВМуткоин (ВК)s", "ВМуткоин (ВК)");
        s = s.replace("ВМуткоин (ВК) s", "ВМуткоин (ВК)");
        s = s.replace("VMCoin (VC)s", "ВМуткоин (ВК)");
        s = s.replace("VMCoin (VC) s", "ВМуткоин (ВК)");
        s = s.replace("VMCoin (VC)", "ВМуткоин (ВК)");
        s = s.replace("VMCoin", "ВМуткоин");
        s = s.replace("VMC", "ВМуткоин");
        s = s.replace("VCs", "ВК");
        s = s.replace("VC's", "ВК");
        s = s.replace("VC", "ВК");
        s = s.replace("emeralds", "ВК");
        s = s.replace("emerald", "ВМуткоин");
        return s;
    }

    private static String exact(String value, String from, String to) {
        return value.equals(from) ? to : value;
    }

    private static String replace(String value, String from, String to) {
        return value.replace(from, to);
    }

    private static String translateTimeUnits(String value) {
        Matcher m = TIME.matcher(value);
        StringBuffer out = new StringBuffer(value.length() + 16);
        while (m.find()) {
            String n = m.group(1);
            String unit = m.group(2);
            String ru = switch (unit) {
                case "min", "m" -> n + " мин";
                case "ms" -> n + " мс";
                case "s" -> n + " с";
                case "h" -> n + " ч";
                case "d" -> n + " дн.";
                case "y" -> n + " г.";
                default -> m.group(0);
            };
            m.appendReplacement(out, Matcher.quoteReplacement(ru));
        }
        m.appendTail(out);
        return out.toString();
    }
}
