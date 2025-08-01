SELECT mi_idx_1.info AS rating_1,
       t_1.title AS northern_dark_movie_1,
       mi_idx_2.info AS rating_2,
       t_2.title AS northern_dark_movie_2
FROM info_type AS it1_1,
     info_type AS it1_2,
     info_type AS it2_1,
     info_type AS it2_2,
     keyword AS k_1,
     keyword AS k_2,
     kind_type AS kt_1,
     kind_type AS kt_2,
     movie_info AS mi_1,
     movie_info AS mi_2,
     movie_info_idx AS mi_idx_1,
     movie_info_idx AS mi_idx_2,
     movie_keyword AS mk_1,
     movie_keyword AS mk_2,
     title AS t_1,
     title AS t_2
WHERE it1_1.info = 'countries'
  AND it2_1.info = 'rating'
  AND k_1.keyword IN ('murder',
                      'murder-in-title',
                      'blood',
                      'violence')
  AND kt_1.kind = 'movie'
  AND mi_1.info IN ('Sweden',
                    'Norway',
                    'Germany',
                    'Denmark',
                    'Swedish',
                    'Denish',
                    'Norwegian',
                    'German',
                    'USA',
                    'American')
  AND mi_idx_1.info < '8.5'
  AND t_1.production_year > 2010
  AND kt_1.id = t_1.kind_id
  AND t_1.id = mi_1.movie_id
  AND t_1.id = mk_1.movie_id
  AND t_1.id = mi_idx_1.movie_id
  AND mk_1.movie_id = mi_1.movie_id
  AND mk_1.movie_id = mi_idx_1.movie_id
  AND mi_1.movie_id = mi_idx_1.movie_id
  AND k_1.id = mk_1.keyword_id
  AND it1_1.id = mi_1.info_type_id
  AND it2_1.id = mi_idx_1.info_type_id
  AND it1_2.info = 'countries'
  AND it2_2.info = 'rating'
  AND k_2.keyword IN ('murder',
                      'murder-in-title',
                      'blood',
                      'violence')
  AND kt_2.kind = 'movie'
  AND mi_2.info IN ('Sweden',
                    'Norway',
                    'Germany',
                    'Denmark',
                    'Swedish',
                    'Denish',
                    'Norwegian',
                    'German',
                    'USA',
                    'American')
  AND mi_idx_2.info < '8.5'
  AND t_2.production_year > 2010
  AND kt_2.id = t_2.kind_id
  AND t_2.id = mi_2.movie_id
  AND t_2.id = mk_2.movie_id
  AND t_2.id = mi_idx_2.movie_id
  AND mk_2.movie_id = mi_2.movie_id
  AND mk_2.movie_id = mi_idx_2.movie_id
  AND mi_2.movie_id = mi_idx_2.movie_id
  AND k_2.id = mk_2.keyword_id
  AND it1_2.id = mi_2.info_type_id
  AND it2_2.id = mi_idx_2.info_type_id
  AND mi_idx_1.info = mi_idx_2.info
GROUP BY mi_idx_1.info,
         mi_idx_2.info,
         t_1.title,
         t_2.title;