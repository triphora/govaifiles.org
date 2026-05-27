<script lang="ts">
	import favicon from '$lib/assets/favicon.svg';
	import { page } from '$app/state';

	let { children } = $props();
	import "../app.css";

	const navItems = [
		{ href: '/', label: 'Explorer' },
		{ href: '/about', label: 'About' },
		{ href: '/graphs', label: 'Data' },
		{ href: '/ats-subgraph', label: 'ATS Subgraph' }
	];

	const currentPath = $derived(String(page.url.pathname));
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
	<title>Government AI Files</title>
</svelte:head>

<div class="site-shell">
	<header class="site-header">
		<div class="site-header__inner">
			<a class="site-brand" href="/">Government AI Files</a>
			<nav class="site-nav" aria-label="Primary">
				{#each navItems as item}
					{@const isActive = item.href === '/' ? currentPath === '/' || currentPath === '/field-guide' : currentPath === item.href}
					<a
						href={item.href}
						class:active={isActive}
						aria-current={isActive ? 'page' : undefined}
					>
						{item.label}
					</a>
				{/each}
			</nav>
		</div>
	</header>

	<main class="site-main">
		{@render children()}
	</main>
</div>
