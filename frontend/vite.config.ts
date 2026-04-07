import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
	Object.assign(process.env, loadEnv(mode, '..', ''));

	return {
		envDir: '..',
		plugins: [sveltekit()]
	};
});
