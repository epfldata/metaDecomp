SELECT mi_1.info, miidx_1.info, t_1.title, mi_2.info, mi_idx_2.info, n_2.name, t_2.title
FROM movie_companies AS mc_1,
    company_type AS ct_1,
    kind_type AS kt_1,
    title AS t_1,
    info_type AS it2_1,
    movie_info AS mi_1,
    info_type AS it_1,
    movie_info_idx AS miidx_1,
    company_name AS cn_1, title AS t_2,
    movie_info_idx AS mi_idx_2,
    movie_keyword AS mk_2,
    info_type AS it2_2,
    movie_info AS mi_2,
    name AS n_2,
    info_type AS it1_2,
    keyword AS k_2,
    cast_info AS ci_2
WHERE kt_1.kind = 'movie'
  AND it_1.info = 'rating'
  AND ct_1.kind = 'production companies'
  AND it2_1.info = 'release dates'
  AND cn_1.country_code = '[de]'
  AND mc_1.company_type_id = ct_1.id
  AND cn_1.id = mc_1.company_id
  AND it_1.id = miidx_1.info_type_id
  AND mi_1.info_type_id = it2_1.id
  AND kt_1.id = t_1.kind_id
  AND mi_1.movie_id = t_1.id
  AND mi_1.movie_id = miidx_1.movie_id
  AND mi_1.movie_id = mc_1.movie_id
  AND t_1.id = miidx_1.movie_id
  AND t_1.id = mc_1.movie_id
  AND miidx_1.movie_id = mc_1.movie_id
  AND k_2.keyword IN ( 'murder' , 'blood' , 'gore' , 'death' , 'female-nudity' )
  AND it1_2.info = 'genres'
  AND mi_2.info = 'Horror'
  AND it2_2.info = 'votes'
  AND n_2.gender = 'm'
  AND ci_2.note IN ( '(writer)' , '(head writer)' , '(written by)' , '(story)' , '(story editor)' )
  AND mi_idx_2.info_type_id = it2_2.id
  AND mi_idx_2.movie_id = mi_2.movie_id
  AND mi_idx_2.movie_id = mk_2.movie_id
  AND mi_idx_2.movie_id = ci_2.movie_id
  AND mi_idx_2.movie_id = t_2.id
  AND mi_2.movie_id = mk_2.movie_id
  AND mi_2.movie_id = ci_2.movie_id
  AND mi_2.movie_id = t_2.id
  AND mk_2.movie_id = ci_2.movie_id
  AND mk_2.movie_id = t_2.id
  AND ci_2.movie_id = t_2.id
  AND n_2.id = ci_2.person_id
  AND k_2.id = mk_2.keyword_id
  AND mi_2.info_type_id = it1_2.id
  AND mi_1.info = mi_2.info
GROUP BY mi_1.info, miidx_1.info, t_1.title, mi_2.info, mi_idx_2.info, n_2.name, t_2.title;