SELECT cn_1.name, mi_idx_1.info, t_1.title, t2_2.title, cn2_2.name, cn1_2.name, mi_idx2_2.info, t1_2.title, mi_idx1_2.info
FROM movie_companies AS mc_1,
    complete_cast AS cc_1,
    company_type AS ct_1,
    kind_type AS kt_1,
    title AS t_1,
    keyword AS k_1,
    company_name AS cn_1,
    comp_cast_type AS cct1_1,
    info_type AS it2_1,
    movie_info AS mi_1,
    movie_keyword AS mk_1,
    info_type AS it1_1,
    movie_info_idx AS mi_idx_1,
    comp_cast_type AS cct2_1, title AS t1_2,
    company_name AS cn1_2,
    movie_info_idx AS mi_idx1_2,
    movie_link AS ml_2,
    movie_companies AS mc2_2,
    title AS t2_2,
    info_type AS it2_2,
    kind_type AS kt2_2,
    company_name AS cn2_2,
    movie_companies AS mc1_2,
    movie_info_idx AS mi_idx2_2,
    kind_type AS kt1_2,
    info_type AS it1_2,
    link_type AS lt_2
WHERE t_1.production_year > 2000
  AND cct2_1.kind != 'complete+verified'
  AND cct1_1.kind = 'crew'
  AND k_1.keyword IN ( 'murder' , 'murder-in-title' , 'blood' , 'violence' )
  AND kt_1.kind IN ( 'movie' , 'episode' )
  AND it2_1.info = 'rating'
  AND mc_1.note LIKE '%(200%)%'
  AND mc_1.note NOT LIKE '%(USA)%'
  AND mi_1.info IN ( 'Sweden' , 'Norway' , 'Germany' , 'Denmark' , 'Swedish' , 'Danish' , 'Norwegian' , 'German' , 'USA' , 'American' )
  AND cn_1.country_code != '[us]'
  AND mi_idx_1.info < '8.5'
  AND it1_1.info = 'countries'
  AND mc_1.company_type_id = ct_1.id
  AND mk_1.movie_id = mc_1.movie_id
  AND mk_1.movie_id = cc_1.movie_id
  AND mk_1.movie_id = t_1.id
  AND mk_1.movie_id = mi_1.movie_id
  AND mk_1.movie_id = mi_idx_1.movie_id
  AND mc_1.movie_id = cc_1.movie_id
  AND mc_1.movie_id = t_1.id
  AND mc_1.movie_id = mi_1.movie_id
  AND mc_1.movie_id = mi_idx_1.movie_id
  AND cc_1.movie_id = t_1.id
  AND cc_1.movie_id = mi_1.movie_id
  AND cc_1.movie_id = mi_idx_1.movie_id
  AND t_1.id = mi_1.movie_id
  AND t_1.id = mi_idx_1.movie_id
  AND mi_1.movie_id = mi_idx_1.movie_id
  AND t_1.kind_id = kt_1.id
  AND cct2_1.id = cc_1.status_id
  AND cc_1.subject_id = cct1_1.id
  AND it1_1.id = mi_1.info_type_id
  AND it2_1.id = mi_idx_1.info_type_id
  AND k_1.id = mk_1.keyword_id
  AND cn_1.id = mc_1.company_id
  AND kt1_2.kind IN ( 'tv series' )
  AND it1_2.info = 'rating'
  AND t2_2.production_year BETWEEN 2005 AND 2008
  AND mi_idx2_2.info < '3.0'
  AND it2_2.info = 'rating'
  AND cn1_2.country_code = '[us]'
  AND lt_2.link IN ( 'sequel' , 'follows' , 'followed by' )
  AND kt2_2.kind IN ( 'tv series' )
  AND t2_2.kind_id = kt2_2.id
  AND t1_2.kind_id = kt1_2.id
  AND mc1_2.movie_id = mi_idx1_2.movie_id
  AND mc1_2.movie_id = t1_2.id
  AND mc1_2.movie_id = ml_2.movie_id
  AND mi_idx1_2.movie_id = t1_2.id
  AND mi_idx1_2.movie_id = ml_2.movie_id
  AND t1_2.id = ml_2.movie_id
  AND cn2_2.id = mc2_2.company_id
  AND mi_idx2_2.info_type_id = it2_2.id
  AND mi_idx2_2.movie_id = mc2_2.movie_id
  AND mi_idx2_2.movie_id = t2_2.id
  AND mi_idx2_2.movie_id = ml_2.linked_movie_id
  AND mc2_2.movie_id = t2_2.id
  AND mc2_2.movie_id = ml_2.linked_movie_id
  AND t2_2.id = ml_2.linked_movie_id
  AND cn1_2.id = mc1_2.company_id
  AND lt_2.id = ml_2.link_type_id
  AND mi_idx1_2.info_type_id = it1_2.id
  AND cn_1.name = cn2_2.name
GROUP BY cn_1.name, mi_idx_1.info, t_1.title, t2_2.title, cn2_2.name, cn1_2.name, mi_idx2_2.info, t1_2.title, mi_idx1_2.info;