SELECT mi_idx_1.info, t_1.title, cn_2.name, mi_idx_2.info, t_2.title
FROM title AS t_1,
    keyword AS k_1,
    movie_info_idx AS mi_idx_1,
    movie_keyword AS mk_1,
    info_type AS it_1, title AS t_2,
    kind_type AS kt_2,
    company_name AS cn_2,
    keyword AS k_2,
    movie_info_idx AS mi_idx_2,
    movie_keyword AS mk_2,
    movie_info AS mi_2,
    comp_cast_type AS cct2_2,
    company_type AS ct_2,
    info_type AS it2_2,
    comp_cast_type AS cct1_2,
    info_type AS it1_2,
    movie_companies AS mc_2,
    complete_cast AS cc_2
WHERE k_1.keyword LIKE '%sequel%'
  AND mi_idx_1.info > '5.0'
  AND t_1.production_year > 2005
  AND it_1.info = 'rating'
  AND t_1.id = mk_1.movie_id
  AND t_1.id = mi_idx_1.movie_id
  AND mk_1.movie_id = mi_idx_1.movie_id
  AND it_1.id = mi_idx_1.info_type_id
  AND k_1.id = mk_1.keyword_id
  AND t_2.production_year > 2000
  AND cct2_2.kind != 'complete+verified'
  AND cct1_2.kind = 'crew'
  AND k_2.keyword IN ( 'murder' , 'murder-in-title' , 'blood' , 'violence' )
  AND kt_2.kind IN ( 'movie' , 'episode' )
  AND it2_2.info = 'rating'
  AND mc_2.note LIKE '%(200%)%'
  AND mc_2.note NOT LIKE '%(USA)%'
  AND mi_2.info IN ( 'Sweden' , 'Norway' , 'Germany' , 'Denmark' , 'Swedish' , 'Danish' , 'Norwegian' , 'German' , 'USA' , 'American' )
  AND cn_2.country_code != '[us]'
  AND mi_idx_2.info < '8.5'
  AND it1_2.info = 'countries'
  AND mc_2.company_type_id = ct_2.id
  AND mk_2.movie_id = mc_2.movie_id
  AND mk_2.movie_id = cc_2.movie_id
  AND mk_2.movie_id = t_2.id
  AND mk_2.movie_id = mi_2.movie_id
  AND mk_2.movie_id = mi_idx_2.movie_id
  AND mc_2.movie_id = cc_2.movie_id
  AND mc_2.movie_id = t_2.id
  AND mc_2.movie_id = mi_2.movie_id
  AND mc_2.movie_id = mi_idx_2.movie_id
  AND cc_2.movie_id = t_2.id
  AND cc_2.movie_id = mi_2.movie_id
  AND cc_2.movie_id = mi_idx_2.movie_id
  AND t_2.id = mi_2.movie_id
  AND t_2.id = mi_idx_2.movie_id
  AND mi_2.movie_id = mi_idx_2.movie_id
  AND t_2.kind_id = kt_2.id
  AND cct2_2.id = cc_2.status_id
  AND cc_2.subject_id = cct1_2.id
  AND it1_2.id = mi_2.info_type_id
  AND it2_2.id = mi_idx_2.info_type_id
  AND k_2.id = mk_2.keyword_id
  AND cn_2.id = mc_2.company_id
  AND mi_idx_1.info = mi_idx_2.info
GROUP BY mi_idx_1.info, t_1.title, cn_2.name, mi_idx_2.info, t_2.title;