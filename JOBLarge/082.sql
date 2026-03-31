SELECT chn_1.name AS character_1,
       t_1.title AS russian_mov_with_actor_producer_1,
       chn_2.name AS character_2,
       t_2.title AS russian_mov_with_actor_producer_2
FROM cast_info AS ci_1,
     cast_info AS ci_2,
     char_name AS chn_1,
     char_name AS chn_2,
     company_name AS cn_1,
     company_name AS cn_2,
     company_type AS ct_1,
     company_type AS ct_2,
     movie_companies AS mc_1,
     movie_companies AS mc_2,
     role_type AS rt_1,
     role_type AS rt_2,
     title AS t_1,
     title AS t_2
WHERE ci_1.note LIKE '%(producer)%'
  AND cn_1.country_code = '[ru]'
  AND rt_1.role = 'actor'
  AND t_1.production_year > 2010
  AND t_1.id = mc_1.movie_id
  AND t_1.id = ci_1.movie_id
  AND ci_1.movie_id = mc_1.movie_id
  AND chn_1.id = ci_1.person_role_id
  AND rt_1.id = ci_1.role_id
  AND cn_1.id = mc_1.company_id
  AND ct_1.id = mc_1.company_type_id
  AND ci_2.note LIKE '%(producer)%'
  AND cn_2.country_code = '[ru]'
  AND rt_2.role = 'actor'
  AND t_2.production_year > 2010
  AND t_2.id = mc_2.movie_id
  AND t_2.id = ci_2.movie_id
  AND ci_2.movie_id = mc_2.movie_id
  AND chn_2.id = ci_2.person_role_id
  AND rt_2.id = ci_2.role_id
  AND cn_2.id = mc_2.company_id
  AND ct_2.id = mc_2.company_type_id
  AND chn_1.name = chn_2.name
GROUP BY chn_1.name,
         chn_2.name,
         t_1.title,
         t_2.title;