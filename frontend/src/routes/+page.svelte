<script lang="ts">
	import { browser } from '$app/environment';
	import { env } from '$env/dynamic/public';
	import { onDestroy, onMount, tick } from 'svelte';
	import AiUseCaseRecord from '../components/AIUseCaseRecord.svelte';
	import FieldGuideContent from '../components/FieldGuideContent.svelte';
	import {
		agencyOptions,
		aiClassificationOptions as configuredAiClassificationOptions,
		complianceOptions,
		dhsComponents, getIcrsForInventoryRecord, getSornsForInventoryRecord,
		impactOptions,
		stageOptions,
		topicOptions as configuredTopicOptions,
		useCaseAnchorId
	} from '$lib/explorer';

	type UseCaseRecord = Record<string, string | undefined | null>;
	type InformationCollectionRequestRecord = Record<string, string | undefined | null>;
	type SystemOfRecordsNoticeRecord = Record<string, string | undefined | null>;
	type FilterKey =
		| 'agency'
		| 'bureau'
		| 'stage'
		| 'impact'
		| 'compliance'
		| 'topic'
		| 'aiClassification';
	type SortKey = 'useCase' | 'agency' | 'stage' | 'impact' | 'topic' | 'aiClassification';
	type EmptyStateMap = Record<string, string>;
	type MultiFilters = Record<FilterKey, string[]>;
	type SearchResponse = {
		hits: UseCaseRecord[];
		found?: number;
		icrs?: InformationCollectionRequestRecord[];
		sorns?: SystemOfRecordsNoticeRecord[];
	};
	type SectionKey = FilterKey | 'year' | 'score';
	type ExpandedSections = Record<SectionKey, boolean>;
	type SortState = { key: SortKey; direction: 'asc' | 'desc' };

	const yearsSupported = {
		from: 2024,
		to: 2025
	}

	const complianceScoreValues = {
		from: 0,
		to: 9
	}

	const defaultExpandedSections: ExpandedSections = {
		year: true,
		agency: true,
		bureau: true,
		stage: true,
		impact: true,
		compliance: true,
		topic: true,
		aiClassification: true,
		score: true
	};

	const defaultFilters: MultiFilters = {
		agency: [],
		bureau: [],
		stage: [],
		impact: [],
		compliance: [],
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
	const topLevelAgencyOptions = agencyOptions;
	const bureauOptionsByAgency: Record<string, string[]> = {
		'Department of Homeland Security': [...dhsComponents]
	};
	const SEARCH_DEBOUNCE_MS = 250;
	const BACKEND_URL = env.PUBLIC_BACKEND_URL ?? '';
	const BLANK_URL_VALUE = '__blank__';
	const agencyFacetAliases: Record<string, string> = {
		'Board of Governors of the Federal Reserve System': 'Federal Reserve Board of Governors',
		'U.S. Agency for Global Media': 'United States Agency for Global Media',
		'U.S. Agency for International Development': 'United States Agency for International Development',
		'U.S. Commission on Civil Rights': 'United States Commission on Civil Rights',
		'U.S. Election Assistance Commission': 'Election Assistance Commission'
	};

	let query = '';
	let loading = false;
	let error = '';
	let mobileFiltersOpen = false;
	let fieldGuideOpen = false;
	let shareDialogOpen = false;
	let fieldGuideDialog: HTMLDivElement | null = null;
	let shareDialog: HTMLDivElement | null = null;
	let lastFocusedElement: HTMLElement | null = null;
	let shareUrl = '';
	let shareTitle = '';
	let shareStatus = '';
	let serverResults: UseCaseRecord[] = [];
	let totalMatches = 0;
	let icrData: InformationCollectionRequestRecord[] = [];
	let sornData: SystemOfRecordsNoticeRecord[] = [];
	let filters: MultiFilters = { ...defaultFilters };
	let expandedSections: ExpandedSections = { ...defaultExpandedSections };
	let sortState: SortState = { key: 'useCase', direction: 'asc' };
	let yearFrom = '';
	let yearTo = '';
	let complianceFrom = '';
	let complianceTo = '';
	let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null;
	let activeSearchController: AbortController | null = null;
	let latestSearchRequest = 0;

	const emptyStateMessages: EmptyStateMap = {
		'Department of Defense':
			'The Department of Defense is exempt from submitting an AI Use Case Inventory for intelligence reasons.',
		'Consumer Financial Protection Bureau':
			'The Consumer Financial Protection Bureau has no reported AI use cases in 2025.',
		'United States Agency for International Development':
			'The United States Agency for International Development has not published a 2025 AI Use Case Inventory. The agency is defunct as of February 2025.',
		'Presidio Trust':
			'The Presidio Trust did not publish a 2025 AI Use Case Inventory.',
		'United States Agency for Global Media':
			'The United States Agency for Global Media has not published a 2025 AI Use Case Inventory.',
		'United States Commission on Civil Rights':
			'The United States Commission on Civil Rights has not published a 2025 AI Use Case Inventory.'
	};

	$: topicOptions = filterOptionsByRetrieval('topic', configuredTopicOptions);
	$: aiClassificationOptions = filterOptionsByRetrieval(
		'aiClassification',
		configuredAiClassificationOptions
	);
	$: agencyFilterOptions = filterOptionsByRetrieval('agency', topLevelAgencyOptions);
	$: stageFilterOptions = filterOptionsByRetrieval('stage', stageOptions);
	$: bureauFilterOptions = filters.agency.flatMap(
		(agency) => bureauOptionsByAgency[agency as keyof typeof bureauOptionsByAgency] ?? []
	);
	$: visibleAgencyFilterOptions = agencyFilterOptions;
	$: visibleBureauFilterOptions = bureauFilterOptions;
	$: visibleStageFilterOptions = stageFilterOptions;
	$: visibleImpactFilterOptions = impactSelectableOptions;
	$: visibleComplianceOptions = complianceOptions;
	$: visibleTopicOptions = topicOptions;
	$: visibleAiClassificationOptions = aiClassificationOptions;
	$: results = [...serverResults].sort((left, right) =>
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
		applyStateFromUrl(new URL(window.location.href));

		const handlePopstate = () => {
			applyStateFromUrl(new URL(window.location.href));
		};

		const handleHashchange = () => {
			void scrollToHashTarget();
		};

		window.addEventListener('popstate', handlePopstate);
		window.addEventListener('hashchange', handleHashchange);

		return () => {
			window.removeEventListener('popstate', handlePopstate);
			window.removeEventListener('hashchange', handleHashchange);
		};
	});

	onDestroy(() => {
		clearPendingSearch();
		activeSearchController?.abort();
		if (browser) {
			document.body.style.overflow = '';
		}
	});

	async function openFieldGuide() {
		if (!browser) {
			return;
		}

		lastFocusedElement =
			document.activeElement instanceof HTMLElement ? document.activeElement : null;
		fieldGuideOpen = true;
		updatePageScrollLock();
		await tick();
		fieldGuideDialog?.focus();
	}

	function closeFieldGuide() {
		fieldGuideOpen = false;
		updatePageScrollLock();
		if (shareDialogOpen) {
			return;
		}

		lastFocusedElement?.focus();
	}

	function updatePageScrollLock() {
		if (!browser) {
			return;
		}

		document.body.style.overflow = fieldGuideOpen || shareDialogOpen ? 'hidden' : '';
	}

	function handleWindowKeydown(event: KeyboardEvent) {
		if (!fieldGuideOpen && !shareDialogOpen) {
			return;
		}

		if (event.key === 'Escape') {
			event.preventDefault();
			if (shareDialogOpen) {
				closeShareDialog();
				return;
			}

			closeFieldGuide();
		}
	}

	async function openShareDialog(anchorId: string, title: string) {
		if (!browser) {
			return;
		}

		const nextUrl = buildUrlFromState();
		nextUrl.hash = anchorId;
		shareUrl = nextUrl.toString();
		shareTitle = title;
		shareStatus = 'Link copied to your clipboard.';
		lastFocusedElement =
			document.activeElement instanceof HTMLElement ? document.activeElement : null;

		try {
			await navigator.clipboard.writeText(shareUrl);
		} catch {
			shareStatus = 'Clipboard copy did not work here. You can still copy the link below.';
		}

		shareDialogOpen = true;
		updatePageScrollLock();
		await tick();
		shareDialog?.focus();
	}

	function closeShareDialog() {
		shareDialogOpen = false;
		updatePageScrollLock();
		if (fieldGuideOpen) {
			return;
		}

		lastFocusedElement?.focus();
	}

	async function copyShareUrlAgain() {
		if (!browser || !shareUrl) {
			return;
		}

		try {
			await navigator.clipboard.writeText(shareUrl);
			shareStatus = 'Link copied to your clipboard.';
		} catch {
			shareStatus = 'Clipboard copy is still blocked. Please copy the link manually.';
		}
	}

	function handleShare(event: CustomEvent<{ anchorId: string; title: string }>) {
		void openShareDialog(event.detail.anchorId, event.detail.title);
	}

	function normalizeFilterValue(value: string | null | undefined) {
		return (value || '').trim();
	}

	function agencyLabel(result: UseCaseRecord) {
		return [normalizeFilterValue(result.bureau_component), normalizeFilterValue(result.agency)]
			.filter(Boolean)
			.join(' · ');
	}

	function getImpactValue(result: UseCaseRecord) {
		return normalizeFilterValue(result.high_impact_status ?? result.is_high_impact);
	}

	function parseYearInput(value: string) {
		const normalizedValue = normalizeFilterValue(value);
		if (!/^\d{4}$/.test(normalizedValue)) {
			return null;
		}

		return Number.parseInt(normalizedValue, 10);
	}

	function uniqueValues(values: Array<string | null | undefined>) {
		const seen = new Set<string>();
		const normalizedValues: string[] = [];

		for (const value of values) {
			const normalizedValue = normalizeFilterValue(value);
			if (!normalizedValue || seen.has(normalizedValue)) {
				continue;
			}

			seen.add(normalizedValue);
			normalizedValues.push(normalizedValue);
		}

		return normalizedValues;
	}

	function parseUrlSelection(url: URL, key: string, allowedValues?: string[]) {
		const values = uniqueValues(url.searchParams.getAll(key));
		if (!allowedValues) {
			return values;
		}

		return values.filter((value) => allowedValues.includes(value));
	}

	function inferAgencySelections(bureauSelections: string[]) {
		const inferredAgencies = Object.entries(bureauOptionsByAgency)
			.filter(([, bureauOptions]) =>
				bureauSelections.some((bureau) => bureauOptions.includes(bureau))
			)
			.map(([agency]) => agency);

		return uniqueValues(inferredAgencies);
	}

	function parseUrlState(url: URL): {
		query: string;
		filters: MultiFilters;
		yearFrom: string;
		yearTo: string;
		sortState: SortState;
	} {
		const agencySelections = parseUrlSelection(url, 'agency', topLevelAgencyOptions);
		const bureauSelections = parseUrlSelection(
			url,
			'bureau',
			Object.values(bureauOptionsByAgency).flat()
		);
		const nextAgencySelections =
			agencySelections.length > 0 ? agencySelections : inferAgencySelections(bureauSelections);
		const availableBureaus = nextAgencySelections.flatMap(
			(agency) => bureauOptionsByAgency[agency as keyof typeof bureauOptionsByAgency] ?? []
		);

		const nextFilters: MultiFilters = {
			agency: nextAgencySelections,
			bureau: bureauSelections.filter((bureau) => availableBureaus.includes(bureau)),
			stage: parseUrlSelection(url, 'stage', stageOptions),
			impact: uniqueValues(
				url.searchParams.getAll('impact').map((value) => (value === BLANK_URL_VALUE ? '' : value))
			).filter((value) => impactSelectableOptions.includes(value)),
			compliance: parseUrlSelection(url, 'compliance', complianceOptions),
			topic: parseUrlSelection(url, 'topic', configuredTopicOptions),
			aiClassification: parseUrlSelection(
				url,
				'aiClassification',
				configuredAiClassificationOptions
			)
		};

		const nextSortKey = url.searchParams.get('sort');
		const nextSortDirection = url.searchParams.get('dir');

		return {
			query: normalizeFilterValue(url.searchParams.get('q')),
			filters: nextFilters,
			yearFrom: normalizeFilterValue(url.searchParams.get('yearFrom')),
			yearTo: normalizeFilterValue(url.searchParams.get('yearTo')),
			sortState: {
				key: sortColumns.some((column) => column.key === nextSortKey)
					? (nextSortKey as SortKey)
					: 'useCase',
				direction: nextSortDirection === 'desc' ? 'desc' : 'asc'
			}
		};
	}

	function buildUrlFromState() {
		const url = new URL(window.location.href);
		const params = new URLSearchParams();

		if (query.trim()) {
			params.set('q', query.trim());
		}

		for (const agency of filters.agency) {
			params.append('agency', agency);
		}

		for (const bureau of filters.bureau) {
			params.append('bureau', bureau);
		}

		for (const stage of filters.stage) {
			params.append('stage', stage);
		}

		for (const impact of filters.impact) {
			params.append('impact', impact === '' ? BLANK_URL_VALUE : impact);
		}

		for (const topic of filters.topic) {
			params.append('topic', topic);
		}

		for (const compliance of filters.compliance) {
			params.append('compliance', compliance);
		}

		for (const aiClassification of filters.aiClassification) {
			params.append('aiClassification', aiClassification);
		}

		if (yearFrom.trim()) {
			params.set('yearFrom', yearFrom.trim());
		}

		if (yearTo.trim()) {
			params.set('yearTo', yearTo.trim());
		}

		if (sortState.key !== 'useCase') {
			params.set('sort', sortState.key);
		}

		if (sortState.direction !== 'asc') {
			params.set('dir', sortState.direction);
		}

		url.search = params.toString();
		return url;
	}

	function syncUrlWithState() {
		if (!browser) {
			return;
		}

		const nextUrl = buildUrlFromState();
		if (nextUrl.toString() === window.location.href) {
			return;
		}

		history.replaceState(history.state, '', nextUrl);
	}

	function applyStateFromUrl(url: URL) {
		const nextState = parseUrlState(url);
		const nextQuery = nextState.query;
		const nextFilters = nextState.filters;
		const nextYearFrom = nextState.yearFrom;
		const nextYearTo = nextState.yearTo;
		const nextSortState = nextState.sortState;
		query = nextQuery;
		filters = nextFilters;
		yearFrom = nextYearFrom;
		yearTo = nextYearTo;
		sortState = nextSortState;

		clearPendingSearch();
		void search();
	}

	async function scrollToHashTarget() {
		if (!browser || !window.location.hash) {
			return;
		}

		await tick();
		const targetId = decodeURIComponent(window.location.hash.slice(1));
		const target = document.getElementById(targetId);
		if (!target) {
			return;
		}

		target.scrollIntoView({ block: 'start' });
	}

	function normalizeFacetValue(key: FilterKey, value: string) {
		const normalizedValue = normalizeFilterValue(value);
		if (!normalizedValue) {
			return '';
		}

		switch (key) {
			case 'agency':
				return agencyFacetAliases[normalizedValue] ?? normalizedValue;
			case 'stage':
				return normalizedValue.replace(/_/g, '-');
			case 'topic': {
				const simplified = normalizedValue.toLowerCase();
				if (simplified === 'law enforcement') {
					return 'Law & Justice';
				}
				if (simplified === 'service delivery') {
					return 'Government Services (includes Benefits and Service Delivery)';
				}
				return normalizedValue;
			}
			case 'aiClassification': {
				if (/^agentic ai/i.test(normalizedValue) || /^agentic-ai/i.test(normalizedValue)) {
					return 'Agentic AI';
				}
				if (/^classical\/predictive machine learning/i.test(normalizedValue)) {
					return 'Classical/Predictive Machine Learning';
				}
				if (/^computer vision/i.test(normalizedValue)) {
					return 'Computer Vision';
				}
				if (/^generative ai/i.test(normalizedValue)) {
					return 'Generative AI';
				}
				if (/^(natural language processing|nlp)/i.test(normalizedValue)) {
					return 'Natural Language Processing';
				}
				if (/^reinforcement learning/i.test(normalizedValue)) {
					return 'Reinforcement Learning';
				}
				if (/^other/i.test(normalizedValue)) {
					return 'Other';
				}
				return normalizedValue;
			}
			default:
				return normalizedValue;
		}
	}

	function expandFilterRequestValues(key: FilterKey, value: string) {
		if (key === 'impact') {
			return [value === '' ? BLANK_URL_VALUE : value];
		}

		if (key === 'bureau' || key === 'compliance') {
			return [value];
		}

		return [normalizeFacetValue(key, value)];
	}

	function filterOptionsByRetrieval(key: FilterKey, options: string[]) {
		return options;
	}

	function isAllSelected(key: FilterKey, options: string[]) {
		return options.length > 0 && options.every((option) => filters[key].includes(option));
	}

	function toggleAllFilterValues(key: FilterKey, options: string[]) {
		const nextValues = isAllSelected(key, options) ? [] : [...options];

		if (key === 'agency') {
			const nextBureauOptions = nextValues.flatMap(
				(agency) => bureauOptionsByAgency[agency as keyof typeof bureauOptionsByAgency] ?? []
			);
			filters = {
				...filters,
				agency: nextValues,
				bureau: filters.bureau.filter((bureau) => nextBureauOptions.includes(bureau))
			};
			applyFilters();
			return;
		}

		filters = {
			...filters,
			[key]: nextValues
		};
		applyFilters();
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
				return getImpactValue(result);
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
			syncUrlWithState();
			void search();
		}, SEARCH_DEBOUNCE_MS);
	}

	function applyFilters() {
		clearPendingSearch();
		syncUrlWithState();
		void search();
	}

	function submitSearch() {
		clearPendingSearch();
		syncUrlWithState();
		void search();
	}

	function handleExactFilters(input: String[]) {
		if (input.length == 1) {
			return `=${input[0]}`;
		} else {
			return JSON.stringify(input);
		}
	}

	async function search() {
		const requestId = ++latestSearchRequest;
		const controller = new AbortController();

		activeSearchController?.abort();
		activeSearchController = controller;
		loading = true;
		error = '';

		const searchParams = new URLSearchParams();

		if (filters.agency.length > 0) {
			searchParams.append('agency', JSON.stringify(filters.agency));
		}

		if (filters.bureau.length > 0) {
			searchParams.append('bureau_component', JSON.stringify(filters.bureau));
		}

		if (filters.stage.length > 0) {
			searchParams.append('stage_of_development', JSON.stringify(filters.stage));
		}

		if (filters.impact.length > 0) {
			searchParams.append('high_impact_status', handleExactFilters(filters.impact));
		}

		if (filters.compliance.length > 0) {
			searchParams.append('compliance_status', handleExactFilters(filters.compliance));
		}

		if (filters.topic.length > 0) {
			searchParams.append('use_case_topic_area', JSON.stringify(filters.topic));
		}

		if (filters.aiClassification.length > 0) {
			searchParams.append('ai_classification', JSON.stringify(filters.aiClassification));
		}

		const parsedYearFrom = parseYearInput(yearFrom);
		const parsedYearTo = parseYearInput(yearTo);

		if (parsedYearFrom !== null && parsedYearTo !== null) {
			searchParams.set('data_year', `[${parsedYearFrom}..${parsedYearTo}]`)
		} else if (parsedYearFrom !== null) {
			searchParams.set('data_year', `[${parsedYearFrom}..${yearsSupported.to}]`)
		} else if (parsedYearTo !== null) {
			searchParams.set('data_year', `[${yearsSupported.from}..${parsedYearTo}]`)
		}

		if (complianceFrom || complianceTo) {
			searchParams.set('risk_management_compliance_score',
					`[${complianceFrom || complianceScoreValues.from}..${complianceTo || complianceScoreValues.to}]`)
		}

		const queryString = `${BACKEND_URL}/ai-use-cases/${encodeURIComponent(query.trim() || '*')}${searchParams.size > 0 ? `?${searchParams.toString()}` : ''}`;

		try {
			const response = await fetch(queryString, { signal: controller.signal });
			if (!response.ok) {
				throw new Error(`HTTP ${response.status}`);
			}

			const payload: SearchResponse = await response.json();
			if (requestId !== latestSearchRequest) {
				return;
			}

			serverResults = payload.hits ?? [];
			totalMatches = payload.found ?? payload.hits?.length ?? 0;
			icrData = payload.icrs ?? [];
			sornData = payload.sorns ?? [];

			void scrollToHashTarget();
		} catch (searchError) {
			if (searchError instanceof DOMException && searchError.name === 'AbortError') {
				return;
			}

			error = searchError instanceof Error ? searchError.message : 'Unknown error';
			serverResults = [];
			totalMatches = 0;
			icrData = [];
			sornData = [];
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

		if (key === 'agency') {
			const nextBureauOptions = nextValues.flatMap(
				(agency) => bureauOptionsByAgency[agency as keyof typeof bureauOptionsByAgency] ?? []
			);

			filters = {
				...filters,
				agency: nextValues,
				bureau: filters.bureau.filter((bureau) => nextBureauOptions.includes(bureau))
			};
			applyFilters();
			return;
		}

		filters = { ...filters, [key]: nextValues };
		applyFilters();
	}

	function clearFilterGroup(key: FilterKey) {
		if (key === 'agency') {
			filters = { ...filters, agency: [], bureau: [] };
			applyFilters();
			return;
		}

		filters = { ...filters, [key]: [] };
		applyFilters();
	}

	function clearAllFilters() {
		query = '';
		filters = { ...defaultFilters };
		yearFrom = '';
		yearTo = '';
		complianceFrom = '';
		complianceTo = '';
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
		syncUrlWithState();
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
			<p class="explorer-kicker">{yearsSupported.from}–{yearsSupported.to} Federal Inventory</p>
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
									on:input={queueSearch}
									placeholder={yearsSupported.from}
									aria-label="Year from"
								/>
								<span class="year-dash">-</span>
								<input
									bind:value={yearTo}
									class="year-input"
									type="text"
									inputmode="numeric"
									maxlength="4"
									on:input={queueSearch}
									placeholder={yearsSupported.to}
									aria-label="Year to"
								/>
							</div>
						</div>
					{/if}
				</section>

				{#if visibleAgencyFilterOptions.length > 0}
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
									class:checked={isAllSelected('agency', visibleAgencyFilterOptions)}
									class="checkbox-item"
									on:click={() => toggleAllFilterValues('agency', visibleAgencyFilterOptions)}
								>
									<span class="checkbox-mark"
										>{isAllSelected('agency', visibleAgencyFilterOptions) ? '✓' : ''}</span
									>
								<span class="checkbox-label">Select All</span>
							</button>
								<!-- TODO add "All Cabinet Agencies" entry -->
								{#each visibleAgencyFilterOptions as agency (agency)}
									<button
										type="button"
										class:checked={filters.agency.includes(agency)}
										class="checkbox-item"
										on:click={() => toggleFilterValue('agency', agency)}
									>
										<span class="checkbox-mark">{filters.agency.includes(agency) ? '✓' : ''}</span>
										<span class="checkbox-label">{agency}</span>
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
				{/if}

				{#if visibleBureauFilterOptions.length > 0}
					<section class="filter-section">
						<button
							type="button"
							class="filter-section__toggle"
							on:click={() => toggleSection('bureau')}
							aria-expanded={expandedSections.bureau}
						>
							<span>Bureau / Component</span>
							<span
								class:expanded={expandedSections.bureau}
								class="filter-section__icon"
								aria-hidden="true"
							></span>
						</button>
						{#if expandedSections.bureau}
							<div class="filter-section__body">
								<div class="checkbox-list">
									<button
										type="button"
										class:checked={isAllSelected('bureau', visibleBureauFilterOptions)}
										class="checkbox-item"
										on:click={() => toggleAllFilterValues('bureau', visibleBureauFilterOptions)}
									>
										<span class="checkbox-mark"
										>{isAllSelected('bureau', visibleBureauFilterOptions) ? '✓' : ''}</span
									>
								<span class="checkbox-label">Select All</span>
							</button>
									{#each visibleBureauFilterOptions as bureau (bureau)}
										<button
											type="button"
											class:checked={filters.bureau.includes(bureau)}
											class="checkbox-item"
											on:click={() => toggleFilterValue('bureau', bureau)}
										>
										<span class="checkbox-mark">{filters.bureau.includes(bureau) ? '✓' : ''}</span
										>
										<span class="checkbox-label">{bureau}</span>
								</button>
								{/each}
								</div>
								{#if filters.bureau.length > 0}
									<button
										type="button"
										class="filter-clear"
										on:click={() => clearFilterGroup('bureau')}>Clear bureau</button
									>
								{/if}
							</div>
						{/if}
					</section>
				{/if}

				{#if visibleStageFilterOptions.length > 0}
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
								class:checked={isAllSelected('stage', visibleStageFilterOptions)}
								class="checkbox-item"
								on:click={() => toggleAllFilterValues('stage', visibleStageFilterOptions)}
							>
								<span class="checkbox-mark"
									>{isAllSelected('stage', visibleStageFilterOptions) ? '✓' : ''}</span
								>
								<span class="checkbox-label">All</span>
							</button>
							{#each visibleStageFilterOptions as stage (stage)}
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
				{/if}

				{#if visibleImpactFilterOptions.length > 0}
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
									class:checked={isAllSelected('impact', visibleImpactFilterOptions)}
									class="checkbox-item"
									on:click={() => toggleAllFilterValues('impact', visibleImpactFilterOptions)}
								>
									<span class="checkbox-mark"
									>{isAllSelected('impact', visibleImpactFilterOptions) ? '✓' : ''}</span
									>
									<span class="checkbox-label">All</span>
								</button>
								{#each impactOptions.filter((option) => visibleImpactFilterOptions.includes(option.value)) as option (option.value)}
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
												class={`checkbox-dot checkbox-dot--${option.value === 'high_impact' ? 'impact-high' : 'impact-low'}`}
											></span>{option.label}</span
										>
									</button>
								{/each}
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
				{/if}

				{#if visibleComplianceOptions.length > 0}
				<section class="filter-section">
					<button
						type="button"
						class="filter-section__toggle"
						on:click={() => toggleSection('compliance')}
						aria-expanded={expandedSections.compliance}
					>
						<span>Compliance</span>
						<span
							class:expanded={expandedSections.compliance}
							class="filter-section__icon"
							aria-hidden="true"
						></span>
					</button>
					{#if expandedSections.compliance}
						<div class="filter-section__body">
							<div class="checkbox-list">
								<button
									type="button"
									class:checked={isAllSelected('compliance', visibleComplianceOptions)}
									class="checkbox-item"
									on:click={() => toggleAllFilterValues('compliance', visibleComplianceOptions)}
								>
									<span class="checkbox-mark"
									>{isAllSelected('compliance', visibleComplianceOptions) ? '✓' : ''}</span
									>
									<span class="checkbox-label">All</span>
								</button>
								{#each visibleComplianceOptions as option (option)}
									<button
										type="button"
										class:checked={filters.compliance.includes(option)}
										class="checkbox-item"
										on:click={() => toggleFilterValue('compliance', option)}
									>
										<span class="checkbox-mark"
										>{filters.compliance.includes(option) ? '✓' : ''}</span
										>
										<span class="checkbox-label">{option}</span>
									</button>
								{/each}
							</div>
							{#if filters.compliance.length > 0}
								<button
									type="button"
									class="filter-clear"
									on:click={() => {
										complianceFrom = '';
										complianceTo = '';
										clearFilterGroup('compliance');
									}}>Clear compliance</button
								>
							{/if}
						</div>
					{/if}
				</section>
				{/if}

				{#if filters.compliance.includes('Not in compliance')}
					<section class="filter-section">
						<button
								type="button"
								class="filter-section__toggle"
								on:click={() => toggleSection('score')}
								aria-expanded={expandedSections.score}
						>
							<span>Compliance Score</span>
							<span
									class:expanded={expandedSections.score}
									class="filter-section__icon"
									aria-hidden="true"
							></span>
						</button>
						{#if expandedSections.score}
							<div class="filter-section__body">
								<div class="year-row">
									<input
											bind:value={complianceFrom}
											class="year-input"
											type="text"
											inputmode="numeric"
											maxlength="1"
											on:input={queueSearch}
											placeholder={complianceScoreValues.from}
											aria-label="Score from"
									/>
									<span class="year-dash">-</span>
									<input
											bind:value={complianceTo}
											class="year-input"
											type="text"
											inputmode="numeric"
											maxlength="1"
											on:input={queueSearch}
											placeholder={complianceScoreValues.to}
											aria-label="Score to"
									/>
								</div>
							</div>
						{/if}
					</section>
				{/if}

				{#if visibleTopicOptions.length > 0}
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
									class:checked={isAllSelected('topic', visibleTopicOptions)}
									class="checkbox-item"
									on:click={() => toggleAllFilterValues('topic', visibleTopicOptions)}
								>
									<span class="checkbox-mark"
										>{isAllSelected('topic', visibleTopicOptions) ? '✓' : ''}</span
									>
									<span class="checkbox-label">Select All</span>
								</button>
								{#each visibleTopicOptions as topic (topic)}
									<button
										type="button"
										class:checked={filters.topic.includes(topic)}
										class="checkbox-item"
										on:click={() => toggleFilterValue('topic', topic)}
									>
										<span class="checkbox-mark">{filters.topic.includes(topic) ? '✓' : ''}</span>
										<span class="checkbox-label">{topic}</span>
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
				{/if}

				{#if visibleAiClassificationOptions.length > 0}
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
									class:checked={isAllSelected('aiClassification', visibleAiClassificationOptions)}
									class="checkbox-item"
									on:click={() =>
										toggleAllFilterValues('aiClassification', visibleAiClassificationOptions)}
								>
									<span class="checkbox-mark"
										>{isAllSelected('aiClassification', visibleAiClassificationOptions) ? '✓' : ''}</span
									>
									<span class="checkbox-label">Select All</span>
								</button>
								{#each visibleAiClassificationOptions as option (option)}
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
				{/if}

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
						<strong>{totalMatches}</strong>
						<span>{results.length === 1 ? 'case' : 'cases'}</span>
						<span>{totalMatches > results.length ? `(${results.length} shown)` : ''}</span>
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
						<AiUseCaseRecord
							{result}
							icrs={getIcrsForInventoryRecord(result, icrData)}
							sorns={getSornsForInventoryRecord(result, sornData)}
							index={index + 1}
							anchorId={useCaseAnchorId(result)}
							on:share={handleShare}
						/>
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

{#if shareDialogOpen}
	<div class="field-guide-modal">
		<button
			type="button"
			class="field-guide-modal__backdrop"
			aria-label="Close share dialog"
			on:click={closeShareDialog}
		></button>
		<div
			bind:this={shareDialog}
			class="field-guide-modal__dialog field-guide-modal__dialog--share"
			role="dialog"
			aria-modal="true"
			aria-labelledby="share-dialog-title"
			aria-describedby="share-dialog-description"
			tabindex="-1"
		>
			<div class="share-dialog-orb" aria-hidden="true"></div>
			<div class="field-guide-modal__body share-dialog-body">
				<div class="share-dialog-heading">
					<h2 id="share-dialog-title">Share this record</h2>
					<p id="share-dialog-description" class="share-dialog-copy">
						The link of {shareTitle} has been copied, share it now!
					</p>
					<p class="share-dialog-status">{shareStatus}</p>
				</div>
				<div class="share-dialog-actions">
					<button
						type="button"
						class="share-dialog-button share-dialog-button--primary"
						on:click={copyShareUrlAgain}
					>
						Copy again
					</button>
					<button
						type="button"
						class="share-dialog-button share-dialog-button--secondary"
						on:click={closeShareDialog}
					>
						Close
					</button>
				</div>
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
		background: #476280;
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

	.field-guide-modal__dialog--share {
		position: relative;
		width: min(620px, 100%);
		background: #ffffff;
		box-shadow: 0 28px 80px rgba(23, 41, 34, 0.18);
	}

	.share-dialog-body {
		max-height: none;
		position: relative;
		gap: 26px;
		padding: 40px 36px 34px;
		z-index: 1;
	}

	.share-dialog-orb {
		position: absolute;
		top: -118px;
		left: 50%;
		width: 300px;
		height: 300px;
		border-radius: 999px;
		background: radial-gradient(
			circle at 30% 30%,
			rgba(132, 184, 158, 0.78),
			rgba(90, 140, 121, 0.96)
		);
		transform: translateX(-50%);
		opacity: 0.14;
		pointer-events: none;
	}

	.share-dialog-heading {
		display: grid;
		gap: 14px;
		justify-items: start;
	}

	.share-dialog-heading h2 {
		margin: 0;
		font-family: var(--font-serif);
		font-size: clamp(2.05rem, 4vw, 2.8rem);
		font-weight: 500;
		line-height: 1;
		letter-spacing: -0.03em;
		color: #24473a;
	}

	.share-dialog-copy {
		margin: 0;
		max-width: 30rem;
		font-size: 1.04rem;
		line-height: 1.65;
		color: #4f675d;
	}

	.share-dialog-status {
		margin: 0;
		font-size: 0.94rem;
		line-height: 1.5;
		color: #6b847a;
	}

	.share-dialog-actions {
		display: flex;
		align-items: center;
		gap: 14px;
		padding-top: 4px;
	}

	.share-dialog-button {
		min-width: 144px;
		padding: 14px 22px;
		border-radius: 999px;
		font-size: 0.98rem;
		font-weight: 600;
		transition:
			transform 0.16s ease,
			background-color 0.16s ease,
			border-color 0.16s ease,
			color 0.16s ease,
			box-shadow 0.16s ease;
	}

	.share-dialog-button:hover {
		transform: translateY(-1px);
	}

	.share-dialog-button--primary {
		background: linear-gradient(180deg, #72a78d, #5d947b);
		color: #ffffff;
		box-shadow: 0 12px 24px rgba(93, 148, 123, 0.24);
	}

	.share-dialog-button--secondary {
		border: 1px solid rgba(115, 144, 130, 0.3);
		background: #ffffff;
		color: #4f675d;
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

		.field-guide-modal__header--share {
			padding: 24px 18px 18px;
			align-items: center;
		}

		.field-guide-modal__close {
			width: fit-content;
		}

		.field-guide-modal__body {
			padding: 16px 18px 18px;
			max-height: calc(92vh - 156px);
		}

		.share-dialog-body {
			padding: 28px 20px 24px;
		}

		.share-dialog-actions {
			flex-direction: column;
		}

		.share-dialog-button {
			width: 100%;
		}

		.share-dialog-orb {
			top: -96px;
			width: 240px;
			height: 240px;
		}
	}
</style>
