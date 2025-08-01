SELECT chn_1.name, t_1.title, mi_2.info, mi_idx_2.info, n_2.name, t_2.title
FROM movie_companies AS mc_1,
    char_name AS chn_1,
    role_type AS rt_1,
    company_type AS ct_1,
    title AS t_1,
    company_name AS cn_1,
    cast_info AS ci_1, title AS t_2,
    company_name AS cn_2,
    movie_info_idx AS mi_idx_2,
    movie_keyword AS mk_2,
    info_type AS it2_2,
    movie_info AS mi_2,
    name AS n_2,
    keyword AS k_2,
    cast_info AS ci_2,
    info_type AS it1_2,
    movie_companies AS mc_2
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
  AND ci_2.note IN ( '(writer)' , '(head writer)' , '(written by)' , '(story)' , '(story editor)' )
  AND it1_2.info = 'genres'
  AND n_2.gender = 'm'
  AND cn_2.name LIKE 'Lionsgate%'
  AND it2_2.info = 'votes'
  AND mi_2.info IN ( 'Horror' , 'Thriller' )
  AND k_2.keyword IN ( 'murder' , 'violence' , 'blood' , 'gore' , 'death' , 'female-nudity' , 'hospital' )
  AND mi_idx_2.info_type_id = it2_2.id
  AND k_2.id = mk_2.keyword_id
  AND mk_2.movie_id = mc_2.movie_id
  AND mk_2.movie_id = t_2.id
  AND mk_2.movie_id = mi_idx_2.movie_id
  AND mk_2.movie_id = mi_2.movie_id
  AND mk_2.movie_id = ci_2.movie_id
  AND mc_2.movie_id = t_2.id
  AND mc_2.movie_id = mi_idx_2.movie_id
  AND mc_2.movie_id = mi_2.movie_id
  AND mc_2.movie_id = ci_2.movie_id
  AND t_2.id = mi_idx_2.movie_id
  AND t_2.id = mi_2.movie_id
  AND t_2.id = ci_2.movie_id
  AND mi_idx_2.movie_id = mi_2.movie_id
  AND mi_idx_2.movie_id = ci_2.movie_id
  AND mi_2.movie_id = ci_2.movie_id
  AND mi_2.info_type_id = it1_2.id
  AND mc_2.company_id = cn_2.id
  AND n_2.id = ci_2.person_id
  AND t_1.title = t_2.title
GROUP BY chn_1.name, t_1.title, mi_2.info, mi_idx_2.info, n_2.name, t_2.title;