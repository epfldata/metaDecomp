SELECT mi_1.info AS movie_budget_1,
       mi_idx_1.info AS movie_votes_1,
       n_1.name AS writer_1,
       t_1.title AS violent_liongate_movie_1,
       mi_2.info AS movie_budget_2,
       mi_idx_2.info AS movie_votes_2,
       n_2.name AS writer_2,
       t_2.title AS violent_liongate_movie_2
FROM cast_info AS ci_1,
     cast_info AS ci_2,
     company_name AS cn_1,
     company_name AS cn_2,
     info_type AS it1_1,
     info_type AS it1_2,
     info_type AS it2_1,
     info_type AS it2_2,
     keyword AS k_1,
     keyword AS k_2,
     movie_companies AS mc_1,
     movie_companies AS mc_2,
     movie_info AS mi_1,
     movie_info AS mi_2,
     movie_info_idx AS mi_idx_1,
     movie_info_idx AS mi_idx_2,
     movie_keyword AS mk_1,
     movie_keyword AS mk_2,
     name AS n_1,
     name AS n_2,
     title AS t_1,
     title AS t_2
WHERE ci_1.note IN ('(writer)',
                    '(head writer)',
                    '(written by)',
                    '(story)',
                    '(story editor)')
  AND cn_1.name LIKE 'Lionsgate%'
  AND it1_1.info = 'genres'
  AND it2_1.info = 'votes'
  AND k_1.keyword IN ('murder',
                      'violence',
                      'blood',
                      'gore',
                      'death',
                      'female-nudity',
                      'hospital')
  AND mi_1.info IN ('Horror',
                    'Thriller')
  AND n_1.gender = 'm'
  AND t_1.id = mi_1.movie_id
  AND t_1.id = mi_idx_1.movie_id
  AND t_1.id = ci_1.movie_id
  AND t_1.id = mk_1.movie_id
  AND t_1.id = mc_1.movie_id
  AND ci_1.movie_id = mi_1.movie_id
  AND ci_1.movie_id = mi_idx_1.movie_id
  AND ci_1.movie_id = mk_1.movie_id
  AND ci_1.movie_id = mc_1.movie_id
  AND mi_1.movie_id = mi_idx_1.movie_id
  AND mi_1.movie_id = mk_1.movie_id
  AND mi_1.movie_id = mc_1.movie_id
  AND mi_idx_1.movie_id = mk_1.movie_id
  AND mi_idx_1.movie_id = mc_1.movie_id
  AND mk_1.movie_id = mc_1.movie_id
  AND n_1.id = ci_1.person_id
  AND it1_1.id = mi_1.info_type_id
  AND it2_1.id = mi_idx_1.info_type_id
  AND k_1.id = mk_1.keyword_id
  AND cn_1.id = mc_1.company_id
  AND ci_2.note IN ('(writer)',
                    '(head writer)',
                    '(written by)',
                    '(story)',
                    '(story editor)')
  AND cn_2.name LIKE 'Lionsgate%'
  AND it1_2.info = 'genres'
  AND it2_2.info = 'votes'
  AND k_2.keyword IN ('murder',
                      'violence',
                      'blood',
                      'gore',
                      'death',
                      'female-nudity',
                      'hospital')
  AND mi_2.info IN ('Horror',
                    'Thriller')
  AND n_2.gender = 'm'
  AND t_2.id = mi_2.movie_id
  AND t_2.id = mi_idx_2.movie_id
  AND t_2.id = ci_2.movie_id
  AND t_2.id = mk_2.movie_id
  AND t_2.id = mc_2.movie_id
  AND ci_2.movie_id = mi_2.movie_id
  AND ci_2.movie_id = mi_idx_2.movie_id
  AND ci_2.movie_id = mk_2.movie_id
  AND ci_2.movie_id = mc_2.movie_id
  AND mi_2.movie_id = mi_idx_2.movie_id
  AND mi_2.movie_id = mk_2.movie_id
  AND mi_2.movie_id = mc_2.movie_id
  AND mi_idx_2.movie_id = mk_2.movie_id
  AND mi_idx_2.movie_id = mc_2.movie_id
  AND mk_2.movie_id = mc_2.movie_id
  AND n_2.id = ci_2.person_id
  AND it1_2.id = mi_2.info_type_id
  AND it2_2.id = mi_idx_2.info_type_id
  AND k_2.id = mk_2.keyword_id
  AND cn_2.id = mc_2.company_id
  AND mi_1.info = mi_2.info
GROUP BY mi_1.info,
         mi_2.info,
         mi_idx_1.info,
         mi_idx_2.info,
         n_1.name,
         n_2.name,
         t_1.title,
         t_2.title;