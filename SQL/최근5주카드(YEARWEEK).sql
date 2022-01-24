
-- 최근 5주 카드
SELECT
    DATE_FORMAT(
      DATE_ADD(coalesce(ch.consumption_dtime,cah.approved_dtime), INTERVAL (WEEKDAY(coalesce(ch.consumption_dtime,cah.approved_dtime))) * -1 DAY),
      '%Y%m%d'
    ) as start,
    DATE_FORMAT(
      DATE_ADD(coalesce(ch.consumption_dtime,cah.approved_dtime), INTERVAL (6 - WEEKDAY(coalesce(ch.consumption_dtime,cah.approved_dtime))) * +1 DAY),
      '%Y%m%d'
    ) as end
    ,
  sum(coalesce(ch.consumption_amt,cah.approved_amt)) as sum
FROM card c left join card_approval_history cah on c.id = cah.card_id left join consumption_history ch on cah.id = ch.card_approval_history_id
where DATE_FORMAT(coalesce(ch.consumption_dtime,cah.approved_dtime),'%Y%m%d') between '20211004' and '20211101'
and c.mydata_user_id = 7
GROUP BY YEARWEEK(ADDDATE(coalesce(ch.consumption_dtime,cah.approved_dtime),-1),'%Y%m%d')
ORDER BY coalesce(ch.consumption_dtime,cah.approved_dtime) desc

-- 최근 5주 현금
SELECT
    DATE_FORMAT(
      DATE_ADD(consumption_dtime , INTERVAL (WEEKDAY(consumption_dtime)) * -1 DAY),
      '%Y%m%d'
    ) as start,
    DATE_FORMAT(
      DATE_ADD(consumption_dtime, INTERVAL (6 - WEEKDAY(consumption_dtime)) * +1 DAY),
      '%Y%m%d'
    ) as end
    , sum(consumption_amt) as sum
FROM consumption_history
where DATE_FORMAT(consumption_dtime,'%Y%m%d') between '20211004' and '20211101'
and mydata_user_id = 7
and card_approval_history_id is null
and deposit_account_transactions_id is null
GROUP BY YEARWEEK(ADDDATE(consumption_dtime,-1),'%Y%m%d')
ORDER BY consumption_dtime desc


