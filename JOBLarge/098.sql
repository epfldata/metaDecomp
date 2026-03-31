SELECT chn_1.name AS voiced_char_1,
       n_1.name AS voicing_actress_1,
       t_1.title AS voiced_animation_1,
       chn_2.name AS voiced_char_2,
       n_2.name AS voicing_actress_2,
       t_2.title AS voiced_animation_2
FROM aka_name AS an_1,
     aka_name AS an_2,
     cast_info AS ci_1,
     cast_info AS ci_2,
     char_name AS chn_1,
     char_name AS chn_2,
     comp_cast_type AS cct1_1,
     comp_cast_type AS cct1_2,
     comp_cast_type AS cct2_1,
     comp_cast_type AS cct2_2,
     company_name AS cn_1,
     company_name AS cn_2,
     complete_cast AS cc_1,
     complete_cast AS cc_2,
     info_type AS it3_1,
     info_type AS it3_2,
     info_type AS it_1,
     info_type AS it_2,
     keyword AS k_1,
     keyword AS k_2,
     movie_companies AS mc_1,
     movie_companies AS mc_2,
     movie_info AS mi_1,
     movie_info AS mi_2,
     movie_keyword AS mk_1,
     movie_keyword AS mk_2,
     name AS n_1,
     name AS n_2,
     person_info AS pi_1,
     person_info AS pi_2,
     role_type AS rt_1,
     role_type AS rt_2,
     title AS t_1,
     title AS t_2
WHERE cct1_1.kind ='cast'
  AND cct2_1.kind ='complete+verified'
  AND ci_1.note IN ('(voice)',
                    '(voice: Japanese version)',
                    '(voice) (uncredited)',
                    '(voice: English version)')
  AND cn_1.country_code ='[us]'
  AND it_1.info = 'release dates'
  AND it3_1.info = 'trivia'
  AND k_1.keyword = 'computer-animation'
  AND mi_1.info IS NOT NULL
  AND (mi_1.info LIKE 'Japan:%200%'
       OR mi_1.info LIKE 'USA:%200%')
  AND n_1.gender ='f'
  AND n_1.name LIKE '%An%'
  AND rt_1.role ='actress'
  AND t_1.production_year BETWEEN 2000 AND 2010
  AND t_1.id = mi_1.movie_id
  AND t_1.id = mc_1.movie_id
  AND t_1.id = ci_1.movie_id
  AND t_1.id = mk_1.movie_id
  AND t_1.id = cc_1.movie_id
  AND mc_1.movie_id = ci_1.movie_id
  AND mc_1.movie_id = mi_1.movie_id
  AND mc_1.movie_id = mk_1.movie_id
  AND mc_1.movie_id = cc_1.movie_id
  AND mi_1.movie_id = ci_1.movie_id
  AND mi_1.movie_id = mk_1.movie_id
  AND mi_1.movie_id = cc_1.movie_id
  AND ci_1.movie_id = mk_1.movie_id
  AND ci_1.movie_id = cc_1.movie_id
  AND mk_1.movie_id = cc_1.movie_id
  AND cn_1.id = mc_1.company_id
  AND it_1.id = mi_1.info_type_id
  AND n_1.id = ci_1.person_id
  AND rt_1.id = ci_1.role_id
  AND n_1.id = an_1.person_id
  AND ci_1.person_id = an_1.person_id
  AND chn_1.id = ci_1.person_role_id
  AND n_1.id = pi_1.person_id
  AND ci_1.person_id = pi_1.person_id
  AND it3_1.id = pi_1.info_type_id
  AND k_1.id = mk_1.keyword_id
  AND cct1_1.id = cc_1.subject_id
  AND cct2_1.id = cc_1.status_id
  AND cct1_2.kind ='cast'
  AND cct2_2.kind ='complete+verified'
  AND ci_2.note IN ('(voice)',
                    '(voice: Japanese version)',
                    '(voice) (uncredited)',
                    '(voice: English version)')
  AND cn_2.country_code ='[us]'
  AND it_2.info = 'release dates'
  AND it3_2.info = 'trivia'
  AND k_2.keyword = 'computer-animation'
  AND mi_2.info IS NOT NULL
  AND (mi_2.info LIKE 'Japan:%200%'
       OR mi_2.info LIKE 'USA:%200%')
  AND n_2.gender ='f'
  AND n_2.name LIKE '%An%'
  AND rt_2.role ='actress'
  AND t_2.production_year BETWEEN 2000 AND 2010
  AND t_2.id = mi_2.movie_id
  AND t_2.id = mc_2.movie_id
  AND t_2.id = ci_2.movie_id
  AND t_2.id = mk_2.movie_id
  AND t_2.id = cc_2.movie_id
  AND mc_2.movie_id = ci_2.movie_id
  AND mc_2.movie_id = mi_2.movie_id
  AND mc_2.movie_id = mk_2.movie_id
  AND mc_2.movie_id = cc_2.movie_id
  AND mi_2.movie_id = ci_2.movie_id
  AND mi_2.movie_id = mk_2.movie_id
  AND mi_2.movie_id = cc_2.movie_id
  AND ci_2.movie_id = mk_2.movie_id
  AND ci_2.movie_id = cc_2.movie_id
  AND mk_2.movie_id = cc_2.movie_id
  AND cn_2.id = mc_2.company_id
  AND it_2.id = mi_2.info_type_id
  AND n_2.id = ci_2.person_id
  AND rt_2.id = ci_2.role_id
  AND n_2.id = an_2.person_id
  AND ci_2.person_id = an_2.person_id
  AND chn_2.id = ci_2.person_role_id
  AND n_2.id = pi_2.person_id
  AND ci_2.person_id = pi_2.person_id
  AND it3_2.id = pi_2.info_type_id
  AND k_2.id = mk_2.keyword_id
  AND cct1_2.id = cc_2.subject_id
  AND cct2_2.id = cc_2.status_id
  AND chn_1.name = chn_2.name
GROUP BY chn_1.name,
         chn_2.name,
         n_1.name,
         n_2.name,
         t_1.title,
         t_2.title;