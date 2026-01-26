package dev.fauser.surveillance_transparency;

import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

public class CountryController implements CrudHandler {
	public final ArrayList<JsonObject> countries = new ArrayList<>();

	@Override
	public void create(@NotNull Context ctx) {
		ctx.json("""
			{"hello":"world"}
			""");
	}

	@Override
	public void getAll(@NotNull Context ctx) {
		ctx.json(Arrays.toString(countries.toArray()));
	}

	@Override
	public void getOne(@NotNull Context ctx, @NotNull String s) {
		Dotenv env = Dotenv.load();
		ArrayList<JsonObject> hits = new ArrayList<>();
		try {
			Main.testSearch(hits, s, env.get("TYPESENSE_API_KEY"),
				env.get("POSTGRES_URL"), env.get("POSTGRES_USER"), env.get("POSTGRES_PASSWORD"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		ctx.json(!hits.isEmpty() ? hits.getFirst().toString() : "[]");
	}

	@Override
	public void update(@NotNull Context ctx, @NotNull String s) {
		ctx.json("""
			{"hello":"world"}
			""");
	}

	@Override
	public void delete(@NotNull Context ctx, @NotNull String s) {
		ctx.json("""
			{"hello":"world"}
			""");
	}
}
