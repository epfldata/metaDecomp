SELECT cn1_1.name AS first_company_1,
       cn2_1.name AS second_company_1,
       mi_idx1_1.info AS first_rating_1,
       mi_idx2_1.info AS second_rating_1,
       t1_1.title AS first_movie_1,
       t2_1.title AS second_movie_1,
       cn1_2.name AS first_company_2,
       cn2_2.name AS second_company_2,
       mi_idx1_2.info AS first_rating_2,
       mi_idx2_2.info AS second_rating_2,
       t1_2.title AS first_movie_2,
       t2_2.title AS second_movie_2
FROM company_name AS cn1_1,
     company_name AS cn1_2,
     company_name AS cn2_1,
     company_name AS cn2_2,
     info_type AS it1_1,
     info_type AS it1_2,
     info_type AS it2_1,
     info_type AS it2_2,
     kind_type AS kt1_1,
     kind_type AS kt1_2,
     kind_type AS kt2_1,
     kind_type AS kt2_2,
     link_type AS lt_1,
     link_type AS lt_2,
     movie_companies AS mc1_1,
     movie_companies AS mc1_2,
     movie_companies AS mc2_1,
     movie_companies AS mc2_2,
     movie_info_idx AS mi_idx1_1,
     movie_info_idx AS mi_idx1_2,
     movie_info_idx AS mi_idx2_1,
     movie_info_idx AS mi_idx2_2,
     movie_link AS ml_1,
     movie_link AS ml_2,
     title AS t1_1,
     title AS t1_2,
     title AS t2_1,
     title AS t2_2
WHERE cn1_1.country_code = '[nl]'
  AND it1_1.info = 'rating'
  AND it2_1.info = 'rating'
  AND kt1_1.kind IN ('tv series')
  AND kt2_1.kind IN ('tv series')
  AND lt_1.link LIKE '%follow%'
  AND mi_idx2_1.info < '3.0'
  AND t2_1.production_year = 2007
  AND lt_1.id = ml_1.link_type_id
  AND t1_1.id = ml_1.movie_id
  AND t2_1.id = ml_1.linked_movie_id
  AND it1_1.id = mi_idx1_1.info_type_id
  AND t1_1.id = mi_idx1_1.movie_id
  AND kt1_1.id = t1_1.kind_id
  AND cn1_1.id = mc1_1.company_id
  AND t1_1.id = mc1_1.movie_id
  AND ml_1.movie_id = mi_idx1_1.movie_id
  AND ml_1.movie_id = mc1_1.movie_id
  AND mi_idx1_1.movie_id = mc1_1.movie_id
  AND it2_1.id = mi_idx2_1.info_type_id
  AND t2_1.id = mi_idx2_1.movie_id
  AND kt2_1.id = t2_1.kind_id
  AND cn2_1.id = mc2_1.company_id
  AND t2_1.id = mc2_1.movie_id
  AND ml_1.linked_movie_id = mi_idx2_1.movie_id
  AND ml_1.linked_movie_id = mc2_1.movie_id
  AND mi_idx2_1.movie_id = mc2_1.movie_id
  AND cn1_2.country_code = '[nl]'
  AND it1_2.info = 'rating'
  AND it2_2.info = 'rating'
  AND kt1_2.kind IN ('tv series')
  AND kt2_2.kind IN ('tv series')
  AND lt_2.link LIKE '%follow%'
  AND mi_idx2_2.info < '3.0'
  AND t2_2.production_year = 2007
  AND lt_2.id = ml_2.link_type_id
  AND t1_2.id = ml_2.movie_id
  AND t2_2.id = ml_2.linked_movie_id
  AND it1_2.id = mi_idx1_2.info_type_id
  AND t1_2.id = mi_idx1_2.movie_id
  AND kt1_2.id = t1_2.kind_id
  AND cn1_2.id = mc1_2.company_id
  AND t1_2.id = mc1_2.movie_id
  AND ml_2.movie_id = mi_idx1_2.movie_id
  AND ml_2.movie_id = mc1_2.movie_id
  AND mi_idx1_2.movie_id = mc1_2.movie_id
  AND it2_2.id = mi_idx2_2.info_type_id
  AND t2_2.id = mi_idx2_2.movie_id
  AND kt2_2.id = t2_2.kind_id
  AND cn2_2.id = mc2_2.company_id
  AND t2_2.id = mc2_2.movie_id
  AND ml_2.linked_movie_id = mi_idx2_2.movie_id
  AND ml_2.linked_movie_id = mc2_2.movie_id
  AND mi_idx2_2.movie_id = mc2_2.movie_id
  AND cn1_1.name = cn1_2.name
GROUP BY cn1_1.name,
         cn1_2.name,
         cn2_1.name,
         cn2_2.name,
         mi_idx1_1.info,
         mi_idx1_2.info,
         mi_idx2_1.info,
         mi_idx2_2.info,
         t1_1.title,
         t1_2.title,
         t2_1.title,
         t2_2.title;