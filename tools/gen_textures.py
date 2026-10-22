"""Генерирует все PNG-текстуры мода без внешних зависимостей. Запуск: python3 tools/gen_textures.py"""
import math, os, struct, zlib

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..',
                    'src', 'main', 'resources', 'assets', 'spiderlord', 'textures')

BLACK  = (18, 10, 24, 255)
DEEP   = (38, 14, 58, 255)
PURPLE = (84, 28, 124, 255)
SKIN   = (234, 208, 224, 255)
HAIR   = (14, 8, 20, 255)
RED    = (235, 22, 44, 255)
LIP    = (150, 20, 56, 255)
CLEAR  = (0, 0, 0, 0)


def blank(w, h):
    return [[CLEAR for _ in range(w)] for _ in range(h)]


def save(rel, img):
    path = os.path.normpath(os.path.join(BASE, rel))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    h, w = len(img), len(img[0])
    raw = b''.join(b'\x00' + bytes(c for px in row for c in px) for row in img)

    def chunk(tag, data):
        return (struct.pack('>I', len(data)) + tag + data +
                struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff))

    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n')
        f.write(chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)))
        f.write(chunk(b'IDAT', zlib.compress(raw, 9)))
        f.write(chunk(b'IEND', b''))


def rect(img, x, y, w, h, fill):
    for j in range(h):
        for i in range(w):
            img[y + j][x + i] = fill(i, j) if callable(fill) else fill


def line(img, x0, y0, x1, y1, color):
    steps = max(abs(x1 - x0), abs(y1 - y0), 1)
    for k in range(steps + 1):
        x = round(x0 + (x1 - x0) * k / steps)
        y = round(y0 + (y1 - y0) * k / steps)
        if 0 <= y < len(img) and 0 <= x < len(img[0]):
            img[y][x] = color


def skin():
    img = blank(64, 64)
    stripe = lambda i, j: PURPLE if (i + j) % 7 == 0 else BLACK
    rows = lambda i, j: PURPLE if j % 4 == 3 else BLACK

    # голова: чёрные волосы, лицо и подбородок светлые
    rect(img, 0, 0, 32, 16, HAIR)
    rect(img, 16, 0, 8, 8, SKIN)       # подбородок
    rect(img, 8, 10, 8, 6, SKIN)       # лицо спереди (чёлка на строках 8-9)
    rect(img, 0, 12, 8, 4, SKIN)       # щёки сбоку
    rect(img, 16, 12, 8, 4, SKIN)
    for x in (9, 10, 13, 14):          # красные глаза
        img[12][x] = RED
    img[10][11] = RED                  # пара «лишних» глаз
    img[10][12] = RED
    img[14][11] = LIP                  # губы
    img[14][12] = LIP

    # торс: корсет с декольте и шнуровкой
    rect(img, 16, 16, 24, 16, stripe)
    rect(img, 21, 20, 6, 4, SKIN)
    rect(img, 23, 24, 2, 8, PURPLE)

    # руки: полосатые рукава, светлые кисти
    rect(img, 40, 16, 16, 16, rows)
    rect(img, 40, 29, 16, 3, SKIN)
    rect(img, 32, 48, 16, 16, rows)
    rect(img, 32, 61, 16, 3, SKIN)

    # ноги
    legs = lambda i, j: PURPLE if (i - j) % 6 == 0 else BLACK
    rect(img, 0, 16, 16, 16, legs)
    rect(img, 16, 48, 16, 16, legs)
    save('entity/spider_lord_skin.png', img)


def extra():
    img = blank(64, 64)
    rect(img, 0, 0, 64, 64, lambda i, j: DEEP if ((i // 3) + (j // 3)) % 5 == 0 else BLACK)
    rect(img, 48, 0, 10, 5, SKIN)      # бюст
    rect(img, 60, 0, 4, 2, RED)        # светящиеся глаза
    save('entity/spider_lord_extra.png', img)


def heart():
    img = blank(16, 16)

    def inside(px, py):
        x = (px + 0.5 - 8) / 6.3
        y = (6.2 - (py + 0.5)) / 5.6
        return (x * x + y * y - 1) ** 3 - x * x * y ** 3 <= 0

    mask = [[inside(x, y) for x in range(16)] for y in range(16)]
    for y in range(16):
        for x in range(16):
            if not mask[y][x]:
                continue
            edge = any(not (0 <= x + dx < 16 and 0 <= y + dy < 16 and mask[y + dy][x + dx])
                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge:
                img[y][x] = BLACK
            else:
                t = y / 15
                img[y][x] = (int(110 - 50 * t), int(30 - 10 * t), int(150 - 50 * t), 255)
    img[3][4] = img[3][5] = (190, 120, 230, 255)   # блик
    for x, y in ((6, 6), (9, 6), (7, 8), (8, 8)):  # паучьи глазки
        img[y][x] = RED
    save('item/spider_heart.png', img)


def icon():
    img = blank(18, 18)
    for ex, ey in ((2, 3), (0, 8), (1, 13), (3, 16)):
        line(img, 8, 9, ex, ey, (160, 70, 210, 255))
        line(img, 9, 9, 17 - ex, ey, (160, 70, 210, 255))
    for y in range(18):
        for x in range(18):
            if (x - 8.5) ** 2 + (y - 10) ** 2 <= 11:
                img[y][x] = BLACK
            if (x - 8.5) ** 2 + (y - 6) ** 2 <= 5:
                img[y][x] = BLACK
    for x, y in ((7, 5), (10, 5)):
        img[y][x] = RED
    save('mob_effect/spider_lord.png', img)


if __name__ == '__main__':
    skin(); extra(); heart(); icon()
    print('textures generated')
