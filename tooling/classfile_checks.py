"""Minimal class-file inspection for validating Mixin's class-level annotation."""
from __future__ import annotations

import struct

MIXIN_DESCRIPTOR = "Lorg/spongepowered/asm/mixin/Mixin;"


def has_class_level_mixin_annotation(data: bytes) -> bool:
    """Return True only when @Mixin is a class-level RuntimeInvisibleAnnotations entry."""
    if len(data) < 10 or data[:4] != b"\xca\xfe\xba\xbe":
        return False

    offset = 8
    cp_count = _u2(data, offset)
    offset += 2
    cp: list[str | None] = [None] * cp_count
    i = 1
    while i < cp_count:
        tag = _u1(data, offset)
        offset += 1
        if tag == 1:  # CONSTANT_Utf8
            length = _u2(data, offset)
            offset += 2
            cp[i] = data[offset:offset + length].decode("utf-8", errors="replace")
            offset += length
        elif tag in (3, 4):
            offset += 4
        elif tag in (5, 6):
            offset += 8
            i += 1
        elif tag in (7, 8, 16, 19, 20):
            offset += 2
        elif tag in (9, 10, 11, 12, 17, 18):
            offset += 4
        elif tag == 15:
            offset += 3
        else:
            raise ValueError(f"Unknown class-file constant-pool tag {tag}")
        i += 1

    # access_flags, this_class, super_class
    offset += 6
    interface_count = _u2(data, offset)
    offset += 2 + 2 * interface_count

    fields_count = _u2(data, offset)
    offset += 2
    for _ in range(fields_count):
        offset = _skip_member(data, offset)

    methods_count = _u2(data, offset)
    offset += 2
    for _ in range(methods_count):
        offset = _skip_member(data, offset)

    attributes_count = _u2(data, offset)
    offset += 2
    for _ in range(attributes_count):
        name_index = _u2(data, offset)
        length = _u4(data, offset + 2)
        info_start = offset + 6
        info_end = info_start + length
        if cp[name_index] == "RuntimeInvisibleAnnotations":
            count = _u2(data, info_start)
            pos = info_start + 2
            for _ in range(count):
                annotation_type_index = _u2(data, pos)
                pair_count = _u2(data, pos + 2)
                pos += 4
                found = cp[annotation_type_index] == MIXIN_DESCRIPTOR
                for _ in range(pair_count):
                    pos += 2  # element_name_index
                    pos = _skip_element_value(data, pos)
                if found:
                    return True
        offset = info_end
    return False


def _skip_member(data: bytes, offset: int) -> int:
    attributes_count = _u2(data, offset + 6)
    offset += 8
    for _ in range(attributes_count):
        length = _u4(data, offset + 2)
        offset += 6 + length
    return offset


def _skip_element_value(data: bytes, offset: int) -> int:
    tag = chr(_u1(data, offset))
    offset += 1
    if tag in "BCDFIJSZsc":
        return offset + 2
    if tag == "e":
        return offset + 4
    if tag == "@":
        pair_count = _u2(data, offset + 2)
        offset += 4
        for _ in range(pair_count):
            offset += 2
            offset = _skip_element_value(data, offset)
        return offset
    if tag == "[":
        count = _u2(data, offset)
        offset += 2
        for _ in range(count):
            offset = _skip_element_value(data, offset)
        return offset
    raise ValueError(f"Unknown annotation element-value tag {tag!r}")


def _u1(data: bytes, offset: int) -> int:
    return data[offset]


def _u2(data: bytes, offset: int) -> int:
    return struct.unpack_from(">H", data, offset)[0]


def _u4(data: bytes, offset: int) -> int:
    return struct.unpack_from(">I", data, offset)[0]
