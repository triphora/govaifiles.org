import filterOptionsCsv from '../../GOVAI_Filter_Options.csv?raw';

const agencyAliases = {
	'Board of Governors of the Federal Reserve System': 'Federal Reserve Board of Governors',
	'U.S. Agency for Global Media': 'United States Agency for Global Media',
	'U.S. Agency for International Development': 'United States Agency for International Development',
	'U.S. Commission on Civil Rights': 'United States Commission on Civil Rights',
	'U.S. Election Assistance Commission': 'Election Assistance Commission'
};

const impactOptionLabels = {
	high_impact: 'High Impact',
	not_high_impact: 'Not High Impact',
	presumed_high_impact_not_high_impact: 'Presumed High Impact / Not High Impact'
};

/** @typedef {Record<string, string>} FilterOptionRow */

/**
 * @param {string} row
 * @returns {string[]}
 */
function parseCsvRow(row) {
	const values = [];
	let currentValue = '';
	let inQuotes = false;

	for (let index = 0; index < row.length; index += 1) {
		const character = row[index];

		if (character === '"') {
			if (inQuotes && row[index + 1] === '"') {
				currentValue += '"';
				index += 1;
			} else {
				inQuotes = !inQuotes;
			}
			continue;
		}

		if (character === ',' && !inQuotes) {
			values.push(currentValue);
			currentValue = '';
			continue;
		}

		currentValue += character;
	}

	values.push(currentValue);
	return values;
}

/**
 * @param {string} source
 * @returns {FilterOptionRow[]}
 */
function parseFilterOptionsCsv(source) {
	const [headerRow, ...dataRows] = source.trim().split(/\r?\n/);
	const headers = parseCsvRow(headerRow);

	return dataRows
		.map((row) => parseCsvRow(row))
		.filter((row) => row.some((value) => value.trim() !== ''))
		.map((row) =>
			Object.fromEntries(headers.map((header, index) => [header, row[index]?.trim() ?? '']))
		);
}

/**
 * @param {FilterOptionRow[]} rows
 * @param {string} key
 * @param {(value: string) => string} [transform]
 * @returns {string[]}
 */
function uniqueColumnValues(rows, key, transform = (value) => value) {
	return [...new Set(rows.map((row) => transform(row[key] ?? '')).filter(Boolean))].sort((a, b) =>
		a.localeCompare(b)
	);
}

const filterOptionRows = parseFilterOptionsCsv(filterOptionsCsv);

export const topLevelAgencyOptions = [
	...uniqueColumnValues(
		filterOptionRows,
		'canonical_agency',
		(value) => agencyAliases[/** @type {keyof typeof agencyAliases} */ (value)] ?? value
	)
];

export const bureauOptionsByAgency = {
	'Department of Homeland Security': [
		'CBP',
		'CISA',
		'CWMD',
		'FEMA',
		'ICE',
		'MGMT',
		'OHS',
		'TSA',
		'USCG',
		'USCIS',
		'USSS'
	]
};

export const agencyOptions = topLevelAgencyOptions;

export const topicOptions = uniqueColumnValues(filterOptionRows, 'use_case_topic_area');

export const aiClassificationOptions = uniqueColumnValues(filterOptionRows, 'ai_classification');

export const complianceOptions = ['In compliance', 'Not in compliance', 'Not required'];

export const fieldGuideGroups = [
	{
		title: 'Explorer Columns',
		items: [
			{
				label: 'Use Case',
				source: 'use_case_name',
				description:
					'The reported name of the AI system or workflow. This stays unchanged in the explorer.'
			},
			{
				label: 'Agency',
				source: 'agency / bureau_component',
				description:
					'The reporting agency and, when available, the bureau or component responsible for the use case.'
			},
			{
				label: 'Stage',
				source: 'stage_of_development',
				description:
					'A shortened display label for the reported lifecycle stage such as Deployed, Pilot, or Retired.'
			},
			{
				label: 'Impact',
				source: 'high_impact_status',
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
				description:
					'The reported technical category of AI, shown with a shorter interface label but the same underlying value.'
			}
		]
	},
	{
		title: 'Problem and Purpose',
		items: [
			{
				label: 'Problem Statement',
				source: 'problem_statement',
				description:
					'Simplified label for the original question asking what problem the AI is intended to solve.'
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
				description:
					'Shown when a use case is marked not high-impact, preserving the agency justification text.'
			}
		]
	},
	{
		title: 'Documentation and Data',
		items: [
			{
				label: 'Operational Date',
				source: 'operational_start_date',
				description:
					'Shorter label for the date when the AI became operational or the pilot started.'
			},
			{
				label: 'Development Method',
				source: 'development_source',
				description:
					'Simplified label for whether the system came from a vendor, contract, or in-house development.'
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
				description:
					'Shortened display label for the narrative description of training, fine-tuning, and evaluation data.'
			},
			{
				label: 'Federal Data Catalog',
				source: 'federal_data_catalog_link',
				description: 'A direct, simplified label for the reported Federal Data Catalog entry.'
			},
			{
				label: 'PII',
				source: 'involves_pii',
				description:
					'Short label for whether the use case involves agency-maintained personally identifiable information.'
			},
			{
				label: 'Privacy Impact Assessment',
				source: 'pia_link',
				description:
					'Short label for the publicly available Privacy Impact Assessment link, when reported.'
			},
			{
				label: 'Model Features',
				source: 'demographic_variables_used',
				description:
					'A simplified label for demographic variables explicitly used as model features.'
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
				description:
					'The agency narrative describing possible impacts and how they were identified.'
			},
			{
				label: 'Independent Review',
				source: 'independent_review_status',
				description: 'Short label for the reported independent review status.'
			},
			{
				label: 'Ongoing Monitoring',
				source: 'ongoing_monitoring_process',
				description:
					'Simplified label for the reported monitoring process covering performance, security, privacy, civil rights, and civil liberties.'
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
				description:
					'Short label for the narrative about consultation and feedback from users or the public.'
			}
		]
	}
];

export const stageOptions = uniqueColumnValues(filterOptionRows, 'stage_of_development');

export const impactOptions = [
	{ value: '', label: 'All' },
	...uniqueColumnValues(filterOptionRows, 'high_impact_status').map((value) => ({
		value,
		label:
			impactOptionLabels[/** @type {keyof typeof impactOptionLabels} */ (value)] ??
			value.replace(/_/g, ' ')
	}))
];

export const dhsComponents = new Set(bureauOptionsByAgency['Department of Homeland Security']);

/**
 * @param {string | null | undefined} value
 * @returns {string}
 */
export function normalizeValue(value) {
	if (value === null || value === undefined) {
		return '[blank]';
	}

	const trimmed = value;
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
 * @returns {string | null}
 */
function extractUseCaseIdFromSeriesDump(value) {
	if (!value || (!value.includes('dtype:') && !value.includes('Use Case ID'))) {
		return null;
	}

	for (const line of value.split(/\r?\n/)) {
		let candidate = line.trim();
		if (!candidate || /^Name:/i.test(candidate) || /^dtype:/i.test(candidate)) {
			continue;
		}

		candidate = candidate
			.replace(/^Use Case ID\s+/i, '')
			.replace(/^\d+\s+/, '')
			.trim();
		if (candidate && !/^(nan|none|null)$/i.test(candidate)) {
			return candidate;
		}
	}

	return null;
}

/**
 * @param {string | null | undefined} value
 * @returns {string}
 */
function slugifyFragment(value) {
	return (
		normalizeValue(value)
			.toLowerCase()
			.replace(/[^a-z0-9]+/g, '-')
			.replace(/^-+|-+$/g, '') || 'item'
	);
}

/**
 * @param {string | null | undefined} value
 * @param {string} [fallback='No ID reported']
 * @returns {string}
 */
export function displayUseCaseId(value, fallback = 'No ID reported') {
	const normalized = normalizeValue(value);
	if (normalized === '[blank]') {
		return fallback;
	}

	return extractUseCaseIdFromSeriesDump(normalized) ?? normalized;
}

/**
 * @param {Record<string, string | null | undefined>} record
 * @returns {string}
 */
export function useCaseAnchorId(record) {
	const useCaseId =
		extractUseCaseIdFromSeriesDump(record.use_case_id) ?? normalizeValue(record.use_case_id);
	const primaryId = useCaseId !== '[blank]' ? useCaseId : record.use_case_name;

	return [
		'use-case',
		slugifyFragment(record.data_year),
		slugifyFragment(record.canonical_agency ?? record.agency),
		slugifyFragment(primaryId),
		slugifyFragment(record.use_case_name)
	].join('-');
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
		case 'Pre_deployment':
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
	if (normalized === 'high_impact') {
		return 'impact-high';
	}

	if (normalized === 'not_high_impact') {
		return 'impact-low';
	}

	return 'impact-unknown';
}

/**
 * @param {string | null | undefined} value
 * @returns {number}
 */
export function parseComplianceScore(value) {
	const parsed = Number.parseInt(value ?? '', 10);
	return Number.isNaN(parsed) ? -1 : parsed;
}

/**
 * @param {string | null | undefined} stage
 * @param {string | null | undefined} impact
 * @param {number} score
 * @returns {'In compliance' | 'Not in compliance' | 'Not required'}
 */
export function getComplianceStatus(stage, impact, score) {
	const isDeployed = displayValue(stage, 'Unknown') === 'Deployed';
	const isHighRisk = displayValue(impact, 'Not reported') === 'high_impact';

	if (!isDeployed || !isHighRisk || score < 0) {
		return 'Not required';
	}

	return score >= 9 ? 'In compliance' : 'Not in compliance';
}

/**
 * @param {string} status
 * @returns {string}
 */
export function getComplianceStatusClass(status) {
	if (status === 'In compliance') {
		return 'compliance-status--good';
	}

	if (status === 'Not in compliance') {
		return 'compliance-status--warn';
	}

	return 'compliance-status--neutral';
}
