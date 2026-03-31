SELECT mi_idx_1.info, t_1.title, chn_2.name, mi_idx_2.info, n_2.name, t_2.title
FROM kind_type AS kt_1,
    title AS t_1,
    keyword AS k_1,
    movie_info_idx AS mi_idx_1,
    info_type AS it2_1,
    movie_info AS mi_1,
    movie_keyword AS mk_1,
    info_type AS it1_1, title AS t_2,
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
WHERE it1_1.info = 'countries'
  AND t_1.production_year > 2010
  AND it2_1.info = 'rating'
  AND kt_1.kind = 'movie'
  AND k_1.keyword IN ( 'murder' , 'murder-in-title' , 'blood' , 'violence' )
  AND mi_idx_1.info < '8.5'
  AND mi_1.info IN ( 'Sweden' , 'Norway' , 'Germany' , 'Denmark' , 'Swedish' , 'Denish' , 'Norwegian' , 'German' , 'USA' , 'American' )
  AND mi_idx_1.info_type_id = it2_1.id
  AND mk_1.keyword_id = k_1.id
  AND t_1.kind_id = kt_1.id
  AND mi_1.movie_id = mi_idx_1.movie_id
  AND mi_1.movie_id = mk_1.movie_id
  AND mi_1.movie_id = t_1.id
  AND mi_idx_1.movie_id = mk_1.movie_id
  AND mi_idx_1.movie_id = t_1.id
  AND mk_1.movie_id = t_1.id
  AND it1_1.id = mi_1.info_type_id
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
  AND mi_idx_1.info = mi_idx_2.info
GROUP BY mi_idx_1.info, t_1.title, chn_2.name, mi_idx_2.info, n_2.name, t_2.title;