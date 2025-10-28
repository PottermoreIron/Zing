local key = KEYS[1]
local tokensKey = KEYS[2]
local lastRefillKey = KEYS[3]

local rate = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])
local ttl = tonumber(ARGV[5])

local lastRefill = tonumber(redis.call('get', lastRefillKey) or '0')
local delta = math.max(0, now - lastRefill)
local filledTokens = math.min(capacity, tonumber(redis.call('get', tokensKey) or '0') + (delta * rate))

if filledTokens >= requested then
    redis.call('setex', tokensKey, ttl, filledTokens - requested)
    redis.call('setex', lastRefillKey, ttl, now)
    return 1
else
    redis.call('setex', tokensKey, ttl, filledTokens)
    redis.call('setex', lastRefillKey, ttl, now)
    return 0
end
