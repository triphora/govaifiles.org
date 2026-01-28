# surveillance-transparency-web

use `.env` file for configuration (a `.env.template` file is provided)

notes:
* [SORNs](https://github.com/emmalurie/surveillance-transparency#sorns-system-of-records-notices): XMLs processed to JSON
* [PRA data](https://github.com/emmalurie/surveillance-transparency#pra-paperwork-reduction-act-data): PDFs processed to CSV metadata
* [AI Use Case Inventory](https://github.com/emmalurie/surveillance-transparency#ai-use-case-inventory): published as CSV, processed to merge/standardize agency names

useful documentation links:
* frontend
  * [SvelteKit](https://svelte.dev/docs/kit)
* backend
  * search
    * [TypeSense](https://typesense.org/docs/guide/building-a-search-application.html)
    * [TypeSense Java](https://github.com/typesense/typesense-java)
    * [TypeSense debug dashboard](https://bfritscher.github.io/typesense-dashboard/#/)
  * db
    * [jOOQ](https://www.jooq.org/doc/3.20/manual/)
  * api
    * [Javalin](https://javalin.io/documentation)