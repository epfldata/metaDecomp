SELECT an_1.name AS acress_pseudonym_1,
       t_1.title AS japanese_anime_movie_1,
       an_2.name AS acress_pseudonym_2,
       t_2.title AS japanese_anime_movie_2
FROM aka_name AS an_1,
     aka_name AS an_2,
     cast_info AS ci_1,
     cast_info AS ci_2,
     company_name AS cn_1,
     company_name AS cn_2,
     movie_companies AS mc_1,
     movie_companies AS mc_2,
     name AS n_1,
     name AS n_2,
     role_type AS rt_1,
     role_type AS rt_2,
     title AS t_1,
     title AS t_2
WHERE ci_1.note ='(voice: English version)'
  AND cn_1.country_code ='[jp]'
  AND mc_1.note LIKE '%(Japan)%'
  AND mc_1.note NOT LIKE '%(USA)%'
  AND (mc_1.note LIKE '%(2006)%'
       OR mc_1.note LIKE '%(2007)%')
  AND n_1.name LIKE '%Yo%'
  AND n_1.name NOT LIKE '%Yu%'
  AND rt_1.role ='actress'
  AND t_1.production_year BETWEEN 2006 AND 2007
  AND (t_1.title LIKE 'One Piece%'
       OR t_1.title LIKE 'Dragon Ball Z%')
  AND an_1.person_id = n_1.id
  AND n_1.id = ci_1.person_id
  AND ci_1.movie_id = t_1.id
  AND t_1.id = mc_1.movie_id
  AND mc_1.company_id = cn_1.id
  AND ci_1.role_id = rt_1.id
  AND an_1.person_id = ci_1.person_id
  AND ci_1.movie_id = mc_1.movie_id
  AND ci_2.note ='(voice: English version)'
  AND cn_2.country_code ='[jp]'
  AND mc_2.note LIKE '%(Japan)%'
  AND mc_2.note NOT LIKE '%(USA)%'
  AND (mc_2.note LIKE '%(2006)%'
       OR mc_2.note LIKE '%(2007)%')
  AND n_2.name LIKE '%Yo%'
  AND n_2.name NOT LIKE '%Yu%'
  AND rt_2.role ='actress'
  AND t_2.production_year BETWEEN 2006 AND 2007
  AND (t_2.title LIKE 'One Piece%'
       OR t_2.title LIKE 'Dragon Ball Z%')
  AND an_2.person_id = n_2.id
  AND n_2.id = ci_2.person_id
  AND ci_2.movie_id = t_2.id
  AND t_2.id = mc_2.movie_id
  AND mc_2.company_id = cn_2.id
  AND ci_2.role_id = rt_2.id
  AND an_2.person_id = ci_2.person_id
  AND ci_2.movie_id = mc_2.movie_id
  AND an_1.name = an_2.name
GROUP BY an_1.name,
         an_2.name,
         t_1.title,
         t_2.title;