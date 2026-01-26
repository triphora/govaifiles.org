<script>
	import { PUBLIC_BACKEND_URL } from '$env/static/public';

	let query = "";
	let result = null;
	let error = null;
	let timeout;

	$: if (query) {
		clearTimeout(timeout);
		timeout = setTimeout(search, 300);
	} else {
		result = null;
		error = null;
	}

	async function search() {
		if (!query) return;

		error = null;
		result = null;

		try {
			const res = await fetch(`${PUBLIC_BACKEND_URL}/country/${query}`);
			if (!res.ok) {
				throw new Error(`HTTP ${res.status}`);
			}
			result = await res.json();
		} catch (e) {
			error = e.message;
		}
	}
</script>

<h1>Welcome to Homepage</h1>
<p>Visit <a href="https://svelte.dev/docs/kit">svelte.dev/docs/kit</a> to read the documentation</p>

<input
				type="text"
				bind:value={query}
				placeholder="Search country..."
/>

<button on:click={search}>
	Search
</button>

{#if error}
	<p style="color: red;">{error}</p>
{/if}

{#if result}
	<pre>{JSON.stringify(result, null, 2)}</pre>
{/if}
