import json
import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class NativeShelfResourceContractTest(unittest.TestCase):
    def test_printf_arguments_preserve_positions_and_native_string_types(self):
        source = ROOT / "feature/shelf/impl/src/commonMain/composeResources/values/strings.xml"
        native = ROOT / "iosApp/iosApp/ru.lproj/NativeShelf.strings"
        values = {}
        for line in native.read_text(encoding="utf-8").splitlines():
            match = re.fullmatch(r'"([^"\\]+)"\s*=\s*("(?:[^"\\]|\\.)*");', line)
            if match:
                values[match[1]] = json.loads(match[2])
        for item in ET.parse(source).getroot().findall("string"):
            expected = [(index, "@" if kind == "s" else kind)
                        for index, kind in re.findall(r"%(\d+)\$([ds])", item.text or "")]
            if expected:
                key = item.attrib["name"]
                with self.subTest(key=key):
                    self.assertIn(key, values)
                    self.assertNotRegex(values[key], r"%\d+\$s")
                    self.assertEqual(expected, re.findall(r"%(\d+)\$([@d])", values[key]))


if __name__ == "__main__":
    unittest.main()
