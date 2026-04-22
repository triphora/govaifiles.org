ALTER TABLE ai_use_cases ADD COLUMN agency_importance INT DEFAULT 0;
UPDATE ai_use_cases
    SET agency_importance = 1
    WHERE canonical_agency ILIKE 'Department of%';
UPDATE ai_use_cases
    SET agency_importance = 2
    WHERE canonical_agency = 'Department of Justice';
UPDATE ai_use_cases
    SET agency_importance = 3
    WHERE canonical_agency = 'Department of Homeland Security';
UPDATE ai_use_cases
    SET agency_importance = 4
    WHERE canonical_agency = 'Department of Homeland Security'
      AND canonical_sub_agency = 'U.S. Immigration and Customs Enforcement';