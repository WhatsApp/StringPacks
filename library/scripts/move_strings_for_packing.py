#!/usr/bin/env python3

# Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
#
# This source code is licensed under the Apache 2.0 license found in
# the LICENSE file in the root directory of this source tree.


import argparse
import glob
import logging
import os
import re

import pack_strings
import string_pack_config
from string_pack_config import LanguageHandlingCase


HEADER = """<?xml version="1.0" encoding="utf-8"?>
<!-- \u0040generated by StringPacks move_strings_for_packing.py script. -->
<resources>
"""

FOOTER = "</resources>\n"

# fmt: off
ROW_PATTERN = re.compile(
    "(?:[ \t]*<!-- .*? -->\n)?"
    "[ \t]*<(string|plurals) name=\"(.+?)\">"
    ".*?"
    "</\\1>\n",
    re.DOTALL,
)
# fmt: on


def get_resource_content_without_header(resource_file_path):
    with open(resource_file_path, "rt") as resource_file:
        # skip headers
        for line in resource_file:
            if "<resources" in line:
                break
        return resource_file.read()


def move_strings(srcfile, dstfile, id_finder, keep_dest):
    output_for_original_file = []
    output_for_new_file = []

    logging.debug("Reading xml file: %s", srcfile)
    data = get_resource_content_without_header(srcfile)

    for match in ROW_PATTERN.finditer(data):
        piece = match.group(0)
        if id_finder.get_id(match.group(2)) is not None:
            output_for_new_file.append(piece)
        else:
            output_for_original_file.append(piece)

    if keep_dest:
        try:
            data = get_resource_content_without_header(dstfile)
            for match in ROW_PATTERN.finditer(data):
                piece = match.group(0)
                output_for_new_file.append(piece)
        except FileNotFoundError:
            # The output file doesn't exist. It's OK.
            pass

    with open(srcfile, "wt") as xml_file:
        logging.debug("Updating: %s", srcfile)
        xml_file.write(HEADER)
        xml_file.write("".join(output_for_original_file))
        xml_file.write(FOOTER)

    if not output_for_new_file:
        logging.debug("No strings to write out")

        # There's no strings to write
        return

    # Create a directory for dstfile, in case it doesn't exist
    os.makedirs(os.path.dirname(dstfile), exist_ok=True)

    with open(dstfile, "wt") as xml_file:
        logging.debug("Writing out: %s", dstfile)
        xml_file.write(HEADER)
        xml_file.write("".join(output_for_new_file))
        xml_file.write(FOOTER)


def get_dest_file(sp_config, source_resource_directory, language_qualifier):
    dest_file = os.path.join(
        sp_config.packable_strings_directory,
        # keep source directory information at destination file to help us debug in future.
        source_resource_directory.replace(os.path.sep, "_"),
        "values-" + language_qualifier,
        "strings.xml",
    )
    logging.info("To file: %s", dest_file)
    return dest_file


# Escape code for color
SET_WARNING_COLOR = "\033[33m\033[41m"  # yellow text with red background
CLEAR_COLOR = "\033[0m"


def move_all_strings(sp_config, keep_dest):
    id_finder = pack_strings.IdFinder(sp_config)
    for resources_directory in sp_config.original_resources_directories:
        path_pattern = os.path.join(resources_directory, "values-*", "strings.xml")

        for string_xml_path in glob.glob(path_pattern):
            values_directory_name = os.path.normpath(string_xml_path).split(os.sep)[-2]
            resource_qualifier = values_directory_name.replace("values-", "")

            handler_case = sp_config.get_handling_case(resource_qualifier)
            if handler_case == LanguageHandlingCase.DROP:
                logging.warning(
                    SET_WARNING_COLOR + "Dropping: " + string_xml_path + CLEAR_COLOR
                )
                move_strings(string_xml_path, os.devnull, id_finder, keep_dest=False)
            elif handler_case == LanguageHandlingCase.PACK:
                logging.info("Moving: %s", string_xml_path)
                move_strings(
                    string_xml_path,
                    get_dest_file(sp_config, resources_directory, resource_qualifier),
                    id_finder,
                    keep_dest,
                )

            elif handler_case == LanguageHandlingCase.KEEP_ORIGINAL:
                logging.info("Keep untouched: %s", string_xml_path)


def main():
    logging.basicConfig(level=logging.INFO)
    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument("--config", help="Location of JSON config file.")
    arg_parser.add_argument("--keep-dest", action="store_true")
    args = arg_parser.parse_args()

    sp_config = string_pack_config.load_config(config_json_file_path=args.config)
    move_all_strings(sp_config, args.keep_dest)


if __name__ == "__main__":
    main()