SELECT mi_1.info, mi_idx_1.info, n_1.name, t_1.title, mi_2.info, mi_idx_2.info, n_2.name, t_2.title
FROM name AS n_1,
    title AS t_1,
    keyword AS k_1,
    movie_info_idx AS mi_idx_1,
    info_type AS it2_1,
    cast_info AS ci_1,
    movie_info AS mi_1,
    movie_keyword AS mk_1,
    info_type AS it1_1, title AS t_2,
    movie_info_idx AS mi_idx_2,
    movie_keyword AS mk_2,
    movie_info AS mi_2,
    name AS n_2,
    comp_cast_type AS cct2_2,
    keyword AS k_2,
    cast_info AS ci_2,
    info_type AS it2_2,
    comp_cast_type AS cct1_2,
    info_type AS it1_2,
    complete_cast AS cc_2
WHERE k_1.keyword IN ( 'murder' , 'blood' , 'gore' , 'death' , 'female-nudity' )
  AND it1_1.info = 'genres'
  AND mi_1.info = 'Horror'
  AND it2_1.info = 'votes'
  AND n_1.gender = 'm'
  AND ci_1.note IN ( '(writer)' , '(head writer)' , '(written by)' , '(story)' , '(story editor)' )
  AND mi_idx_1.info_type_id = it2_1.id
  AND mi_idx_1.movie_id = mi_1.movie_id
  AND mi_idx_1.movie_id = mk_1.movie_id
  AND mi_idx_1.movie_id = ci_1.movie_id
  AND mi_idx_1.movie_id = t_1.id
  AND mi_1.movie_id = mk_1.movie_id
  AND mi_1.movie_id = ci_1.movie_id
  AND mi_1.movie_id = t_1.id
  AND mk_1.movie_id = ci_1.movie_id
  AND mk_1.movie_id = t_1.id
  AND ci_1.movie_id = t_1.id
  AND n_1.id = ci_1.person_id
  AND k_1.id = mk_1.keyword_id
  AND mi_1.info_type_id = it1_1.id
  AND cct1_2.kind IN ( 'cast' , 'crew' )
  AND mi_2.info IN ( 'Horror' , 'Thriller' )
  AND k_2.keyword IN ( 'murder' , 'violence' , 'blood' , 'gore' , 'death' , 'female-nudity' , 'hospital' )
  AND it1_2.info = 'genres'
  AND cct2_2.kind = 'complete+verified'
  AND t_2.production_year > 2000
  AND n_2.gender = 'm'
  AND it2_2.info = 'votes'
  AND ci_2.note IN ( '(writer)' , '(head writer)' , '(written by)' , '(story)' , '(story editor)' )
  AND cct2_2.id = cc_2.status_id
  AND cc_2.subject_id = cct1_2.id
  AND k_2.id = mk_2.keyword_id
  AND mi_idx_2.info_type_id = it2_2.id
  AND ci_2.movie_id = cc_2.movie_id
  AND ci_2.movie_id = mi_2.movie_id
  AND ci_2.movie_id = mk_2.movie_id
  AND ci_2.movie_id = t_2.id
  AND ci_2.movie_id = mi_idx_2.movie_id
  AND cc_2.movie_id = mi_2.movie_id
  AND cc_2.movie_id = mk_2.movie_id
  AND cc_2.movie_id = t_2.id
  AND cc_2.movie_id = mi_idx_2.movie_id
  AND mi_2.movie_id = mk_2.movie_id
  AND mi_2.movie_id = t_2.id
  AND mi_2.movie_id = mi_idx_2.movie_id
  AND mk_2.movie_id = t_2.id
  AND mk_2.movie_id = mi_idx_2.movie_id
  AND t_2.id = mi_idx_2.movie_id
  AND mi_2.info_type_id = it1_2.id
  AND n_2.id = ci_2.person_id
  AND n_1.name = n_2.name
GROUP BY mi_1.info, mi_idx_1.info, n_1.name, t_1.title, mi_2.info, mi_idx_2.info, n_2.name, t_2.title;