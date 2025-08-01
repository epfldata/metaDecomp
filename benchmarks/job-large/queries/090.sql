SELECT chn_1.name, n_1.name, t_1.title, lt_2.link, t1_2.title, t2_2.title
FROM movie_companies AS mc_1,
    char_name AS chn_1,
    complete_cast AS cc_1,
    name AS n_1,
    title AS t_1,
    keyword AS k_1,
    comp_cast_type AS cct2_1,
    comp_cast_type AS cct1_1,
    cast_info AS ci_1,
    movie_info AS mi_1,
    movie_keyword AS mk_1,
    info_type AS it3_1,
    info_type AS it_1,
    role_type AS rt_1,
    aka_name AS an_1,
    company_name AS cn_1,
    person_info AS pi_1, title AS t1_2,
    movie_link AS ml_2,
    keyword AS k_2,
    movie_keyword AS mk_2,
    title AS t2_2,
    link_type AS lt_2
WHERE cct1_1.kind = 'cast'
  AND mi_1.info IS NOT NULL
  AND ( mi_1.info LIKE 'Japan:%200%' OR mi_1.info LIKE 'USA:%200%' )
  AND k_1.keyword = 'computer-animation'
  AND t_1.title = 'Shrek 2'
  AND t_1.production_year BETWEEN 2000 AND 2010
  AND cn_1.country_code = '[us]'
  AND cct2_1.kind = 'complete+verified'
  AND it_1.info = 'release dates'
  AND it3_1.info = 'trivia'
  AND ci_1.note IN ( '(voice)' , '(voice) (uncredited)' , '(voice: English version)' )
  AND n_1.gender = 'f'
  AND n_1.name LIKE '%An%'
  AND chn_1.name = 'Queen'
  AND rt_1.role = 'actress'
  AND cn_1.id = mc_1.company_id
  AND ci_1.person_role_id = chn_1.id
  AND cc_1.subject_id = cct1_1.id
  AND mk_1.keyword_id = k_1.id
  AND ci_1.movie_id = cc_1.movie_id
  AND ci_1.movie_id = mc_1.movie_id
  AND ci_1.movie_id = mi_1.movie_id
  AND ci_1.movie_id = mk_1.movie_id
  AND ci_1.movie_id = t_1.id
  AND cc_1.movie_id = mc_1.movie_id
  AND cc_1.movie_id = mi_1.movie_id
  AND cc_1.movie_id = mk_1.movie_id
  AND cc_1.movie_id = t_1.id
  AND mc_1.movie_id = mi_1.movie_id
  AND mc_1.movie_id = mk_1.movie_id
  AND mc_1.movie_id = t_1.id
  AND mi_1.movie_id = mk_1.movie_id
  AND mi_1.movie_id = t_1.id
  AND mk_1.movie_id = t_1.id
  AND mi_1.info_type_id = it_1.id
  AND an_1.person_id = ci_1.person_id
  AND an_1.person_id = n_1.id
  AND an_1.person_id = pi_1.person_id
  AND ci_1.person_id = n_1.id
  AND ci_1.person_id = pi_1.person_id
  AND n_1.id = pi_1.person_id
  AND pi_1.info_type_id = it3_1.id
  AND rt_1.id = ci_1.role_id
  AND cct2_1.id = cc_1.status_id
  AND k_2.keyword = '10,000-mile-club'
  AND lt_2.id = ml_2.link_type_id
  AND k_2.id = mk_2.keyword_id
  AND t1_2.id = ml_2.movie_id
  AND t1_2.id = mk_2.movie_id
  AND ml_2.movie_id = mk_2.movie_id
  AND t2_2.id = ml_2.linked_movie_id
  AND t_1.title = t1_2.title
GROUP BY chn_1.name, n_1.name, t_1.title, lt_2.link, t1_2.title, t2_2.title;