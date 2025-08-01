SELECT t_1.title, chn_2.name, n_2.name, t_2.title
FROM movie_companies AS mc_1,
    title AS t_1,
    keyword AS k_1,
    company_name AS cn_1,
    movie_keyword AS mk_1, title AS t_2,
    company_name AS cn_2,
    info_type AS it_2,
    char_name AS chn_2,
    movie_keyword AS mk_2,
    aka_name AS an_2,
    comp_cast_type AS cct1_2,
    movie_info AS mi_2,
    name AS n_2,
    keyword AS k_2,
    info_type AS it3_2,
    cast_info AS ci_2,
    movie_companies AS mc_2,
    role_type AS rt_2,
    complete_cast AS cc_2,
    person_info AS pi_2,
    comp_cast_type AS cct2_2
WHERE cn_1.country_code = '[de]'
  AND k_1.keyword = 'character-name-in-title'
  AND cn_1.id = mc_1.company_id
  AND mk_1.keyword_id = k_1.id
  AND mc_1.movie_id = mk_1.movie_id
  AND mc_1.movie_id = t_1.id
  AND mk_1.movie_id = t_1.id
  AND cct1_2.kind = 'cast'
  AND mi_2.info IS NOT NULL
  AND ( mi_2.info LIKE 'Japan:%200%' OR mi_2.info LIKE 'USA:%200%' )
  AND k_2.keyword = 'computer-animation'
  AND t_2.title = 'Shrek 2'
  AND t_2.production_year BETWEEN 2000 AND 2010
  AND cn_2.country_code = '[us]'
  AND cct2_2.kind = 'complete+verified'
  AND it_2.info = 'release dates'
  AND it3_2.info = 'trivia'
  AND ci_2.note IN ( '(voice)' , '(voice) (uncredited)' , '(voice: English version)' )
  AND n_2.gender = 'f'
  AND n_2.name LIKE '%An%'
  AND chn_2.name = 'Queen'
  AND rt_2.role = 'actress'
  AND cn_2.id = mc_2.company_id
  AND ci_2.person_role_id = chn_2.id
  AND cc_2.subject_id = cct1_2.id
  AND mk_2.keyword_id = k_2.id
  AND ci_2.movie_id = cc_2.movie_id
  AND ci_2.movie_id = mc_2.movie_id
  AND ci_2.movie_id = mi_2.movie_id
  AND ci_2.movie_id = mk_2.movie_id
  AND ci_2.movie_id = t_2.id
  AND cc_2.movie_id = mc_2.movie_id
  AND cc_2.movie_id = mi_2.movie_id
  AND cc_2.movie_id = mk_2.movie_id
  AND cc_2.movie_id = t_2.id
  AND mc_2.movie_id = mi_2.movie_id
  AND mc_2.movie_id = mk_2.movie_id
  AND mc_2.movie_id = t_2.id
  AND mi_2.movie_id = mk_2.movie_id
  AND mi_2.movie_id = t_2.id
  AND mk_2.movie_id = t_2.id
  AND mi_2.info_type_id = it_2.id
  AND an_2.person_id = ci_2.person_id
  AND an_2.person_id = n_2.id
  AND an_2.person_id = pi_2.person_id
  AND ci_2.person_id = n_2.id
  AND ci_2.person_id = pi_2.person_id
  AND n_2.id = pi_2.person_id
  AND pi_2.info_type_id = it3_2.id
  AND rt_2.id = ci_2.role_id
  AND cct2_2.id = cc_2.status_id
  AND t_1.title = t_2.title
GROUP BY t_1.title, chn_2.name, n_2.name, t_2.title;