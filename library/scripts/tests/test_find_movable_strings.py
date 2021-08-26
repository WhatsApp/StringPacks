# Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
#
# This source code is licensed under the Apache 2.0 license found in
# the LICENSE file in the root directory of this source tree.

import unittest

import find_movable_strings as sp_find
import string_pack_config
from tests import test_util


class TestPackStringsMethods(unittest.TestCase):
    def test_find_strings_used_in_xml_with_layout(self):
        self.assertSetEqual(
            {"button", "button_url", "image", "description", "title", "app_title"},
            sp_find.find_strings_used_in_xml(
                test_util.get_res_path("test_layout.xml"), frozenset()
            ),
        )

    def test_find_strings_used_in_xml_with_safe_widget(self):
        self.assertSetEqual(
            {"image", "title", "app_title"},
            sp_find.find_strings_used_in_xml(
                test_util.get_res_path("test_layout.xml"), frozenset({"Button"})
            ),
        )

    def test_find_strings_used_in_xml_with_resources(self):
        self.assertSetEqual(
            {"other_string", "string_array_two", "string_array_one", "style_text"},
            sp_find.find_strings_used_in_xml(
                test_util.get_res_path("test_resources.xml"), frozenset()
            ),
        )

    def test_do_not_pack_strings_not_packed_into_resource(self):
        manual_non_movable = "manual"
        sp_config = string_pack_config.StringPackConfig()
        sp_config.do_not_pack = set([manual_non_movable, "other_string"])
        self.assertSetEqual(
            {
                "other_string",
                "string_array_two",
                "string_array_one",
                "style_text",
                manual_non_movable,
            },
            sp_find.generate_non_movable_set(
                sp_config,
                [test_util.get_res_path("test_resources.xml")],
            ),
        )
