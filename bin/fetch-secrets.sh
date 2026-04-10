#!/bin/bash
#
# Fetches build secrets from the team 1Password vault:
#   - secrets/secrets.properties (Mux env keys, optional overrides)
#   - app/google-services.json   (Firebase config — without it, Firebase is disabled at build time)
#
# Prerequisites:
#   - Membership in the `client-api-secrets` 1Password vault.
#   - 1Password CLI installed: https://developer.1password.com/docs/cli/
#   - Signed in: `eval $(op signin)` (or biometric unlock if configured).
#
# Open-source contributors who don't have vault access can skip running this;
# the build will still succeed and Firebase will be disabled.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if ! command -v op >/dev/null 2>&1; then
  echo "error: 1Password CLI ('op') not found." >&2
  echo "Install: https://developer.1password.com/docs/cli/get-started/" >&2
  exit 1
fi

op inject \
  --in-file "$REPO_ROOT/secrets/secrets.properties.tpl" \
  --out-file "$REPO_ROOT/secrets/secrets.properties"
echo "Wrote $REPO_ROOT/secrets/secrets.properties"

op read "op://client-api-secrets/Firebase analytics AndroidTV json/google-services.json" \
  > "$REPO_ROOT/app/google-services.json"
echo "Wrote $REPO_ROOT/app/google-services.json"
