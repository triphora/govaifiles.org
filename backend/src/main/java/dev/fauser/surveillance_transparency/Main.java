package dev.fauser.surveillance_transparency;

import com.google.gson.*;
import dev.fauser.surveillance_transparency.generated.db.tables.records.FrtRecordsRecord;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import org.jooq.DSLContext;
import org.jooq.JSONFormat;
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

		APIHandler crud = new APIHandler();

		searchSetup(crud.countries, env.get("TYPESENSE_API_KEY"),
			env.get("POSTGRES_URL"), env.get("POSTGRES_USER"), env.get("POSTGRES_PASSWORD"));

		var app = Javalin.create(config -> {
			config.useVirtualThreads = true;
			config.http.asyncTimeout = 10_000L;
			config.router.apiBuilder(() -> {
				crud("dhs_2025/{query}", crud);
			});
		});
		app.before(ctx -> {
			ctx.header("Access-Control-Allow-Origin", "*");
		}).start(7070);
	}

	static void searchSetup(ArrayList<JsonObject> results, String apiKey, String url, String user, String password) throws Exception {
		List<Node> nodes = new ArrayList<>();

		nodes.add(new Node("http", "localhost", "8108"));

		Configuration configuration = new Configuration(nodes, Duration.ofSeconds(2), apiKey);

		Client client = new Client(configuration);

		List<Field> fields = new ArrayList<>();
		fields.add(new Field().name(".*").type(FieldTypes.AUTO));

		CollectionSchema collectionSchema = new CollectionSchema();
		collectionSchema.name("FRTRecords").fields(fields);

		boolean hasCollection = false;

		for (var i : client.collections().retrieve()) {
			if (i.getName().equals("FRTRecords")) {
				hasCollection = true;
				break;
			}
		}

		if (hasCollection) {
			client.collections("FRTRecords").delete();
		}

		client.collections().create(collectionSchema);

		try (Connection conn = DriverManager.getConnection("jdbc:" + url, user, password)) {
			DSLContext ctx = DSL.using(conn);

			Result<FrtRecordsRecord> records = ctx.selectFrom(FRT_RECORDS).fetch();

			JSONFormat jsonFormat = new JSONFormat().header(false).recordFormat(JSONFormat.RecordFormat.OBJECT);

			JsonArray recordsArray = JsonParser.parseString(records.formatJSON(jsonFormat)).getAsJsonArray();

			StringBuilder sb = new StringBuilder();

			for (JsonElement element : recordsArray) {
				sb.append(element.toString()).append("\n");
			}

			ImportDocumentsParameters queryParameters = new ImportDocumentsParameters();
			queryParameters.action(IndexAction.UPSERT);

			client.collections("FRTRecords").documents().import_(sb.toString(), queryParameters);
		}

		SearchParameters searchParameters = new SearchParameters().q("*").limit(250);
		SearchResult searchResult = client.collections("FRTRecords").documents().search(searchParameters);

		searchResult.getHits().forEach((hit) ->
			results.add(new Gson().toJsonTree(hit.getDocument()).getAsJsonObject()));
	}
}