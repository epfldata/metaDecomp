SELECT mi_1.info, mi_idx_1.info, n_1.name, t_1.title, lt_2.link, t1_2.title, t2_2.title
FROM complete_cast AS cc_1,
    name AS n_1,
    title AS t_1,
    keyword AS k_1,
    comp_cast_type AS cct1_1,
    info_type AS it2_1,
    cast_info AS ci_1,
    movie_info AS mi_1,
    movie_keyword AS mk_1,
    info_type AS it1_1,
    movie_info_idx AS mi_idx_1,
    comp_cast_type AS cct2_1, title AS t1_2,
    movie_link AS ml_2,
    keyword AS k_2,
    movie_keyword AS mk_2,
    title AS t2_2,
    link_type AS lt_2
WHERE cct1_1.kind IN ( 'cast' , 'crew' )
  AND mi_1.info IN ( 'Horror' , 'Thriller' )
  AND k_1.keyword IN ( 'murder' , 'violence' , 'blood' , 'gore' , 'death' , 'female-nudity' , 'hospital' )
  AND it1_1.info = 'genres'
  AND cct2_1.kind = 'complete+verified'
  AND t_1.production_year > 2000
  AND n_1.gender = 'm'
  AND it2_1.info = 'votes'
  AND ci_1.note IN ( '(writer)' , '(head writer)' , '(written by)' , '(story)' , '(story editor)' )
  AND cct2_1.id = cc_1.status_id
  AND cc_1.subject_id = cct1_1.id
  AND k_1.id = mk_1.keyword_id
  AND mi_idx_1.info_type_id = it2_1.id
  AND ci_1.movie_id = cc_1.movie_id
  AND ci_1.movie_id = mi_1.movie_id
  AND ci_1.movie_id = mk_1.movie_id
  AND ci_1.movie_id = t_1.id
  AND ci_1.movie_id = mi_idx_1.movie_id
  AND cc_1.movie_id = mi_1.movie_id
  AND cc_1.movie_id = mk_1.movie_id
  AND cc_1.movie_id = t_1.id
  AND cc_1.movie_id = mi_idx_1.movie_id
  AND mi_1.movie_id = mk_1.movie_id
  AND mi_1.movie_id = t_1.id
  AND mi_1.movie_id = mi_idx_1.movie_id
  AND mk_1.movie_id = t_1.id
  AND mk_1.movie_id = mi_idx_1.movie_id
  AND t_1.id = mi_idx_1.movie_id
  AND mi_1.info_type_id = it1_1.id
  AND n_1.id = ci_1.person_id
  AND k_2.keyword = '10,000-mile-club'
  AND lt_2.id = ml_2.link_type_id
  AND k_2.id = mk_2.keyword_id
  AND t1_2.id = ml_2.movie_id
  AND t1_2.id = mk_2.movie_id
  AND ml_2.movie_id = mk_2.movie_id
  AND t2_2.id = ml_2.linked_movie_id
  AND t_1.title = t1_2.title
GROUP BY mi_1.info, mi_idx_1.info, n_1.name, t_1.title, lt_2.link, t1_2.title, t2_2.title;