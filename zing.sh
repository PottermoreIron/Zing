#!/usr/bin/env bash
# zing.sh — Zing platform service manager
#
# Usage:
#   ./zing.sh start   [service]   — start all or one service
#   ./zing.sh stop    [service]   — stop  all or one service
#   ./zing.sh restart [service]   — restart
#   ./zing.sh status              — show status of every service
#   ./zing.sh logs    <service>   — tail a service log
#
# Services: nacos | redis | auth | member | gateway | all (default)
# Secrets:  read from .env.local in this directory

set -euo pipefail

# ── Paths ─────────────────────────────────────────────────────────────────────
ZING_ROOT="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$ZING_ROOT/.env.local"
LOG_DIR="$ZING_ROOT/logs"
PID_DIR="$ZING_ROOT/.pids"
NACOS_HOME="/Users/yecao/Me/CodeTool/nacos"

mkdir -p "$LOG_DIR" "$PID_DIR"

# ── Colors ────────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

info()    { echo -e "${BLUE}[INFO]${NC} $*"; }
ok()      { echo -e "${GREEN}[ OK ]${NC} $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()     { echo -e "${RED}[ERR ]${NC} $*" >&2; }
section() { echo -e "\n${BOLD}${CYAN}══ $* ══${NC}"; }

# ── Env loader ────────────────────────────────────────────────────────────────
# Reads KEY=VALUE lines from .env.local and exports them into this shell.
# Comment lines (#...) and blank lines are ignored.
load_env() {
    [ -f "$ENV_FILE" ] || { warn ".env.local not found — relying on built-in defaults"; return; }
    while IFS= read -r line || [ -n "$line" ]; do
        [[ "$line" =~ ^[[:space:]]*# ]] && continue
        [[ -z "${line//[[:space:]]/}" ]] && continue
        [[ "$line" =~ ^[A-Za-z_][A-Za-z0-9_]*= ]] || continue
        local key="${line%%=*}"
        local val="${line#*=}"
        export "$key"="$val"
    done < "$ENV_FILE"
}

load_env

# ── Defaults (override any missing vars from .env.local) ──────────────────────
: "${NACOS_SERVER_ADDR:=localhost:8848}"
: "${NACOS_NAMESPACE:=dev}"
: "${REDIS_HOST:=localhost}"
: "${REDIS_PORT:=6379}"
: "${REDIS_PASSWORD:=}"
: "${REDIS_DATABASE:=0}"
: "${AUTH_SERVICE_PORT:=8081}"
: "${MEMBER_SERVICE_PORT:=11000}"
: "${GATEWAY_PORT:=8090}"
: "${DB_HOST:=localhost}"
: "${DB_PORT:=3306}"
: "${DB_NAME:=member}"
: "${DB_USERNAME:=root}"
: "${DB_PASSWORD:=}"
: "${AUTH_OAUTH2_ENABLED:=false}"
: "${GOOGLE_CLIENT_ID:=}"
: "${GOOGLE_CLIENT_SECRET:=}"
: "${GITHUB_CLIENT_ID:=}"
: "${GITHUB_CLIENT_SECRET:=}"
: "${FACEBOOK_CLIENT_ID:=}"
: "${FACEBOOK_CLIENT_SECRET:=}"
: "${APPLE_CLIENT_ID:=}"
: "${APPLE_CLIENT_SECRET:=}"

# ── Proxy ─────────────────────────────────────────────────────────────────────
# Java does not honour the system proxy automatically.
# If HTTPS_PROXY or http_proxy is set in the environment, parse it out and
# forward it to the JVM so RestClient can reach external APIs (e.g. Google OAuth2).
JVM_PROXY_OPTS=""
_proxy_url="${HTTPS_PROXY:-${https_proxy:-${HTTP_PROXY:-${http_proxy:-}}}}"
if [ -n "$_proxy_url" ]; then
    _proxy_host="$(echo "$_proxy_url" | sed -E 's|https?://||' | cut -d: -f1)"
    _proxy_port="$(echo "$_proxy_url" | sed -E 's|https?://||' | cut -d: -f2 | tr -d '/')"
    JVM_PROXY_OPTS="-Dhttp.proxyHost=${_proxy_host} -Dhttp.proxyPort=${_proxy_port} -Dhttps.proxyHost=${_proxy_host} -Dhttps.proxyPort=${_proxy_port} -Dhttp.nonProxyHosts=localhost|127.*|10.*|192.168.*"
    info "Using proxy: ${_proxy_host}:${_proxy_port} (from env)"
fi
AUTH_JAR="$ZING_ROOT/auth/auth-service/target/auth-service-0.0.1-SNAPSHOT.jar"
MEMBER_JAR="$ZING_ROOT/member/member-service/target/member-service-0.0.1-SNAPSHOT.jar"
GATEWAY_JAR="$ZING_ROOT/gateway/target/gateway-0.0.1-SNAPSHOT.jar"

# ── PID helpers ───────────────────────────────────────────────────────────────
pid_file() { echo "$PID_DIR/$1.pid"; }
save_pid() { echo "$2" > "$(pid_file "$1")"; }
read_pid()  { local f; f="$(pid_file "$1")"; [ -f "$f" ] && cat "$f" || echo ""; }

is_port_open() { lsof -ti :"$1" &>/dev/null; }

kill_port() {
    local pids; pids="$(lsof -ti :"$1" 2>/dev/null || true)"
    [ -n "$pids" ] && echo "$pids" | xargs kill -9 2>/dev/null || true
}

wait_for_port() {
    local name="$1" port="$2" max="${3:-40}"
    local i=0
    printf "${BLUE}[INFO]${NC} Waiting for %s on :%s " "$name" "$port"
    while ! lsof -ti :"$port" &>/dev/null; do
        printf '.'; sleep 1; i=$((i+1))
        if [ "$i" -ge "$max" ]; then
            echo ""
            err "$name did not start within ${max}s. Check logs/$name.log"
            return 1
        fi
    done
    echo " ready"
}

# ── Nacos ─────────────────────────────────────────────────────────────────────
start_nacos() {
    section "Nacos"
    if is_port_open 8848; then
        ok "Nacos already running on :8848"
        return 0
    fi
    info "Starting Nacos in standalone mode..."
    sh "$NACOS_HOME/bin/startup.sh" -m standalone >> "$LOG_DIR/nacos-startup.log" 2>&1
    wait_for_port "Nacos" 8848 40
    ok "Nacos started"
}

stop_nacos() {
    section "Nacos"
    if ! is_port_open 8848; then
        warn "Nacos is not running"
        return 0
    fi
    info "Stopping Nacos..."
    sh "$NACOS_HOME/bin/shutdown.sh" >> "$LOG_DIR/nacos-startup.log" 2>&1 || true
    sleep 3
    kill_port 8848
    ok "Nacos stopped"
}

# ── Redis ─────────────────────────────────────────────────────────────────────
start_redis() {
    section "Redis"
    if is_port_open "$REDIS_PORT"; then
        ok "Redis already running on :$REDIS_PORT"
        return 0
    fi
    info "Starting Redis via brew services..."
    brew services start redis > /dev/null 2>&1 || {
        warn "brew services failed, falling back to redis-server..."
        redis-server --daemonize yes --port "$REDIS_PORT"
    }
    wait_for_port "Redis" "$REDIS_PORT" 15
    ok "Redis started"
}

stop_redis() {
    section "Redis"
    warn "Redis is managed by brew — skipping (use 'brew services stop redis' to stop manually)"
}

# ── auth-service ──────────────────────────────────────────────────────────────
start_auth() {
    section "auth-service"
    if is_port_open "$AUTH_SERVICE_PORT"; then
        ok "auth-service already running on :$AUTH_SERVICE_PORT"
        return 0
    fi
    [ -f "$AUTH_JAR" ] || { err "JAR not found: $AUTH_JAR"; return 1; }
    info "Starting auth-service on :$AUTH_SERVICE_PORT..."
    # shellcheck disable=SC2086
    nohup java $JVM_PROXY_OPTS -jar "$AUTH_JAR" \
        --server.port="$AUTH_SERVICE_PORT" \
        --auth.oauth2.enabled="$AUTH_OAUTH2_ENABLED" \
        --GOOGLE_CLIENT_ID="$GOOGLE_CLIENT_ID" \
        --GOOGLE_CLIENT_SECRET="$GOOGLE_CLIENT_SECRET" \
        --GITHUB_CLIENT_ID="$GITHUB_CLIENT_ID" \
        --GITHUB_CLIENT_SECRET="$GITHUB_CLIENT_SECRET" \
        --FACEBOOK_CLIENT_ID="$FACEBOOK_CLIENT_ID" \
        --FACEBOOK_CLIENT_SECRET="$FACEBOOK_CLIENT_SECRET" \
        --APPLE_CLIENT_ID="$APPLE_CLIENT_ID" \
        --APPLE_CLIENT_SECRET="$APPLE_CLIENT_SECRET" \
        > "$LOG_DIR/auth-service.log" 2>&1 &
    local pid=$!
    save_pid "auth" "$pid"
    wait_for_port "auth-service" "$AUTH_SERVICE_PORT" 40
    ok "auth-service started (PID $pid)"
}

stop_auth() {
    section "auth-service"
    local pid; pid="$(read_pid auth)"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        info "Stopping auth-service (PID $pid)..."
        kill "$pid" 2>/dev/null || true
        # Wait until port is actually free before declaring success
        local i=0
        while lsof -ti :"$AUTH_SERVICE_PORT" &>/dev/null && [ $i -lt 15 ]; do
            sleep 1; i=$((i+1))
        done
        kill_port "$AUTH_SERVICE_PORT" 2>/dev/null || true
    elif is_port_open "$AUTH_SERVICE_PORT"; then
        info "Stopping auth-service on :$AUTH_SERVICE_PORT..."
        kill_port "$AUTH_SERVICE_PORT"
    else
        warn "auth-service is not running"
        return 0
    fi
    rm -f "$(pid_file auth)"
    ok "auth-service stopped"
}

# ── member-service ────────────────────────────────────────────────────────────
start_member() {
    section "member-service"
    if is_port_open "$MEMBER_SERVICE_PORT"; then
        ok "member-service already running on :$MEMBER_SERVICE_PORT"
        return 0
    fi
    [ -f "$MEMBER_JAR" ] || { err "JAR not found: $MEMBER_JAR"; return 1; }
    info "Starting member-service on :$MEMBER_SERVICE_PORT..."
    nohup java -jar "$MEMBER_JAR" \
        --server.port="$MEMBER_SERVICE_PORT" \
        --DB_HOST="$DB_HOST" \
        --DB_PORT="$DB_PORT" \
        --DB_NAME="$DB_NAME" \
        --DB_USERNAME="$DB_USERNAME" \
        --DB_PASSWORD="$DB_PASSWORD" \
        --RABBITMQ_HOST="${RABBITMQ_HOST:-localhost}" \
        --RABBITMQ_PORT="${RABBITMQ_PORT:-5672}" \
        --RABBITMQ_USERNAME="${RABBITMQ_USERNAME:-guest}" \
        --RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-guest}" \
        > "$LOG_DIR/member-service.log" 2>&1 &
    local pid=$!
    save_pid "member" "$pid"
    wait_for_port "member-service" "$MEMBER_SERVICE_PORT" 40
    ok "member-service started (PID $pid)"
}

stop_member() {
    section "member-service"
    local pid; pid="$(read_pid member)"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        info "Stopping member-service (PID $pid)..."
        kill "$pid" 2>/dev/null || true
        local i=0
        while lsof -ti :"$MEMBER_SERVICE_PORT" &>/dev/null && [ $i -lt 15 ]; do
            sleep 1; i=$((i+1))
        done
        kill_port "$MEMBER_SERVICE_PORT" 2>/dev/null || true
    elif is_port_open "$MEMBER_SERVICE_PORT"; then
        info "Stopping member-service on :$MEMBER_SERVICE_PORT..."
        kill_port "$MEMBER_SERVICE_PORT"
    else
        warn "member-service is not running"
        return 0
    fi
    rm -f "$(pid_file member)"
    ok "member-service stopped"
}

# ── gateway ───────────────────────────────────────────────────────────────────
start_gateway() {
    section "gateway"
    if is_port_open "$GATEWAY_PORT"; then
        ok "gateway already running on :$GATEWAY_PORT"
        return 0
    fi
    [ -f "$GATEWAY_JAR" ] || { err "JAR not found: $GATEWAY_JAR"; return 1; }
    info "Starting gateway on :$GATEWAY_PORT..."
    # gateway YAML resolves vars via ${KEY}, so pass as JVM system properties (-D)
    # shellcheck disable=SC2086
    nohup java $JVM_PROXY_OPTS \
        -DNACOS_SERVER_ADDR="$NACOS_SERVER_ADDR" \
        -DNACOS_NAMESPACE="$NACOS_NAMESPACE" \
        -DREDIS_HOST="$REDIS_HOST" \
        -DREDIS_PORT="$REDIS_PORT" \
        -DREDIS_PASSWORD="$REDIS_PASSWORD" \
        -DREDIS_DATABASE="$REDIS_DATABASE" \
        -DGATEWAY_PORT="$GATEWAY_PORT" \
        -jar "$GATEWAY_JAR" \
        > "$LOG_DIR/gateway.log" 2>&1 &
    local pid=$!
    save_pid "gateway" "$pid"
    wait_for_port "gateway" "$GATEWAY_PORT" 20
    ok "gateway started (PID $pid)"
}

stop_gateway() {
    section "gateway"
    local pid; pid="$(read_pid gateway)"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        info "Stopping gateway (PID $pid)..."
        kill "$pid" 2>/dev/null || true
        local i=0
        while lsof -ti :"$GATEWAY_PORT" &>/dev/null && [ $i -lt 15 ]; do
            sleep 1; i=$((i+1))
        done
        kill_port "$GATEWAY_PORT" 2>/dev/null || true
    elif is_port_open "$GATEWAY_PORT"; then
        info "Stopping gateway on :$GATEWAY_PORT..."
        kill_port "$GATEWAY_PORT"
    else
        warn "gateway is not running"
        return 0
    fi
    rm -f "$(pid_file gateway)"
    ok "gateway stopped"
}

# ── Status ────────────────────────────────────────────────────────────────────
check_svc() {
    local label="$1" port="$2"
    if lsof -ti :"$port" &>/dev/null; then
        local pid; pid="$(lsof -ti :"$port" | head -1)"
        printf "  ${GREEN}●${NC} %-20s :%-6s PID %s\n" "$label" "$port" "$pid"
    else
        printf "  ${RED}○${NC} %-20s :%-6s (stopped)\n" "$label" "$port"
    fi
}

cmd_status() {
    section "Zing Platform Status"
    check_svc "nacos"          8848
    check_svc "redis"          "$REDIS_PORT"
    check_svc "auth-service"   "$AUTH_SERVICE_PORT"
    check_svc "member-service" "$MEMBER_SERVICE_PORT"
    check_svc "gateway"        "$GATEWAY_PORT"
    echo ""
}

# ── Logs ──────────────────────────────────────────────────────────────────────
cmd_logs() {
    local svc="${1:-}"
    case "$svc" in
        nacos)   tail -f "$NACOS_HOME/logs/nacos.log" 2>/dev/null || tail -f "$LOG_DIR/nacos-startup.log" ;;
        redis)   warn "Redis logs are managed by brew. Run: brew services log redis" ;;
        auth)    tail -f "$LOG_DIR/auth-service.log" ;;
        member)  tail -f "$LOG_DIR/member-service.log" ;;
        gateway) tail -f "$LOG_DIR/gateway.log" ;;
        *)
            err "Usage: $0 logs {nacos|redis|auth|member|gateway}"
            exit 1
            ;;
    esac
}

# ── Start / Stop / Restart ────────────────────────────────────────────────────
cmd_start() {
    local svc="${1:-all}"
    case "$svc" in
        all)
            start_nacos
            start_redis
            start_auth
            start_member
            start_gateway
            cmd_status
            ;;
        nacos)   start_nacos ;;
        redis)   start_redis ;;
        auth)    start_auth ;;
        member)  start_member ;;
        gateway) start_gateway ;;
        *) err "Unknown service: $svc"; exit 1 ;;
    esac
}

cmd_stop() {
    local svc="${1:-all}"
    case "$svc" in
        all)
            stop_gateway
            stop_member
            stop_auth
            stop_redis
            stop_nacos
            cmd_status
            ;;
        nacos)   stop_nacos ;;
        redis)   stop_redis ;;
        auth)    stop_auth ;;
        member)  stop_member ;;
        gateway) stop_gateway ;;
        *) err "Unknown service: $svc"; exit 1 ;;
    esac
}

cmd_restart() {
    local svc="${1:-all}"
    cmd_stop "$svc"
    sleep 2
    cmd_start "$svc"
}

# ── Usage ─────────────────────────────────────────────────────────────────────
usage() {
    echo -e "${BOLD}Zing Platform Manager${NC}"
    echo ""
    echo -e "  ${BOLD}Usage:${NC} $0 <command> [service]"
    echo ""
    echo -e "  ${BOLD}Commands:${NC}"
    echo "    start   [service]   Start all services or a specific one"
    echo "    stop    [service]   Stop all services or a specific one"
    echo "    restart [service]   Restart all or one"
    echo "    status              Show running status of all services"
    echo "    logs    <service>   Tail the log of a service"
    echo ""
    echo -e "  ${BOLD}Services:${NC} nacos | redis | auth | member | gateway | all (default)"
    echo ""
    echo -e "  ${BOLD}Examples:${NC}"
    echo "    $0 start                # start everything"
    echo "    $0 start gateway        # start only gateway"
    echo "    $0 stop auth            # stop auth-service"
    echo "    $0 restart              # restart all"
    echo "    $0 status               # show status"
    echo "    $0 logs auth            # tail auth-service log"
    echo ""
    echo -e "  ${BOLD}Config:${NC}  edit ${CYAN}.env.local${NC} to change ports, secrets, or credentials"
}

# ── Main ──────────────────────────────────────────────────────────────────────
CMD="${1:-help}"
shift || true

case "$CMD" in
    start)   cmd_start   "${1:-all}" ;;
    stop)    cmd_stop    "${1:-all}" ;;
    restart) cmd_restart "${1:-all}" ;;
    status)  cmd_status ;;
    logs)    cmd_logs    "${1:-}" ;;
    help|-h|--help) usage ;;
    *) err "Unknown command: $CMD"; echo ""; usage; exit 1 ;;
esac
