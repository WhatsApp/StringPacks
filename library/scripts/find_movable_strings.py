#!/usr/bin/env python3

# Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
#
# This source code is licensed under the Apache 2.0 license found in
# the LICENSE file in the root directory of this source tree.


import argparse
import logging
import math
import re
import subprocess
from os import path
from typing import Set
from xml.etree import ElementTree

import string_pack_config
from move_strings_for_packing import get_resource_content_without_header


# Escape code for color
SET_WARNING_COLOR = "\033[33m\033[41m"  # yellow text with red background
CLEAR_COLOR = "\033[0m"

NAMESPACE_AND_ATTRIB_RE = re.compile("^\{(.+)\}(.+)$")


def separate_namespace(attribute_name):
    match = NAMESPACE_AND_ATTRIB_RE.match(attribute_name)
    if match:
        return match.groups()
    else:
        return None, attribute_name


STRING_USAGE_RE = re.compile("@string/([A-Za-z0-9_]+)")

OK_NAMESPACES = {"http://schemas.android.com/tools"}

# Previously, we would just generate a list of string ids and their mapping, one pair per line
# broken with newlines.
# Unfortunately, this breaks if there are around 8k strings or so as it results in the method's
# bytecode size exceeding the JVM limit (64k).
# Thus, we must instead break the list into parts if we hit this limit.
# But then we can't just generate a list with newlines - we also need to generate the code
# surrounding the list pieces so the developer can still easily use one statement
# (getStringPacksMapping()) to use the generated code.
MAX_IDS_PER_METHOD = 8000

DONOTPACK_RE = re.compile('<(?:string|plurals) name="([^"]+)".*donotpack="true"')


def find_donotpack_strings(filename):
    result = set()
    data = get_resource_content_without_header(filename)
    for match in DONOTPACK_RE.finditer(data):
        result.add(match.group(1))
    return result


def find_strings_used_in_xml(filename, safe_widgets):
    result = set()
    # Ignore the file if it throws an error while parsing
    # Started seeing this in some of the xml files, that were not layout files. We do not expect
    # this error to be thrown in layout files or files that we are interested in
    try:
        tree = ElementTree.parse(filename)
        for node in tree.findall(".//"):
            if node.tag in safe_widgets:
                continue  # Certain widgets can handle @string just fine
            if node.text is not None:
                for string in STRING_USAGE_RE.findall(node.text):
                    result.add(string)
            for key, value in node.attrib.items():
                string_usage_match = STRING_USAGE_RE.search(value)
                if string_usage_match:
                    namespace, attrib = separate_namespace(key)
                    if namespace in OK_NAMESPACES:
                        continue  # Certain namespace are safe to use @string in
                    result.add(string_usage_match.group(1))
    except ElementTree.ParseError:
        logging.warning(
            SET_WARNING_COLOR
            + "Dropping the file becase of ParseError: "
            + filename
            + CLEAR_COLOR
        )
    finally:
        return result


NAME_CATCHER_RE = re.compile('<(string|plurals) name="([^"]+)"')


def output_string_ids_map(sp_config, strings_to_move):
    string_pack_ids = []
    for index, string_tuple in enumerate(sorted(strings_to_move)):
        string_type, string_name = string_tuple
        string_pack_ids.append((" " * 10 + "R.%s.%s,") % (string_type, string_name))

    class_file_path = sp_config.pack_ids_class_file_path
    if class_file_path is None or not path.exists(class_file_path):
        # No class file is provided, print to console directly to let people copy/paste later.
        for pack_id in string_pack_ids:
            print(pack_id)
        return

    # Directly update the class file with latest ids.
    with open(class_file_path, "rt") as pack_ids_file:
        existing_class_file_lines = pack_ids_file.readlines()

    region_start_index = None
    region_end_index = None
    for i, line in enumerate(existing_class_file_lines):
        if "// region" in line:
            region_start_index = i
        elif "// endregion" in line:
            region_end_index = i

    if region_start_index is None or region_end_index is None:
        print(
            "Can't find the String Pack IDs map region in %s to update content."
            % class_file_path
        )
        return

    with open(class_file_path, "wt") as pack_ids_file:
        pack_ids_file.writelines(
            existing_class_file_lines[0 : region_start_index + 1]
            + (
                generate_kotlin(string_pack_ids)
                if class_file_path.endswith(".kt")
                else generate_java(string_pack_ids)
            )
            + existing_class_file_lines[region_end_index:]
        )
    print("Updated: " + class_file_path)


def generate_java(string_pack_ids):
    if len(string_pack_ids) <= MAX_IDS_PER_METHOD:
        # No need for sub-methods, just create the whole array in the main method
        return generate_java_internal(string_pack_ids, "getStringPacksMapping")

    result = []
    result += " " * 2 + "private static int[] getStringPacksMapping() {\n"
    result += (
        " " * 6 + "final int[] result = new int[" + str(len(string_pack_ids)) + "];\n"
    )
    result += " " * 6 + "int[] part;\n"

    parts = math.ceil(len(string_pack_ids) / MAX_IDS_PER_METHOD)
    for i in range(0, parts):
        result += " " * 6 + "part = getStringPacksMappingPart" + str(i) + "();\n"
        result += (
            " " * 6
            + "System.arraycopy(part, 0, result, "
            + str(MAX_IDS_PER_METHOD * i)
            + ", part.length);\n"
        )

    result += " " * 6 + "return result;\n"
    result += " " * 2 + "}\n"
    result += "\n"

    # Create submethods
    for i in range(0, parts):
        start = i * MAX_IDS_PER_METHOD
        end = start + MAX_IDS_PER_METHOD
        result += generate_java_internal(
            string_pack_ids[start:end], "getStringPacksMappingPart%s" % i
        )

    return result


def generate_java_internal(string_pack_ids, method_name):
    result = []
    result += " " * 2 + "private static int[] %s() {\n" % method_name
    result += " " * 6 + "return new int[]{\n"
    for line in string_pack_ids:
        result += line + "\n"
    result += " " * 6 + "};\n"
    result += " " * 2 + "}\n"
    result += "\n"
    return result


def generate_kotlin(string_pack_ids):
    if len(string_pack_ids) <= MAX_IDS_PER_METHOD:
        # No need for sub-methods, just create the whole array in the main method
        return generate_kotlin_internal(string_pack_ids, "getStringPacksMapping")

    result = []
    result += "fun getStringPacksMapping(): Array<Int> {\n"
    result += " " * 4 + "val result = int[" + str(len(string_pack_ids)) + "]\n"
    result += " " * 4 + "var part: Array<Int>\n"

    parts = math.ceil(len(string_pack_ids) / MAX_IDS_PER_METHOD)
    for i in range(0, parts):
        result += " " * 4 + "part = getStringPacksMappingPart" + str(i) + "()\n"
        result += (
            " " * 4
            + "System.arraycopy(part, 0, result, "
            + str(MAX_IDS_PER_METHOD * i)
            + ", part.length)\n"
        )

    result += " " * 4 + "result\n"
    result += "}\n"
    result += "\n"

    # Create submethods
    for i in range(0, parts):
        start = i * MAX_IDS_PER_METHOD
        end = start + MAX_IDS_PER_METHOD
        result += generate_kotlin_internal(
            string_pack_ids[start:end], "getStringPacksMappingPart%s" % i
        )

    return result


def generate_kotlin_internal(string_pack_ids, method_name):
    result = []
    result += "fun %s(): Array<Int> {\n" % method_name
    result += " " * 4 + "intArrayOf(\n"
    for line in string_pack_ids:
        result += line + "\n"
    result += " " * 4 + ")\n"
    result += "}\n"
    result += "\n"
    return result


def generate_non_movable_set(sp_config, xml_files) -> Set:
    not_movable = set()
    for filename in xml_files:
        if not filename:
            continue
        if filename.endswith("/values/strings.xml"):
            not_movable.update(find_donotpack_strings(filename))
            continue
        if filename.endswith("/strings.xml"):
            continue
        not_movable.update(
            find_strings_used_in_xml(filename, sp_config.safe_widget_classes)
        )
    return not_movable


def find_movable_strings(sp_config, print_reverse=False):
    xml_files = subprocess.check_output(
        sp_config.find_resource_files_command, shell=True, encoding="ASCII"
    ).split("\n")

    not_movable = generate_non_movable_set(sp_config, xml_files)

    strings_to_move = set()
    for source_file in sp_config.get_default_string_files():
        with open(source_file) as english_sources:
            for match in NAME_CATCHER_RE.findall(english_sources.read()):
                msg_name = match[1]
                if msg_name in not_movable:
                    if print_reverse:
                        print(msg_name)
                    continue
                strings_to_move.add(match)

    # Don't output IDs if we are interested in the unmovable strings.
    if not print_reverse:
        output_string_ids_map(sp_config, strings_to_move)


def main():
    parser = argparse.ArgumentParser(description="Find strings to move to string packs")
    parser.add_argument(
        "--reverse",
        action="store_true",
        help="List the strings that cannot be moved, instead of those that can be moved.",
    )

    parser.add_argument("--config", help="Location of JSON config file.")
    args = parser.parse_args()

    sp_config = string_pack_config.load_config(config_json_file_path=args.config)
    find_movable_strings(sp_config, args.reverse)


if __name__ == "__main__":
    main()
