# surveillance-transparency-web

Requirements:
* Java Development Kit (tested with 25, but anything above 17 should work)
* [Docker-compose](https://docs.docker.com/compose/install/)
* [pnpm](https://pnpm.io/installation) (plain npm may also work, untested)

Getting started:
1. Configure your data by copying `.env.template` to `.env` and changing any values
1. Populate inventory data with `git submodule init && git submodule update`
1. Start backend support services (search and database) with `docker compose up` and allow it to run in the background
1. In the `backend` directory, run `./gradlew build`
1. Run `java -jar build/libs/backend-all.jar` and allow it to run in the background
   * Make sure `.env` is still in your classpath
1. In the `frontend` directory, run `pnpm install` followed by `pnpm dev`
1. You should now have a working site on `localhost:4173`
   * If you want a production-ready site, run `pnpm build` followed by `pnpm serve`