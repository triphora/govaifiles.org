<script>
	import { PUBLIC_BACKEND_URL } from '$env/static/public';
	import FrtRecord from "../components/FRTRecord.svelte";

	let query = "";

	let filters = {
		category: null,
		aiUseCase: false,
		sorns: false,
		praDocs: false,
	}

	let results = null;
	let error = null;

	$: if (query || filters) {
		search();
	} else {
		results = null;
		error = null;
	}

	async function search() {
		let queryString = `${PUBLIC_BACKEND_URL}/dhs_2025/${query || "*"}?`

		if (filters.category) {
			queryString += `use_case_category=${filters.category}&`
		}

		if (filters.aiUseCase) {
			queryString += `in_ai_inventory=true&`
		}

		if (filters.sorns) {
			queryString += `in_sorns=true&`
		}

		if (filters.praDocs) {
			queryString += `in_pra=true`
		}

		try {
			const res = await fetch(queryString);
			if (!res.ok) {
				throw new Error(`HTTP ${res.status}`);
			}
			results = await res.json();
		} catch (e) {
			error = e.message;
		}
	}
</script>

<h1>AI Disclosures</h1>

<div class="flexbox">
<fieldset class="flex-content">
	<legend>Use Case Category</legend>
	<div>
		<input type="radio" name="category" id="all" on:click={filters.category = null} checked />
		<label for="all">All</label>
	</div>
	<div>
		<input type="radio" name="category" id="law_enforcement" on:click={filters.category = "law_enforcement"} />
		<label for="law_enforcement">Law Enforcement</label>
	</div>
	<div>
		<input type="radio" name="category" id="immigration" on:click={filters.category = "immigration"} />
		<label for="immigration">Immigration</label>
	</div>
	<div>
		<input type="radio" name="category" id="benefits" on:click={filters.category = "benefits"} />
		<label for="benefits">Benefits</label>
	</div>
	<div>
		<input type="radio" name="category" id="transportation" on:click={filters.category = "transportation"} />
		<label for="transportation">Transportation</label>
	</div>
	<div>
		<input type="radio" name="category" id="internal" on:click={filters.category = "internal"} />
		<label for="internal">Internal</label>
	</div>
	<div>
		<input type="radio" name="category" id="health" on:click={filters.category = "health"} />
		<label for="health">Health</label>
	</div>
	<div>
		<input type="radio" name="category" id="other" on:click={filters.category = "other"} />
		<label for="other">Uncategorized</label>
	</div>
</fieldset>

<fieldset class="flex-content">
	<legend>Disclosure Patterns</legend>
	<div>
		<input type="checkbox" name="category" id="AIUseCase" bind:checked={filters.aiUseCase}/>
		<label for="AIUseCase">AI Use Case Inventory</label>
	</div>
	<div>
		<input type="checkbox" name="category" id="SORNs" bind:checked={filters.sorns} />
		<label for="SORNs">Systems of Records Notices</label>
	</div>
	<div>
		<input type="checkbox" name="category" id="PRA" bind:checked={filters.praDocs} />
		<label for="PRA">Paperwork Reduction Act Documents</label>
	</div>
</fieldset>
</div>

<input type="text" bind:value={query} placeholder="Search..." />

<button on:click={search}>
	Search
</button>

{#if error}
	<p style="color: red;">{error}</p>
{/if}

{#if results && results.length == 0}
	<p style="color: red;">No results</p>
{/if}

{#each results as result (result.id)}
	<FrtRecord {result} />
{/each}

<style>
	.flexbox {
		display: flex;
		flex-wrap: wrap;
		gap: 1em;
	}

	.flex-content {
		flex: 1 1 calc(50% - 1em);
		box-sizing: border-box;
	}

	fieldset {
		margin-bottom: 1em;
	}
</style>