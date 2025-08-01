SELECT cn_1.name, lt_1.link, t_1.title, cn_2.name, lt_2.link, t_2.title
FROM movie_companies AS mc_1,
    company_type AS ct_1,
    title AS t_1,
    keyword AS k_1,
    movie_link AS ml_1,
    company_name AS cn_1,
    movie_keyword AS mk_1,
    link_type AS lt_1, title AS t_2,
    company_name AS cn_2,
    movie_link AS ml_2,
    keyword AS k_2,
    movie_keyword AS mk_2,
    comp_cast_type AS cct1_2,
    movie_info AS mi_2,
    comp_cast_type AS cct2_2,
    company_type AS ct_2,
    movie_companies AS mc_2,
    complete_cast AS cc_2,
    link_type AS lt_2
WHERE mc_1.note IS NULL
  AND lt_1.link LIKE '%follow%'
  AND ct_1.kind = 'production companies'
  AND cn_1.country_code != '[pl]'
  AND ( cn_1.name LIKE '%Film%' OR cn_1.name LIKE '%Warner%' )
  AND k_1.keyword = 'sequel'
  AND t_1.production_year BETWEEN 1950 AND 2000
  AND mk_1.keyword_id = k_1.id
  AND cn_1.id = mc_1.company_id
  AND mc_1.company_type_id = ct_1.id
  AND mc_1.movie_id = ml_1.movie_id
  AND mc_1.movie_id = mk_1.movie_id
  AND mc_1.movie_id = t_1.id
  AND ml_1.movie_id = mk_1.movie_id
  AND ml_1.movie_id = t_1.id
  AND mk_1.movie_id = t_1.id
  AND lt_1.id = ml_1.link_type_id
  AND lt_2.link LIKE '%follow%'
  AND t_2.production_year BETWEEN 1950 AND 2000
  AND k_2.keyword = 'sequel'
  AND ct_2.kind = 'production companies'
  AND mc_2.note IS NULL
  AND cct2_2.kind = 'complete'
  AND cct1_2.kind IN ( 'cast' , 'crew' )
  AND mi_2.info IN ( 'Sweden' , 'Germany' , 'Swedish' , 'German' )
  AND cn_2.country_code != '[pl]'
  AND ( cn_2.name LIKE '%Film%' OR cn_2.name LIKE '%Warner%' )
  AND cc_2.status_id = cct2_2.id
  AND cc_2.subject_id = cct1_2.id
  AND k_2.id = mk_2.keyword_id
  AND ct_2.id = mc_2.company_type_id
  AND mi_2.movie_id = cc_2.movie_id
  AND mi_2.movie_id = mc_2.movie_id
  AND mi_2.movie_id = t_2.id
  AND mi_2.movie_id = mk_2.movie_id
  AND mi_2.movie_id = ml_2.movie_id
  AND cc_2.movie_id = mc_2.movie_id
  AND cc_2.movie_id = t_2.id
  AND cc_2.movie_id = mk_2.movie_id
  AND cc_2.movie_id = ml_2.movie_id
  AND mc_2.movie_id = t_2.id
  AND mc_2.movie_id = mk_2.movie_id
  AND mc_2.movie_id = ml_2.movie_id
  AND t_2.id = mk_2.movie_id
  AND t_2.id = ml_2.movie_id
  AND mk_2.movie_id = ml_2.movie_id
  AND mc_2.company_id = cn_2.id
  AND ml_2.link_type_id = lt_2.id
  AND cn_1.name = cn_2.name
GROUP BY cn_1.name, lt_1.link, t_1.title, cn_2.name, lt_2.link, t_2.title;