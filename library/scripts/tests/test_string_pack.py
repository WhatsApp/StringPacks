#!/usr/bin/env python3
# Copyright (c) Meta Platforms, Inc. and affiliates.
#
# This source code is licensed under the Apache 2.0 license found in
# the LICENSE file in the root directory of this source tree.

import unittest
from typing import Optional

import pack_strings
import string_pack
from tests import test_util


class FakeIdFinder(object):
    TEST_DATA = {"first_plurals": 0, "first_string": 1, "second_string": 2}

    def get_id(self, resource_name: str) -> Optional[int]:
        return FakeIdFinder.TEST_DATA.get(resource_name)


class TestStringPackMethods(unittest.TestCase):
    def test_not_nullified(self):
        self.assertDictEqual(
            {
                0: {0: "many plurals", 1: "zero plurals", 2: "one plural"},
                1: "first string",
            },
            string_pack.read_string_dict(
                "en",  # Don't care
                test_util.get_res_path("test_resources_en.xml"),
                FakeIdFinder(),
                pack_strings.noop_plural_handler,
            ),
        )

    def test_nullified_string(self):
        self.assertDictEqual(
            {0: {0: "many plurals", 1: "zero plurals", 2: "one plural"}},
            string_pack.read_string_dict(
                "en",  # Don't care
                test_util.get_res_path("test_resources_en.xml"),
                FakeIdFinder(),
                pack_strings.noop_plural_handler,
                {"R.string.first_string"},
            ),
        )

    def test_nullified_plurals(self):
        self.assertDictEqual(
            {1: "first string"},
            string_pack.read_string_dict(
                "en",  # Don't care
                test_util.get_res_path("test_resources_en.xml"),
                FakeIdFinder(),
                pack_strings.noop_plural_handler,
                {"R.plurals.first_plurals"},
            ),
        )
