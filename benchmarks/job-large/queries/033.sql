SELECT chn_1.name, n_1.name, t_1.title, lt_2.link, t1_2.title, t2_2.title
FROM movie_companies AS mc_1,
    char_name AS chn_1,
    name AS n_1,
    title AS t_1,
    keyword AS k_1,
    company_name AS cn_1,
    cast_info AS ci_1,
    movie_info AS mi_1,
    movie_keyword AS mk_1,
    info_type AS it_1,
    role_type AS rt_1,
    aka_name AS an_1, title AS t1_2,
    movie_link AS ml_2,
    keyword AS k_2,
    movie_keyword AS mk_2,
    title AS t2_2,
    link_type AS lt_2
WHERE t_1.production_year > 2010
  AND rt_1.role = 'actress'
  AND cn_1.country_code = '[us]'
  AND n_1.name LIKE '%An%'
  AND n_1.gender = 'f'
  AND it_1.info = 'release dates'
  AND ci_1.note IN ( '(voice)' , '(voice: Japanese version)' , '(voice) (uncredited)' , '(voice: English version)' )
  AND mi_1.info IS NOT NULL
  AND ( mi_1.info LIKE 'Japan:%201%' OR mi_1.info LIKE 'USA:%201%' )
  AND k_1.keyword IN ( 'hero' , 'martial-arts' , 'hand-to-hand-combat' )
  AND rt_1.id = ci_1.role_id
  AND mc_1.company_id = cn_1.id
  AND chn_1.id = ci_1.person_role_id
  AND mi_1.movie_id = ci_1.movie_id
  AND mi_1.movie_id = mc_1.movie_id
  AND mi_1.movie_id = mk_1.movie_id
  AND mi_1.movie_id = t_1.id
  AND ci_1.movie_id = mc_1.movie_id
  AND ci_1.movie_id = mk_1.movie_id
  AND ci_1.movie_id = t_1.id
  AND mc_1.movie_id = mk_1.movie_id
  AND mc_1.movie_id = t_1.id
  AND mk_1.movie_id = t_1.id
  AND mi_1.info_type_id = it_1.id
  AND k_1.id = mk_1.keyword_id
  AND n_1.id = an_1.person_id
  AND n_1.id = ci_1.person_id
  AND an_1.person_id = ci_1.person_id
  AND k_2.keyword = '10,000-mile-club'
  AND lt_2.id = ml_2.link_type_id
  AND k_2.id = mk_2.keyword_id
  AND t1_2.id = ml_2.movie_id
  AND t1_2.id = mk_2.movie_id
  AND ml_2.movie_id = mk_2.movie_id
  AND t2_2.id = ml_2.linked_movie_id
  AND t_1.title = t1_2.title
GROUP BY chn_1.name, n_1.name, t_1.title, lt_2.link, t1_2.title, t2_2.title;