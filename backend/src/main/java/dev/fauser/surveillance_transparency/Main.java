package dev.fauser.surveillance_transparency;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.fauser.surveillance_transparency.generated.db.tables.records.AiUseCaseInventory_2026Record;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.typesense.api.Client;
import org.typesense.api.Configuration;
import org.typesense.api.FieldTypes;
import org.typesense.model.CollectionSchema;
import org.typesense.model.Field;
import org.typesense.model.ImportDocumentsParameters;
import org.typesense.model.IndexAction;
import org.typesense.model.SearchParameters;
import org.typesense.model.SearchResult;
import org.typesense.resources.Node;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static dev.fauser.surveillance_transparency.generated.db.Tables.*;
import static io.javalin.apibuilder.ApiBuilder.*;

public class Main {
	public static void main(String[] args) throws Exception {
		System.setProperty("org.jooq.no-logo", "true");
		System.setProperty("org.jooq.no-tips", "true");
		System.setProperty("org.jooq.log.org.jooq.impl.DefaultExecuteContext.logVersionSupport", "ERROR");

		Dotenv env = Dotenv.load();

		CountryController crud = new CountryController();

		searchSetup(crud.countries, env.get("SEARCH_QUERY"), env.get("TYPESENSE_API_KEY"),
			env.get("POSTGRES_URL"), env.get("POSTGRES_USER"), env.get("POSTGRES_PASSWORD"));

		var app = Javalin.create(config -> {
			config.useVirtualThreads = true;
			config.http.asyncTimeout = 10_000L;
			config.router.apiBuilder(() -> {
				crud("country/{country-id}", crud);
			});
		});
		app.before(ctx -> {
			ctx.header("Access-Control-Allow-Origin", "*");
		}).start(7070);
	}

	static void searchSetup(ArrayList<JsonObject> results, String searchTerm, String apiKey, String url, String user, String password) throws Exception {
		List<Node> nodes = new ArrayList<>();

		nodes.add(new Node("http", "localhost", "8108"));

		Configuration configuration = new Configuration(nodes, Duration.ofSeconds(2), apiKey);

		Client client = new Client(configuration);

		List<Field> fields = new ArrayList<>();
		fields.add(new Field().name("id").type(FieldTypes.STRING));
		fields.add(new Field().name("name").type(FieldTypes.STRING));
		fields.add(new Field().name("bureau").type(FieldTypes.STRING));
		fields.add(new Field().name("email").type(FieldTypes.STRING));
		fields.add(new Field().name("stage_of_development").type(FieldTypes.STRING));
		fields.add(new Field().name("high_impact").type(FieldTypes.STRING));
		fields.add(new Field().name("justification").type(FieldTypes.STRING));

		CollectionSchema collectionSchema = new CollectionSchema();
		collectionSchema.name("AIUseCase2025").fields(fields)/*.defaultSortingField("high_impact")*/;

		boolean hasCollection = false;

		for (var i : client.collections().retrieve()) {
			if (i.getName().equals("AIUseCase2025")) {
				hasCollection = true;
				break;
			}
		}

		if (hasCollection) {
			client.collections("AIUseCase2025").delete();
		}

		client.collections().create(collectionSchema);

		try (Connection conn = DriverManager.getConnection("jdbc:" + url, user, password)) {
			DSLContext ctx = DSL.using(conn);

			Result<AiUseCaseInventory_2026Record> records = ctx.selectFrom(AI_USE_CASE_INVENTORY_2026).fetch();

			StringBuilder entries = new StringBuilder();
			for (AiUseCaseInventory_2026Record record : records) {
				entries.append("""
					{"id": "%s", "name": "%s", "bureau": "%s", "email": "%s", "stage_of_development": "%s", "high_impact": "%s", "justification": "%s"}
					""".formatted(
					record.getId(),
					record.getName(),
					record.getBureau(),
					record.getEmail(),
					record.getStageOfDevelopment(),
					record.getHighImpact(),
					record.getJustification()
				)).append("\n");
			}

			ImportDocumentsParameters queryParameters = new ImportDocumentsParameters();
			queryParameters.action(IndexAction.UPSERT);

			client.collections("AIUseCase2025").documents().import_(entries.toString(), queryParameters);
		}

		SearchParameters searchParameters = new SearchParameters()
			.q(searchTerm)
			.queryBy("name,bureau,justification");
		SearchResult searchResult = client.collections("AIUseCase2025").documents().search(searchParameters);

		searchResult.getHits().forEach((hit) ->
			results.add(new Gson().toJsonTree(hit.getDocument()).getAsJsonObject()));
	}

	static void searchTest(String searchTerm, ArrayList<JsonObject> results, String apiKey) throws Exception {
		List<Node> nodes = new ArrayList<>();

		nodes.add(new Node("http", "localhost", "8108"));

		Configuration configuration = new Configuration(nodes, Duration.ofSeconds(2), apiKey);

		Client client = new Client(configuration);

		SearchParameters searchParameters = new SearchParameters()
			.q(searchTerm)
			.queryBy("name,bureau,justification");
		SearchResult searchResult = client.collections("AIUseCase2025").documents().search(searchParameters);

		searchResult.getHits().forEach((hit) ->
			results.add(new Gson().toJsonTree(hit.getDocument()).getAsJsonObject()));
	}
}