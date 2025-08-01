SELECT cn_1.name AS movie_company_1,
       mi_idx_1.info AS rating_1,
       t_1.title AS drama_horror_movie_1,
       cn_2.name AS movie_company_2,
       mi_idx_2.info AS rating_2,
       t_2.title AS drama_horror_movie_2
FROM company_name AS cn_1,
     company_name AS cn_2,
     company_type AS ct_1,
     company_type AS ct_2,
     info_type AS it1_1,
     info_type AS it1_2,
     info_type AS it2_1,
     info_type AS it2_2,
     movie_companies AS mc_1,
     movie_companies AS mc_2,
     movie_info AS mi_1,
     movie_info AS mi_2,
     movie_info_idx AS mi_idx_1,
     movie_info_idx AS mi_idx_2,
     title AS t_1,
     title AS t_2
WHERE cn_1.country_code = '[us]'
  AND ct_1.kind = 'production companies'
  AND it1_1.info = 'genres'
  AND it2_1.info = 'rating'
  AND mi_1.info IN ('Drama',
                    'Horror')
  AND mi_idx_1.info > '8.0'
  AND t_1.production_year BETWEEN 2005 AND 2008
  AND t_1.id = mi_1.movie_id
  AND t_1.id = mi_idx_1.movie_id
  AND mi_1.info_type_id = it1_1.id
  AND mi_idx_1.info_type_id = it2_1.id
  AND t_1.id = mc_1.movie_id
  AND ct_1.id = mc_1.company_type_id
  AND cn_1.id = mc_1.company_id
  AND mc_1.movie_id = mi_1.movie_id
  AND mc_1.movie_id = mi_idx_1.movie_id
  AND mi_1.movie_id = mi_idx_1.movie_id
  AND cn_2.country_code = '[us]'
  AND ct_2.kind = 'production companies'
  AND it1_2.info = 'genres'
  AND it2_2.info = 'rating'
  AND mi_2.info IN ('Drama',
                    'Horror')
  AND mi_idx_2.info > '8.0'
  AND t_2.production_year BETWEEN 2005 AND 2008
  AND t_2.id = mi_2.movie_id
  AND t_2.id = mi_idx_2.movie_id
  AND mi_2.info_type_id = it1_2.id
  AND mi_idx_2.info_type_id = it2_2.id
  AND t_2.id = mc_2.movie_id
  AND ct_2.id = mc_2.company_type_id
  AND cn_2.id = mc_2.company_id
  AND mc_2.movie_id = mi_2.movie_id
  AND mc_2.movie_id = mi_idx_2.movie_id
  AND mi_2.movie_id = mi_idx_2.movie_id
  AND cn_1.name = cn_2.name
GROUP BY cn_1.name,
         cn_2.name,
         mi_idx_1.info,
         mi_idx_2.info,
         t_1.title,
         t_2.title;