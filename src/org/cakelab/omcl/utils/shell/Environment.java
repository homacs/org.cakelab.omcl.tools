package org.cakelab.omcl.utils.shell;

import java.util.HashMap;
import java.util.Map;

import org.cakelab.json.JSONArray;
import org.cakelab.json.JSONObject;

public class Environment extends HashMap<String, String>{
	private static final long serialVersionUID = 1L;

	public Environment(Map<String, String> getenv) {
		this.putAll(getenv);
	}

	/**
	 * Enumerates recursively through the JSONObject and adds
	 * all members as name:value pair variables to this Environment 
	 * instance.
	 * 
	 * TODO: candidate for removal
	 * 
	 * @param json
	 */
	public void putAllJson(JSONObject json) {
		for (Map.Entry<String, Object> entry : json.entrySet()) {
			putJson(entry.getKey(), entry.getValue());
		}
	}

	private void putJson(String key, Object value) {
		if (value instanceof JSONObject) {
			JSONObject json = (JSONObject) value;
			for (Map.Entry<String, Object> entry : json.entrySet()) {
				putJson(key + "." + entry.getKey(), entry.getValue());
			}
		} else if (value instanceof JSONArray) {
			JSONArray json = (JSONArray) value;
			for (int i = 0; i < json.size(); i++) {
				value = json.get(i);
				putJson(key + "[" + i + "]", value);
			}
		} else {
			if (value != null) put(key, value.toString());
		}
	}
}
