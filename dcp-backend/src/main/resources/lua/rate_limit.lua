-- 限流的 Redis Key (例如: rate_limit:RecordController:apply:test01)
local key = KEYS[1]
-- 允许的最大访问次数
local limit = tonumber(ARGV[1])
-- 限流的时间窗口（秒）
local window = tonumber(ARGV[2])

-- 获取当前访问次数
local current = redis.call('get', key)

if current and tonumber(current) >= limit then
    -- 超过限制，返回 0 表示拦截
    return 0
end

-- 没超过限制，次数 +1
current = redis.call('incr', key)

if tonumber(current) == 1 then
    -- 如果是第一次访问，设置过期时间
    redis.call('expire', key, window)
end

-- 返回 1 表示放行
return 1