SELECT chn_1.name, mi_idx_1.info, n_1.name, t_1.title, lt_2.link, t1_2.title, t2_2.title
FROM char_name AS chn_1,
    complete_cast AS cc_1,
    name AS n_1,
    kind_type AS kt_1,
    title AS t_1,
    keyword AS k_1,
    comp_cast_type AS cct1_1,
    info_type AS it2_1,
    cast_info AS ci_1,
    movie_keyword AS mk_1,
    movie_info_idx AS mi_idx_1,
    comp_cast_type AS cct2_1, title AS t1_2,
    movie_link AS ml_2,
    keyword AS k_2,
    movie_keyword AS mk_2,
    title AS t2_2,
    link_type AS lt_2
WHERE cct1_1.kind = 'cast'
  AND it2_1.info = 'rating'
  AND cct2_1.kind LIKE '%complete%'
  AND k_1.keyword IN ( 'superhero' , 'marvel-comics' , 'based-on-comic' , 'tv-special' , 'fight' , 'violence' , 'magnet' , 'web' , 'claw' , 'laser' )
  AND ( chn_1.name LIKE '%man%' OR chn_1.name LIKE '%Man%' )
  AND chn_1.name IS NOT NULL
  AND t_1.production_year > 2000
  AND kt_1.kind = 'movie'
  AND mi_idx_1.info > '7.0'
  AND kt_1.id = t_1.kind_id
  AND n_1.id = ci_1.person_id
  AND k_1.id = mk_1.keyword_id
  AND cct1_1.id = cc_1.subject_id
  AND cct2_1.id = cc_1.status_id
  AND mi_idx_1.info_type_id = it2_1.id
  AND t_1.id = cc_1.movie_id
  AND t_1.id = mk_1.movie_id
  AND t_1.id = ci_1.movie_id
  AND t_1.id = mi_idx_1.movie_id
  AND cc_1.movie_id = mk_1.movie_id
  AND cc_1.movie_id = ci_1.movie_id
  AND cc_1.movie_id = mi_idx_1.movie_id
  AND mk_1.movie_id = ci_1.movie_id
  AND mk_1.movie_id = mi_idx_1.movie_id
  AND ci_1.movie_id = mi_idx_1.movie_id
  AND chn_1.id = ci_1.person_role_id
  AND k_2.keyword = '10,000-mile-club'
  AND lt_2.id = ml_2.link_type_id
  AND k_2.id = mk_2.keyword_id
  AND t1_2.id = ml_2.movie_id
  AND t1_2.id = mk_2.movie_id
  AND ml_2.movie_id = mk_2.movie_id
  AND t2_2.id = ml_2.linked_movie_id
  AND t_1.title = t1_2.title
GROUP BY chn_1.name, mi_idx_1.info, n_1.name, t_1.title, lt_2.link, t1_2.title, t2_2.title;