SELECT an1_1.name AS costume_designer_pseudo_1,
       t_1.title AS movie_with_costumes_1,
       an1_2.name AS costume_designer_pseudo_2,
       t_2.title AS movie_with_costumes_2
FROM aka_name AS an1_1,
     aka_name AS an1_2,
     cast_info AS ci_1,
     cast_info AS ci_2,
     company_name AS cn_1,
     company_name AS cn_2,
     movie_companies AS mc_1,
     movie_companies AS mc_2,
     name AS n1_1,
     name AS n1_2,
     role_type AS rt_1,
     role_type AS rt_2,
     title AS t_1,
     title AS t_2
WHERE cn_1.country_code ='[us]'
  AND rt_1.role ='costume designer'
  AND an1_1.person_id = n1_1.id
  AND n1_1.id = ci_1.person_id
  AND ci_1.movie_id = t_1.id
  AND t_1.id = mc_1.movie_id
  AND mc_1.company_id = cn_1.id
  AND ci_1.role_id = rt_1.id
  AND an1_1.person_id = ci_1.person_id
  AND ci_1.movie_id = mc_1.movie_id
  AND cn_2.country_code ='[us]'
  AND rt_2.role ='costume designer'
  AND an1_2.person_id = n1_2.id
  AND n1_2.id = ci_2.person_id
  AND ci_2.movie_id = t_2.id
  AND t_2.id = mc_2.movie_id
  AND mc_2.company_id = cn_2.id
  AND ci_2.role_id = rt_2.id
  AND an1_2.person_id = ci_2.person_id
  AND ci_2.movie_id = mc_2.movie_id
  AND an1_1.name = an1_2.name
GROUP BY an1_1.name,
         an1_2.name,
         t_1.title,
         t_2.title;