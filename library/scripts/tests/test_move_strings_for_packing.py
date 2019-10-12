#!/usr/bin/env python3

# Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
#
# This source code is licensed under the Apache 2.0 license found in
# the LICENSE file in the root directory of this source tree.


import unittest
from unittest import mock

import move_strings_for_packing as sp_move
from tests import test_util


class TestPackStringsMethods(unittest.TestCase):
    def test_get_dest_file(self):
        sp_config = mock.Mock()
        sp_config.packable_strings_directory = "app/string-packs/strings"

        self.assertEqual(
            "app/string-packs/strings/src_main_res_/values-zh/strings.xml",
            sp_move.get_dest_file(sp_config, "src/main/res/", "zh"),
        )

    def test_get_resource_file_content(self):
        self.assertEqual(
            (
                '    <string name="hello_world">Hello World!</string>\n'
                "    <!-- Comment for string -->\n"
                '    <string name="sentence">The quick brown fox jumps over the lazy dog.</string>\n'
                "</resources>\n"
            ),
            sp_move.get_resource_content_without_header(
                test_util.get_res_path("test_move_resources.xml")
            ),
        )
