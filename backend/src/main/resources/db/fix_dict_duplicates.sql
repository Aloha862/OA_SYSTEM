USE oa_management;

DELETE d1
FROM sys_dict_data d1
JOIN sys_dict_data d2
  ON d1.type_code = d2.type_code
 AND d1.dict_value = d2.dict_value
 AND d1.dict_label = d2.dict_label
 AND d1.id > d2.id;

ALTER TABLE sys_dict_data
  ADD UNIQUE KEY uk_type_value_label (type_code, dict_value, dict_label);

