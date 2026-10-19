from pathlib import Path
import textwrap
import unittest


workflow = (Path(__file__).resolve().parents[2] / ".github/workflows/ios-testflight.yml").read_text(encoding="utf-8")
start = workflow.index("          associated_domains = ")
end = workflow.index("          if profile.get('Entitlements', {}).get('get-task-allow')", start)
domain_check = compile(textwrap.dedent(workflow[start:end]), "ios-testflight-domain-check", "exec")


class AssociatedDomainsProfileTest(unittest.TestCase):
    def test_accepts_apple_string_wildcard_and_domain_arrays(self):
        for domains in ("*", ["*"], ["applinks:coffeepeek.by"]):
            with self.subTest(domains=domains):
                exec(domain_check, {"profile": {"Entitlements": {
                    "com.apple.developer.associated-domains": domains,
                }}})

    def test_rejects_missing_unrelated_and_malformed_entitlements(self):
        for domains in (
            None, False, [], "", "*other", "applinks:coffeepeek.by", {"*": True},
            ["applinks:other.example"], ["applinks:coffeepeek.by.evil.example"],
        ):
            with self.subTest(domains=domains):
                with self.assertRaises(SystemExit):
                    exec(domain_check, {"profile": {"Entitlements": {
                        "com.apple.developer.associated-domains": domains,
                    }}})
        with self.assertRaises(SystemExit):
            exec(domain_check, {"profile": {}})


if __name__ == "__main__":
    unittest.main()
