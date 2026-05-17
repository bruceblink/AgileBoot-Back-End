#!/usr/bin/env python3
"""Compatibility wrapper for scripts/secret_tool.py generate-rsa."""

from __future__ import annotations

import sys

from secret_tool import main


if __name__ == "__main__":
    sys.argv.insert(1, "generate-rsa")
    main()
