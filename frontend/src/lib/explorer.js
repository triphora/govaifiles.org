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
					'The reported name of the AI system or workflow.'
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
					'The reported lifecycle stage such as Deployed, Pilot, Pre-Deployment, or Retired.'
			},
			{
				label: 'Impact',
				source: 'high_impact_status',
				description: 'The status of whether the use case is high-impact or not.'
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
					'The reported technical category of AI, such as generative AI, natural language processing, or classical/predictive machine learning.'
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
					'What problem is the AI intended to solve?'
			},
			{
				label: 'Expected Benefits',
				source: 'expected_benefits',
				description:
					'What are the expected benefits and positive outcomes from the AI for an agency’s mission and/or the general public?'
			},
			{
				label: 'AI System Outputs',
				source: 'system_outputs',
				description: 'Describe the AI system’s outputs.'
			},
			{
				label: 'Impact Justification',
				source: 'justification',
				description:
					'If the entry is presumed high-impact but determined not high-impact, the agency provides a justification as to why it is not high-impact.'
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
					'Date when AI use case became operational or the pilot’s start date'
			},
			{
				label: 'Development Method',
				source: 'development_source',
				description:
					'Was the system involved in this use case purchased from a vendor or developed under contract(s) or in-house?'
			},
			{
				label: 'Vendor Name',
				source: 'vendor_name',
				description: 'The reported vendor name when one exists.'
			},
			{
				label: 'Authorization (ATO)',
				source: 'has_ato',
				description: 'Does this AI use case have an associated Authorization to Operate (ATO)?'
			},
			{
				label: 'System Name',
				source: 'systems_name',
				description:
					'The reported system name associated with the use case.'
			},
			{
				label: 'Training and Evaluation Data',
				source: 'training_and_evaluation_data',
				description:
					'Describe any data used to train, fine-tune, and/or evaluate performance of the model(s) used in this use case.'
			},
			{
				label: 'Federal Data Catalog',
				source: 'federal_data_catalog_link',
				description:
					'If the data is required to be publicly disclosed as an open government data asset, provide a link to the entry on the Federal Data Catalog.'
			},
			{
				label: 'PII',
				source: 'involves_pii',
				description:
					'Does this AI use case involve personally identifiable information (PII) that is maintained by the agency?'
			},
			{
				label: 'Privacy Impact Assessment',
				source: 'pia_link',
				description:
					'If publicly available, provide the link to the AI use case’s associated Privacy Impact Assessment (PIA).'
			},
			{
				label: 'Demographic Variables',
				source: 'demographic_variables_used',
				description:
					'Which, if any, demographic variables does the AI use case explicitly use as model features?'
			},
			{
				label: 'Custom Code',
				source: 'includes_custom_code',
				description:
					'Does this project include custom-developed code?'
			},
			{
				label: 'Open Source Code',
				source: 'open_source_code_link',
				description:
					'If the code is open source, provide the link for the publicly available source code.'
			}
		]
	},
	{
		title: 'Risk Management',
		items: [
			{
				label: 'Compliance Score',
				source: 'risk_management_compliance_score',
				description:
					'The total count of Risk Management values that are not fully completed. For instance, entries that are "in progress" or not reported are not considered to be complete.'
			},
			{
				label: 'Pre-deploy Test',
				source: 'pre_deployment_testing_status',
				description:
					'Has pre-deployment testing been conducted for this AI use case?' +
					'<br/><br/><i>Practice: Complete AI Impact Assessment</i>'
			},
			{
				label: 'Impact Assessment',
				source: 'ai_impact_assessment_status',
				description:
					'Has an AI impact assessment been completed for this AI use case?' +
					'<br/><br/><i>Practice: Complete AI Impact Assessment</i>'
			},
			{
				label: 'Potential Impact',
				source: 'potential_impacts_description',
				description:
					'What are the potential impacts of using the AI for this particular use case and how were they identified?' +
					'<br/><br/><i>Subpractice: Complete AI Impact Assessment</i>'
			},
			{
				label: 'Independent Review',
				source: 'independent_review_status',
				description:
					'Has as independent review of the AI use case been conducted?' +
					'<br/><br/><i>Subpractice: Complete AI Impact Assessment</i>'
			},
			{
				label: 'Ongoing Monitoring',
				source: 'ongoing_monitoring_process',
				description:
					'Is there a process to conduct ongoing monitoring to identify any adverse impacts to the performance and security of the AI functionality, as well as to privacy, civil rights, and civil liberties?' +
					'<br/><br/><i>Practice: Conduct Ongoing Monitoring for Performance and Potential Adverse Impacts</i>'
			},
			{
				label: 'Operator Training',
				source: 'operator_training_status',
				description: 'Has the agency established sufficient and periodic training for operators of the AI to interpret and act on the its output and managed associated risks?' +
					'<br/><br/><i>Practice: Ensure Adequate Human Training and Assessment</i>'
			},
			{
				label: 'Fail-safe',
				source: 'fail_safe_status',
				description:
					'Does this AI use case have an appropriate fail-safe that minimizes the risk of significant harm?' +
					'<br/><br/><i>Practice: Provide Additional Human Oversight, Intervention, and Accountability</i>'
			},
			{
				label: 'Appeal Process',
				source: 'appeal_process_status',
				description:
					'Is there an established appeal process in the event that an impacted individual would like to appeal or contest the AI system’s outcome?' +
					'<br/><br/><i>Practice: Offer Consistent Remedies or Appeals</i>'
			},
			{
				label: 'Public Feedback',
				source: 'public_and_user_feedback',
				description:
					'What steps has the agency taken to consult and incorporate feedback from end users of this AI use case and the public?' +
					'<br/><br/><i>Practice: Consult and Incorporate Feedback from End Users and the Public</i>'
			}
		]
	},
	{
		title: 'Related Disclosures',
		items: [
			{
				label: 'Related Systems of Notice Records',
				source: 'data_links:sorn',
				description:
					'Links to Systems of Notice Records that either disclose the same record under a different scheme or that use the inventory record as a data-dependency.'
			},
			{
				label: 'Related Information Collection Requests',
				source: 'data_links:pra',
				description:
					'Links to Information Collection Requests under the Paperwork Reduction Act that either disclose the same record under a different scheme or that use the inventory record as a data-dependency.'
			}
		]
	}
];

export const stageOptions = uniqueColumnValues(filterOptionRows, 'stage_of_development');

export const impactOptions = [
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
		slugifyFragment(record.data_year.toString()),
		slugifyFragment(record.canonical_agency ?? record.agency),
		slugifyFragment(primaryId),
		slugifyFragment(record.use_case_name)
	].join('-');
}

/**
 * @param {Record<string, string | null | undefined>} record
 * @param {InformationCollectionRequestRecord[]} icrData
 * @returns {InformationCollectionRequestRecord[]}
 */
export function getIcrsForInventoryRecord(record, icrData) {
	let dataLinks = record.data_links;
	if (!dataLinks || (dataLinks && dataLinks.length <= 0)) {
		return [];
	}

	let icrs = [];
	for (let entry of dataLinks) {
		if (entry.startsWith("pra:")) {
			const icrEntry = icrData.filter((record) =>
				record.referenceNumber === entry.substring(4));
			if (icrEntry.length === 1) {
				icrs.push(icrEntry[0])
			}
		}
	}
	return icrs;
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
