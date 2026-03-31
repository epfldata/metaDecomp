SELECT mi_idx_1.info, t_1.title, cn_2.name, mi_idx_2.info, t_2.title
FROM kind_type AS kt_1,
    title AS t_1,
    keyword AS k_1,
    movie_info_idx AS mi_idx_1,
    info_type AS it2_1,
    movie_info AS mi_1,
    movie_keyword AS mk_1,
    info_type AS it1_1, title AS t_2,
    kind_type AS kt_2,
    company_name AS cn_2,
    keyword AS k_2,
    movie_info_idx AS mi_idx_2,
    movie_keyword AS mk_2,
    info_type AS it2_2,
    movie_info AS mi_2,
    company_type AS ct_2,
    info_type AS it1_2,
    movie_companies AS mc_2
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
  AND t_2.production_year > 2008
  AND it2_2.info = 'rating'
  AND mi_idx_2.info < '7.0'
  AND it1_2.info = 'countries'
  AND kt_2.kind IN ( 'movie' , 'episode' )
  AND k_2.keyword IN ( 'murder' , 'murder-in-title' , 'blood' , 'violence' )
  AND mc_2.note LIKE '%(200%)%'
  AND mc_2.note NOT LIKE '%(USA)%'
  AND mi_2.info IN ( 'Germany' , 'German' , 'USA' , 'American' )
  AND cn_2.country_code != '[us]'
  AND mc_2.company_type_id = ct_2.id
  AND mk_2.movie_id = mc_2.movie_id
  AND mk_2.movie_id = mi_idx_2.movie_id
  AND mk_2.movie_id = mi_2.movie_id
  AND mk_2.movie_id = t_2.id
  AND mc_2.movie_id = mi_idx_2.movie_id
  AND mc_2.movie_id = mi_2.movie_id
  AND mc_2.movie_id = t_2.id
  AND mi_idx_2.movie_id = mi_2.movie_id
  AND mi_idx_2.movie_id = t_2.id
  AND mi_2.movie_id = t_2.id
  AND cn_2.id = mc_2.company_id
  AND t_2.kind_id = kt_2.id
  AND mi_idx_2.info_type_id = it2_2.id
  AND it1_2.id = mi_2.info_type_id
  AND k_2.id = mk_2.keyword_id
  AND mi_idx_1.info = mi_idx_2.info
GROUP BY mi_idx_1.info, t_1.title, cn_2.name, mi_idx_2.info, t_2.title;