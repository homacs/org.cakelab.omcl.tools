package org.cakelab.omcl.utils.json;

import java.io.File;
import java.io.IOException;

import org.cakelab.json.codec.JSONCodecException;

public class JsonSaveTask {
	private File file;
	private JsonSaveable item;
	public JsonSaveTask(JsonSaveable item, File file) {
		this.item = item;
		this.file = file;
	}
	public void save() throws IOException, JSONCodecException {
		File dir = file.getParentFile();
		if (!dir.exists()) dir.mkdirs();
		item.save(file);
	}
}
