package dev.fauser.surveillance_transparency;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import io.javalin.http.NotImplementedResponse;
import org.jetbrains.annotations.NotNull;
import org.typesense.api.Client;
import org.typesense.api.Configuration;
import org.typesense.model.SearchParameters;
import org.typesense.model.SearchResult;
import org.typesense.resources.Node;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class APIHandler implements CrudHandler {
	public final ArrayList<JsonObject> countries = new ArrayList<>();

	@Override
	public void create(@NotNull Context ctx) {
		throw new NotImplementedResponse("not implemented");
	}

	@Override
	public void getAll(@NotNull Context ctx) {
		ctx.json(Arrays.toString(countries.toArray()));
	}

	@Override
	public void getOne(@NotNull Context ctx, @NotNull String s) {
		String useCaseCategory = ctx.queryParam("use_case_category");
		boolean inAiInventory = Boolean.parseBoolean(ctx.queryParam("in_ai_inventory"));
		boolean inSorns = Boolean.parseBoolean(ctx.queryParam("in_sorns"));
		boolean inPraDocs = Boolean.parseBoolean(ctx.queryParam("in_pra"));

		Dotenv env = Dotenv.load();
		ArrayList<JsonObject> hits = new ArrayList<>();

		try {
			List<Node> nodes = new ArrayList<>();

			nodes.add(new Node("http", "localhost", "8108"));

			Configuration configuration = new Configuration(nodes, Duration.ofSeconds(2), env.get("TYPESENSE_API_KEY"));

			Client client = new Client(configuration);

			SearchParameters searchParameters = new SearchParameters()
				.q(s)
				.queryBy("matching_name,canonical_agency,canonical_sub_agency,use_case_category,matched_terms,ai_description,sorn_description,pra_description,sorn_document_number,sorn_system_name")
				.limit(250);

			List<String> filters = new ArrayList<>();

			if (useCaseCategory != null) filters.add("use_case_category:" + useCaseCategory.toUpperCase());
			if (inAiInventory) filters.add("in_ai_inventory:true");
			if (inSorns) filters.add("in_sorns:true");
			if (inPraDocs) filters.add("in_pra:true");

			if (!filters.isEmpty()) {
				searchParameters.filterBy(String.join(" && ", filters));
			}

			SearchResult searchResult = client.collections("FRTRecords").documents().search(searchParameters);

			searchResult.getHits().forEach((hit) ->
				hits.add(new Gson().toJsonTree(hit.getDocument()).getAsJsonObject()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		ctx.json(!hits.isEmpty() ? hits.toString() : "[]");
	}

	@Override
	public void update(@NotNull Context ctx, @NotNull String s) {
		throw new NotImplementedResponse("not implemented");
	}

	@Override
	public void delete(@NotNull Context ctx, @NotNull String s) {
		throw new NotImplementedResponse("not implemented");
	}
}
