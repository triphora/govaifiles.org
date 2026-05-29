UPDATE ai_use_cases
    SET risk_management_compliance_score = risk_management_compliance_score - 1
    WHERE potential_impacts_identified !~ '[Pp]rogress';

UPDATE ai_use_cases
    SET risk_management_compliance_score = risk_management_compliance_score + 1
    WHERE potential_impacts_identified !~ 'In-Progress';

UPDATE ai_use_cases SET risk_management_compliance_score = -1
WHERE stage_of_development != 'Deployed' OR high_impact_status != 'high_impact' OR data_year = '2024'
OR high_impact_status IS NULL OR stage_of_development IS NULL;