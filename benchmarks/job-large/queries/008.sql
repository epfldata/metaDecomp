SELECT n_1.name AS of_person_1,
       t_1.title AS biography_movie_1,
       n_2.name AS of_person_2,
       t_2.title AS biography_movie_2
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
WHERE an_1.name LIKE '%a%'
  AND it_1.info ='mini biography'
  AND lt_1.link ='features'
  AND n_1.name_pcode_cf LIKE 'D%'
  AND n_1.gender='m'
  AND pi_1.note ='Volker Boehm'
  AND t_1.production_year BETWEEN 1980 AND 1984
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
  AND an_2.name LIKE '%a%'
  AND it_2.info ='mini biography'
  AND lt_2.link ='features'
  AND n_2.name_pcode_cf LIKE 'D%'
  AND n_2.gender='m'
  AND pi_2.note ='Volker Boehm'
  AND t_2.production_year BETWEEN 1980 AND 1984
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
         t_1.title,
         t_2.title;