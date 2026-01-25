package dev.fauser.surveillance_transparency;

import dev.fauser.surveillance_transparency.generated.db.tables.records.TestRecord;
import org.apache.commons.cli.*;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.typesense.api.*;
import org.typesense.model.*;
import org.typesense.resources.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static dev.fauser.surveillance_transparency.generated.db.tables.Test.TEST;

public class Main {
	public static void main(String[] args) throws Exception {
		System.setProperty("org.jooq.no-logo", "true");
		System.setProperty("org.jooq.no-tips", "true");


		Options options = new Options();

		// TODO use .env
		Option query = Option.builder("q").longOpt("query").required().hasArg().get();
		Option apiKey = Option.builder("k").longOpt("key").required().hasArg().get();

		options.addOption(query);
		options.addOption(apiKey);

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);

		initSearch(cmd.getOptionValue(query), cmd.getOptionValue(apiKey));

		testDatabase();
	}

	private static void initSearch(String searchTerm, String apiKey) throws Exception {
		List<Node> nodes = new ArrayList<>();

		nodes.add(new Node("http", "localhost", "8108"));

		Configuration configuration = new Configuration(nodes, Duration.ofSeconds(2), apiKey);

		Client client = new Client(configuration);


		boolean hasCountries = false;

		for (var i : client.collections().retrieve()) {
			if (i.getName().equals("Countries")) {
				hasCountries = true;
				break;
			}
		}

		if (!hasCountries) {
			List<Field> fields = new ArrayList<>();
			fields.add(new Field().name("countryName").type(FieldTypes.STRING));
			fields.add(new Field().name("capital").type(FieldTypes.STRING));
			fields.add(new Field().name("gdp").type(FieldTypes.INT32).facet(true).sort(true));

			CollectionSchema collectionSchema = new CollectionSchema();
			collectionSchema.name("Countries").fields(fields).defaultSortingField("gdp");

			client.collections().create(collectionSchema);

			String countries = """
				{"countryName": "Japan", "capital": "Tokyo", "gdp": 100}
				{"countryName": "Canada", "capital": "Ottawa", "gdp": 200}
				{"countryName": "United States of America", "capital": "Washington, D.C.", "gdp": 300}
				""";

			ImportDocumentsParameters queryParameters = new ImportDocumentsParameters();
			queryParameters.action(IndexAction.CREATE);

			client.collections("Countries").documents().import_(countries, queryParameters);
		}

		SearchParameters searchParameters = new SearchParameters()
			.q(searchTerm)
			.queryBy("countryName,capital")
			.prefix("true,false");
		SearchResult searchResult = client.collections("Countries").documents().search(searchParameters);

		searchResult.getHits().forEach((hit) -> {
			Log.info(hit.getDocument().toString());
		});
	}

	private static void testDatabase() throws Exception {
		// TODO use .env
		String url = "jdbc:postgresql://localhost:5432/postgres";
		String user = "admin";
		String password = "uhSIDUFSDUHFijjfksd";

		try (Connection conn = DriverManager.getConnection(url, user, password)) {
			DSLContext ctx = DSL.using(conn);

			Result<TestRecord> result = ctx.selectFrom(TEST).fetch();

			Log.info("\n" + result.format());
		}
	}
}