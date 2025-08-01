SELECT mi_1.info, miidx_1.info, t_1.title, mi_2.info, t_2.title
FROM movie_companies AS mc_1,
    company_type AS ct_1,
    kind_type AS kt_1,
    title AS t_1,
    info_type AS it2_1,
    movie_info AS mi_1,
    info_type AS it_1,
    movie_info_idx AS miidx_1,
    company_name AS cn_1, title AS t_2,
    company_name AS cn_2,
    keyword AS k_2,
    movie_keyword AS mk_2,
    movie_info AS mi_2,
    aka_title AS at_2,
    company_type AS ct_2,
    info_type AS it1_2,
    movie_companies AS mc_2
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
  AND cn_2.country_code = '[us]'
  AND t_2.production_year > 2000
  AND it1_2.info = 'release dates'
  AND mi_2.info LIKE 'USA:% 200%'
  AND mi_2.note LIKE '%internet%'
  AND mc_2.note LIKE '%(200%)%'
  AND mc_2.note LIKE '%(worldwide)%'
  AND mk_2.movie_id = mi_2.movie_id
  AND mk_2.movie_id = t_2.id
  AND mk_2.movie_id = at_2.movie_id
  AND mk_2.movie_id = mc_2.movie_id
  AND mi_2.movie_id = t_2.id
  AND mi_2.movie_id = at_2.movie_id
  AND mi_2.movie_id = mc_2.movie_id
  AND t_2.id = at_2.movie_id
  AND t_2.id = mc_2.movie_id
  AND at_2.movie_id = mc_2.movie_id
  AND mk_2.keyword_id = k_2.id
  AND mi_2.info_type_id = it1_2.id
  AND mc_2.company_type_id = ct_2.id
  AND mc_2.company_id = cn_2.id
  AND mi_1.info = mi_2.info
GROUP BY mi_1.info, miidx_1.info, t_1.title, mi_2.info, t_2.title;