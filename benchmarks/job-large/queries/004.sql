SELECT mc_1.note AS production_note_1,
       t_1.title AS movie_title_1,
       t_1.production_year AS movie_year_1,
       mc_2.note AS production_note_2,
       t_2.title AS movie_title_2,
       t_2.production_year AS movie_year_2
FROM company_type AS ct_1,
     company_type AS ct_2,
     info_type AS it_1,
     info_type AS it_2,
     movie_companies AS mc_1,
     movie_companies AS mc_2,
     movie_info_idx AS mi_idx_1,
     movie_info_idx AS mi_idx_2,
     title AS t_1,
     title AS t_2
WHERE ct_1.kind = 'production companies'
  AND it_1.info = 'top 250 rank'
  AND mc_1.note NOT LIKE '%(as Metro-Goldwyn-Mayer Pictures)%'
  AND (mc_1.note LIKE '%(co-production)%')
  AND t_1.production_year >2010
  AND ct_1.id = mc_1.company_type_id
  AND t_1.id = mc_1.movie_id
  AND t_1.id = mi_idx_1.movie_id
  AND mc_1.movie_id = mi_idx_1.movie_id
  AND it_1.id = mi_idx_1.info_type_id
  AND ct_2.kind = 'production companies'
  AND it_2.info = 'top 250 rank'
  AND mc_2.note NOT LIKE '%(as Metro-Goldwyn-Mayer Pictures)%'
  AND (mc_2.note LIKE '%(co-production)%')
  AND t_2.production_year >2010
  AND ct_2.id = mc_2.company_type_id
  AND t_2.id = mc_2.movie_id
  AND t_2.id = mi_idx_2.movie_id
  AND mc_2.movie_id = mi_idx_2.movie_id
  AND it_2.id = mi_idx_2.info_type_id
  AND mc_1.note = mc_2.note
GROUP BY mc_1.note,
         mc_2.note,
         t_1.production_year,
         t_1.title,
         t_2.production_year,
         t_2.title;