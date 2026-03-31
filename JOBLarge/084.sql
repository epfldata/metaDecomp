SELECT cn_1.name, mi_idx_1.info, t_1.title, n_2.name, t_2.title
FROM movie_companies AS mc_1,
    company_type AS ct_1,
    title AS t_1,
    movie_info_idx AS mi_idx_1,
    company_name AS cn_1,
    info_type AS it2_1,
    movie_info AS mi_1,
    info_type AS it1_1, title AS t_2,
    company_name AS cn_2,
    info_type AS it_2,
    cast_info AS ci_2,
    char_name AS chn_2,
    aka_name AS an_2,
    movie_info AS mi_2,
    name AS n_2,
    movie_companies AS mc_2,
    role_type AS rt_2
WHERE it1_1.info = 'genres'
  AND it2_1.info = 'rating'
  AND t_1.production_year BETWEEN 2005 AND 2008
  AND cn_1.country_code = '[us]'
  AND mi_idx_1.info > '8.0'
  AND ct_1.kind = 'production companies'
  AND mi_1.info IN ( 'Drama' , 'Horror' )
  AND mc_1.company_type_id = ct_1.id
  AND mc_1.company_id = cn_1.id
  AND mi_idx_1.info_type_id = it2_1.id
  AND mi_1.movie_id = mc_1.movie_id
  AND mi_1.movie_id = mi_idx_1.movie_id
  AND mi_1.movie_id = t_1.id
  AND mc_1.movie_id = mi_idx_1.movie_id
  AND mc_1.movie_id = t_1.id
  AND mi_idx_1.movie_id = t_1.id
  AND it1_1.id = mi_1.info_type_id
  AND ci_2.note IN ( '(voice)' , '(voice: Japanese version)' , '(voice) (uncredited)' , '(voice: English version)' )
  AND n_2.name LIKE '%Ang%'
  AND n_2.gender = 'f'
  AND cn_2.country_code = '[us]'
  AND rt_2.role = 'actress'
  AND t_2.production_year BETWEEN 2005 AND 2009
  AND ( mc_2.note LIKE '%(USA)%' OR mc_2.note LIKE '%(worldwide)%' )
  AND mc_2.note IS NOT NULL
  AND ( mi_2.info LIKE 'Japan:%200%' OR mi_2.info LIKE 'USA:%200%' )
  AND mi_2.info IS NOT NULL
  AND it_2.info = 'release dates'
  AND t_2.id = ci_2.movie_id
  AND t_2.id = mc_2.movie_id
  AND t_2.id = mi_2.movie_id
  AND ci_2.movie_id = mc_2.movie_id
  AND ci_2.movie_id = mi_2.movie_id
  AND mc_2.movie_id = mi_2.movie_id
  AND mi_2.info_type_id = it_2.id
  AND mc_2.company_id = cn_2.id
  AND ci_2.person_role_id = chn_2.id
  AND rt_2.id = ci_2.role_id
  AND an_2.person_id = n_2.id
  AND an_2.person_id = ci_2.person_id
  AND n_2.id = ci_2.person_id
  AND t_1.title = t_2.title
GROUP BY cn_1.name, mi_idx_1.info, t_1.title, n_2.name, t_2.title;