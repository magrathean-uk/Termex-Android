#!/usr/bin/env bash
set -euo pipefail

TERMEX_LIVE_VM_FIXTURE_ROOT_DEFAULT="/Users/bolyki/dev/test/termex-vms"

live_vm_fixture_root() {
  printf '%s\n' "${TERMEX_VM_FIXTURE_ROOT:-$TERMEX_LIVE_VM_FIXTURE_ROOT_DEFAULT}"
}

live_vm_resolve_file() {
  local candidate
  for candidate in "$@"; do
    if [[ -f "$candidate" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  return 1
}

live_vm_login_json() {
  local root="$1"
  live_vm_resolve_file "$root/login.json" "$root/raw/login.json"
}

live_vm_summary_md() {
  local root="$1"
  live_vm_resolve_file "$root/README.md" "$root/SUMMARY.md" "$root/raw/SUMMARY.md"
}

android_variant_app_id_suffix() {
  case "${1:-dev}" in
    dev) printf '.dev\n' ;;
    releaseProof) printf '.proof\n' ;;
    *) printf '.%s\n' "${1:-dev}" ;;
  esac
}

capitalize_ascii() {
  local value="${1:-}"
  if [[ -z "$value" ]]; then
    printf '\n'
    return 0
  fi
  printf '%s%s\n' "$(printf '%s' "${value:0:1}" | tr '[:lower:]' '[:upper:]')" "${value:1}"
}

live_vm_test_list_for_plan() {
  local plan="${1:-full}"

  case "$plan" in
    smoke)
      cat <<'EOF'
com.termex.app.RealSshFlowTest#keyAuthConnectsToLiveServer
com.termex.app.RealSshFlowTest#passwordAuthConnectsToLiveServer
com.termex.app.RealSshFlowTest#certificateAuthConnectsToLiveServer
EOF
      ;;
    auth)
      cat <<'EOF'
com.termex.app.RealSshFlowTest#keyAuthConnectsToLiveServer
com.termex.app.RealSshFlowTest#passwordAuthConnectsToLiveServer
com.termex.app.RealSshFlowTest#wrongPasswordDoesNotCrash
com.termex.app.RealSshFlowTest#certificateAuthConnectsToLiveServer
EOF
      ;;
    trust)
      cat <<'EOF'
com.termex.app.RealSshFlowTest#slowHostTrustThenConnectsToLiveServer
com.termex.app.RealSshFlowTest#rejectingUnknownHostKeyStopsConnection
EOF
      ;;
    changed-host)
      cat <<'EOF'
com.termex.app.RealSshFlowTest#trustsLiveHostKeyBeforeRotation
com.termex.app.RealSshFlowTest#changedHostKeyPromptsOnReconnect
EOF
      ;;
    routing)
      cat <<'EOF'
com.termex.app.RealSshFlowTest#jumpHostConnectsToLiveServer
com.termex.app.RealSshFlowTest#localForwardOpensSshBanner
com.termex.app.RealSshFlowTest#remoteForwardExposesServiceOnHost
com.termex.app.RealSshFlowTest#dynamicForwardRoutesSocksTraffic
EOF
      ;;
    agent)
      cat <<'EOF'
com.termex.app.RealSshFlowTest#forwardAgentMakesSshAuthSockVisible
EOF
      ;;
    session)
      cat <<'EOF'
com.termex.app.RealSshFlowTest#persistentTmuxSessionRestoresAfterRelaunch
EOF
      ;;
    full|all)
      cat <<'EOF'
com.termex.app.RealSshFlowTest#trustsLiveHostKeyBeforeRotation
com.termex.app.RealSshFlowTest#changedHostKeyPromptsOnReconnect
com.termex.app.RealSshFlowTest#jumpHostConnectsToLiveServer
com.termex.app.RealSshFlowTest#localForwardOpensSshBanner
com.termex.app.RealSshFlowTest#remoteForwardExposesServiceOnHost
com.termex.app.RealSshFlowTest#dynamicForwardRoutesSocksTraffic
com.termex.app.RealSshFlowTest#forwardAgentMakesSshAuthSockVisible
com.termex.app.RealSshFlowTest#rejectingUnknownHostKeyStopsConnection
com.termex.app.RealSshFlowTest#slowHostTrustThenConnectsToLiveServer
com.termex.app.RealSshFlowTest#keyAuthConnectsToLiveServer
com.termex.app.RealSshFlowTest#wrongPasswordDoesNotCrash
com.termex.app.RealSshFlowTest#certificateAuthConnectsToLiveServer
com.termex.app.RealSshFlowTest#persistentTmuxSessionRestoresAfterRelaunch
com.termex.app.RealSshFlowTest#passwordAuthConnectsToLiveServer
EOF
      ;;
    custom)
      if [[ -n "${TERMEX_UI_TEST_CLASS:-}" ]]; then
        printf '%s\n' "$TERMEX_UI_TEST_CLASS"
      else
        echo "TERMEX_UI_TEST_CLASS is required for plan: custom" >&2
        return 2
      fi
      ;;
    *)
      echo "unknown live VM test plan: $plan" >&2
      return 2
      ;;
  esac
}
