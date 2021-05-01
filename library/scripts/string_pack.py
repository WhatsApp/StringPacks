# Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
#
# This source code is licensed under the Apache 2.0 license found in
# the LICENSE file in the root directory of this source tree.

import collections
import os
import re
import sys
import logging
from xml.etree import ElementTree


# This must be kept in sync with the `quantityIndex()` method in ParsedStringPack.java
_IDS_FOR_QUANTITY = {"other": 0, "zero": 1, "one": 2, "two": 3, "few": 4, "many": 5}


def normalize_locale(android_config_name):
    if re.match("^[a-z]{2}$", android_config_name):  # xx
        return android_config_name
    elif re.match("^[a-z]{2}-r[A-Z]{2}$", android_config_name):  # xx-rYY
        return android_config_name[:2] + "-" + android_config_name[-2:]
    elif re.match("^b\+[a-z]{2}\+[A-Z][a-z]{3}$", android_config_name):  # b+xx+Zzzz
        return android_config_name[2:4] + "-" + android_config_name[5:]
    else:
        raise NotImplementedError()


def extract_locale_from_file_name(file_name):
    escaped_sep = re.escape(os.path.sep)
    match = re.search(escaped_sep + "values-(.*)" + escaped_sep, file_name)
    assert match is not None
    return normalize_locale(match.group(1))


def unescape(text):
    if not text:
        return ""
    if len(text) >= 2 and text.startswith('"') and text.endswith('"'):
        return text[1:-1]  # Strip the quotation marks
    else:
        return text.replace(r"\'", "'").replace(r"\"", '"').replace(r"\n", "\n")


class TreeBuilderWithComments(ElementTree.TreeBuilder):
    COMMENT_TAG = "comment"

    def comment(self, data):
        # Comment with 'generated' is put by the script, we can skip it.
        if "\u0040generated" in data:
            return

        self.start(self.COMMENT_TAG, {})
        self.data(data)
        self.end(self.COMMENT_TAG)


def read_string_dict(locale, file_name, id_finder, plural_handler):
    result_dict = {}
    try:
        root = ElementTree.parse(
            file_name, parser=ElementTree.XMLParser(target=TreeBuilderWithComments())
        ).getroot()
    except FileNotFoundError:
        # Missing files are OK. They just mean no strings.
        return result_dict

    last_comment = ""
    for element in root:
        tag = element.tag

        if tag == TreeBuilderWithComments.COMMENT_TAG:
            # See if the comments include any metadata about plurals that we need to pass on.
            last_comment = element.text
            continue

        assert tag in ["string", "plurals"]
        string_name = element.attrib["name"]
        id = id_finder.get_id(string_name)
        if id is None:
            # No integer ID was found for the string. The string was most probably removed,
            # but still remains in the translations (such strings will be cleaned up next time
            # move_strings_for_packing.py is run). Log a warning and skip the string.
            sys.stderr.write(
                "No ID found for '%s' while packing %s\n" % (string_name, file_name)
            )
            continue
        if element.tag == "string":
            text = element.text
            result_dict[id] = unescape(text)
        else:  # plurals
            plural_dict = {}
            for item in element:
                assert item.tag == "item"
                quantity = item.attrib["quantity"]
                if plural_handler(locale, last_comment, quantity):
                    continue
                quantity_id = _IDS_FOR_QUANTITY[quantity]
                plural_dict[quantity_id] = unescape(item.text)
            result_dict[id] = plural_dict
    return result_dict


def blob_append_32_bit(blob, integer):
    assert 0 <= integer < 2 ** 31
    blob.append(integer & 0xFF)
    blob.append((integer & 0xFF00) >> 8)
    blob.append((integer & 0xFF0000) >> 16)
    blob.append((integer & 0xFF000000) >> 24)


def blob_append_16_bit(blob, integer):
    assert 0 <= integer < 2 ** 15
    blob.append(integer & 0xFF)
    blob.append((integer & 0xFF00) >> 8)


def blob_append_locale(blob, locale):
    assert len(locale) in [2, 5, 7]
    blob += locale.encode("ASCII")
    if len(locale) == 2:
        blob += b"\0\0\0\0\0"
    elif len(locale) == 5:
        blob += b"\0\0"


class StringBuffer(object):
    "A large byte buffer that just holds strings."

    def __init__(self, encoding):
        self.encoding = encoding
        self.store = bytearray()

    def add(self, string_or_plural):
        if type(string_or_plural) is dict:  # Plural
            result = {}
            for quantity_id, string in string_or_plural.items():
                result[quantity_id] = self.add_string(string)
            return result
        else:
            return self.add_string(string_or_plural)

    def add_string(self, string):
        string_bytes = string.encode(encoding=self.encoding)
        bytes_len = len(string_bytes)
        if bytes_len == 0:  # empty string
            return 0, 0
        location = self.store.find(string_bytes)
        if location == -1:
            # Not found. But before trying to add it, see if a prefix of the new string
            # is at the end of the store buffer. If that's the case, we can save a few bytes
            # by sharing that.
            prefix = bytearray(string_bytes[:-1])
            while prefix and not self.store.endswith(prefix):
                del prefix[-1]
            if prefix:
                # Some part of the prefix remains, which means it matches the end of the buffer.
                start = len(self.store) - len(prefix)
                self.store += string_bytes[len(prefix) :]
            else:
                # Add the string to the end of the buffer.
                start = len(self.store)
                self.store += string_bytes
            return start, bytes_len
        else:
            return location, bytes_len


class LocaleStore(object):
    def __init__(self):
        self.strings = {}
        self.plurals = {}

    def add_plural_or_string(self, id, plural_or_string):
        if type(plural_or_string) is dict:
            self.add_plural(id, plural_or_string)
        else:
            self.add_string(id, plural_or_string)

    def add_string(self, id, string):
        assert id not in self.strings
        self.strings[id] = string

    def add_plural(self, id, plural):
        assert id not in self.plurals
        self.plurals[id] = plural

    def get_binary_blob(self):
        blob = bytearray()
        blob_append_16_bit(blob, len(self.strings))
        blob_append_16_bit(blob, len(self.plurals))
        # Write the strings. Note that the parser in ParsedStringPack.java expects this to be
        # sorted by ID.
        for id in sorted(self.strings):
            blob_append_16_bit(blob, id)
            start, length = self.strings[id]
            blob_append_32_bit(blob, start)
            blob_append_16_bit(blob, length)
        # Write the plurals
        for id in sorted(self.plurals):
            blob_append_16_bit(blob, id)
            plural = self.plurals[id]
            blob.append(len(plural))  # Just one byte
            for quantity_id in sorted(plural):
                blob.append(quantity_id)  # Just one byte
                start, length = plural[quantity_id]
                blob_append_32_bit(blob, start)
                blob_append_16_bit(blob, length)
        return bytes(blob)


# Keep in sync with `ENCODINGS` in ParsedStringPack.java
_ENCODING_ID = {"UTF-8": 0, "UTF-16BE": 1}

# 2 bytes for number of locales, 4 bytes for starting index of locale data, 1 byte for the encoding
# of string data, and 4 bytes for starting index of the string data. Totalling 11 bytes.
_HEADER_SIZE = 11

# Each locale takes 11 bytes, right after the header. 7 bytes for the locale itself
# (see blob_append_locale), and 4 bytes for a pointer to where its table starts in
# file.
_LOCALE_HEADER_SIZE = 11


class StringPack(object):
    "The full string pack, with information about locales, ids, plurals, etc"

    def __init__(self, encoding):
        assert encoding in _ENCODING_ID
        self.encoding = encoding
        self.store = collections.defaultdict(dict)

    def add_for_locale(self, locale, string_dict):
        locale_dict = self.store[locale]
        for key, value in string_dict.items():
            if key in locale_dict:
                logging.warning(
                    "Warning: id {} being overridden by:{}, previous value:{}".format(
                        key, value, locale_dict[key]
                    )
                )
            locale_dict[key] = value

    def compile(self):
        self.string_buffer = StringBuffer(encoding=self.encoding)
        locales = sorted(self.store.keys())
        self.locales_info = bytearray()
        locale_blobs_total_size = 0
        self.locale_blobs = []
        for locale in locales:
            blob_append_locale(self.locales_info, locale)
            locale_store = LocaleStore()
            for id, value in self.store[locale].items():
                locale_store.add_plural_or_string(id, self.string_buffer.add(value))
            locale_blob = bytes(locale_store.get_binary_blob())
            blob_append_32_bit(self.locales_info, locale_blobs_total_size)  # start
            locale_blobs_total_size += len(locale_blob)
            self.locale_blobs.append(locale_blob)

        self.header_blob = bytearray()
        blob_append_16_bit(self.header_blob, len(locales))  # Number of locales
        blob_append_32_bit(
            self.header_blob, _HEADER_SIZE + len(locales) * _LOCALE_HEADER_SIZE
        )  # Start of locale data
        self.header_blob.append(_ENCODING_ID[self.encoding])  # Just one byte
        blob_append_32_bit(
            self.header_blob,
            _HEADER_SIZE
            + len(locales) * _LOCALE_HEADER_SIZE
            + sum([len(blob) for blob in self.locale_blobs]),
        )  # Start of string data

    def string_buffer_size(self):
        return len(self.string_buffer.store)

    def write_to_file(self, pack_file_name):
        with open(pack_file_name, "wb") as pack_file:
            pack_file.write(self.header_blob)
            pack_file.write(self.locales_info)
            for locale_blob in self.locale_blobs:
                pack_file.write(locale_blob)
            pack_file.write(self.string_buffer.store)


def build(input_file_names, output_file_name, id_finder, plural_handler):
    """Builds the string pack and writes it to a file.

    It tries both UTF-8 and UTF-16 to see which one is smaller, and then writes
    the string pack in that encoding."""
    packs = []
    for encoding in _ENCODING_ID.keys():
        full_store = StringPack(encoding=encoding)
        for input_file_name in input_file_names:
            locale = extract_locale_from_file_name(input_file_name)
            full_store.add_for_locale(
                locale,
                read_string_dict(locale, input_file_name, id_finder, plural_handler),
            )
        full_store.compile()
        packs.append(full_store)

    smallest_pack = min(packs, key=lambda p: p.string_buffer_size())
    smallest_pack.write_to_file(output_file_name)
