SELECT t_1.title, t2_2.title, cn2_2.name, cn1_2.name, mi_idx2_2.info, t1_2.title, mi_idx1_2.info
FROM movie_companies AS mc_1,
    company_type AS ct_1,
    title AS t_1,
    movie_info AS mi_1,
    info_type AS it_1, title AS t1_2,
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
WHERE mi_1.info IN ( 'Sweden' , 'Norway' , 'Germany' , 'Denmark' , 'Swedish' , 'Denish' , 'Norwegian' , 'German' )
  AND mc_1.note LIKE '%(France)%'
  AND mc_1.note LIKE '%(theatrical)%'
  AND ct_1.kind = 'production companies'
  AND t_1.production_year > 2005
  AND mc_1.company_type_id = ct_1.id
  AND it_1.id = mi_1.info_type_id
  AND t_1.id = mi_1.movie_id
  AND t_1.id = mc_1.movie_id
  AND mi_1.movie_id = mc_1.movie_id
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
  AND t_1.title = t1_2.title
GROUP BY t_1.title, t2_2.title, cn2_2.name, cn1_2.name, mi_idx2_2.info, t1_2.title, mi_idx1_2.info;