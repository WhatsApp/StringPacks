#!/usr/bin/env python3

# Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
#
# This source code is licensed under the Apache 2.0 license found in
# the LICENSE file in the root directory of this source tree.


import argparse
import re
import subprocess
from os import path
from xml.etree import ElementTree

import string_pack_config


NAMESPACE_AND_ATTRIB_RE = re.compile("^\{(.+)\}(.+)$")


def separate_namespace(attribute_name):
    match = NAMESPACE_AND_ATTRIB_RE.match(attribute_name)
    if match:
        return match.groups()
    else:
        return None, attribute_name


STRING_USAGE_RE = re.compile("@string/([A-Za-z0-9_]+)")

OK_NAMESPACES = {"http://schemas.android.com/tools"}


def find_strings_used_in_xml(filename, safe_widgets):
    result = set()
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
    return result


NAME_CATCHER_RE = re.compile('<(string|plurals) name="([^"]+)"')


def output_string_ids_map(sp_config, strings_to_move):
    string_pack_ids = []
    for index, string_tuple in enumerate(sorted(strings_to_move)):
        string_type, string_name = string_tuple
        string_pack_ids.append(
            (" " * 10 + "R.%s.%s, %d,") % (string_type, string_name, index)
        )

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
            + [line + "\n" for line in string_pack_ids]
            + existing_class_file_lines[region_end_index:]
        )
    print("Updated: " + class_file_path)


def find_movable_strings(sp_config, print_reverse=False):
    xml_files = subprocess.check_output(
        sp_config.find_resource_files_command, shell=True, encoding="ASCII"
    ).split("\n")

    not_movable = set()
    for filename in xml_files:
        if (
            not filename
            # We assume strings.xml files only have declarations, no references.
            or filename.endswith("/strings.xml")
        ):
            continue
        not_movable.update(
            find_strings_used_in_xml(filename, sp_config.safe_widget_classes)
        )

    strings_to_move = set()
    for source_file in sp_config.get_default_string_files():
        with open(source_file) as english_sources:
            for match in NAME_CATCHER_RE.findall(english_sources.read()):
                msg_type, msg_name = match
                if msg_type == "string" and msg_name in not_movable:
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
