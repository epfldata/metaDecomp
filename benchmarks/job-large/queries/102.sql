SELECT mi_1.info AS movie_budget_1,
       mi_idx_1.info AS movie_votes_1,
       t_1.title AS movie_title_1,
       mi_2.info AS movie_budget_2,
       mi_idx_2.info AS movie_votes_2,
       t_2.title AS movie_title_2
FROM cast_info AS ci_1,
     cast_info AS ci_2,
     info_type AS it1_1,
     info_type AS it1_2,
     info_type AS it2_1,
     info_type AS it2_2,
     movie_info AS mi_1,
     movie_info AS mi_2,
     movie_info_idx AS mi_idx_1,
     movie_info_idx AS mi_idx_2,
     name AS n_1,
     name AS n_2,
     title AS t_1,
     title AS t_2
WHERE ci_1.note IN ('(writer)',
                    '(head writer)',
                    '(written by)',
                    '(story)',
                    '(story editor)')
  AND it1_1.info = 'genres'
  AND it2_1.info = 'votes'
  AND mi_1.info IN ('Horror',
                    'Action',
                    'Sci-Fi',
                    'Thriller',
                    'Crime',
                    'War')
  AND n_1.gender = 'm'
  AND t_1.id = mi_1.movie_id
  AND t_1.id = mi_idx_1.movie_id
  AND t_1.id = ci_1.movie_id
  AND ci_1.movie_id = mi_1.movie_id
  AND ci_1.movie_id = mi_idx_1.movie_id
  AND mi_1.movie_id = mi_idx_1.movie_id
  AND n_1.id = ci_1.person_id
  AND it1_1.id = mi_1.info_type_id
  AND it2_1.id = mi_idx_1.info_type_id
  AND ci_2.note IN ('(writer)',
                    '(head writer)',
                    '(written by)',
                    '(story)',
                    '(story editor)')
  AND it1_2.info = 'genres'
  AND it2_2.info = 'votes'
  AND mi_2.info IN ('Horror',
                    'Action',
                    'Sci-Fi',
                    'Thriller',
                    'Crime',
                    'War')
  AND n_2.gender = 'm'
  AND t_2.id = mi_2.movie_id
  AND t_2.id = mi_idx_2.movie_id
  AND t_2.id = ci_2.movie_id
  AND ci_2.movie_id = mi_2.movie_id
  AND ci_2.movie_id = mi_idx_2.movie_id
  AND mi_2.movie_id = mi_idx_2.movie_id
  AND n_2.id = ci_2.person_id
  AND it1_2.id = mi_2.info_type_id
  AND it2_2.id = mi_idx_2.info_type_id
  AND mi_1.info = mi_2.info
GROUP BY mi_1.info,
         mi_2.info,
         mi_idx_1.info,
         mi_idx_2.info,
         t_1.title,
         t_2.title;