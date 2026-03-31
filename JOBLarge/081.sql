SELECT n_1.name, chn_2.name, mi_idx_2.info, n_2.name, t_2.title
FROM movie_companies AS mc_1,
    name AS n_1,
    title AS t_1,
    keyword AS k_1,
    company_name AS cn_1,
    cast_info AS ci_1,
    movie_keyword AS mk_1, title AS t_2,
    kind_type AS kt_2,
    movie_keyword AS mk_2,
    name AS n_2,
    complete_cast AS cc_2,
    comp_cast_type AS cct2_2,
    keyword AS k_2,
    cast_info AS ci_2,
    movie_info_idx AS mi_idx_2,
    char_name AS chn_2,
    info_type AS it2_2,
    comp_cast_type AS cct1_2
WHERE k_1.keyword = 'character-name-in-title'
  AND n_1.name LIKE 'B%'
  AND cn_1.country_code = '[us]'
  AND cn_1.id = mc_1.company_id
  AND n_1.id = ci_1.person_id
  AND mk_1.movie_id = mc_1.movie_id
  AND mk_1.movie_id = t_1.id
  AND mk_1.movie_id = ci_1.movie_id
  AND mc_1.movie_id = t_1.id
  AND mc_1.movie_id = ci_1.movie_id
  AND t_1.id = ci_1.movie_id
  AND k_1.id = mk_1.keyword_id
  AND cct1_2.kind = 'cast'
  AND it2_2.info = 'rating'
  AND cct2_2.kind LIKE '%complete%'
  AND k_2.keyword IN ( 'superhero' , 'marvel-comics' , 'based-on-comic' , 'tv-special' , 'fight' , 'violence' , 'magnet' , 'web' , 'claw' , 'laser' )
  AND ( chn_2.name LIKE '%man%' OR chn_2.name LIKE '%Man%' )
  AND chn_2.name IS NOT NULL
  AND t_2.production_year > 2000
  AND kt_2.kind = 'movie'
  AND mi_idx_2.info > '7.0'
  AND kt_2.id = t_2.kind_id
  AND n_2.id = ci_2.person_id
  AND k_2.id = mk_2.keyword_id
  AND cct1_2.id = cc_2.subject_id
  AND cct2_2.id = cc_2.status_id
  AND mi_idx_2.info_type_id = it2_2.id
  AND t_2.id = cc_2.movie_id
  AND t_2.id = mk_2.movie_id
  AND t_2.id = ci_2.movie_id
  AND t_2.id = mi_idx_2.movie_id
  AND cc_2.movie_id = mk_2.movie_id
  AND cc_2.movie_id = ci_2.movie_id
  AND cc_2.movie_id = mi_idx_2.movie_id
  AND mk_2.movie_id = ci_2.movie_id
  AND mk_2.movie_id = mi_idx_2.movie_id
  AND ci_2.movie_id = mi_idx_2.movie_id
  AND chn_2.id = ci_2.person_role_id
  AND n_1.name = n_2.name
GROUP BY n_1.name, chn_2.name, mi_idx_2.info, n_2.name, t_2.title;