export const agencyOptions = [
	'Department of Agriculture',
	'Department of Commerce',
	'Department of Defense',
	'Department of Education',
	'Department of Energy',
	'Department of Health and Human Services',
	'Department of Homeland Security',
	'CBP',
	'CISA',
	'CWMD',
	'DHS',
	'FEMA',
	'ICE',
	'MGMT',
	'OHS',
	'TSA',
	'USCG',
	'USCIS',
	'USSS',
	'Department of Housing and Urban Development',
	'Department of Justice',
	'Department of Labor',
	'Department of State',
	'Department of the Interior',
	'Department of the Treasury',
	'Department of Transportation',
	'Department of Veterans Affairs',
	'Commodity Futures Trading Commission',
	'Consumer Financial Protection Bureau',
	'Election Assistance Commission',
	'Environmental Protection Agency',
	'Equal Employment Opportunity Commission',
	'Federal Deposit Insurance Corporation',
	'Federal Energy Regulatory Commission',
	'Federal Housing Finance Agency',
	'Federal Reserve Board of Governors',
	'Federal Trade Commission',
	'General Services Administration',
	'National Aeronautics and Space Administration',
	'National Science Foundation',
	'National Archives and Records Administration',
	'National Credit Union Administration',
	'Office of Personnel Management',
	'National Transportation Safety Board',
	'Pension Benefit Guaranty Corporation',
	'Securities and Exchange Commission',
	'Social Security Administration',
	'Tennessee Valley Authority',
	'United States Agency for International Development',
	'United States Trade and Development Agency'
];

export const fieldGuideGroups = [
	{
		title: 'Explorer Columns',
		items: [
			{
				label: 'Use Case',
				source: 'use_case_name',
				description: 'The reported name of the AI system or workflow. This stays unchanged in the explorer.'
			},
			{
				label: 'Agency',
				source: 'agency / bureau_component',
				description: 'The reporting agency and, when available, the bureau or component responsible for the use case.'
			},
			{
				label: 'Stage',
				source: 'stage_of_development',
				description: 'A shortened display label for the reported lifecycle stage such as Deployed, Pilot, or Retired.'
			},
			{
				label: 'Impact',
				source: 'is_high_impact',
				description: 'A shorter version of the agency-reported high-impact status.'
			},
			{
				label: 'Topic Area',
				source: 'use_case_topic_area',
				description: 'The policy or operational domain where the use case sits.'
			},
			{
				label: 'AI Classification',
				source: 'ai_classification',
				description: 'The reported technical category of AI, shown with a shorter interface label but the same underlying value.'
			}
		]
	},
	{
		title: 'Problem and Purpose',
		items: [
			{
				label: 'Problem Statement',
				source: 'problem_statement',
				description: 'Simplified label for the original question asking what problem the AI is intended to solve.'
			},
			{
				label: 'Expected Benefits',
				source: 'expected_benefits',
				description: 'Shorter label for the reported mission or public benefits.'
			},
			{
				label: 'AI System Outputs',
				source: 'system_outputs',
				description: 'Simplified label for the reported outputs of the system.'
			},
			{
				label: 'Impact Justification',
				source: 'justification',
				description: 'Shown when a use case is marked not high-impact, preserving the agency justification text.'
			}
		]
	},
	{
		title: 'Documentation and Data',
		items: [
			{
				label: 'Operational Date',
				source: 'operational_start_date',
				description: 'Shorter label for the date when the AI became operational or the pilot started.'
			},
			{
				label: 'Development Method',
				source: 'development_source',
				description: 'Simplified label for whether the system came from a vendor, contract, or in-house development.'
			},
			{
				label: 'Vendor Name',
				source: 'vendor_name',
				description: 'The reported vendor name when one exists.'
			},
			{
				label: 'Authorization (ATO)',
				source: 'has_ato',
				description: 'Shortened display label for Authorization to Operate.'
			},
			{
				label: 'System Name',
				source: 'systems_name',
				description: 'The reported system name associated with the use case.'
			},
			{
				label: 'Training and Evaluation Data',
				source: 'training_and_evaluation_data',
				description: 'Shortened display label for the narrative description of training, fine-tuning, and evaluation data.'
			},
			{
				label: 'Federal Data Catalog',
				source: 'federal_data_catalog_link',
				description: 'A direct, simplified label for the reported Federal Data Catalog entry.'
			},
			{
				label: 'PII',
				source: 'involves_pii',
				description: 'Short label for whether the use case involves agency-maintained personally identifiable information.'
			},
			{
				label: 'Privacy Impact Assessment',
				source: 'pia_link',
				description: 'Short label for the publicly available Privacy Impact Assessment link, when reported.'
			},
			{
				label: 'Model Features',
				source: 'demographic_variables_used',
				description: 'A simplified label for demographic variables explicitly used as model features.'
			},
			{
				label: 'Custom Code',
				source: 'includes_custom_code',
				description: 'Shortened label for whether the project includes custom-developed code.'
			},
			{
				label: 'Open Source Code',
				source: 'open_source_code_link',
				description: 'Short label for the public source code link, if one is reported.'
			}
		]
	},
	{
		title: 'Risk Management',
		items: [
			{
				label: 'Pre-deploy Test',
				source: 'pre_deployment_testing_status',
				description: 'Simplified label for the reported pre-deployment testing status.'
			},
			{
				label: 'Impact Assessment',
				source: 'ai_impact_assessment_status',
				description: 'Short label for whether an AI impact assessment was completed.'
			},
			{
				label: 'Potential Impact',
				source: 'potential_impacts_description',
				description: 'The agency narrative describing possible impacts and how they were identified.'
			},
			{
				label: 'Independent Review',
				source: 'independent_review_status',
				description: 'Short label for the reported independent review status.'
			},
			{
				label: 'Ongoing Monitoring',
				source: 'ongoing_monitoring_process',
				description: 'Simplified label for the reported monitoring process covering performance, security, privacy, civil rights, and civil liberties.'
			},
			{
				label: 'Operator Training',
				source: 'operator_training_status',
				description: 'Short label for periodic training for operators.'
			},
			{
				label: 'Fail-safe',
				source: 'fail_safe_status',
				description: 'Short label for whether a fail-safe exists to reduce significant harm.'
			},
			{
				label: 'Appeal Process',
				source: 'appeal_process_status',
				description: 'Short label for whether impacted individuals can appeal the system outcome.'
			},
			{
				label: 'Public Feedback',
				source: 'public_and_user_feedback',
				description: 'Short label for the narrative about consultation and feedback from users or the public.'
			}
		]
	}
];

export const stageOptions = ['Deployed', 'Pilot', 'Pre-deployment', 'Retired'];

export const impactOptions = [
	{ value: '', label: 'All' },
	{ value: 'High-impact', label: 'High Impact' },
	{ value: 'Not high-impact', label: 'Not High Impact' }
];

export const dhsComponents = new Set(['CBP', 'CISA', 'CWMD', 'DHS', 'FEMA', 'ICE', 'MGMT', 'OHS', 'TSA', 'USCG', 'USCIS', 'USSS']);

/**
 * @param {string | null | undefined} value
 * @returns {string}
 */
export function normalizeValue(value) {
	if (value === null || value === undefined) {
		return '[blank]';
	}

	const trimmed = value.trim();
	return trimmed === '' ? '[blank]' : trimmed;
}

/**
 * @param {string | null | undefined} value
 * @returns {boolean}
 */
export function hasValue(value) {
	return normalizeValue(value) !== '[blank]';
}

/**
 * @param {string | null | undefined} value
 * @param {string} [fallback='Not reported']
 * @returns {string}
 */
export function displayValue(value, fallback = 'Not reported') {
	const normalized = normalizeValue(value);
	return normalized === '[blank]' ? fallback : normalized;
}

/**
 * @param {string | null | undefined} value
 * @returns {boolean}
 */
export function isUrl(value) {
	return /^https?:\/\//i.test(displayValue(value, ''));
}

/**
 * @param {string | null | undefined} value
 * @returns {string}
 */
export function stageClass(value) {
	switch (displayValue(value, 'Unknown')) {
		case 'Deployed':
			return 'stage-deployed';
		case 'Pilot':
			return 'stage-pilot';
		case 'Pre-deployment':
			return 'stage-pre';
		case 'Retired':
			return 'stage-retired';
		default:
			return 'stage-unknown';
	}
}

/**
 * @param {string | null | undefined} value
 * @returns {string}
 */
export function impactClass(value) {
	const normalized = displayValue(value, 'Not reported');
	if (normalized === 'High-impact') {
		return 'impact-high';
	}

	if (normalized === 'Not high-impact') {
		return 'impact-low';
	}

	return 'impact-unknown';
}
