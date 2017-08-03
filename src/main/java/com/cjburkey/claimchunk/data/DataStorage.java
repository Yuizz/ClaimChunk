package com.cjburkey.claimchunk.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DataStorage<T> {

	private final List<T> data = new ArrayList<>();
	private final Class<T[]> referenceClass;
	private File file;
	
	public DataStorage(Class<T[]> referenceClass, File file) {
		this.file = file;
		this.referenceClass = referenceClass;
		data.clear();
	}
	
	public void write() throws IOException {
		if (file == null) {
			return;
		}
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		if (file.exists()) {
			file.delete();
		}
		FileWriter writer = null;
		try {
			writer = new FileWriter(file);
			writer.write(getGson().toJson(data));
			writer.close();
		} catch (Exception e) {
			throw e;
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	
	public void read() throws IOException {
		if (file == null || !file.exists()) {
			return;
		}
		StringBuilder json = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = reader.readLine()) != null) {
				json.append(line);
				json.append('\n');
			}
			reader.close();
		} catch (IOException e) {
			throw e;
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		//List<T> out = getGson().fromJson(json.toString(), new TypeToken<List<T>>(){}.getType());
		T[] out = getGson().fromJson(json.toString(), referenceClass);
		if (out != null) {
			data.clear();
			for (T t : out) {
				data.add(t);
			}
		}
	}
	
	public void addObject(T obj) {
		data.add(obj);
	}
	
	public void emptyObjects() {
		data.clear();
	}
	
	public List<T> getObjects() {
		return new ArrayList<T>(data);
	}
	
	private Gson getGson() {
		return new GsonBuilder().setPrettyPrinting().serializeNulls().create();
	}
	
}