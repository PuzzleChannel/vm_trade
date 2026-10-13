from pathlib import Path
from zipfile import ZipFile
import subprocess
import base64
import re
import tempfile
import os

ROOT = Path(__file__).resolve().parents[1]
VERSION = next((line.split('=', 1)[1].strip() for line in (ROOT / 'gradle.properties').read_text(encoding='utf-8').splitlines() if line.startswith('version=')), None)
if not VERSION:
    raise SystemExit('version= missing from gradle.properties')
JAR = ROOT / f'build/libs/vm-trade-{VERSION}.jar'
CLASSES = [
    'net/onlyjugs/client/screen/MarketScreen.class',
    'net/onlyjugs/client/screen/TradeChestScreen.class',
    'net/onlyjugs/client/screen/VillagerTradeScreen.class',
    'net/onlyjugs/net/OpenTradeNet.class',
    'net/onlyjugs/net/OpenTradeNet$1.class',
    'net/onlyjugs/net/OpenTradeNet$2.class',
    'net/onlyjugs/net/OpenTradeNet$3.class',
    'net/onlyjugs/net/OpenTradeNet$4.class',
    'net/onlyjugs/OpenTrade.class',
    'net/onlyjugs/OpenTradeConfig.class',
    'net/onlyjugs/OpenTradeClient.class',
    'net/onlyjugs/block/TradeChestBlock.class',
    'net/onlyjugs/block/TradeChestBlock$1.class',
]

IGNORE = {
    '<init>', '<clinit>', 'BootstrapMethods', 'Code', 'ConstantValue', 'InnerClasses',
    'LineNumberTable', 'Lookup', 'MethodParameters', 'Signature', 'SourceFile',
    'StackMapTable', 'Builder', 'Default', 'Entry', 'HistoryPoint', 'TooltipContext',
    'NORMAL', 'GUI_TEXTURED', 'ADVANCED', 'Level', 'D', 'F', 'I', 'J', 'W', 'M', 'Y',
    'Z', 'VMCoin', 'minecraft:emerald',
    # These strings are server console diagnostics, not player-facing UI.
    '[OpenTrade] Failed to read config: \x01 — using defaults.',
    '[OpenTrade] Failed to write default config: \x01'
}

EXPLICIT = {
    'Market': 'Маркет',
    'for': 'за',
    'Incoming': 'Входящие доставки',
    'No incoming deliveries': 'Нет входящих доставок',
    'Trade Chest': 'Торговый сундук',
    'Buy': 'Купить',
    'Sell': 'Продать',
    '+1': '+1 ВК',
    '+64': '+64 ВК',
    '-1': '−1 ВК',
    '-64': '−64 ВК',
    'VMCoin (VC)s': 'ВМуткоин (ВК)',
    'ВМуткоин (ВК)s': 'ВМуткоин (ВК)',
    '32x for Diamond Block 5 VMCoin (VC)s': '32× Diamond Block за 5 ВМуткоин (ВК)',
    'Incoming — ETA: 10m': 'Входящая доставка — прибытие через: 10 мин',
    'ETA: 20s': 'Прибытие через: 20 с',
    'Wanted: 12x at 4 E': 'Нужно: 12x по 4 ВК',
    'This trade chest is owned by another player.': 'Это торговый сундук другого игрока.',
    '§eBuy order: 3× @ 5 E': '§eЗаявка на покупку: 3× по 5 ВК',
    '§7Stock: 3 / 64': '§7В наличии: 3 / 64',
    '§7Price: 5 VMCoin (VC)s': '§7Цена: 5 ВМуткоин (ВК)',
    '§7Price: 1 VMCoin (VC)s × 64 = 64 VMCoin (VC)s': '§7Цена за весь стак: 1 ВМуткоин (ВК)',

    '§7No listings yet.': '§7Объявлений пока нет.',
    "§cCan't buy the whole stack — buy one at a time.": "§cНельзя купить весь стак — покупайте по одному.",
    '§cNo listings at that price anymore.': '§cПо этой цене объявлений больше нет.',
    '§cNot enough \x01 to escrow that order.': '§cНедостаточно \x01 для резервирования этой заявки.',
    '§cYou already have a buy order for this item at \x01 E. Cancel it first to use a different price.': '§cУ вас уже есть заявка на покупку этого предмета по цене \x01 ВК. Сначала отмените её, чтобы изменить цену.',
    '§cNot enough emeralds or chest space.': '§cНедостаточно ВК или места в торговом сундуке.',
}


def utf8s(data: bytes):
    p = 8
    cp = int.from_bytes(data[p:p+2], 'big')
    p += 2
    out = []
    i = 1
    while i < cp:
        t = data[p]
        p += 1
        if t == 1:
            n = int.from_bytes(data[p:p+2], 'big')
            p += 2
            out.append(data[p:p+n].decode('utf-8', 'replace'))
            p += n
        elif t in (3, 4):
            p += 4
        elif t in (5, 6):
            p += 8
            i += 1
        elif t in (7, 8, 16, 19, 20):
            p += 2
        elif t in (9, 10, 11, 12, 17, 18):
            p += 4
        elif t == 15:
            p += 3
        i += 1
    return out


def likely_visible(s: str) -> bool:
    if not s or len(s) >= 220 or '\n' in s or '\t' in s:
        return False
    if s in IGNORE:
        return False
    if s.startswith('(') or '/' in s or s.startswith('L'):
        return False
    if not re.search(r'[A-Za-z ]', s):
        return False
    # Keep display strings and UI words, but ignore pure identifiers/signatures.
    if re.fullmatch(r'[A-Za-z_$%0-9:+,\.\[\]-]+', s):
        return s in EXPLICIT
    return True

vals = []
with ZipFile(ROOT / 'vendor/open-trade-1.4.2.jar') as z:
    for c in CLASSES:
        for s in utf8s(z.read(c)):
            if likely_visible(s):
                vals.append(s)
vals = sorted(set(vals) | set(EXPLICIT))

runner_source = '''
import java.io.*;
import java.util.*;
import java.util.Base64;
import com.puzzlechannel.vmtrade.UiTextTranslations;
public class Runner {
  public static void main(String[] a) throws Exception {
    var br = new BufferedReader(new InputStreamReader(System.in, java.nio.charset.StandardCharsets.US_ASCII));
    String line;
    while ((line = br.readLine()) != null) {
      String s = new String(Base64.getDecoder().decode(line), java.nio.charset.StandardCharsets.UTF_8);
      String t = UiTextTranslations.translate(s);
      System.out.println(Base64.getEncoder().encodeToString(t.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
  }
}
'''

# Gradle's normal Java source set is build/classes/java/main. Keep the
# release_compile fallback for the standalone archive builder used by older tooling.
CLASSES_DIR = ROOT / 'build/classes/java/main'
if not CLASSES_DIR.is_dir():
    CLASSES_DIR = ROOT / 'build/release_compile'
STUBS_JAR = ROOT / 'build/api-stubs.jar'
if not CLASSES_DIR.is_dir():
    raise SystemExit('Compiled VM Trade classes are missing; run ./gradlew build first')
if not STUBS_JAR.is_file():
    raise SystemExit('API stubs jar is missing; run ./gradlew build first')
CLASSPATH = os.pathsep.join((str(CLASSES_DIR), str(STUBS_JAR)))

with tempfile.TemporaryDirectory() as td_raw:
    td = Path(td_raw)
    runner = td / 'Runner.java'
    runner.write_text(runner_source, encoding='utf-8')
    subprocess.run([
        'javac', '--release', '21', '-encoding', 'UTF-8',
        '-cp', CLASSPATH, '-d', str(td), str(runner)
    ], check=True)

    payload = ''.join(base64.b64encode(s.encode('utf-8')).decode('ascii') + '\n' for s in vals)
    proc = subprocess.run([
        'java', '-Dfile.encoding=UTF-8', '-cp', os.pathsep.join((CLASSPATH, str(td))), 'Runner'
    ], input=payload.encode('ascii'), stdout=subprocess.PIPE, check=True)
    out = [base64.b64decode(x).decode('utf-8') for x in proc.stdout.splitlines()]
    lookup = dict(zip(vals, out))

    remaining = []
    for src, dst in lookup.items():
        if src == dst and (src in EXPLICIT or any(w in src for w in [
            'Back', 'Offer', 'Orders', 'Trades', 'Price', 'Qty', 'Chest', 'Incoming',
            'No ', 'This trade chest', 'Search', 'Buy', 'Sell', 'Market', 'Delivery', 'Click', 'Hold',
            'Not enough', 'You ', 'Your ', 'Trade ', 'ETA', 'Wanted', 'No offers',
            'Convert', 'Place', 'Withdraw', 'Deposit', 'Balance', 'Wallet',
            'History', 'Listing', 'Listings', 'Order', 'Orders', 'Seller', 'Buyer',
            'Owner', 'Quantity', 'Amount', 'Status', 'Total', 'Sort', 'Delivery',
            'Stock', 'Volume', 'Offer', 'Confirm', 'Close', 'Refresh', 'Details',
            'Select', 'Available', 'Unavailable', 'Pending', 'Completed', 'Failed',
            'Success', 'Item Name', 'Price Chart', 'Price Graph', 'Best Price',
            'Average Price', 'Last Price'
        ])):
            remaining.append(src)
    if remaining:
        raise AssertionError('Untranslated visible literals: ' + repr(sorted(remaining)))

    for src, expected in EXPLICIT.items():
        actual = lookup.get(src)
        if actual != expected:
            raise AssertionError(f'Translation mismatch: {src!r} -> {actual!r}, expected {expected!r}')

# Check resource-backed controls too: these are easy to overlook because the
# runtime literal translator does not process Component.translatable resource keys.
import json
EXPECTED_BUTTONS = {
    'opentrade.button.list': 'Выставить',
    'opentrade.button.cancel': 'Снять',
    'opentrade.button.deposit': 'Пополнить',
    'opentrade.button.withdraw': 'Вывести',
}
for rel in ('assets/opentrade/lang/ru_ru.json', 'assets/vmtrade/lang/ru_ru.json'):
    resource = json.loads((ROOT / 'src/main/resources' / rel).read_text(encoding='utf-8'))
    for key, expected in EXPECTED_BUTTONS.items():
        actual = resource.get(key)
        if actual != expected:
            raise AssertionError(f'{rel}: {key!r} -> {actual!r}; expected {expected!r}')

print(f'VM Trade translation coverage: OK ({len(vals)} constants scanned + {len(EXPLICIT)} regression cases)')
