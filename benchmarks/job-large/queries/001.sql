SELECT cn_1.name, lt_1.link, t_1.title, lt_2.link, t1_2.title, t2_2.title
FROM movie_companies AS mc_1,
    complete_cast AS cc_1,
    company_type AS ct_1,
    title AS t_1,
    keyword AS k_1,
    comp_cast_type AS cct2_1,
    movie_link AS ml_1,
    company_name AS cn_1,
    comp_cast_type AS cct1_1,
    movie_info AS mi_1,
    movie_keyword AS mk_1,
    link_type AS lt_1, title AS t1_2,
    movie_link AS ml_2,
    keyword AS k_2,
    movie_keyword AS mk_2,
    title AS t2_2,
    link_type AS lt_2
WHERE lt_1.link LIKE '%follow%'
  AND t_1.production_year BETWEEN 1950 AND 2000
  AND k_1.keyword = 'sequel'
  AND ct_1.kind = 'production companies'
  AND mc_1.note IS NULL
  AND cct2_1.kind = 'complete'
  AND cct1_1.kind IN ( 'cast' , 'crew' )
  AND mi_1.info IN ( 'Sweden' , 'Germany' , 'Swedish' , 'German' )
  AND cn_1.country_code != '[pl]'
  AND ( cn_1.name LIKE '%Film%' OR cn_1.name LIKE '%Warner%' )
  AND cc_1.status_id = cct2_1.id
  AND cc_1.subject_id = cct1_1.id
  AND k_1.id = mk_1.keyword_id
  AND ct_1.id = mc_1.company_type_id
  AND mi_1.movie_id = cc_1.movie_id
  AND mi_1.movie_id = mc_1.movie_id
  AND mi_1.movie_id = t_1.id
  AND mi_1.movie_id = mk_1.movie_id
  AND mi_1.movie_id = ml_1.movie_id
  AND cc_1.movie_id = mc_1.movie_id
  AND cc_1.movie_id = t_1.id
  AND cc_1.movie_id = mk_1.movie_id
  AND cc_1.movie_id = ml_1.movie_id
  AND mc_1.movie_id = t_1.id
  AND mc_1.movie_id = mk_1.movie_id
  AND mc_1.movie_id = ml_1.movie_id
  AND t_1.id = mk_1.movie_id
  AND t_1.id = ml_1.movie_id
  AND mk_1.movie_id = ml_1.movie_id
  AND mc_1.company_id = cn_1.id
  AND ml_1.link_type_id = lt_1.id
  AND k_2.keyword = '10,000-mile-club'
  AND lt_2.id = ml_2.link_type_id
  AND k_2.id = mk_2.keyword_id
  AND t1_2.id = ml_2.movie_id
  AND t1_2.id = mk_2.movie_id
  AND ml_2.movie_id = mk_2.movie_id
  AND t2_2.id = ml_2.linked_movie_id
  AND lt_1.link = lt_2.link
GROUP BY cn_1.name, lt_1.link, t_1.title, lt_2.link, t1_2.title, t2_2.title;