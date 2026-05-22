<script lang="ts">
	import { createEventDispatcher } from 'svelte';
	import {
		displayUseCaseId,
		displayValue,
		getComplianceStatus,
		getComplianceStatusClass,
		hasValue,
		impactClass,
		isUrl,
		parseComplianceScore,
		stageClass
	} from '$lib/explorer';

	type UseCaseRecord = Record<string, string | undefined | null>;

	export let result: UseCaseRecord;
	export let index: number;
	export let anchorId: string;

	const dispatch = createEventDispatcher<{
		share: { anchorId: string; title: string };
	}>();

	let expanded = index === 1;
	let sectionTwoRequirements = false;
	let sectionThreeRequirements = false;
	let sectionFiveRequirements = false;
	let summaryFields: Array<{ label: string; value: string | undefined | null; show: boolean }> = [];
	let documentationFields: Array<{
		label: string;
		value: string | undefined | null;
		show: boolean;
	}> = [];
	let dataFields: Array<{ label: string; value: string | undefined | null; show: boolean }> = [];
	let riskFields: Array<{
		label: string;
		value: string | undefined | null;
		show: boolean;
		wide?: boolean;
	}> = [];
	let complianceScore = -1;
	let complianceStatus = 'Not required';
	let complianceStatusClass = 'compliance-status--neutral';
	let showComplianceGauge = false;
	let complianceGaugeDegrees = 0;

	$: sectionTwoRequirements =
		result.stage_of_development !== 'Retired' && parseInt(result.data_year ?? '', 10) >= 2025;
	$: sectionThreeRequirements = ['Unknown', 'Pilot', 'Deployed'].includes(
		result.stage_of_development || ''
	);
	$: sectionFiveRequirements =
		['Unknown', 'Deployed'].includes(result.stage_of_development || '') &&
		['high_impact', '[blank]', ''].includes(result.high_impact_status || '');

	$: summaryFields = [
		{
			label: 'Purpose Statement',
			value: result.purpose_and_benefits,
			show: sectionTwoRequirements || hasValue(result.problem_statement)
		},
		{
			label: 'Expected Benefits',
			value: result.expected_benefits,
			show: sectionTwoRequirements || hasValue(result.expected_benefits)
		},
		{
			label: 'AI System Outputs',
			value: result.system_outputs,
			show: sectionTwoRequirements || hasValue(result.system_outputs)
		}
	];

	$: documentationFields = [
		{
			label: 'Operational Date',
			value: result.operational_start_date,
			show: sectionThreeRequirements || hasValue(result.operational_start_date)
		},
		{
			label: 'Development Source Type',
			value: result.development_source_type,
			show: sectionThreeRequirements || hasValue(result.development_source)
		},
		{
			label: 'Vendor Name(s)',
			value: result.vendor_name,
			show: sectionThreeRequirements || hasValue(result.vendor_name)
		},
		{
			label: 'Authorization (ATO)',
			value: result.has_ato,
			show: sectionThreeRequirements || hasValue(result.has_ato)
		}
	];

	$: dataFields = [
		{
			label: 'PII',
			value: result.uses_pii,
			show: sectionThreeRequirements || hasValue(result.involves_pii)
		},
		{
			label: 'Custom Code',
			value: result.has_custom_code,
			show: sectionThreeRequirements || hasValue(result.includes_custom_code)
		},
		{
			label: 'Federal Data Catalog',
			value: result.federal_data_catalog_link,
			show: sectionThreeRequirements || hasValue(result.federal_data_catalog_link)
		},
		{
			label: 'Privacy Impact Assessment',
			value: result.pia_link,
			show: sectionThreeRequirements || hasValue(result.pia_link)
		},
		{
			label: 'Demographic Variables',
			value: result.demographic_variables_used,
			show: sectionThreeRequirements || hasValue(result.demographic_variables_used)
		},
		{
			label: 'Open Source Code',
			value: result.open_source_code_link,
			show: sectionThreeRequirements || hasValue(result.open_source_code_link)
		},
		{
			label: 'Training and Evaluation Data',
			value: result.training_data_description,
			show: sectionThreeRequirements || hasValue(result.training_and_evaluation_data)
		}
	];

	$: riskFields = [
		{
			label: 'Pre-deploy Test',
			value: result.pre_deployment_testing_conducted,
			show: sectionFiveRequirements || hasValue(result.pre_deployment_testing_status)
		},
		{
			label: 'Impact Assessment',
			value: result.ai_impact_assessment_completed,
			show: sectionFiveRequirements || hasValue(result.ai_impact_assessment_status)
		},
		{
			label: 'Independent Review',
			value: result.independent_review_conducted,
			show: sectionFiveRequirements || hasValue(result.independent_review_status)
		},
		{
			label: 'Fail-safe',
			value: result.failsafe_in_place,
			show: sectionFiveRequirements || hasValue(result.fail_safe_status)
		},
		{
			label: 'Potential Impact',
			value: result.potential_impacts_identified,
			show: sectionFiveRequirements || hasValue(result.potential_impacts_description),
			wide: true
		},
		{
			label: 'Ongoing Monitoring',
			value: result.ongoing_monitoring_established,
			show: sectionFiveRequirements || hasValue(result.ongoing_monitoring_process),
			wide: true
		},
		{
			label: 'Operator Training',
			value: result.operator_training_established,
			show: sectionFiveRequirements || hasValue(result.operator_training_status),
			wide: true
		},
		{
			label: 'Appeal Process',
			value: result.appeal_process_available,
			show: sectionFiveRequirements || hasValue(result.appeal_process_status),
			wide: true
		},
		{
			label: 'Public Feedback',
			value: result.user_feedback_steps,
			show: sectionFiveRequirements || hasValue(result.public_and_user_feedback),
			wide: true
		}
	];

	$: complianceScore = parseComplianceScore(result.risk_management_compliance_score);
	$: complianceStatus = getComplianceStatus(
		result.stage_of_development,
		result.high_impact_status,
		complianceScore
	);
	$: complianceStatusClass = getComplianceStatusClass(complianceStatus);
	$: showComplianceGauge = result.data_year >= 2025 && complianceScore >= 0 && complianceStatus !== 'Not required';
	$: complianceGaugeDegrees = (Math.max(0, Math.min(complianceScore, 9)) / 9) * 360;

	function impactLabel(value: string | undefined | null) {
		const normalized = displayValue(value, 'Not reported');
		if (normalized === 'high_impact') {
			return 'High Impact';
		}

		if (normalized === 'not_high_impact' || normalized === 'presumed_high_impact_not_high_impact') {
			return 'Not High Impact';
		}

		return normalized;
	}

	function stageLabel(value: string | undefined | null) {
		return displayValue(value, 'Unknown');
	}

	function agencyLabel() {
		return [result.bureau_component, result.canonical_agency].filter(Boolean).join(' · ');
	}

	function renderValue(value: string | undefined | null) {
		return displayValue(value);
	}

	function shareRecord() {
		dispatch('share', {
			anchorId,
			title: displayValue(result.use_case_name, 'this use case')
		});
	}
</script>

<article id={anchorId} class:expanded class="record-card">
	<div class="record-summary-shell">
		<button
			type="button"
			class="record-summary"
			on:click={() => (expanded = !expanded)}
			aria-expanded={expanded}
		>
			<div class="record-summary__lead">
				<div class="record-index">{index}</div>
				<div>
					<h3>{result.use_case_name}</h3>
					<p class="record-meta">
						{displayUseCaseId(result.use_case_id, 'No ID reported')}
						{#if hasValue(result.problem_statement)}
							<span
								>{displayValue(result.problem_statement).slice(0, 150)}{displayValue(
									result.problem_statement
								).length > 150
									? '...'
									: ''}</span
							>
						{/if}
					</p>
					<div class="record-compliance">
						<div class="record-compliance__copy">
							<span>Compliance</span>
							<p>
								<strong class={`compliance-status ${complianceStatusClass}`}
									>{complianceStatus}</strong
								>
							</p>
						</div>
						{#if showComplianceGauge}
							<div
								class={`compliance-gauge ${complianceStatus === 'In compliance' ? 'compliance-gauge--good' : 'compliance-gauge--warn'}`}
								style={`--gauge-deg: ${complianceGaugeDegrees}deg;`}
								aria-label={`Risk management compliance score ${complianceScore} out of 9`}
							>
								<div class="compliance-gauge__inner">
									<strong>{complianceScore}</strong>
									<span>/9</span>
								</div>
							</div>
						{/if}
					</div>
				</div>
			</div>

			<div class="record-summary__cols">
				<div class="record-summary__item">
					<span>Agency</span>
					<p>{agencyLabel() || 'Not reported'}</p>
				</div>
				<div class="record-summary__item">
					<span>Stage</span>
					<p>
						<span class={`stage-pill ${stageClass(result.stage_of_development)}`}
							>{stageLabel(result.stage_of_development)}</span
						>
					</p>
				</div>
				<div class="record-summary__item">
					<span>Impact</span>
					<p>
						<span class={`impact-pill ${impactClass(result.high_impact_status)}`}
							>{impactLabel(result.high_impact_status)}</span
						>
					</p>
				</div>
				<div class="record-summary__item">
					<span>Topic Area</span>
					<p>{renderValue(result.use_case_topic_area)}</p>
				</div>
				<div class="record-summary__item">
					<span>AI Classification</span>
					<p>{renderValue(result.ai_classification)}</p>
				</div>
			</div>

			<span class="record-chevron" aria-hidden="true">›</span>
		</button>

		<button
			type="button"
			class="record-share"
			on:click={shareRecord}
			aria-label={`Share ${displayValue(result.use_case_name, 'this use case')}`}
		>
			Share
		</button>
	</div>

	{#if expanded}
		<div class="record-detail">
			<div class="record-detail__columns">
				<section class="record-section">
					<p class="record-section__label">Problem and Purpose</p>

					{#if sectionTwoRequirements || hasValue(result.use_case_topic_area)}
						<div class="record-field">
							<h4>Topic Area</h4>
							<div class="record-richtext {hasValue(result.use_case_topic_area) ? '' : 'is-empty'}">
								{renderValue(result.use_case_topic_area)}
							</div>
						</div>
					{/if}

					{#if sectionTwoRequirements || hasValue(result.ai_classification)}
						<div class="record-field">
							<h4>AI Classification</h4>
							<div class="record-richtext {hasValue(result.ai_classification) ? '' : 'is-empty'}">
								{renderValue(result.ai_classification)}
							</div>
						</div>
					{/if}

					{#each summaryFields as field}
						{#if field.show}
							<div class="record-field">
								<h4>{field.label}</h4>
								<div class={`record-richtext ${hasValue(field.value) ? '' : 'is-empty'}`}>
									{renderValue(field.value)}
								</div>
							</div>
						{/if}
					{/each}
				</section>

				<section class="record-section">
					<p class="record-section__label">Documentation and Data</p>

					<div class="record-doc-grid">
						{#each documentationFields as field}
							{#if field.show}
								<div class="record-key-value">
									<h4>{field.label}</h4>
									<div class:blank={!hasValue(field.value)}>{renderValue(field.value)}</div>
								</div>
							{/if}
						{/each}
					</div>

					{#if sectionThreeRequirements || hasValue(result.system_names)}
						<div class="record-field record-field--spaced">
							<h4>System Names</h4>
							<div class={`record-richtext ${hasValue(result.system_names) ? '' : 'is-empty'}`}>
								{renderValue(result.system_names)}
							</div>
						</div>
					{/if}

					<div class="record-data-list">
						{#each dataFields as field}
							{#if field.show}
								<div class="record-key-value">
									<h4>{field.label}</h4>
									{#if isUrl(field.value)}
										<a href={displayValue(field.value)} target="_blank" rel="noreferrer"
											>{displayValue(field.value)}</a
										>
									{:else}
										<div class:blank={!hasValue(field.value)}>{renderValue(field.value)}</div>
									{/if}
								</div>
							{/if}
						{/each}
					</div>
				</section>
			</div>

			{#if riskFields.some((field) => field.show)}
				<section class="record-section record-section--risk">
					<div class="record-section__header">
						<p class="record-section__label">Risk Management</p>
						{#if complianceScore >= 0}
							<div class="record-section__meta-group">
								<p class="record-section__meta">Number of True:</p>
								<div
									class={`compliance-gauge compliance-gauge--compact ${complianceStatus === 'In compliance' ? 'compliance-gauge--good' : 'compliance-gauge--warn'}`}
									style={`--gauge-deg: ${complianceGaugeDegrees}deg;`}
									aria-label={`Risk management compliance score ${complianceScore} out of 9`}
								>
									<div class="compliance-gauge__inner">
										<strong>{complianceScore}</strong>
										<span>/9</span>
									</div>
								</div>
							</div>
						{/if}
					</div>
					<div class="record-risk-grid">
						{#each riskFields as field}
							{#if field.show}
								<div class:wide={field.wide} class="record-key-value">
									<h4>{field.label}</h4>
									<div class:blank={!hasValue(field.value)}>{renderValue(field.value)}</div>
								</div>
							{/if}
						{/each}
					</div>
				</section>
			{/if}
		</div>
	{/if}
</article>

<style>
	.record-card {
		border-radius: var(--radius-xl);
		background: var(--surface-card);
		border: 1px solid rgba(167, 190, 180, 0.55);
		overflow: hidden;
		box-shadow: var(--shadow-soft);
		scroll-margin-top: 100px;
	}

	.record-summary-shell {
		display: grid;
		grid-template-columns: minmax(0, 1fr) auto;
		gap: 12px;
		padding: 18px;
		align-items: start;
	}

	.record-summary {
		width: 100%;
		display: grid;
		grid-template-columns:
			minmax(0, 2.35fr) minmax(140px, 1.1fr) minmax(104px, 0.7fr) minmax(110px, 0.8fr)
			minmax(120px, 0.95fr) minmax(160px, 1.2fr) 24px;
		gap: 12px;
		align-items: start;
		padding: 0;
		color: inherit;
		text-align: left;
	}

	.record-share {
		align-self: center;
		padding: 10px 14px;
		border-radius: 999px;
		border: 1px solid var(--line);
		background: var(--surface);
		color: var(--ink-soft);
		font-family: var(--font-mono);
		font-size: 0.72rem;
		letter-spacing: 0.12em;
		text-transform: uppercase;
		transition:
			transform 0.16s ease,
			border-color 0.16s ease,
			background-color 0.16s ease,
			color 0.16s ease;
	}

	.record-share:hover {
		transform: translateY(-1px);
		border-color: var(--brand);
		color: var(--brand-dark);
	}

	.record-summary__lead {
		display: flex;
		gap: 12px;
		min-width: 0;
	}

	.record-index {
		width: 28px;
		height: 28px;
		border-radius: 999px;
		background: rgba(255, 255, 255, 0.92);
		font-family: var(--font-mono);
		font-size: 0.8rem;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		color: var(--ink-muted);
		flex-shrink: 0;
	}

	h3 {
		margin: 0;
		font-size: 1.08rem;
		line-height: 1.35;
	}

	.record-meta,
	.record-summary__cols p {
		margin: 6px 0 0;
		font-size: 0.94rem;
		line-height: 1.5;
		color: var(--ink-soft);
	}

	.record-summary__item {
		display: grid;
		gap: 6px;
	}

	.record-summary__item > span {
		display: none;
		font-family: var(--font-mono);
		font-size: 0.68rem;
		letter-spacing: 0.14em;
		text-transform: uppercase;
		color: var(--ink-muted);
	}

	.record-meta {
		display: grid;
		gap: 4px;
	}

	.record-compliance {
		display: flex;
		align-items: center;
		gap: 14px;
		margin-top: 14px;
		flex-wrap: wrap;
	}

	.record-compliance__copy {
		display: grid;
		gap: 4px;
	}

	.record-compliance__copy > span {
		font-family: var(--font-mono);
		font-size: 0.68rem;
		letter-spacing: 0.14em;
		text-transform: uppercase;
		color: var(--ink-muted);
	}

	.record-compliance__copy p {
		margin: 0;
	}

	.compliance-status {
		display: inline-flex;
		align-items: center;
		padding: 7px 11px;
		border-radius: 999px;
		font-size: 0.8rem;
		font-family: var(--font-mono);
		font-weight: 600;
	}

	.compliance-status--good {
		background: #dcefe5;
		color: #2f6f54;
	}

	.compliance-status--warn {
		background: #f7e5d9;
		color: #9a5c21;
	}

	.compliance-status--neutral {
		background: #edf1ec;
		color: #66756d;
	}

	.compliance-gauge {
		--gauge-deg: 0deg;
		--gauge-fill: #8a9f92;
		position: relative;
		width: 68px;
		height: 68px;
		border-radius: 50%;
		background:
			radial-gradient(circle at center, rgba(248, 250, 246, 0.98) 0 58%, transparent 59%),
			conic-gradient(
				var(--gauge-fill) 0deg var(--gauge-deg),
				rgba(184, 197, 190, 0.3) var(--gauge-deg) 360deg
			);
		box-shadow: inset 0 0 0 1px rgba(167, 190, 180, 0.35);
		flex-shrink: 0;
	}

	.compliance-gauge--good {
		--gauge-fill: #4b8c69;
	}

	.compliance-gauge--warn {
		--gauge-fill: #c18244;
	}

	.compliance-gauge__inner {
		position: absolute;
		inset: 0;
		display: grid;
		place-items: center;
		align-content: center;
		gap: 1px;
		text-align: center;
	}

	.compliance-gauge__inner strong {
		font-size: 1rem;
		line-height: 1;
		color: var(--ink);
	}

	.compliance-gauge__inner span {
		font-family: var(--font-mono);
		font-size: 0.62rem;
		letter-spacing: 0.08em;
		text-transform: uppercase;
		color: var(--ink-muted);
	}

	.record-summary__cols {
		display: contents;
	}

	.record-chevron {
		align-self: center;
		justify-self: end;
		font-size: 1.8rem;
		color: var(--brand-dark);
		transform: rotate(0deg);
		transition: transform 0.16s ease;
	}

	.expanded .record-chevron {
		transform: rotate(90deg);
	}

	.stage-pill,
	.impact-pill {
		display: inline-flex;
		align-items: center;
		border-radius: 999px;
		padding: 6px 10px;
		font-size: 0.8rem;
		font-family: var(--font-mono);
	}

	.stage-deployed {
		background: #dcefe5;
		color: #2f6f54;
	}

	.stage-pilot {
		background: #e8edf6;
		color: #476280;
	}

	.stage-pre {
		background: var(--warning-pale);
		color: var(--warning);
	}

	.stage-retired,
	.stage-unknown {
		background: #ecefea;
		color: #6b7772;
	}

	.impact-high {
		background: var(--danger-pale);
		color: var(--danger);
	}

	.impact-low {
		background: #edf3ef;
		color: #5d7168;
	}

	.impact-unknown {
		background: #f1f2ee;
		color: var(--ink-muted);
	}

	.record-detail {
		border-top: 1px solid rgba(167, 190, 180, 0.6);
		padding: 0 18px 18px;
	}

	.record-detail__columns {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 24px;
		padding-top: 18px;
	}

	.record-section {
		display: grid;
		gap: 16px;
	}

	.record-section--risk {
		margin-top: 22px;
		padding-top: 20px;
		border-top: 1px solid rgba(167, 190, 180, 0.6);
	}

	.record-section__header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16px;
		padding-bottom: 10px;
		border-bottom: 1px solid rgba(167, 190, 180, 0.6);
	}

	.record-section__label {
		display: flex;
		align-items: center;
		margin: 0;
		font-family: var(--font-mono);
		font-size: 0.72rem;
		line-height: 1.1;
		letter-spacing: 0.16em;
		text-transform: uppercase;
		color: var(--ink-muted);
	}

	.record-section__meta {
		margin: 0;
		display: flex;
		align-items: center;
		font-family: var(--font-mono);
		font-size: 0.72rem;
		line-height: 1.1;
		letter-spacing: 0.12em;
		text-transform: uppercase;
		color: var(--ink-muted);
		text-align: right;
	}

	.record-section__meta-group {
		display: flex;
		align-items: center;
		justify-content: flex-end;
		gap: 14px;
	}

	.compliance-gauge--compact {
		width: 54px;
		height: 54px;
	}

	.compliance-gauge--compact .compliance-gauge__inner strong {
		font-size: 0.92rem;
	}

	.compliance-gauge--compact .compliance-gauge__inner span {
		font-size: 0.56rem;
	}

	.record-field,
	.record-key-value {
		display: grid;
		gap: 6px;
	}

	.record-field--spaced {
		padding-top: 4px;
	}

	h4 {
		margin: 0;
		font-size: 0.92rem;
		font-weight: 500;
		color: var(--ink-soft);
	}

	.record-richtext,
	.record-key-value div,
	.record-key-value a {
		font-size: 0.97rem;
		line-height: 1.65;
		color: var(--ink);
		word-break: break-word;
	}

	.record-doc-grid {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 16px;
	}

	.record-data-list,
	.record-risk-grid {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 16px 20px;
	}

	.record-risk-grid .wide {
		grid-column: 1 / -1;
	}

	.blank,
	.is-empty {
		color: var(--ink-muted);
		font-style: italic;
	}

	@media (max-width: 1100px) {
		.record-summary-shell {
			align-items: stretch;
		}

		.record-summary {
			grid-template-columns: minmax(0, 1fr) 24px;
			gap: 14px;
		}

		.record-share {
			align-self: start;
		}

		.record-summary__cols {
			grid-column: 1 / -1;
			display: grid;
			grid-template-columns: repeat(2, minmax(0, 1fr));
			gap: 12px;
			padding-left: 40px;
		}

		.record-summary__item > span {
			display: inline-block;
		}

		.record-summary__cols p {
			margin: 0;
		}

		.record-detail__columns,
		.record-data-list,
		.record-risk-grid,
		.record-doc-grid {
			grid-template-columns: 1fr;
		}
	}

	@media (max-width: 720px) {
		.record-card {
			border-radius: 22px;
		}

		.record-summary-shell {
			grid-template-columns: 1fr;
			padding: 16px;
			gap: 14px;
		}

		.record-summary__lead {
			grid-column: 1 / -1;
		}

		.record-compliance {
			justify-content: space-between;
		}

		.record-share {
			width: fit-content;
		}

		.record-summary__cols {
			padding-left: 0;
			grid-template-columns: 1fr;
		}

		.record-index {
			width: 24px;
			height: 24px;
			font-size: 0.72rem;
		}
	}
</style>
