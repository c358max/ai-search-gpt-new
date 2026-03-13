#!/usr/bin/env bash
set -euo pipefail

# 전체 네임스페이스에서 elastic/ai-search 관련 Pod를 빠르게 확인합니다.
kubectl get pods -A | grep -E 'elastic|ai-search' || true

# pod 확인