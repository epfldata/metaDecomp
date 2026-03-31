SELECT chn_1.name, t_1.title, cn_2.name, mi_idx_2.info, t_2.title
FROM movie_companies AS mc_1,
    char_name AS chn_1,
    role_type AS rt_1,
    company_type AS ct_1,
    title AS t_1,
    company_name AS cn_1,
    cast_info AS ci_1, title AS t_2,
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
WHERE ci_1.note LIKE '%(voice)%'
  AND ci_1.note LIKE '%(uncredited)%'
  AND t_1.production_year > 2005
  AND rt_1.role = 'actor'
  AND cn_1.country_code = '[ru]'
  AND ct_1.id = mc_1.company_type_id
  AND ci_1.role_id = rt_1.id
  AND mc_1.company_id = cn_1.id
  AND chn_1.id = ci_1.person_role_id
  AND t_1.id = mc_1.movie_id
  AND t_1.id = ci_1.movie_id
  AND mc_1.movie_id = ci_1.movie_id
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
  AND t_1.title = t_2.title
GROUP BY chn_1.name, t_1.title, cn_2.name, mi_idx_2.info, t_2.title;