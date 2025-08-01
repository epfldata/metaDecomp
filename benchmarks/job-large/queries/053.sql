SELECT t_1.title AS american_movie_1,
       t_2.title AS american_movie_2
FROM company_type AS ct_1,
     company_type AS ct_2,
     info_type AS it_1,
     info_type AS it_2,
     movie_companies AS mc_1,
     movie_companies AS mc_2,
     movie_info AS mi_1,
     movie_info AS mi_2,
     title AS t_1,
     title AS t_2
WHERE ct_1.kind = 'production companies'
  AND mc_1.note NOT LIKE '%(TV)%'
  AND mc_1.note LIKE '%(USA)%'
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
  AND t_1.production_year > 1990
  AND t_1.id = mi_1.movie_id
  AND t_1.id = mc_1.movie_id
  AND mc_1.movie_id = mi_1.movie_id
  AND ct_1.id = mc_1.company_type_id
  AND it_1.id = mi_1.info_type_id
  AND ct_2.kind = 'production companies'
  AND mc_2.note NOT LIKE '%(TV)%'
  AND mc_2.note LIKE '%(USA)%'
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
  AND t_2.production_year > 1990
  AND t_2.id = mi_2.movie_id
  AND t_2.id = mc_2.movie_id
  AND mc_2.movie_id = mi_2.movie_id
  AND ct_2.id = mc_2.company_type_id
  AND it_2.id = mi_2.info_type_id
  AND t_1.title = t_2.title
GROUP BY t_1.title,
         t_2.title;