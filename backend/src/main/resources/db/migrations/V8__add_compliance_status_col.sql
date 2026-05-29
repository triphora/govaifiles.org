CREATE TYPE compliance_status AS ENUM ('In compliance', 'Not in compliance', 'Not required');

ALTER TABLE ai_use_cases ADD COLUMN compliance_status compliance_status NOT NULL DEFAULT 'Not in compliance';
UPDATE ai_use_cases SET compliance_status = 'In compliance' WHERE risk_management_compliance_score >= 9;
UPDATE ai_use_cases SET compliance_status = 'Not required' WHERE risk_management_compliance_score = -1;