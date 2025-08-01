SELECT mi_idx_1.info, t_1.title, chn_2.name, n_2.name, t_2.title
FROM kind_type AS kt_1,
    title AS t_1,
    keyword AS k_1,
    movie_info_idx AS mi_idx_1,
    info_type AS it2_1,
    movie_info AS mi_1,
    movie_keyword AS mk_1,
    info_type AS it1_1, title AS t_2,
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
WHERE it1_1.info = 'countries'
  AND t_1.production_year > 2010
  AND it2_1.info = 'rating'
  AND kt_1.kind = 'movie'
  AND k_1.keyword IN ( 'murder' , 'murder-in-title' , 'blood' , 'violence' )
  AND mi_idx_1.info < '8.5'
  AND mi_1.info IN ( 'Sweden' , 'Norway' , 'Germany' , 'Denmark' , 'Swedish' , 'Denish' , 'Norwegian' , 'German' , 'USA' , 'American' )
  AND mi_idx_1.info_type_id = it2_1.id
  AND mk_1.keyword_id = k_1.id
  AND t_1.kind_id = kt_1.id
  AND mi_1.movie_id = mi_idx_1.movie_id
  AND mi_1.movie_id = mk_1.movie_id
  AND mi_1.movie_id = t_1.id
  AND mi_idx_1.movie_id = mk_1.movie_id
  AND mi_idx_1.movie_id = t_1.id
  AND mk_1.movie_id = t_1.id
  AND it1_1.id = mi_1.info_type_id
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
  AND t_1.title = t_2.title
GROUP BY mi_idx_1.info, t_1.title, chn_2.name, n_2.name, t_2.title;