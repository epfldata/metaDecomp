SELECT n_1.name AS cast_member_name_1,
       pi_1.info AS cast_member_info_1,
       n_2.name AS cast_member_name_2,
       pi_2.info AS cast_member_info_2
FROM aka_name AS an_1,
     aka_name AS an_2,
     cast_info AS ci_1,
     cast_info AS ci_2,
     info_type AS it_1,
     info_type AS it_2,
     link_type AS lt_1,
     link_type AS lt_2,
     movie_link AS ml_1,
     movie_link AS ml_2,
     name AS n_1,
     name AS n_2,
     person_info AS pi_1,
     person_info AS pi_2,
     title AS t_1,
     title AS t_2
WHERE an_1.name IS NOT NULL
  AND (an_1.name LIKE '%a%'
       OR an_1.name LIKE 'A%')
  AND it_1.info ='mini biography'
  AND lt_1.link IN ('references',
                    'referenced in',
                    'features',
                    'featured in')
  AND n_1.name_pcode_cf BETWEEN 'A' AND 'F'
  AND (n_1.gender='m'
       OR (n_1.gender = 'f'
           AND n_1.name LIKE 'A%'))
  AND pi_1.note IS NOT NULL
  AND t_1.production_year BETWEEN 1980 AND 2010
  AND n_1.id = an_1.person_id
  AND n_1.id = pi_1.person_id
  AND ci_1.person_id = n_1.id
  AND t_1.id = ci_1.movie_id
  AND ml_1.linked_movie_id = t_1.id
  AND lt_1.id = ml_1.link_type_id
  AND it_1.id = pi_1.info_type_id
  AND pi_1.person_id = an_1.person_id
  AND pi_1.person_id = ci_1.person_id
  AND an_1.person_id = ci_1.person_id
  AND ci_1.movie_id = ml_1.linked_movie_id
  AND an_2.name IS NOT NULL
  AND (an_2.name LIKE '%a%'
       OR an_2.name LIKE 'A%')
  AND it_2.info ='mini biography'
  AND lt_2.link IN ('references',
                    'referenced in',
                    'features',
                    'featured in')
  AND n_2.name_pcode_cf BETWEEN 'A' AND 'F'
  AND (n_2.gender='m'
       OR (n_2.gender = 'f'
           AND n_2.name LIKE 'A%'))
  AND pi_2.note IS NOT NULL
  AND t_2.production_year BETWEEN 1980 AND 2010
  AND n_2.id = an_2.person_id
  AND n_2.id = pi_2.person_id
  AND ci_2.person_id = n_2.id
  AND t_2.id = ci_2.movie_id
  AND ml_2.linked_movie_id = t_2.id
  AND lt_2.id = ml_2.link_type_id
  AND it_2.id = pi_2.info_type_id
  AND pi_2.person_id = an_2.person_id
  AND pi_2.person_id = ci_2.person_id
  AND an_2.person_id = ci_2.person_id
  AND ci_2.movie_id = ml_2.linked_movie_id
  AND n_1.name = n_2.name
GROUP BY n_1.name,
         n_2.name,
         pi_1.info,
         pi_2.info;