package dev.fauser.surveillance_transparency;

import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import io.javalin.http.NotImplementedResponse;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

public class DHS2025Controller implements CrudHandler {
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
		Dotenv env = Dotenv.load();
		ArrayList<JsonObject> hits = new ArrayList<>();
		try {
			Main.searchTest(s, hits, env.get("TYPESENSE_API_KEY"));
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
