-- 餐饮门店优惠券秒杀 Lua 脚本
--
-- KEYS[1] = coupon:activity:{activityId}:stock    活动库存
-- KEYS[2] = coupon:activity:{activityId}:users    已领取用户集合
-- KEYS[3] = coupon:activity:{activityId}:soldout  售罄标记
-- ARGV[1] = userId
--
-- 返回值：
-- 0 = 成功，已完成 Redis 预扣库存和用户领取标记
-- 1 = 库存不足 / 已售罄
-- 2 = 用户重复领取
-- 3 = 活动未预热到 Redis

local stockKey = KEYS[1]
local userSetKey = KEYS[2]
local soldOutKey = KEYS[3]
local userId = ARGV[1]

local function setWithKeepTtl(key, value)
    local ttl = redis.call('PTTL', key)
    if ttl ~= nil and ttl > 0 then
        redis.call('PSETEX', key, ttl, value)
    else
        redis.call('SET', key, value)
    end
end

local stockValue = redis.call('GET', stockKey)
if stockValue == false or stockValue == nil then
    return 3
end

local soldOut = redis.call('GET', soldOutKey)
if soldOut == 'true' then
    return 1
end

local exists = redis.call('SISMEMBER', userSetKey, userId)
if exists == 1 then
    return 2
end

local stock = tonumber(stockValue)
if stock == nil or stock <= 0 then
    setWithKeepTtl(soldOutKey, 'true')
    return 1
end

local newStock = redis.call('DECR', stockKey)
redis.call('SADD', userSetKey, userId)

if newStock <= 0 then
    setWithKeepTtl(soldOutKey, 'true')
end

return 0
