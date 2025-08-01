SELECT chn_1.name, t_1.title, chn_2.name, n_2.name, t_2.title
FROM movie_companies AS mc_1,
    char_name AS chn_1,
    role_type AS rt_1,
    company_type AS ct_1,
    title AS t_1,
    company_name AS cn_1,
    cast_info AS ci_1, title AS t_2,
    company_name AS cn_2,
    info_type AS it_2,
    char_name AS chn_2,
    movie_keyword AS mk_2,
    aka_name AS an_2,
    movie_info AS mi_2,
    name AS n_2,
    keyword AS k_2,
    cast_info AS ci_2,
    movie_companies AS mc_2,
    role_type AS rt_2
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
  AND t_2.production_year > 2010
  AND rt_2.role = 'actress'
  AND cn_2.country_code = '[us]'
  AND n_2.name LIKE '%An%'
  AND n_2.gender = 'f'
  AND it_2.info = 'release dates'
  AND ci_2.note IN ( '(voice)' , '(voice: Japanese version)' , '(voice) (uncredited)' , '(voice: English version)' )
  AND mi_2.info IS NOT NULL
  AND ( mi_2.info LIKE 'Japan:%201%' OR mi_2.info LIKE 'USA:%201%' )
  AND k_2.keyword IN ( 'hero' , 'martial-arts' , 'hand-to-hand-combat' )
  AND rt_2.id = ci_2.role_id
  AND mc_2.company_id = cn_2.id
  AND chn_2.id = ci_2.person_role_id
  AND mi_2.movie_id = ci_2.movie_id
  AND mi_2.movie_id = mc_2.movie_id
  AND mi_2.movie_id = mk_2.movie_id
  AND mi_2.movie_id = t_2.id
  AND ci_2.movie_id = mc_2.movie_id
  AND ci_2.movie_id = mk_2.movie_id
  AND ci_2.movie_id = t_2.id
  AND mc_2.movie_id = mk_2.movie_id
  AND mc_2.movie_id = t_2.id
  AND mk_2.movie_id = t_2.id
  AND mi_2.info_type_id = it_2.id
  AND k_2.id = mk_2.keyword_id
  AND n_2.id = an_2.person_id
  AND n_2.id = ci_2.person_id
  AND an_2.person_id = ci_2.person_id
  AND chn_1.name = chn_2.name
GROUP BY chn_1.name, t_1.title, chn_2.name, n_2.name, t_2.title;