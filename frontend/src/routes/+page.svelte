<script lang="ts">
	import { browser } from '$app/environment';
	import { PUBLIC_BACKEND_URL } from '$env/static/public';
	import { onDestroy, onMount, tick } from 'svelte';
	import AiUseCaseRecord from '../components/AIUseCaseRecord.svelte';
	import FieldGuideContent from '../components/FieldGuideContent.svelte';
	import { agencyOptions, impactOptions, stageOptions } from '$lib/explorer';

	type UseCaseRecord = Record<string, string | undefined | null>;
	type FilterKey = 'agency' | 'stage' | 'impact' | 'topic' | 'aiClassification';
	type SortKey = 'useCase' | 'agency' | 'stage' | 'impact' | 'topic' | 'aiClassification';
	type EmptyStateMap = Record<string, string>;
	type MultiFilters = Record<FilterKey, string[]>;
	type SectionKey = FilterKey | 'year';
	type ExpandedSections = Record<SectionKey, boolean>;
	type SortState = { key: SortKey; direction: 'asc' | 'desc' };

	const defaultExpandedSections: ExpandedSections = {
		year: true,
		agency: true,
		stage: true,
		impact: true,
		topic: true,
		aiClassification: true
	};

	const defaultFilters: MultiFilters = {
		agency: [],
		stage: [],
		impact: [],
		topic: [],
		aiClassification: []
	};

	const sortColumns: Array<{ key: SortKey; label: string }> = [
		{ key: 'useCase', label: 'Use Case' },
		{ key: 'agency', label: 'Agency' },
		{ key: 'stage', label: 'Stage' },
		{ key: 'impact', label: 'Impact' },
		{ key: 'topic', label: 'Topic Area' },
		{ key: 'aiClassification', label: 'AI Classification' }
	];
	const impactValueOptions = impactOptions
		.filter((option) => option.value)
		.map((option) => option.value);
	const impactSelectableOptions = [...impactValueOptions, ''];
	const SEARCH_DEBOUNCE_MS = 250;

	let query = '';
	let loading = false;
	let error = '';
	let mobileFiltersOpen = false;
	let fieldGuideOpen = false;
	let fieldGuideDialog: HTMLDivElement | null = null;
	let lastFocusedElement: HTMLElement | null = null;
	let serverResults: UseCaseRecord[] = [];
	let filters: MultiFilters = { ...defaultFilters };
	let expandedSections: ExpandedSections = { ...defaultExpandedSections };
	let sortState: SortState = { key: 'useCase', direction: 'asc' };
	let yearFrom = '';
	let yearTo = '';
	let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null;
	let activeSearchController: AbortController | null = null;
	let latestSearchRequest = 0;

	const emptyStateMessages: EmptyStateMap = {
		'Department of Defense':
			'The Department of Defense is exempt from submitting an AI Use Case Inventory for intelligence reasons.',
		'Consumer Financial Protection Bureau':
			'The Consumer Financial Protection Bureau has no reported AI use cases in 2025.',
		'National Transportation Safety Board':
			'The National Transportation Safety Board has no reported AI use cases in 2025.',
		'Social Security Administration':
			'The Social Security Administration has not yet published its AI Use Case Inventory (as of 2/13/26).',
		'United States Agency for International Development':
			'The United States Agency for International Development has not yet published its AI Use Case Inventory (as of 2/13/26). The agency is defunct as of February 2025.',
		'Postal Regulatory Commission':
			'The Postal Regulatory Commission has not yet published its AI Use Case Inventory (as of 2/13/26).',
		'Presidio Trust':
			'The Presidio Trust has not yet published its AI Use Case Inventory (as of 2/13/26).',
		'United States Agency for Global Media':
			'The United States Agency for Global Media has not yet published its AI Use Case Inventory (as of 2/13/26).',
		'United States Commission on Civil Rights':
			'The United States Commission on Civil Rights has not yet published its AI Use Case Inventory (as of 2/13/26).'
	};

	$: topicOptions = [
		...new Set(
			serverResults
				.map((result) => normalizeFilterValue(result.use_case_topic_area))
				.filter(Boolean)
		)
	].sort((a, b) => a.localeCompare(b));
	$: aiClassificationOptions = [
		...new Set(
			serverResults.map((result) => normalizeFilterValue(result.ai_classification)).filter(Boolean)
		)
	].sort((a, b) => a.localeCompare(b));
	$: agencyFilterOptions = agencyOptions.filter((agency) =>
		serverResults.some((result) => getAgencyValues(result).includes(agency))
	);
	$: filteredResults = serverResults.filter((result) => matchesFilters(result, filters));
	$: yearFilteredResults = filteredResults.filter(matchesYearRange);
	$: results = [...yearFilteredResults].sort((left, right) =>
		compareRecords(left, right, sortState)
	);
	$: activeFilterCount =
		Object.values(filters).reduce((count, values) => count + values.length, 0) +
		(yearFrom ? 1 : 0) +
		(yearTo ? 1 : 0);
	$: emptyStateMessage =
		filters.agency.length === 1
			? emptyStateMessages[filters.agency[0]] ||
				'If you think you should be getting results, check your query and filters.'
			: 'If you think you should be getting results, check your query and filters.';

	onMount(() => {
		search();
	});

	onDestroy(() => {
		clearPendingSearch();
		activeSearchController?.abort();
		unlockPageScroll();
	});

	async function openFieldGuide() {
		if (!browser) {
			return;
		}

		lastFocusedElement =
			document.activeElement instanceof HTMLElement ? document.activeElement : null;
		fieldGuideOpen = true;
		lockPageScroll();
		await tick();
		fieldGuideDialog?.focus();
	}

	function closeFieldGuide() {
		fieldGuideOpen = false;
		unlockPageScroll();
		lastFocusedElement?.focus();
	}

	function lockPageScroll() {
		if (!browser) {
			return;
		}

		document.body.style.overflow = 'hidden';
	}

	function unlockPageScroll() {
		if (!browser) {
			return;
		}

		document.body.style.overflow = '';
	}

	function handleWindowKeydown(event: KeyboardEvent) {
		if (!fieldGuideOpen) {
			return;
		}

		if (event.key === 'Escape') {
			event.preventDefault();
			closeFieldGuide();
		}
	}

	function normalizeFilterValue(value: string | null | undefined) {
		return (value || '').trim();
	}

	function normalizeComparisonValue(value: string | null | undefined) {
		return normalizeFilterValue(value).toLowerCase();
	}

	function agencyLabel(result: UseCaseRecord) {
		return [normalizeFilterValue(result.bureau_component), normalizeFilterValue(result.agency)]
			.filter(Boolean)
			.join(' · ');
	}

	function getAgencyValues(result: UseCaseRecord) {
		return [
			normalizeFilterValue(result.bureau_component),
			normalizeFilterValue(result.agency)
		].filter(Boolean);
	}

	function matchesAnySelection(selections: string[], value: string) {
		if (selections.length === 0) {
			return true;
		}

		const normalizedValue = normalizeComparisonValue(value);
		return selections.some((selection) => normalizeComparisonValue(selection) === normalizedValue);
	}

	function matchesFilters(result: UseCaseRecord, activeFilters: MultiFilters) {
		const matchesAgency =
			activeFilters.agency.length === 0 ||
			getAgencyValues(result).some((value) => matchesAnySelection(activeFilters.agency, value));

		return (
			matchesAgency &&
			matchesAnySelection(activeFilters.stage, normalizeFilterValue(result.stage_of_development)) &&
			matchesAnySelection(activeFilters.impact, normalizeFilterValue(result.is_high_impact)) &&
			matchesAnySelection(activeFilters.topic, normalizeFilterValue(result.use_case_topic_area)) &&
			matchesAnySelection(
				activeFilters.aiClassification,
				normalizeFilterValue(result.ai_classification)
			)
		);
	}

	function matchesYearRange(result: UseCaseRecord) {
		const from = yearFrom;
		const to = yearTo;
		const lowerBound = Math.min(from, to);
		const upperBound = Math.max(from, to);
		if (from === null && to === null) {
			return true;
		}

		const year: number = result.data_year;
		if (year === null) {
			return false;
		}

		if (lowerBound !== null && year < lowerBound) {
			return false;
		}

		if (upperBound !== null && year > upperBound) {
			return false;
		}

		return true;
	}

	function countMatchingOption(key: FilterKey, value: string) {
		return serverResults.filter((result) => {
			if (key === 'agency') {
				return getAgencyValues(result).includes(value);
			}

			if (key === 'stage') {
				return normalizeFilterValue(result.stage_of_development) === value;
			}

			if (key === 'impact') {
				return normalizeFilterValue(result.is_high_impact) === value;
			}

			if (key === 'topic') {
				return normalizeFilterValue(result.use_case_topic_area) === value;
			}

			return normalizeFilterValue(result.ai_classification) === value;
		}).length;
	}

	function countBlankImpact() {
		return serverResults.filter((result) => normalizeFilterValue(result.is_high_impact) === '')
			.length;
	}

	function countAllForGroup(key: FilterKey, options: string[]) {
		if (key === 'impact') {
			return serverResults.filter((result) => {
				const value = normalizeFilterValue(result.is_high_impact);
				return options.includes(value) || value === '';
			}).length;
		}

		return serverResults.filter((result) => {
			if (key === 'agency') {
				return getAgencyValues(result).some((value) => options.includes(value));
			}

			if (key === 'stage') {
				return options.includes(normalizeFilterValue(result.stage_of_development));
			}

			if (key === 'topic') {
				return options.includes(normalizeFilterValue(result.use_case_topic_area));
			}

			return options.includes(normalizeFilterValue(result.ai_classification));
		}).length;
	}

	function isAllSelected(key: FilterKey, options: string[]) {
		return options.length > 0 && options.every((option) => filters[key].includes(option));
	}

	function toggleAllFilterValues(key: FilterKey, options: string[]) {
		filters = {
			...filters,
			[key]: isAllSelected(key, options) ? [] : [...options]
		};
	}

	function getSortableValue(result: UseCaseRecord, key: SortKey) {
		switch (key) {
			case 'useCase':
				return normalizeFilterValue(result.use_case_name);
			case 'agency':
				return agencyLabel(result);
			case 'stage':
				return normalizeFilterValue(result.stage_of_development);
			case 'impact':
				return normalizeFilterValue(result.is_high_impact);
			case 'topic':
				return normalizeFilterValue(result.use_case_topic_area);
			case 'aiClassification':
				return normalizeFilterValue(result.ai_classification);
		}
	}

	function compareRecords(left: UseCaseRecord, right: UseCaseRecord, currentSort: SortState) {
		const direction = currentSort.direction === 'asc' ? 1 : -1;
		const leftValue = getSortableValue(left, currentSort.key);
		const rightValue = getSortableValue(right, currentSort.key);

		const leftBlank = leftValue === '';
		const rightBlank = rightValue === '';

		if (leftBlank && rightBlank) {
			return 0;
		}

		if (leftBlank) {
			return 1;
		}

		if (rightBlank) {
			return -1;
		}

		return leftValue.localeCompare(rightValue, undefined, { sensitivity: 'base' }) * direction;
	}

	function clearPendingSearch() {
		if (searchDebounceTimer) {
			clearTimeout(searchDebounceTimer);
			searchDebounceTimer = null;
		}
	}

	function queueSearch() {
		clearPendingSearch();
		searchDebounceTimer = setTimeout(() => {
			searchDebounceTimer = null;
			void search();
		}, SEARCH_DEBOUNCE_MS);
	}

	function submitSearch() {
		clearPendingSearch();
		void search();
	}

	async function search() {
		const requestId = ++latestSearchRequest;
		const controller = new AbortController();

		activeSearchController?.abort();
		activeSearchController = controller;
		loading = true;
		error = '';

		const queryString = `${PUBLIC_BACKEND_URL}/ai-use-cases/${encodeURIComponent(query.trim() || '*')}?`;

		try {
			const response = await fetch(queryString, { signal: controller.signal });
			if (!response.ok) {
				throw new Error(`HTTP ${response.status}`);
			}

			const nextResults: UseCaseRecord[] = await response.json();
			const nextTopicOptions = [
				...new Set(
					nextResults
						.map((result) => normalizeFilterValue(result.use_case_topic_area))
						.filter(Boolean)
				)
			];
			const nextAiClassificationOptions = [
				...new Set(
					nextResults
						.map((result) => normalizeFilterValue(result.ai_classification))
						.filter(Boolean)
				)
			];

			if (requestId !== latestSearchRequest) {
				return;
			}

			serverResults = nextResults;
			filters = {
				...filters,
				topic: filters.topic.filter((value) => nextTopicOptions.includes(value)),
				aiClassification: filters.aiClassification.filter((value) =>
					nextAiClassificationOptions.includes(value)
				)
			};
		} catch (searchError) {
			if (searchError instanceof DOMException && searchError.name === 'AbortError') {
				return;
			}

			error = searchError instanceof Error ? searchError.message : 'Unknown error';
			serverResults = [];
		} finally {
			if (requestId === latestSearchRequest) {
				loading = false;
				activeSearchController = null;
			}
		}
	}

	function toggleFilterValue(key: FilterKey, value: string) {
		const nextValues = filters[key].includes(value)
			? filters[key].filter((entry) => entry !== value)
			: [...filters[key], value];

		filters = { ...filters, [key]: nextValues };
	}

	function clearFilterGroup(key: FilterKey) {
		filters = { ...filters, [key]: [] };
	}

	function clearAllFilters() {
		query = '';
		filters = { ...defaultFilters };
		yearFrom = 2024;
		yearTo = 2025;
		expandedSections = { ...defaultExpandedSections };
		mobileFiltersOpen = false;
		sortState = { key: 'useCase', direction: 'asc' };
		submitSearch();
	}

	function toggleSection(key: SectionKey) {
		expandedSections = { ...expandedSections, [key]: !expandedSections[key] };
	}

	function toggleSort(key: SortKey) {
		sortState =
			sortState.key === key
				? { key, direction: sortState.direction === 'asc' ? 'desc' : 'asc' }
				: { key, direction: 'asc' };
	}

	function sortIndicator(key: SortKey) {
		if (sortState.key !== key) {
			return '±';
		}

		return sortState.direction === 'asc' ? '↑' : '↓';
	}
</script>

<svelte:window on:keydown={handleWindowKeydown} />

<section class="explorer-page">
	<div class="explorer-hero">
		<div>
			<p class="explorer-kicker">2025 Federal Inventory</p>
			<h1>AI Use Case Inventory</h1>
			<p class="explorer-intro">
				Browse the consolidated federal AI use case inventory with a calmer layout, simplified field
				names, and direct links to the original reporting fields.
			</p>
		</div>

		<form class="explorer-search" on:submit|preventDefault={submitSearch}>
			<label class="visually-hidden" for="search-use-cases">Search use cases</label>
			<input
				id="search-use-cases"
				type="search"
				bind:value={query}
				on:input={queueSearch}
				placeholder="Search use cases, agencies, vendors, systems..."
			/>
			<button type="submit">Search</button>
		</form>
	</div>

	<div class="explorer-mobile-actions">
		<button
			type="button"
			class="explorer-mobile-toggle"
			on:click={() => (mobileFiltersOpen = !mobileFiltersOpen)}
		>
			{mobileFiltersOpen ? 'Hide filters' : 'Show filters'}
			<span>{activeFilterCount} active</span>
		</button>
	</div>

	<div class="explorer-layout">
		<aside class:open={mobileFiltersOpen} class="explorer-sidebar">
			<div class="filter-panel">
				<div class="filter-panel__header">
					<div>
						<p class="filter-panel__eyebrow">Filters</p>
						<h2>Refine the inventory</h2>
					</div>
					<button type="button" class="filter-reset" on:click={clearAllFilters}>Reset all</button>
				</div>

				<section class="filter-section">
					<button
						type="button"
						class="filter-section__toggle"
						on:click={() => toggleSection('year')}
						aria-expanded={expandedSections.year}
					>
						<span>Year</span>
						<span
							class:expanded={expandedSections.year}
							class="filter-section__icon"
							aria-hidden="true"
						></span>
					</button>
					{#if expandedSections.year}
						<div class="filter-section__body">
							<div class="year-row">
								<input
									bind:value={yearFrom}
									class="year-input"
									type="text"
									inputmode="numeric"
									maxlength="4"
									placeholder="2024"
									aria-label="Year from"
								/>
								<span class="year-dash">-</span>
								<input
									bind:value={yearTo}
									class="year-input"
									type="text"
									inputmode="numeric"
									maxlength="4"
									placeholder="2025"
									aria-label="Year to"
								/>
							</div>
						</div>
					{/if}
				</section>

				<section class="filter-section">
					<button
						type="button"
						class="filter-section__toggle"
						on:click={() => toggleSection('agency')}
						aria-expanded={expandedSections.agency}
					>
						<span>Agency</span>
						<span
							class:expanded={expandedSections.agency}
							class="filter-section__icon"
							aria-hidden="true"
						></span>
					</button>
					{#if expandedSections.agency}
						<div class="filter-section__body">
							<div class="checkbox-list checkbox-list--scrollable">
								<button
									type="button"
									class:checked={isAllSelected('agency', agencyFilterOptions)}
									class="checkbox-item"
									on:click={() => toggleAllFilterValues('agency', agencyFilterOptions)}
								>
									<span class="checkbox-mark"
										>{isAllSelected('agency', agencyFilterOptions) ? '✓' : ''}</span
									>
									<span class="checkbox-label">Select All</span>
									<span class="checkbox-count"
										>{countAllForGroup('agency', agencyFilterOptions)}</span
									>
								</button>
								{#each agencyFilterOptions as agency (agency)}
									<button
										type="button"
										class:checked={filters.agency.includes(agency)}
										class="checkbox-item"
										on:click={() => toggleFilterValue('agency', agency)}
									>
										<span class="checkbox-mark">{filters.agency.includes(agency) ? '✓' : ''}</span>
										<span class="checkbox-label">{agency}</span>
										<span class="checkbox-count">{countMatchingOption('agency', agency)}</span>
									</button>
								{/each}
							</div>
							{#if filters.agency.length > 0}
								<button
									type="button"
									class="filter-clear"
									on:click={() => clearFilterGroup('agency')}>Clear agency</button
								>
							{/if}
						</div>
					{/if}
				</section>

				<section class="filter-section">
					<button
						type="button"
						class="filter-section__toggle"
						on:click={() => toggleSection('stage')}
						aria-expanded={expandedSections.stage}
					>
						<span>Stage</span>
						<span
							class:expanded={expandedSections.stage}
							class="filter-section__icon"
							aria-hidden="true"
						></span>
					</button>
					{#if expandedSections.stage}
						<div class="filter-section__body">
							<div class="checkbox-list">
								<button
									type="button"
									class:checked={isAllSelected('stage', stageOptions)}
									class="checkbox-item"
									on:click={() => toggleAllFilterValues('stage', stageOptions)}
								>
									<span class="checkbox-mark"
										>{isAllSelected('stage', stageOptions) ? '✓' : ''}</span
									>
									<span class="checkbox-label">All</span>
									<span class="checkbox-count">{countAllForGroup('stage', stageOptions)}</span>
								</button>
								{#each stageOptions as stage (stage)}
									<button
										type="button"
										class:checked={filters.stage.includes(stage)}
										class="checkbox-item"
										on:click={() => toggleFilterValue('stage', stage)}
									>
										<span class="checkbox-mark">{filters.stage.includes(stage) ? '✓' : ''}</span>
										<span class="checkbox-label checkbox-label--with-dot"
											><span
												class={`checkbox-dot checkbox-dot--${stage.toLowerCase().replace(/[^a-z]+/g, '-')}`}
											></span>{stage}</span
										>
										<span class="checkbox-count">{countMatchingOption('stage', stage)}</span>
									</button>
								{/each}
							</div>
							{#if filters.stage.length > 0}
								<button
									type="button"
									class="filter-clear"
									on:click={() => clearFilterGroup('stage')}>Clear stage</button
								>
							{/if}
						</div>
					{/if}
				</section>

				<section class="filter-section">
					<button
						type="button"
						class="filter-section__toggle"
						on:click={() => toggleSection('impact')}
						aria-expanded={expandedSections.impact}
					>
						<span>Impact</span>
						<span
							class:expanded={expandedSections.impact}
							class="filter-section__icon"
							aria-hidden="true"
						></span>
					</button>
					{#if expandedSections.impact}
						<div class="filter-section__body">
							<div class="checkbox-list">
								<button
									type="button"
									class:checked={isAllSelected('impact', impactSelectableOptions)}
									class="checkbox-item"
									on:click={() => toggleAllFilterValues('impact', impactSelectableOptions)}
								>
									<span class="checkbox-mark"
										>{isAllSelected('impact', impactSelectableOptions) ? '✓' : ''}</span
									>
									<span class="checkbox-label">All</span>
									<span class="checkbox-count"
										>{countAllForGroup('impact', impactValueOptions) + countBlankImpact()}</span
									>
								</button>
								{#each impactOptions.filter((option) => option.value) as option (option.value)}
									<button
										type="button"
										class:checked={filters.impact.includes(option.value)}
										class="checkbox-item"
										on:click={() => toggleFilterValue('impact', option.value)}
									>
										<span class="checkbox-mark"
											>{filters.impact.includes(option.value) ? '✓' : ''}</span
										>
										<span class="checkbox-label checkbox-label--with-dot"
											><span
												class={`checkbox-dot checkbox-dot--${option.value === 'High-impact' ? 'impact-high' : 'impact-low'}`}
											></span>{option.label}</span
										>
										<span class="checkbox-count">{countMatchingOption('impact', option.value)}</span
										>
									</button>
								{/each}
								<button
									type="button"
									class:checked={filters.impact.includes('')}
									class="checkbox-item"
									on:click={() => toggleFilterValue('impact', '')}
								>
									<span class="checkbox-mark">{filters.impact.includes('') ? '✓' : ''}</span>
									<span class="checkbox-label checkbox-label--with-dot"
										><span class="checkbox-dot checkbox-dot--blank"></span>(Blanks)</span
									>
									<span class="checkbox-count">{countBlankImpact()}</span>
								</button>
							</div>
							{#if filters.impact.length > 0}
								<button
									type="button"
									class="filter-clear"
									on:click={() => clearFilterGroup('impact')}>Clear impact</button
								>
							{/if}
						</div>
					{/if}
				</section>

				<section class="filter-section">
					<button
						type="button"
						class="filter-section__toggle"
						on:click={() => toggleSection('topic')}
						aria-expanded={expandedSections.topic}
					>
						<span>Topic Area</span>
						<span
							class:expanded={expandedSections.topic}
							class="filter-section__icon"
							aria-hidden="true"
						></span>
					</button>
					{#if expandedSections.topic}
						<div class="filter-section__body">
							<div class="checkbox-list checkbox-list--scrollable">
								<button
									type="button"
									class:checked={isAllSelected('topic', topicOptions)}
									class="checkbox-item"
									on:click={() => toggleAllFilterValues('topic', topicOptions)}
								>
									<span class="checkbox-mark"
										>{isAllSelected('topic', topicOptions) ? '✓' : ''}</span
									>
									<span class="checkbox-label">Select All</span>
									<span class="checkbox-count">{countAllForGroup('topic', topicOptions)}</span>
								</button>
								{#each topicOptions as topic (topic)}
									<button
										type="button"
										class:checked={filters.topic.includes(topic)}
										class="checkbox-item"
										on:click={() => toggleFilterValue('topic', topic)}
									>
										<span class="checkbox-mark">{filters.topic.includes(topic) ? '✓' : ''}</span>
										<span class="checkbox-label">{topic}</span>
										<span class="checkbox-count">{countMatchingOption('topic', topic)}</span>
									</button>
								{/each}
							</div>
							{#if filters.topic.length > 0}
								<button
									type="button"
									class="filter-clear"
									on:click={() => clearFilterGroup('topic')}>Clear topic area</button
								>
							{/if}
						</div>
					{/if}
				</section>

				<section class="filter-section">
					<button
						type="button"
						class="filter-section__toggle"
						on:click={() => toggleSection('aiClassification')}
						aria-expanded={expandedSections.aiClassification}
					>
						<span>AI Classification</span>
						<span
							class:expanded={expandedSections.aiClassification}
							class="filter-section__icon"
							aria-hidden="true"
						></span>
					</button>
					{#if expandedSections.aiClassification}
						<div class="filter-section__body">
							<div class="checkbox-list checkbox-list--scrollable">
								<button
									type="button"
									class:checked={isAllSelected('aiClassification', aiClassificationOptions)}
									class="checkbox-item"
									on:click={() =>
										toggleAllFilterValues('aiClassification', aiClassificationOptions)}
								>
									<span class="checkbox-mark"
										>{isAllSelected('aiClassification', aiClassificationOptions) ? '✓' : ''}</span
									>
									<span class="checkbox-label">Select All</span>
									<span class="checkbox-count"
										>{countAllForGroup('aiClassification', aiClassificationOptions)}</span
									>
								</button>
								{#each aiClassificationOptions as option (option)}
									<button
										type="button"
										class:checked={filters.aiClassification.includes(option)}
										class="checkbox-item"
										on:click={() => toggleFilterValue('aiClassification', option)}
									>
										<span class="checkbox-mark"
											>{filters.aiClassification.includes(option) ? '✓' : ''}</span
										>
										<span class="checkbox-label">{option}</span>
										<span class="checkbox-count"
											>{countMatchingOption('aiClassification', option)}</span
										>
									</button>
								{/each}
							</div>
							{#if filters.aiClassification.length > 0}
								<button
									type="button"
									class="filter-clear"
									on:click={() => clearFilterGroup('aiClassification')}
									>Clear AI classification</button
								>
							{/if}
						</div>
					{/if}
				</section>

				<div class="filter-note">
					<p>
						Displayed labels are shortened for readability. The
						<button type="button" class="filter-note__link" on:click={openFieldGuide}>
							field guide
						</button>
						maps each one back to the original reported field.
					</p>
				</div>
			</div>
		</aside>

		<div class="explorer-results">
			<div class="results-toolbar">
				<div>
					<p class="results-toolbar__eyebrow">Explorer</p>
					<div class="results-count">
						<strong>{results.length}{results.length === 250 ? '+' : ''}</strong>
						<span>{results.length === 1 ? 'case' : 'cases'}</span>
					</div>
				</div>

				<button type="button" class="field-guide-link" on:click={openFieldGuide}>
					Field Guide
					<span>?</span>
				</button>
			</div>

			<div class="results-table-header">
				{#each sortColumns as column (column.key)}
					<button
						type="button"
						class:active={sortState.key === column.key}
						class="results-table-header__button"
						on:click={() => toggleSort(column.key)}
					>
						<span>{column.label}</span>
						<span class="results-table-header__icon" aria-hidden="true"
							>{sortIndicator(column.key)}</span
						>
					</button>
				{/each}
			</div>

			{#if error}
				<div class="status-panel status-panel--error">
					<p>We could not load inventory results right now.</p>
					<small>{error}</small>
				</div>
			{:else if loading}
				<div class="status-panel">
					<p>Loading the latest results...</p>
				</div>
			{:else if results.length === 0}
				<div class="status-panel">
					<p>{emptyStateMessage}</p>
				</div>
			{:else}
				<div class="results-list">
					{#each results as result, index (`${result.data_year}-${result.use_case_id}-${result.agency}-${result.use_case_name}-${result.validation_notes}`)}
						<AiUseCaseRecord {result} index={index + 1} />
					{/each}
				</div>
			{/if}
		</div>
	</div>
</section>

{#if fieldGuideOpen}
	<div class="field-guide-modal">
		<button
			type="button"
			class="field-guide-modal__backdrop"
			aria-label="Close field guide"
			on:click={closeFieldGuide}
		></button>
		<div
			bind:this={fieldGuideDialog}
			class="field-guide-modal__dialog"
			role="dialog"
			aria-modal="true"
			aria-labelledby="field-guide-modal-title"
			aria-describedby="field-guide-modal-description"
			tabindex="-1"
		>
			<div class="field-guide-modal__header">
				<div>
					<p class="field-guide-modal__kicker">Reference</p>
					<h2 id="field-guide-modal-title">Field Guide</h2>
					<p id="field-guide-modal-description" class="field-guide-modal__intro">
						The explorer uses shorter labels on the homepage, while the underlying data stays the
						same.
					</p>
				</div>
				<button type="button" class="field-guide-modal__close" on:click={closeFieldGuide}>
					Close
				</button>
			</div>

			<div class="field-guide-modal__body">
				<FieldGuideContent />
			</div>
		</div>
	</div>
{/if}

<style>
	.explorer-page {
		display: grid;
		gap: 24px;
	}

	.explorer-hero {
		display: grid;
		gap: 20px;
		padding: 8px 0 0;
	}

	.explorer-kicker,
	.results-toolbar__eyebrow,
	.filter-panel__eyebrow {
		margin: 0 0 8px;
		font-family: var(--font-mono);
		font-size: 0.72rem;
		letter-spacing: 0.16em;
		text-transform: uppercase;
		color: var(--ink-muted);
	}

	h1 {
		margin: 0;
		font-family: var(--font-serif);
		font-size: clamp(3.6rem, 6vw, 5rem);
		font-weight: 400;
		line-height: 0.94;
		letter-spacing: -0.03em;
		color: var(--brand);
	}

	.explorer-intro {
		margin: 14px 0 0;
		max-width: 68ch;
		font-size: 1rem;
		line-height: 1.7;
		color: var(--ink-soft);
	}

	.explorer-search {
		display: flex;
		align-items: stretch;
		gap: 12px;
	}

	.explorer-search input {
		flex: 1;
		min-width: 0;
		padding: 18px 22px;
		border: 1.5px solid var(--line-strong);
		border-radius: 26px;
		background: rgba(255, 255, 255, 0.75);
		box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
	}

	.explorer-search input:focus {
		outline: 2px solid rgba(90, 140, 121, 0.22);
		border-color: var(--brand);
	}

	.explorer-search button,
	.explorer-mobile-toggle,
	.filter-reset,
	.checkbox-item,
	.filter-clear,
	.field-guide-link,
	.results-table-header__button,
	.filter-section__toggle,
	.year-input {
		transition:
			transform 0.16s ease,
			border-color 0.16s ease,
			background-color 0.16s ease,
			color 0.16s ease;
	}

	.explorer-search button {
		padding: 0 24px;
		border-radius: 22px;
		background: var(--accent);
		color: white;
		font-weight: 700;
	}

	.explorer-search button:hover,
	.explorer-mobile-toggle:hover,
	.filter-reset:hover,
	.checkbox-item:hover,
	.filter-clear:hover,
	.field-guide-link:hover,
	.results-table-header__button:hover,
	.filter-section__toggle:hover {
		transform: translateY(-1px);
	}

	.explorer-mobile-actions {
		display: none;
	}

	.explorer-layout {
		display: grid;
		grid-template-columns: minmax(256px, 296px) minmax(0, 1fr);
		gap: 24px;
		align-items: start;
	}

	.explorer-sidebar {
		position: sticky;
		top: 86px;
		align-self: start;
		height: calc(100vh - 110px);
		max-height: calc(100vh - 110px);
	}

	.filter-panel,
	.results-toolbar,
	.status-panel,
	.results-table-header {
		background: rgba(255, 255, 255, 0.7);
		border: 1px solid rgba(167, 190, 180, 0.55);
		box-shadow: var(--shadow-soft);
	}

	.filter-panel {
		padding: 20px;
		border-radius: var(--radius-lg);
		display: grid;
		gap: 18px;
		height: 100%;
		max-height: 100%;
		overflow-y: auto;
		overscroll-behavior: contain;
	}

	.filter-panel__header {
		display: flex;
		justify-content: space-between;
		gap: 18px;
		align-items: flex-start;
	}

	.filter-panel__header h2 {
		margin: 0;
		font-size: 1.05rem;
	}

	.filter-reset,
	.filter-clear {
		padding: 10px 12px;
		border-radius: 999px;
		background: var(--surface-strong);
		color: var(--ink-soft);
		font-size: 0.92rem;
	}

	.filter-section {
		padding-top: 18px;
		border-top: 1px solid var(--line);
	}

	.filter-section:first-of-type {
		padding-top: 0;
		border-top: 0;
	}

	.filter-section__toggle {
		width: 100%;
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
		text-align: left;
	}

	.filter-section__toggle span:first-child {
		font-family: var(--font-mono);
		font-size: 0.74rem;
		text-transform: uppercase;
		letter-spacing: 0.14em;
		color: var(--ink-muted);
	}

	.filter-section__icon {
		position: relative;
		width: 16px;
		height: 16px;
		flex-shrink: 0;
		transform: rotate(0deg);
		transform-origin: center;
	}

	.filter-section__icon::before,
	.filter-section__icon::after {
		content: '';
		position: absolute;
		top: 50%;
		left: 50%;
		width: 14px;
		height: 1.5px;
		background: var(--brand-dark);
		border-radius: 999px;
		transform: translate(-50%, -50%);
	}

	.filter-section__icon::after {
		transform: translate(-50%, -50%) rotate(90deg);
	}

	.filter-section__icon.expanded {
		transform: rotate(45deg);
	}

	.filter-section__body {
		display: grid;
		gap: 12px;
		padding-top: 12px;
	}

	.year-row {
		display: grid;
		grid-template-columns: minmax(0, 1fr) 18px minmax(0, 1fr);
		gap: 10px;
		align-items: center;
	}

	.year-input {
		width: 100%;
		padding: 12px 14px;
		border-radius: 12px;
		border: 1px solid var(--line);
		background: var(--surface);
		font-family: var(--font-mono);
		text-align: center;
		color: var(--ink);
	}

	.year-dash {
		text-align: center;
		color: var(--ink-muted);
	}

	.checkbox-list {
		display: flex;
		flex-direction: column;
		gap: 2px;
	}

	.checkbox-list--scrollable {
		max-height: 280px;
		overflow: auto;
		padding-right: 4px;
	}

	.checkbox-item {
		display: grid;
		grid-template-columns: 18px minmax(0, 1fr) auto;
		align-items: center;
		gap: 10px;
		padding: 8px 2px;
		border-radius: 8px;
		color: var(--ink-soft);
		text-align: left;
	}

	.checkbox-item.checked {
		color: var(--ink);
	}

	.checkbox-mark {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 18px;
		height: 18px;
		border-radius: 4px;
		border: 1.5px solid var(--ink-muted);
		background: #fff;
		font-size: 0.78rem;
		line-height: 1;
		color: #fff;
	}

	.checkbox-item.checked .checkbox-mark {
		background: var(--ink);
		border-color: var(--ink);
	}

	.checkbox-label {
		min-width: 0;
		font-size: 0.98rem;
		line-height: 1.35;
	}

	.checkbox-label--with-dot {
		display: inline-flex;
		align-items: center;
		gap: 10px;
	}

	.checkbox-dot {
		width: 10px;
		height: 10px;
		border-radius: 999px;
		flex-shrink: 0;
		background: #c7d1cc;
		border: 1px solid transparent;
	}

	.checkbox-dot--deployed {
		background: #2f6f54;
	}

	.checkbox-dot--pilot {
		background: #476280;
	}

	.checkbox-dot--pre-deployment {
		background: #b67e32;
	}

	.checkbox-dot--retired {
		background: #889690;
	}

	.checkbox-dot--impact-high {
		background: #8a4a4a;
	}

	.checkbox-dot--impact-low {
		background: #bcc7c2;
	}

	.checkbox-dot--blank {
		background: #f2f3ef;
		border-color: #bcc7c2;
	}

	.checkbox-count {
		font-family: var(--font-mono);
		font-size: 0.75rem;
		color: var(--ink-muted);
	}

	.results-table-header__button.active {
		background: var(--brand-pale);
		border-color: var(--brand);
		color: var(--brand-dark);
	}

	.filter-note {
		padding: 16px;
		border-radius: 16px;
		background: var(--surface-strong);
		color: var(--ink-soft);
		font-size: 0.94rem;
		line-height: 1.6;
	}

	.filter-note p {
		margin: 0;
	}

	.filter-note__link {
		display: inline;
		padding: 0;
		color: var(--accent);
		text-decoration: underline;
		text-underline-offset: 0.16em;
	}

	.field-guide-modal {
		position: fixed;
		inset: 0;
		z-index: 60;
		display: grid;
		place-items: center;
		padding: 24px;
		background: rgba(18, 38, 32, 0.44);
		backdrop-filter: blur(10px);
	}

	.field-guide-modal__backdrop {
		position: absolute;
		inset: 0;
	}

	.field-guide-modal__dialog {
		position: relative;
		width: min(980px, 100%);
		max-height: min(88vh, 960px);
		overflow: hidden;
		border: 1px solid rgba(167, 190, 180, 0.6);
		border-radius: 28px;
		background: linear-gradient(180deg, rgba(249, 250, 247, 0.98), rgba(244, 247, 242, 0.96));
		box-shadow: 0 30px 80px rgba(15, 30, 25, 0.24);
	}

	.field-guide-modal__header {
		display: flex;
		justify-content: space-between;
		gap: 18px;
		align-items: flex-start;
		padding: 24px 24px 18px;
		border-bottom: 1px solid var(--line);
	}

	.field-guide-modal__kicker {
		margin: 0 0 8px;
		font-family: var(--font-mono);
		font-size: 0.72rem;
		letter-spacing: 0.16em;
		text-transform: uppercase;
		color: var(--ink-muted);
	}

	.field-guide-modal__header h2 {
		margin: 0;
		font-family: var(--font-serif);
		font-size: clamp(2rem, 4vw, 3rem);
		font-weight: 400;
		line-height: 0.98;
		color: var(--brand-dark);
	}

	.field-guide-modal__intro {
		margin: 12px 0 0;
		max-width: 62ch;
		color: var(--ink-soft);
		line-height: 1.6;
	}

	.field-guide-modal__close {
		flex-shrink: 0;
		padding: 11px 14px;
		border-radius: 999px;
		border: 1px solid var(--line);
		background: var(--surface);
		color: var(--ink-soft);
	}

	.field-guide-modal__body {
		display: grid;
		gap: 18px;
		padding: 20px 24px 24px;
		max-height: calc(min(88vh, 960px) - 128px);
		overflow: auto;
	}

	.explorer-results {
		display: grid;
		gap: 12px;
	}

	.results-toolbar {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 18px;
		padding: 18px 22px;
		border-radius: var(--radius-lg);
	}

	.results-count {
		display: flex;
		align-items: baseline;
		gap: 8px;
	}

	.results-count strong {
		font-size: clamp(2rem, 3vw, 2.6rem);
		line-height: 1;
		font-weight: 500;
		color: var(--ink);
	}

	.results-count span {
		color: var(--ink-soft);
	}

	.field-guide-link {
		display: inline-flex;
		align-items: center;
		gap: 10px;
		padding: 12px 14px;
		border-radius: 999px;
		border: 1px solid var(--line);
		background: var(--surface);
		color: var(--ink-soft);
	}

	.field-guide-link span {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 20px;
		height: 20px;
		border-radius: 999px;
		border: 1px solid currentColor;
		font-size: 0.78rem;
	}

	.results-table-header {
		display: grid;
		grid-template-columns:
			minmax(0, 2.35fr) minmax(140px, 1.1fr) minmax(104px, 0.7fr) minmax(110px, 0.8fr)
			minmax(120px, 0.95fr) minmax(160px, 1.2fr);
		gap: 12px;
		padding: 12px 18px;
		border-radius: var(--radius-lg);
	}

	.results-table-header__button {
		display: inline-flex;
		align-items: center;
		justify-content: space-between;
		gap: 8px;
		width: 100%;
		padding: 10px 12px;
		border-radius: 14px;
		border: 1px solid transparent;
		font-family: var(--font-mono);
		font-size: 0.7rem;
		text-transform: uppercase;
		letter-spacing: 0.12em;
		color: var(--ink-muted);
		text-align: left;
	}

	.results-table-header__icon {
		font-size: 0.9rem;
		letter-spacing: normal;
	}

	.results-list {
		display: grid;
		gap: 12px;
	}

	.status-panel {
		padding: 32px 28px;
		border-radius: var(--radius-lg);
		color: var(--ink-soft);
	}

	.status-panel p,
	.status-panel small {
		margin: 0;
		line-height: 1.65;
	}

	.status-panel--error {
		border-color: rgba(138, 74, 74, 0.35);
		background: rgba(255, 246, 244, 0.84);
		color: var(--danger);
	}

	@media (max-width: 1100px) {
		.explorer-layout {
			grid-template-columns: 1fr;
		}

		.explorer-sidebar {
			position: static;
			display: none;
			height: auto;
			max-height: none;
		}

		.explorer-sidebar.open,
		.explorer-mobile-actions {
			display: block;
		}

		.explorer-mobile-toggle {
			width: 100%;
			display: flex;
			justify-content: space-between;
			align-items: center;
			padding: 16px 18px;
			border-radius: 18px;
			background: rgba(255, 255, 255, 0.82);
			border: 1px solid rgba(167, 190, 180, 0.55);
			box-shadow: var(--shadow-soft);
			color: var(--ink);
		}

		.explorer-mobile-toggle span {
			color: var(--ink-soft);
			font-size: 0.92rem;
		}

		.results-table-header {
			grid-template-columns: repeat(2, minmax(0, 1fr));
		}
	}

	@media (max-width: 720px) {
		h1 {
			font-size: clamp(2.9rem, 12vw, 4rem);
		}

		.explorer-search,
		.results-toolbar {
			flex-direction: column;
		}

		.explorer-search button,
		.field-guide-link,
		.results-toolbar {
			width: 100%;
		}

		.results-toolbar {
			align-items: stretch;
		}

		.results-table-header {
			grid-template-columns: 1fr;
		}

		.filter-panel__header {
			flex-direction: column;
		}

		.filter-reset,
		.filter-clear {
			width: fit-content;
		}

		.year-row {
			grid-template-columns: 1fr;
		}

		.year-dash {
			display: none;
		}

		.field-guide-modal {
			padding: 12px;
		}

		.field-guide-modal__dialog {
			max-height: 92vh;
			border-radius: 22px;
		}

		.field-guide-modal__header {
			flex-direction: column;
			padding: 20px 18px 16px;
		}

		.field-guide-modal__close {
			width: fit-content;
		}

		.field-guide-modal__body {
			padding: 16px 18px 18px;
			max-height: calc(92vh - 156px);
		}
	}
</style>
