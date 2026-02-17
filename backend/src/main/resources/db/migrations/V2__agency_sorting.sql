ALTER TABLE ai_use_case_2025 ADD COLUMN agency_importance INT DEFAULT 0;
UPDATE ai_use_case_2025 SET agency_importance = 1 WHERE agency ILIKE 'Department Of%';
UPDATE ai_use_case_2025 SET agency_importance = 2 WHERE agency = 'Department Of Justice';
UPDATE ai_use_case_2025 SET agency_importance = 3 WHERE agency = 'Department Of Homeland Security';
UPDATE ai_use_case_2025 SET agency_importance = 4 WHERE agency = 'Department Of Homeland Security' AND bureau_component = 'ICE';